# -*- coding: utf-8 -*-
"""行为面对账 · 网络包面第 4 项：S2C 收件人策略

契约：每个 S2C 包，服务端发给谁？
  单人 / 追踪该实体者 / 追踪者+自己 / 半径内 / 全服
发窄了别的玩家看不到效果，发宽了是信息泄露——两者都是玩家可观察差异。

判据按「发送动词」而非包名，扫全仓（发送点常在业务代码里，不在 network 包内——
按目录划范围正是「扫描面要按谁可能犯这个错划」那条教训的形态）。
活性自报与结论正交：扫了几个文件、抓到几处发送点、认出几个包有发送点。
"""
import re
import subprocess
from collections import defaultdict

import os as _os
# 仓库根 = 本文件所在目录向上三级（docs/tools/o8/x.py → 仓库根）
REPO = _os.path.dirname(_os.path.dirname(_os.path.dirname(
    _os.path.dirname(_os.path.abspath(__file__)))))
REFS = {"HEAD": "本树", "port/1.21.11-fabric": "基准"}

# 发送动词 -> 收件人语义
SEND_KINDS = [
    (re.compile(r"NetworkHandler\.sendToClientPlayer\s*\("), "单人"),
    (re.compile(r"NetworkHandler\.sendToNearby\s*\([^;]*?,[^;]*?,\s*\d"), "半径内"),
    (re.compile(r"NetworkHandler\.sendToNearby\s*\("), "追踪者+自己"),
    (re.compile(r"PacketDistributor\.sendToPlayersTrackingEntityAndSelf\s*\("), "追踪者+自己"),
    (re.compile(r"PacketDistributor\.sendToPlayersTrackingEntity\s*\("), "追踪者"),
    (re.compile(r"PacketDistributor\.sendToPlayer\s*\("), "单人"),
    (re.compile(r"PacketDistributor\.sendToAllPlayers\s*\("), "全服"),
    (re.compile(r"ServerPlayNetworking\.send\s*\("), "单人"),
    (re.compile(r"PlayerLookup\.around\s*\("), "半径内"),
    (re.compile(r"PlayerLookup\.tracking\s*\("), "追踪者"),
    (re.compile(r"PlayerLookup\.all\s*\("), "全服"),
]

RE_TYPE_OPEN = re.compile(r"new\s+(?:CustomPacketPayload\.)?Type\s*<[^>]*>\s*\(")
RE_STR = re.compile(r'"([^"\\]*)"')


def git(*a):
    r = subprocess.run(["git", "-C", REPO] + list(a), capture_output=True,
                       text=True, encoding="utf-8", errors="replace")
    r.check_returncode()
    return r.stdout


def strip_comments(s):
    s = re.sub(r"/\*.*?\*/", " ", s, flags=re.S)
    return re.sub(r"//[^\n]*", " ", s)


def split_blocks(src):
    """把源码切成「方法体」粒度的块：从类体内深度 1 的 '{' 到其配对 '}'。
    发送点与 payload 构造几乎总在同一方法里，故这是配对的正确粒度。"""
    blocks = []
    depth = 0
    start = None
    for i, c in enumerate(src):
        if c == '{':
            depth += 1
            if depth == 2 and start is None:   # 1=类体, 2=方法体
                start = i
        elif c == '}':
            if depth == 2 and start is not None:
                blocks.append(src[start:i + 1])
                start = None
            depth -= 1
    if not blocks:          # 没有类体嵌套时退化为整份文件
        blocks = [src]
    return blocks


def payload_id(src):
    m = RE_TYPE_OPEN.search(src)
    if not m:
        return None
    i = m.end() - 1
    d = 0
    for j in range(i, len(src)):
        if src[j] == '(':
            d += 1
        elif src[j] == ')':
            d -= 1
            if d == 0:
                lits = RE_STR.findall(src[i:j + 1])
                return lits[-1] if lits else None
    return None


def scan(ref):
    files = [p for p in git("ls-tree", "-r", "--name-only", ref).splitlines() if p.endswith(".java")]
    # 类名 -> 线上 id
    cls2id = {}
    for p in files:
        if "/network/message/" not in p:
            continue
        src = git("show", "%s:%s" % (ref, p))
        wid = payload_id(src)
        if wid:
            cls2id[p.rsplit("/", 1)[-1][:-5]] = wid

    # 发送点：一行里同时出现「发送动词」与某个 payload 类名
    recips = defaultdict(set)
    sites = 0
    scanned = 0
    for p in files:
        src = strip_comments(git("show", "%s:%s" % (ref, p)))
        if not any(rx.search(src) for rx, _ in SEND_KINDS):
            continue
        scanned += 1
        # ⚠ 曾按 `;` 断句配对，实证漏掉「先赋值给局部变量、再发送」这一普遍写法
        #   （ItemBreakPackage / SyncBaublePackage 等全漏）。改按**方法体**配对：
        #   以顶层大括号块切分，发送动词与包名同处一块即算配对。
        for stmt in split_blocks(src):
            kind = None
            for rx, k in SEND_KINDS:
                if rx.search(stmt):
                    kind = k
                    break
            if not kind:
                continue
            for cls, wid in cls2id.items():
                if re.search(r"\b%s\b" % re.escape(cls), stmt):
                    recips[wid].add(kind)
                    sites += 1
    return recips, cls2id, dict(files=len(files), sender_files=scanned,
                                sites=sites, with_send=len(recips))


def main():
    data = {}
    for ref, label in REFS.items():
        r, c, live = scan(ref)
        data[ref] = (r, c)
        print("[活性] %-6s 文件 %d · 含发送动词的文件 %d · 配对上的发送点 %d · 有发送点的包 %d / 共 %d"
              % (label, live["files"], live["sender_files"], live["sites"],
                 live["with_send"], len(c)))
    print()

    (hr, hc), (br, bc) = data["HEAD"], data["port/1.21.11-fabric"]
    common = sorted(set(hr) & set(br))
    print("=== 收件人策略对差（两树都有发送点的包 %d）===" % len(common))
    diff = 0
    for w in sorted(common):
        if hr[w] != br[w]:
            diff += 1
            print("   ⚠ %-30s 本树=%-22s 基准=%s"
                  % (w, "/".join(sorted(hr[w])), "/".join(sorted(br[w]))))
    print("   策略不同：%d" % diff)

    only_b = sorted(set(br) - set(hr))
    only_h = sorted(set(hr) - set(br))
    print("\n-- 基准有发送点、本树无（%d）--" % len(only_b))
    for w in only_b:
        print("   %-30s 基准=%s   本树是否还有这个包？%s"
              % (w, "/".join(sorted(br[w])), "有" if w in hc.values() else "无"))
    print("\n-- 本树有发送点、基准无（%d）--" % len(only_h))
    for w in only_h:
        print("   %-30s 本树=%s" % (w, "/".join(sorted(hr[w]))))

    # 本树有包但一个发送点都没配对上 —— 可能是死包，也可能是我的配对漏了
    nosend = sorted(set(hc.values()) - set(hr))
    print("\n-- ⛔ 本树有此包却没配对到任何发送点（%d）：死包 或 配对盲区，需人工判 --" % len(nosend))
    for w in nosend:
        print("   %s" % w)


if __name__ == "__main__":
    main()
