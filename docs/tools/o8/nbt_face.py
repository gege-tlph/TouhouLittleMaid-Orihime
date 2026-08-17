# -*- coding: utf-8 -*-
"""行为面对账 · 存档面下半：实体/方块实体的 NBT 键 与 SavedData

契约：什么活过重启？
  少存一个键 = 静默丢数据；键名改了 = 旧档读不出来（同样是丢）。

判据：截出 addAdditionalSaveData / readAdditionalSaveData 的方法体（大括号配对），
取其中的字符串字面量作为「NBT 键集」，两树对差。
不截方法体就等于在整份文件里找字面量——那是「indexOf 在整份文件里找调用点」那条教训。

活性自报与结论正交：每棵树认出几个类、每个方法体多长、抽到几个键。
"""
import re
import subprocess

import os as _os
# 仓库根 = 本文件所在目录向上三级（docs/tools/o8/x.py → 仓库根）
REPO = _os.path.dirname(_os.path.dirname(_os.path.dirname(
    _os.path.dirname(_os.path.abspath(__file__)))))
REFS = {"HEAD": "本树", "port/1.21.11-fabric": "基准", "origin/26.1": "宿主"}
METHODS = ("addAdditionalSaveData", "readAdditionalSaveData")
RE_STR = re.compile(r'"([^"\\]*)"')


def git(*a):
    r = subprocess.run(["git", "-C", REPO] + list(a), capture_output=True,
                       text=True, encoding="utf-8", errors="replace")
    r.check_returncode()
    return r.stdout


def strip_comments(s):
    s = re.sub(r"/\*.*?\*/", " ", s, flags=re.S)
    return re.sub(r"//[^\n]*", " ", s)


def bodies(src, name):
    out = []
    for m in re.finditer(r"\b%s\s*\(" % re.escape(name), src):
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
        semi = src.find(";", k)
        if b < 0 or (0 <= semi < b):      # 是调用而不是定义
            continue
        d = 0
        for j in range(b, len(src)):
            if src[j] == '{':
                d += 1
            elif src[j] == '}':
                d -= 1
                if d == 0:
                    out.append(src[b:j + 1])
                    break
    return out


def scan(ref):
    files = [p for p in git("ls-tree", "-r", "--name-only", ref).splitlines() if p.endswith(".java")]
    res = {}
    total_len = 0
    nbodies = 0
    for p in files:
        raw = git("show", "%s:%s" % (ref, p))
        if not any(m in raw for m in METHODS):
            continue
        src = strip_comments(raw)
        keys = set()
        for m in METHODS:
            for b in bodies(src, m):
                nbodies += 1
                total_len += len(b)
                keys |= set(RE_STR.findall(b))
        if keys:
            res[p.rsplit("/", 1)[-1][:-5]] = keys
    return res, dict(files=len(files), classes=len(res), bodies=nbodies,
                     avg=(total_len // nbodies if nbodies else 0))


def main():
    data = {}
    for ref, label in REFS.items():
        d, live = scan(ref)
        data[ref] = d
        print("[活性] %-4s 文件 %d · 有存档方法且抽到键的类 %d · 方法体 %d 个 · 均长 %d 字符 · 键合计 %d"
              % (label, live["files"], live["classes"], live["bodies"], live["avg"],
                 sum(len(v) for v in d.values())))
    print()

    head, base = data["HEAD"], data["port/1.21.11-fabric"]
    host = data["origin/26.1"]

    print("=== 逐类 NBT 键对差（本树 vs 基准）===")
    reg = 0
    for cls in sorted(set(head) & set(base)):
        miss = base[cls] - head[cls]
        extra = head[cls] - base[cls]
        if miss:
            reg += 1
            hosth = host.get(cls, set())
            print("   ⚠ %-30s 基准有本树无：%s" % (cls, sorted(miss)))
            print("      %-30s 宿主有这些吗：%s" % ("", sorted(miss & hosth) or "都没有"))
        if extra:
            print("   + %-30s 本树多出：%s" % (cls, sorted(extra)))
    print("   缺键的类：%d" % reg)

    print("\n=== 仅基准有存档键的类（%d）===" % len(set(base) - set(head)))
    for cls in sorted(set(base) - set(head)):
        print("   %-30s %s" % (cls, sorted(base[cls])[:6]))
    print("\n=== 仅本树有存档键的类（%d）===" % len(set(head) - set(base)))
    for cls in sorted(set(head) - set(base)):
        print("   %-30s %s" % (cls, sorted(head[cls])[:6]))


if __name__ == "__main__":
    main()
