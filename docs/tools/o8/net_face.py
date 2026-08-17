# -*- coding: utf-8 -*-
"""行为面对账 · 网络包面（O8 第二面）

契约（「谁在什么条件下会看到什么」）分五项，本脚本机械产出前三项：
  1. payload 线上 id      —— 存在性
  2. 方向                 —— C2S / S2C
  3. 注册完整性           —— 两侧各自的 PayloadTypeRegistry + receiver 是否齐全
第 4/5 项（收件人策略、handler 权限校验）需逐个读，由本脚本先圈出范围。

活性自报与结论正交：扫了几个 ref、几个文件、认出几个 id、几处注册调用。
"""
import re
import subprocess
import sys
from collections import defaultdict

import os as _os
# 仓库根 = 本文件所在目录向上三级（docs/tools/o8/x.py → 仓库根）
REPO = _os.path.dirname(_os.path.dirname(_os.path.dirname(
    _os.path.dirname(_os.path.abspath(__file__)))))
REFS = ["HEAD", "port/1.21.11-fabric", "origin/26.1"]

# id 声明：不依赖 helper 名（本树 modLoc / 基准 getResourceLocation / 两者
# 又都可能写 Identifier.fromNamespaceAndPath(常量, "path")）——按 helper 名写正则
# 是树间不对称的，会同时造出假的「仅本树有」和假的「基准没有」。
# 做法：定位 `new …Type<…>(`，括号配对截出调用跨度，取跨度内最后一个字符串字面量。
RE_TYPE_OPEN = re.compile(r"new\s+(?:CustomPacketPayload\.)?Type\s*<[^>]*>\s*\(")
RE_STR = re.compile(r'"([^"\\]*)"')


def extract_payload_id(src):
    """返回 (wire_id, 原始跨度) 或 (None, None)"""
    m = RE_TYPE_OPEN.search(src)
    if not m:
        return None, None
    i = m.end() - 1  # 指向 '('
    depth = 0
    for j in range(i, len(src)):
        c = src[j]
        if c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                span = src[i:j + 1]
                lits = RE_STR.findall(span)
                # fromNamespaceAndPath("ns","path") 取后者；modLoc("path") 只有一个
                return (lits[-1] if lits else None), span
    return None, None

RE_REG_S2C_TYPE = re.compile(r"PayloadTypeRegistry\.clientboundPlay\(\)\.register\s*\(\s*(\w+)\.")
RE_REG_C2S_TYPE = re.compile(r"PayloadTypeRegistry\.serverboundPlay\(\)\.register\s*\(\s*(\w+)\.")
RE_CLIENT_RECV = re.compile(r"ClientPlayNetworking\.registerGlobalReceiver\s*\(\s*(\w+)\.")
RE_SERVER_RECV = re.compile(r"ServerPlayNetworking\.registerGlobalReceiver\s*\(\s*(\w+)\.")
# 本仓库把两者包进了 helper，故也认 helper 调用
RE_HELPER_S2C = re.compile(r"registerS2CPacket\s*\(\s*(\w+)\.")
RE_HELPER_C2S = re.compile(r"registerC2SPacket\s*\(\s*(\w+)\.")


