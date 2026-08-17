# -*- coding: utf-8 -*-
"""行为面对账 · 网络包面第 5 项：C2S handler 的权限/归属校验

契约：一个 C2S 包被处理前，服务端有没有验证「发包的这个玩家有资格动这个目标」？
判据不按方法名写（那会被改名绕过），按**成因**写：handler 体内是否出现
「把 sender 与目标关联起来」的判定。

对差方式：同一个线上 id，基准有守卫而本树没有 = 移植回归；反之 = 我们更严。
活性自报与结论正交：解析出几个 handler、handler 体平均多少字符。
"""
import re
import subprocess

import os as _os
# 仓库根 = 本文件所在目录向上三级（docs/tools/o8/x.py → 仓库根）
REPO = _os.path.dirname(_os.path.dirname(_os.path.dirname(
    _os.path.dirname(_os.path.abspath(__file__)))))
REFS = {"HEAD": "本树", "port/1.21.11-fabric": "基准"}

RE_TYPE_OPEN = re.compile(r"new\s+(?:CustomPacketPayload\.)?Type\s*<[^>]*>\s*\(")
RE_STR = re.compile(r'"([^"\\]*)"')

# 守卫「成因」词表：任何把 sender 与目标绑起来 / 检查权限 / 检查距离的判定
GUARD_PATTERNS = {
    "归属": r"isOwnedBy|getOwnerUUID|getOwner\s*\(\s*\)|ownerUUID|isMaidOwner|sameOwner",
    "距离": r"closerThan|distanceTo|distanceToSqr|canInteractWith",
    "权限": r"hasPermissions|hasPermission|canEditSite|isCreative|GameModeUtil|permissionLevel|isOp\b",
    "容器": r"containerMenu\s+instanceof|getContainerMenu\s*\(\s*\)\s*instanceof|menu\s+instanceof",
    "存活/类型": r"instanceof\s+EntityMaid|instanceof\s+EntityChair|getEntity\s*\(",
}
RE_GUARDS = {k: re.compile(v) for k, v in GUARD_PATTERNS.items()}


def git(*a):
    r = subprocess.run(["git", "-C", REPO] + list(a), capture_output=True,
                       text=True, encoding="utf-8", errors="replace")
    r.check_returncode()
    return r.stdout


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


def method_body(src, name):
    """按大括号配对截出方法体——不截就等于在整份文件里找，
    那正是「契约测试用 indexOf 在整份文件里找调用点」那条教训的形态。"""
    bodies = []
    for m in re.finditer(r"\b(?:static\s+)?(?:void|<[^>]*>\s*void)\s+%s\s*\(" % re.escape(name), src):
        # 找到参数表的右括号，再找方法体的 {
        i = src.index("(", m.start())
        d = 0
        k = i
        for k in range(i, len(src)):
            if src[k] == '(':
                d += 1
            elif src[k] == ')':
                d -= 1
                if d == 0:
                    break
        b = src.find("{", k)
        if b < 0:
            continue
        d = 0
        for j in range(b, len(src)):
            if src[j] == '{':
                d += 1
            elif src[j] == '}':
                d -= 1
                if d == 0:
                    bodies.append(src[b:j + 1])
                    break
    return bodies


def strip_comments(s):
    s = re.sub(r"/\*.*?\*/", " ", s, flags=re.S)
    s = re.sub(r"//[^\n]*", " ", s)
    return s


def scan(ref):
    files = [p for p in git("ls-tree", "-r", "--name-only", ref).splitlines() if p.endswith(".java")]
    nh = [p for p in files if p.endswith("network/NetworkHandler.java")]
    nhsrc = git("show", "%s:%s" % (ref, nh[0])) if nh else ""
    c2s_classes = set(re.findall(r"registerC2SPacket\s*\(\s*(\w+)\.", nhsrc))

    out = {}
    parsed = 0
    total_len = 0
    for path in files:
        cls = path.rsplit("/", 1)[-1][:-5]
        if cls not in c2s_classes:
            continue
        src = git("show", "%s:%s" % (ref, path))
        wid = payload_id(src)
        if not wid:
            continue
        # 按「成因」取范围，不按方法名猜：payload 类是单一用途的，
        # 守卫可能落在任意私有 helper 里（实证 SetAttackListPackage 的守卫在 writeList），
        # 故取整份文件、剥注释与 import 行。
        body = strip_comments(src)
        body = "\n".join(l for l in body.splitlines() if not l.lstrip().startswith("import "))
        parsed += 1
        total_len += len(body)
        found = sorted(k for k, rx in RE_GUARDS.items() if rx.search(body))
        out[wid] = (cls, found, len(body))
    return out, dict(c2s=len(c2s_classes), parsed=parsed,
                     avg=(total_len // parsed if parsed else 0))


def main():
    data = {}
    for ref, label in REFS.items():
        d, live = scan(ref)
        data[ref] = d
        print("[活性] %-22s C2S 注册 %d · 解析出 handler %d · handler 体均长 %d 字符"
              % (label, live["c2s"], live["parsed"], live["avg"]))
        empty = [w for w, (c, f, ln) in d.items() if ln == 0]
        if empty:
            print("        ⛔ handler 体截取为空（解析器盲区）：%s" % ", ".join(sorted(empty)))
    print()

    head, base = data["HEAD"], data["port/1.21.11-fabric"]
    common = sorted(set(head) & set(base))
    print("=== C2S 守卫对差（共有 id %d）===" % len(common))
    regress, stricter = [], []
    for w in common:
        hc, hf, _ = head[w]
        bc, bf, _ = base[w]
        if set(bf) - set(hf):
            regress.append((w, hc, sorted(set(bf) - set(hf)), hf, bf))
        if set(hf) - set(bf):
            stricter.append((w, hc, sorted(set(hf) - set(bf))))

    print("\n-- ⚠ 基准有守卫、本树没有（候选移植回归 %d）--" % len(regress))
    for w, c, miss, hf, bf in regress:
        print("   %-30s %-32s 少：%-18s 本树=%s 基准=%s" % (w, c, ",".join(miss), hf or "无", bf or "无"))

    print("\n-- 本树更严（%d，仅供核对，不是问题）--" % len(stricter))
    for w, c, extra in stricter:
        print("   %-30s %-32s 多：%s" % (w, c, ",".join(extra)))

    print("\n-- 两树皆无任何守卫（%d）：需人工判断该不该有 --" % 0)
    none_both = [(w, head[w][0]) for w in common if not head[w][1] and not base[w][1]]
    for w, c in none_both:
        print("   %-30s %s" % (w, c))
    print("   小计 %d" % len(none_both))

    only_head = sorted(set(head) - set(base))
    if only_head:
        print("\n-- 仅本树有的 C2S（%d）：无基准可比，需人工判断 --" % len(only_head))
        for w in only_head:
            print("   %-30s %-32s 守卫=%s" % (w, head[w][0], ",".join(head[w][1]) or "无"))


if __name__ == "__main__":
    main()
