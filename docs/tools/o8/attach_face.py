# -*- coding: utf-8 -*-
"""行为面对账 · 存档与 attachment 面（O8 第三面）

契约（「谁在什么条件下会看到什么 / 什么活过重启」）：
  1. attachment id            —— 存在性
  2. persistent(codec)        —— 活不活过重启（不持久化 = 重载即丢）
  3. syncWith(predicate)      —— 客户端读不读得到（不同步 = 专服上空，单人档测不出）
  4. copyOnDeath              —— 玩家死亡后留不留
  5. initializer              —— 缺省值语义

判据按括号配对截出 `AttachmentRegistry.create*(...)` 的整个调用跨度再找链式方法，
不按行匹配——链式写法跨行是常态（§7.9 的经验：正则不覆盖链式写法就等于扫不到）。
活性自报与结论正交：扫了几个文件、抓到几个注册点、每个注册点跨度多长。
"""
import re
import subprocess

import os as _os
# 仓库根 = 本文件所在目录向上三级（docs/tools/o8/x.py → 仓库根）
REPO = _os.path.dirname(_os.path.dirname(_os.path.dirname(
    _os.path.dirname(_os.path.abspath(__file__)))))
REFS = {"HEAD": "本树", "port/1.21.11-fabric": "基准", "origin/26.1": "宿主"}

RE_CREATE = re.compile(r"AttachmentRegistry\s*\.\s*(create\w*)\s*\(")
RE_STR = re.compile(r'"([^"\\]*)"')

TRAITS = {
    "persistent": re.compile(r"\.persistent\s*\("),
    "syncWith": re.compile(r"\.syncWith\s*\("),
    "copyOnDeath": re.compile(r"\.copyOnDeath\s*\("),
    "initializer": re.compile(r"\.initializer\s*\("),
}
RE_SYNC_PRED = re.compile(r"AttachmentSyncPredicate\s*\.\s*(\w+)")


def git(*a):
    r = subprocess.run(["git", "-C", REPO] + list(a), capture_output=True,
                       text=True, encoding="utf-8", errors="replace")
    r.check_returncode()
    return r.stdout


def strip_comments(s):
    s = re.sub(r"/\*.*?\*/", " ", s, flags=re.S)
    return re.sub(r"//[^\n]*", " ", s)


def span_from(src, open_paren_idx):
    d = 0
    for j in range(open_paren_idx, len(src)):
        if src[j] == '(':
            d += 1
        elif src[j] == ')':
            d -= 1
            if d == 0:
                return src[open_paren_idx:j + 1]
    return src[open_paren_idx:]


def scan(ref):
    files = [p for p in git("ls-tree", "-r", "--name-only", ref).splitlines() if p.endswith(".java")]
    found = {}
    sites = 0
    scanned = 0
    spans_len = 0
    unnamed = []
    for p in files:
        raw = git("show", "%s:%s" % (ref, p))
        if "AttachmentRegistry" not in raw:
            continue
        scanned += 1
        src = strip_comments(raw)
        for m in RE_CREATE.finditer(src):
            i = src.index("(", m.start())
            span = span_from(src, i)
            sites += 1
            spans_len += len(span)
            lits = RE_STR.findall(span)
            if not lits:
                unnamed.append((p, m.group(1)))
                continue
            aid = lits[0]
            traits = sorted(k for k, rx in TRAITS.items() if rx.search(span))
            pred = RE_SYNC_PRED.search(span)
            found[aid] = dict(file=p, factory=m.group(1), traits=traits,
                              pred=pred.group(1) if pred else None)
    live = dict(files=len(files), att_files=scanned, sites=sites,
                avg_span=(spans_len // sites if sites else 0), unnamed=unnamed)
    return found, live


def main():
    data = {}
    for ref, label in REFS.items():
        d, live = scan(ref)
        data[ref] = d
        print("[活性] %-4s 文件 %d · 含 AttachmentRegistry 的文件 %d · 注册点 %d · 跨度均长 %d 字符 · 认出 id %d"
              % (label, live["files"], live["att_files"], live["sites"], live["avg_span"], len(d)))
        if live["unnamed"]:
            print("        ⛔ 注册点抽不出 id（解析器盲区）：%s" % live["unnamed"])
    print()

    head, base, host = data["HEAD"], data["port/1.21.11-fabric"], data["origin/26.1"]
    allids = sorted(set(head) | set(base) | set(host))

    print("=== 逐 id 对照（本树 / 基准 / 宿主）===")
    print("%-34s %-26s %-26s %-26s" % ("id", "本树", "基准", "宿主"))
    for a in allids:
        def fmt(d):
            if a not in d:
                return "—"
            t = d[a]
            s = "+".join(x[:4] for x in t["traits"]) or "裸"
            if t["pred"]:
                s += "(%s)" % t["pred"]
            return s
        print("%-34s %-26s %-26s %-26s" % (a, fmt(head), fmt(base), fmt(host)))

    print("\n=== ⚠ 本树 vs 基准：能力缺失（基准有该能力、本树没有）===")
    reg = 0
    for a in sorted(set(head) & set(base)):
        miss = set(base[a]["traits"]) - set(head[a]["traits"])
        if miss:
            reg += 1
            print("   %-32s 少：%-24s 本树=%s 基准=%s"
                  % (a, ",".join(sorted(miss)), head[a]["traits"], base[a]["traits"]))
        if base[a]["pred"] != head[a]["pred"]:
            print("   %-32s 同步范围不同：本树=%s 基准=%s" % (a, head[a]["pred"], base[a]["pred"]))
    print("   能力缺失：%d" % reg)

    print("\n=== 仅基准有（%d）===" % len(set(base) - set(head)))
    for a in sorted(set(base) - set(head)):
        print("   %-32s %s  宿主有？%s" % (a, base[a]["traits"], "是" if a in host else "否"))
    print("\n=== 仅本树有（%d）===" % len(set(head) - set(base)))
    for a in sorted(set(head) - set(base)):
        print("   %-32s %s  宿主有？%s" % (a, head[a]["traits"], "是" if a in host else "否"))


if __name__ == "__main__":
    main()
