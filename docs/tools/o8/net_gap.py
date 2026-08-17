# -*- coding: utf-8 -*-
"""堵活性缺口：哪些文件含 CustomPacketPayload 却没被认出 id？"""
import re
import subprocess

import os as _os
# 仓库根 = 本文件所在目录向上三级（docs/tools/o8/x.py → 仓库根）
REPO = _os.path.dirname(_os.path.dirname(_os.path.dirname(
    _os.path.dirname(_os.path.abspath(__file__)))))
REFS = ["HEAD", "port/1.21.11-fabric", "origin/26.1"]
RE_TYPE_DECL = re.compile(
    r"new\s+(?:CustomPacketPayload\.)?Type\s*<[^>]*>\s*\(\s*"
    r"(?:modLoc|IdentifierUtil\.modLoc|id)\s*\(\s*\"([^\"]+)\"")
RE_TYPE_DECL_FQ = re.compile(
    r"new\s+(?:CustomPacketPayload\.)?Type\s*<[^>]*>\s*\(\s*"
    r"(?:ResourceLocation|Identifier)\.fromNamespaceAndPath\s*\(\s*\"[^\"]+\"\s*,\s*\"([^\"]+)\"")


def git(*a):
    r = subprocess.run(["git", "-C", REPO] + list(a), capture_output=True,
                       text=True, encoding="utf-8", errors="replace")
    r.check_returncode()
    return r.stdout


for ref in REFS:
    files = [p for p in git("ls-tree", "-r", "--name-only", ref).splitlines() if p.endswith(".java")]
    print("=== %s ===" % ref)
    for path in files:
        src = git("show", "%s:%s" % (ref, path))
        if "CustomPacketPayload" not in src:
            continue
        if RE_TYPE_DECL.search(src) or RE_TYPE_DECL_FQ.search(src):
            continue
        # 未认出：打印它对 Type 的处置，判断是真 payload 还是基础设施
        hits = [l.strip() for l in src.splitlines()
                if "Type<" in l or "Type <" in l or "implements CustomPacketPayload" in l
                or "extends CustomPacketPayload" in l or "CustomPacketPayload>" in l]
        print("  %s" % path)
        for h in hits[:4]:
            print("       | %s" % h)
    print()