def git(*args):
    r = subprocess.run(["git", "-C", REPO] + list(args),
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    if r.returncode != 0:
        raise RuntimeError("git %s -> rc=%d\n%s" % (" ".join(args), r.returncode, r.stderr))
    return r.stdout


def list_files(ref):
    out = git("ls-tree", "-r", "--name-only", ref)
    return [p for p in out.splitlines() if p.endswith(".java")]


def read(ref, path):
    return git("show", "%s:%s" % (ref, path))


def scan_ref(ref):
    """返回 (payloads, regs, liveness)"""
    files = list_files(ref)
    # payload 候选：任何声明了 CustomPacketPayload.Type 的 java 文件（不限目录——
    # 按目录划范围正是「扫描面按功能归属划」那条教训的形态）
    payloads = {}          # simple class name -> wire id
    id_owner = {}          # wire id -> [class names]
    unparsed = []          # 自称 payload 却抽不出 id —— 解析器的已知盲区，必须显式报出
    scanned = 0
    decl_hits = 0
    for path in files:
        src = read(ref, path)
        if "CustomPacketPayload" not in src:
            continue
        scanned += 1
        cls = path.rsplit("/", 1)[-1][:-5]
        wid, _span = extract_payload_id(src)
        if wid:
            decl_hits += 1
            payloads[cls] = wid
            id_owner.setdefault(wid, []).append(cls)
        elif "implements CustomPacketPayload" in src:
            # 声明了自己是 payload 却抽不出 id —— 必须显式暴露，不许静默跳过
            unparsed.append((path, cls))

    # 注册面：只在注册中枢里找，但中枢文件本身要机械定位（不写死路径）
    regs = defaultdict(set)
    reg_calls = 0
    for path in files:
        if not path.endswith(".java"):
            continue
        low = path.lower()
        if "network" not in low and "init" not in low:
            continue
        src = read(ref, path)
        for rx, tag in ((RE_REG_S2C_TYPE, "S2C_TYPE"), (RE_REG_C2S_TYPE, "C2S_TYPE"),
                        (RE_CLIENT_RECV, "CLIENT_RECV"), (RE_SERVER_RECV, "SERVER_RECV"),
                        (RE_HELPER_S2C, "S2C_HELPER"), (RE_HELPER_C2S, "C2S_HELPER")):
            for m in rx.finditer(src):
                regs[m.group(1)].add(tag)
                reg_calls += 1

    live = dict(files=len(files), payload_files=scanned, ids=decl_hits,
                reg_calls=reg_calls, reg_classes=len(regs), unparsed=unparsed)
    return payloads, regs, live, id_owner


def direction(tags):
    s2c = bool(tags & {"S2C_TYPE", "S2C_HELPER", "CLIENT_RECV"})
    c2s = bool(tags & {"C2S_TYPE", "C2S_HELPER", "SERVER_RECV"})
    if s2c and c2s:
        return "BOTH"
    if s2c:
        return "S2C"
    if c2s:
        return "C2S"
    return "UNREG"


def completeness(tags):
    """S2C 要 clientbound 注册 + client receiver；C2S 要 serverbound 注册 + server receiver"""
    d = direction(tags)
    missing = []
    if d in ("S2C", "BOTH"):
        if not (tags & {"S2C_TYPE", "S2C_HELPER"}):
            missing.append("缺 clientboundPlay 注册")
        if not (tags & {"CLIENT_RECV"}):
            missing.append("缺 client receiver")
    if d in ("C2S", "BOTH"):
        if not (tags & {"C2S_TYPE", "C2S_HELPER"}):
            missing.append("缺 serverboundPlay 注册")
        if not (tags & {"SERVER_RECV", "C2S_HELPER"}):
            missing.append("缺 server receiver")
    return missing


def main():
    data = {}
    for ref in REFS:
        p, r, live, owner = scan_ref(ref)
        data[ref] = (p, r, live, owner)
        print("[活性] %-22s 文件 %d · 含 CustomPacketPayload %d · 认出 id %d · 注册调用 %d · 被注册类 %d"
              % (ref, live["files"], live["payload_files"], live["ids"],
                 live["reg_calls"], live["reg_classes"]))
        dup = {k: v for k, v in owner.items() if len(v) > 1}
        if dup:
            print("        ⚠ 同一 id 多个类：%s" % dup)
        if live["unparsed"]:
            print("        ⛔ 自称 payload 却抽不出 id（解析器盲区，%d 个）：" % len(live["unparsed"]))
            for p, c in live["unparsed"]:
                print("            %s" % p)
    print()

    head_p, head_r = data["HEAD"][0], data["HEAD"][1]
    base_p, base_r = data["port/1.21.11-fabric"][0], data["port/1.21.11-fabric"][1]
    host_p = data["origin/26.1"][0]

    head_ids = {v: k for k, v in head_p.items()}
    base_ids = {v: k for k, v in base_p.items()}
    host_ids = {v: k for k, v in host_p.items()}

    only_base = sorted(set(base_ids) - set(head_ids))
    only_head = sorted(set(head_ids) - set(base_ids))
    both = sorted(set(head_ids) & set(base_ids))

    print("=== 1. 线上 id 存在性（本树 %d · 基准 %d · 宿主 %d）===" % (len(head_ids), len(base_ids), len(host_ids)))
    print("\n-- 仅基准有（%d）：本树少了这些线上 id --" % len(only_base))
    for i in only_base:
        print("   %-34s %-34s 宿主有？%s" % (i, base_ids[i], "是" if i in host_ids else "否"))
    print("\n-- 仅本树有（%d）--" % len(only_head))
    for i in only_head:
        print("   %-34s %-34s 宿主有？%s" % (i, head_ids[i], "是" if i in host_ids else "否"))

    print("\n=== 2. 共有 id 的方向对账（%d）===" % len(both))
    diffs = 0
    for i in both:
        hd = direction(head_r.get(head_ids[i], set()))
        bd = direction(base_r.get(base_ids[i], set()))
        if hd != bd:
            diffs += 1
            print("   ⚠ %-32s 本树=%-6s 基准=%-6s  (%s / %s)" % (i, hd, bd, head_ids[i], base_ids[i]))
    print("   方向不同：%d" % diffs)

    print("\n=== 3. 本树注册完整性（对每个 payload 类）===")
    bad = 0
    for cls, wid in sorted(head_p.items(), key=lambda kv: kv[1]):
        tags = head_r.get(cls, set())
        if not tags:
            print("   ⚠ %-34s %-30s 未出现在任何注册调用里" % (wid, cls))
            bad += 1
            continue
        miss = completeness(tags)
        if miss:
            print("   ⚠ %-34s %-30s %s" % (wid, cls, "，".join(miss)))
            bad += 1
    print("   本树注册面异常：%d" % bad)

    print("\n=== 3b. 基准注册完整性（同法，用于分辨「本树独有的洞」与「两边都这样」）===")
    bbad = 0
    for cls, wid in sorted(base_p.items(), key=lambda kv: kv[1]):
        tags = base_r.get(cls, set())
        if not tags:
            print("   ⚠ %-34s %-30s 未出现在任何注册调用里" % (wid, cls))
            bbad += 1
            continue
        miss = completeness(tags)
        if miss:
            print("   ⚠ %-34s %-30s %s" % (wid, cls, "，".join(miss)))
            bbad += 1
    print("   基准注册面异常：%d" % bbad)


if __name__ == "__main__":
    main()
