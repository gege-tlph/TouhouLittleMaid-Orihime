# -*- coding: utf-8 -*-
"""行为面对账 · 资源与注册面（O8 第四面）上半：资源文件

契约：这个构建生成/携带哪些资源文件？少一个 = 少一块内容（贴图缺失、配方没有、标签不生效）。

⚠ 26.1.2 改过资源目录布局（models/item → items 等），按完整路径对差全是噪声。
故判据用「基名 + 顶层类别」：基准有而本树**任何位置都没有同名文件** 才算缺。
类别用于把 lang / 模型 / 贴图 / 数据分开读。

活性自报与结论正交：每棵树各类别多少文件、基名去重后多少。
"""
import subprocess
from collections import defaultdict

import os as _os
# 仓库根 = 本文件所在目录向上三级（docs/tools/o8/x.py → 仓库根）
REPO = _os.path.dirname(_os.path.dirname(_os.path.dirname(
    _os.path.dirname(_os.path.abspath(__file__)))))
REFS = {"HEAD": "本树", "port/1.21.11-fabric": "基准", "origin/26.1": "宿主"}


def git(*a):
    r = subprocess.run(["git", "-C", REPO] + list(a), capture_output=True,
                       text=True, encoding="utf-8", errors="replace")
    r.check_returncode()
    return r.stdout


def category(p):
    parts = p.split("/")
    if "assets" in parts:
        i = parts.index("assets")
        return "assets/" + (parts[i + 2] if len(parts) > i + 2 else "?")
    if "data" in parts:
        i = parts.index("data")
        return "data/" + (parts[i + 2] if len(parts) > i + 2 else "?")
    return "其它"


def scan(ref):
    files = [p for p in git("ls-tree", "-r", "--name-only", ref,
                            "--", "src/main/resources", "src/main/generated").splitlines() if p]
    names = defaultdict(set)     # 基名 -> 类别集合
    bycat = defaultdict(set)     # 类别 -> 基名集合
    for p in files:
        b = p.rsplit("/", 1)[-1]
        c = category(p)
        names[b].add(c)
        bycat[c].add(b)
    return files, names, bycat


def main():
    data = {}
    for ref, label in REFS.items():
        f, n, c = scan(ref)
        data[ref] = (f, n, c)
        print("[活性] %-4s 资源文件 %d · 去重基名 %d · 类别 %d" % (label, len(f), len(n), len(c)))
    print()

    hf, hn, hc = data["HEAD"]
    bf, bn, bc = data["port/1.21.11-fabric"]
    of, on, oc = data["origin/26.1"]

    missing = sorted(set(bn) - set(hn))
    print("=== ⚠ 基准有、本树任何位置都无同名文件（%d）===" % len(missing))
    bycat = defaultdict(list)
    for b in missing:
        for c in sorted(bn[b]):
            bycat[c].append(b)
    for c in sorted(bycat, key=lambda k: -len(bycat[k])):
        items = bycat[c]
        inhost = [b for b in items if b in on]
        print("\n  [%s] %d 个（其中宿主也有的 %d 个）" % (c, len(items), len(inhost)))
        for b in items[:18]:
            print("      %-46s 宿主有？%s" % (b, "是" if b in on else "否"))
        if len(items) > 18:
            print("      … 另 %d 个" % (len(items) - 18))

    extra = sorted(set(hn) - set(bn))
    print("\n=== 本树有、基准无（%d，抽样 20）===" % len(extra))
    for b in extra[:20]:
        print("   %-46s 宿主有？%s" % (b, "是" if b in on else "否"))


if __name__ == "__main__":
    main()
