# -*- coding: utf-8 -*-
"""行为面对账 · 资源面下半：lang 键对差（玩家可见文本）

契约：玩家在屏幕上看到的每一句话。少一个键 = 屏幕上显示原始键名。
⚠ 反向也要看：本树有键而**代码里没人用**，属死键；本树代码用了键而 lang 里没有，属显示事故。

⚠ **聚合口径**（2026-08-18 修）：原实现按**文件基名**归键
（`per[p.rsplit("/",1)[-1]]`），而本仓库有 8 个命名空间各带一份 `en_us.json`，
于是 143 个文件互相覆盖、只剩最后一个（9 键），「差异 0」是假的。
正确口径按 MC 的实际行为定：**同一 locale 的所有命名空间被合并成一张扁平表**，
故聚合单位 = locale，且必须能报出每个键来自哪个命名空间。
教训：**聚合的 key 必须唯一标识被聚合物**；活性数（文件 143）与结论数（键 9）
不自洽时，先怀疑聚合口径。

活性自报与结论正交：每棵树几个 lang 文件、几个命名空间、各 locale 多少键、
代码里抓到多少处键引用。
"""
import json
import os as _os
import re
import subprocess
from collections import defaultdict

# 仓库根 = 本文件所在目录向上三级（docs/tools/o8/x.py → 仓库根）
REPO = _os.path.dirname(_os.path.dirname(_os.path.dirname(
    _os.path.dirname(_os.path.abspath(__file__)))))
REFS = {"HEAD": "本树", "port/1.21.11-fabric": "基准", "origin/26.1": "宿主"}

# 对差只做这两个 locale：其余语种是翻译进度问题，不是移植回归
LOCALES = ("en_us.json", "zh_cn.json")

RE_NS = re.compile(r"/assets/([^/]+)/lang/")


def git(*a):
    r = subprocess.run(["git", "-C", REPO] + list(a), capture_output=True,
                       text=True, encoding="utf-8", errors="replace")
    r.check_returncode()
    return r.stdout


def scan(ref):
    """返回 (per_path, per_locale, namespaces)

    per_path   : 完整路径 -> 键集合（唯一标识，绝不覆盖）
    per_locale : locale 文件名 -> 键 -> 该键出现在哪些命名空间
    """
    files = [p for p in git("ls-tree", "-r", "--name-only", ref,
                            "--", "src/main/resources").splitlines()
             if "/lang/" in p and p.endswith(".json")]
    per_path = {}
    per_locale = defaultdict(lambda: defaultdict(set))
    namespaces = set()
    for p in files:
        raw = git("show", "%s:%s" % (ref, p)).lstrip("﻿")
        m = RE_NS.search(p)
        ns = m.group(1) if m else "?"
        namespaces.add(ns)
        try:
            keys = set(json.loads(raw).keys())
        except Exception as e:
            keys = set()
            print("   ⛔ %s 解析失败：%s（%s）" % (ref, p, e))
        per_path[p] = keys
        loc = p.rsplit("/", 1)[-1]
        for k in keys:
            per_locale[loc][k].add(ns)
    return per_path, per_locale, namespaces


def main():
    data = {}
    for ref, label in REFS.items():
        per_path, per_locale, ns = scan(ref)
        data[ref] = (per_path, per_locale)
        tot = sum(len(v) for v in per_path.values())
        print("[活性] %-4s lang 文件 %d · 命名空间 %d · 键次合计 %d · %s"
              % (label, len(per_path), len(ns), tot,
                 " · ".join("%s %d 键" % (l[:-5], len(per_locale.get(l, {})))
                            for l in LOCALES)))
        # 口径自检：文件数远大于 locale 数时，若某 locale 的键数与最大单文件键数相同，
        # 多半又退化成了「只剩一个文件」——显式报出，不让它静默。
        for l in LOCALES:
            paths = [p for p in per_path if p.endswith("/" + l)]
            if paths:
                biggest = max(len(per_path[p]) for p in paths)
                if len(per_locale.get(l, {})) == biggest and len(paths) > 1:
                    print("        ⚠ %s：%d 个文件但并集等于最大单文件（%d 键），"
                          "检查是否又发生覆盖" % (l, len(paths), biggest))
    print()

    for loc in LOCALES:
        h = data["HEAD"][1].get(loc, {})
        b = data["port/1.21.11-fabric"][1].get(loc, {})
        o = data["origin/26.1"][1].get(loc, {})
        miss = sorted(set(b) - set(h))
        print("=== %s：基准有、本树无（%d / 基准共 %d 键）===" % (loc, len(miss), len(b)))
        buckets = defaultdict(list)
        for k in miss:
            buckets["|".join(sorted(b[k]))].append(k)
        for ns in sorted(buckets, key=lambda x: -len(buckets[x])):
            ks = buckets[ns]
            inhost = sum(1 for k in ks if k in o)
            print("   [命名空间 %s] %d 个（宿主也有 %d 个）" % (ns, len(ks), inhost))
            for k in ks[:8]:
                print("       %s" % k)
            if len(ks) > 8:
                print("       … 另 %d 个" % (len(ks) - 8))
        extra = sorted(set(h) - set(b))
        print("   （本树有、基准无：%d）" % len(extra))
        print()

    # 本树代码引用了但 lang 里没有的键（显示事故）
    print("=== 本树代码引用了、但 en_us/zh_cn 都没有的键 ===")
    hl = data["HEAD"][1]
    hall = set(hl.get("en_us.json", {})) | set(hl.get("zh_cn.json", {}))
    files = [p for p in git("ls-tree", "-r", "--name-only", "HEAD",
                            "--", "src/main/java").splitlines() if p.endswith(".java")]
    used = set()
    RE_KEY = re.compile(
        r'"((?:gui|ai|entity|item|block|advancements|config|message|tooltip|key|death|sound|command)'
        r'\.touhou_little_maid\.[a-z0-9_.]+)"')
    for p in files:
        for m in RE_KEY.finditer(git("show", "HEAD:%s" % p)):
            used.add(m.group(1))
    print("   活性：代码里抓到 %d 个键引用；本树 en_us∪zh_cn 共 %d 个键" % (len(used), len(hall)))
    orphan = sorted(used - hall)
    print("   ⚠ 代码用了但 lang 没有（%d）：" % len(orphan))
    for k in orphan[:30]:
        print("       %s" % k)
    if len(orphan) > 30:
        print("       … 另 %d 个" % (len(orphan) - 30))
    print("   ⚠ 已知盲区：只认字面量键，`Component.translatable(常量)` 与拼接键抓不到；"
          "上面那个「抓到 N 处」就是这个盲区的量级指示")


if __name__ == "__main__":
    main()
