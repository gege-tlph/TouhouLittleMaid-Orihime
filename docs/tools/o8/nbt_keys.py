# -*- coding: utf-8 -*-
"""行为面对账 · 存档面：持久化键全集对差（按成因，不按书写形态）

「持久化键」的成因 = 传给 NBT / ValueOutput / ValueInput 存取器的第一个实参所代表的字符串。
它可能写成字面量（本树多），也可能写成具名常量（基准多）——只认字面量会漏掉整棵树。
故：先建全仓「常量名 → 字符串值」表，再解析每个存取点的第一个实参。

活性自报与结论正交：常量表多大、扫了几个文件、抓到几个存取点、其中几个解析失败。
解析失败必须显式报出，不许静默丢弃（静默丢弃 = 假的「没有差异」）。
"""
import re
import subprocess
from collections import defaultdict

import os as _os
# 仓库根 = 本文件所在目录向上三级（docs/tools/o8/x.py → 仓库根）
REPO = _os.path.dirname(_os.path.dirname(_os.path.dirname(
    _os.path.dirname(_os.path.abspath(__file__)))))
REFS = {"HEAD": "本树", "port/1.21.11-fabric": "基准", "origin/26.1": "宿主"}

# 存取器方法名：只取对 NBT/ValueOutput/ValueInput 有辨识度的，排除裸 put/get（会命中 Map）
ACCESSORS = (
    "putString|putBoolean|putInt|putFloat|putDouble|putLong|putShort|putByte|"
    "putIntArray|putByteArray|putUUID|putLongArray|"
    "store|storeNullable|"
    "getString|getBoolean|getInt|getFloat|getDouble|getLong|getShort|getByte|"
    "getIntArray|getByteArray|getUUID|getLongArray|getCompound|getList|"
    "getStringOr|getBooleanOr|getIntOr|getFloatOr|getDoubleOr|getLongOr|getShortOr|getByteOr|"
    "read|readOptional|child|childOrEmpty|childrenList|childrenListOrEmpty|list|listOrEmpty|"
    "contains|discard|remove"
)
RE_ACCESS = re.compile(r"\.(?:%s)\s*\(\s*([A-Za-z_$][\w$.]*|\"[^\"\\]*\")" % ACCESSORS)
RE_CONST = re.compile(r"""(?:static\s+final|final\s+static)\s+String\s+(\w+)\s*=\s*"([^"\\]*)"\s*;""")
RE_CONST2 = re.compile(r"""\bString\s+(\w+)\s*=\s*"([^"\\]*)"\s*;""")

NBT_HINT = re.compile(r"ValueOutput|ValueInput|CompoundTag|NbtOps|serializeNBT|deserializeNBT")


def git(*a):
    r = subprocess.run(["git", "-C", REPO] + list(a), capture_output=True,
                       text=True, encoding="utf-8", errors="replace")
    r.check_returncode()
    return r.stdout


def strip_comments(s):
    s = re.sub(r"/\*.*?\*/", " ", s, flags=re.S)
    return re.sub(r"//[^\n]*", " ", s)


def scan(ref):
    files = [p for p in git("ls-tree", "-r", "--name-only", ref).splitlines() if p.endswith(".java")]
    srcs = {}
    consts = {}
    for p in files:
        raw = git("show", "%s:%s" % (ref, p))
        s = strip_comments(raw)
        srcs[p] = s
        for m in RE_CONST.finditer(s):
            consts[m.group(1)] = m.group(2)
        for m in RE_CONST2.finditer(s):
            consts.setdefault(m.group(1), m.group(2))
        # 简单名也登记（Foo.BAR 形式的引用要能解析到 BAR）

    keys = defaultdict(set)
    sites = 0
    unresolved = defaultdict(set)
    scanned = 0
    for p, s in srcs.items():
        if not NBT_HINT.search(s):
            continue
        scanned += 1
        cls = p.rsplit("/", 1)[-1][:-5]
        for m in RE_ACCESS.finditer(s):
            arg = m.group(1)
            sites += 1
            if arg.startswith('"'):
                keys[cls].add(arg[1:-1])
                continue
            simple = arg.split(".")[-1]
            if simple in consts:
                keys[cls].add(consts[simple])
            elif arg in consts:
                keys[cls].add(consts[arg])
            else:
                unresolved[cls].add(arg)
    live = dict(files=len(files), consts=len(consts), nbt_files=scanned,
                sites=sites, classes=len(keys),
                unresolved=sum(len(v) for v in unresolved.values()))
    return keys, unresolved, live


def main():
    data = {}
    for ref, label in REFS.items():
        k, u, live = scan(ref)
        data[ref] = (k, u)
        print("[活性] %-4s 文件 %d · 常量表 %d 条 · 含 NBT 迹象的文件 %d · 存取点 %d · "
              "有键的类 %d · 未解析实参 %d"
              % (label, live["files"], live["consts"], live["nbt_files"],
                 live["sites"], live["classes"], live["unresolved"]))
    print()

    (hk, hu), (bk, bu), (ok_, ou) = (data["HEAD"], data["port/1.21.11-fabric"], data["origin/26.1"])

    # 全树键集（跨类合并）——类名可能改，先看整体是否丢键
    hall = set().union(*hk.values()) if hk else set()
    ball = set().union(*bk.values()) if bk else set()
    oall = set().union(*ok_.values()) if ok_ else set()
    print("=== 全树键集规模：本树 %d · 基准 %d · 宿主 %d ===" % (len(hall), len(ball), len(oall)))

    miss = sorted(ball - hall)
    print("\n=== ⚠ 基准有、本树全树都没有的键（%d）===" % len(miss))
    for k in miss:
        owners = sorted(c for c, v in bk.items() if k in v)
        print("   %-42s 基准归属：%-40s 宿主有？%s"
              % (k, ",".join(owners)[:40], "是" if k in oall else "否"))

    extra = sorted(hall - ball)
    print("\n=== 本树有、基准没有的键（%d）===" % len(extra))
    for k in extra:
        owners = sorted(c for c, v in hk.items() if k in v)
        print("   %-42s 本树归属：%-40s 宿主有？%s"
              % (k, ",".join(owners)[:40], "是" if k in oall else "否"))

    print("\n=== ⛔ 未解析实参（解析器盲区，前 25 条）===")
    shown = 0
    for cls, args in sorted(hu.items()):
        for a in sorted(args):
            if shown >= 25:
                break
            print("   本树 %-34s %s" % (cls, a))
            shown += 1
        if shown >= 25:
            break


if __name__ == "__main__":
    main()
