# -*- coding: utf-8 -*-
"""反向缺口扫描：origin/1.21.1 有、origin/26.1 没有的类。

**这个工具回答的问题与 PORT_26X_AUDIT 的差异化清单相反。**
差异化清单回答「我们多出来的部分搬齐了没」；本工具回答「**代码宿主自己在迁移时丢了什么**」。
后者不会出现在任何差异化清单里——因为它本来就不是差异化。

用法：
    PYTHONIOENCODING=utf-8 python docs/tools/host_gap.py            # 汇总
    PYTHONIOENCODING=utf-8 python docs/tools/host_gap.py <模块名>    # 展开某个桶
    PYTHONIOENCODING=utf-8 python docs/tools/host_gap.py --ledger   # 核对判定账本，退出码非 0 = 账本与现实脱节

判定账本 = `docs/tools/host_gap_ledger.tsv`，**逐条记录每个残余类的去向**，
目的是：真动手实现时不必再查一遍（这个仓库 1600+ 个源文件，重查一次的代价很高）。
`--ledger` 会双向核对——残余里有而账本没登记的、账本登记了但已不在残余里的，都报出来。
**账本不是写完就算数的文档，是一份会被机械核对的数据。**

判据与已知陷阱：
- 只比**路径**会把「换包」误报成删除，故先按简单类名兜一次；
- 宿主有 `TileEntity*` → `BlockEntity*` 的改名约定，单独兜一层，否则凭空多 16 条误报；
- 剩下的**仍然不等于回归**：可能是架构替换（如 JS 动画 → molang 绑定）、
  也可能是生态不允许（对应模组在 26.1.2 Fabric 上根本不存在，见审计 §2.2）。
  **本工具只产出候选与分类，判定必须人工逐条做。**
"""
import collections
import subprocess
import sys

OLD_REF = "origin/1.21.1"
NEW_REF = "origin/26.1"

# 审计 §2.2 实查结论：这些模组在 26.1.2 Fabric 上不存在，其兼容代码被删/停用属预期。
ECOSYSTEM_IMPOSSIBLE = {
    "gun", "tacz", "swarfare", "emi", "immersivemelodies", "ponder",
    "embeddium", "accessories", "sbackpack", "improvedmobs", "slashblade",
    "ironchest", "theoneprobe", "kubejs", "aquaculture", "simplehats", "justmorecakes",
    "jmc", "top",
}

# 已**逐条取证**的整包架构替换：源包整体被目标包取代，类名不对应，无法靠改名规则匹配。
# 每条都要写清取证方式，别凭「看着像」往里加。
KNOWN_REPLACED = {
    "cn/sh1rocu/touhoulittlemaid/util/itemhandler/":
        ("cn/sh1rocu/touhoulittlemaid/util/transfer/",
         "Forge IItemHandler 垫片 → Fabric 资源句柄 API（26.1 有 transfer 包共 12+ 个类，"
         "且 TaskBowAttack 等已 import CombinedResourceHandler）"),
    "com/github/tartaricacid/touhoulittlemaid/client/animation/script/":
        ("com/github/tartaricacid/touhoulittlemaid/client/animation/gecko/molang/",
         "Nashorn JS 动画脚本 wrapper → molang 绑定（26.1 有 gecko/molang 绑定包）"),
    "com/github/tartaricacid/simplebedrockmodel/":
        ("(合并回主包)",
         "build.gradle 里那行 include 依赖注释着「又合并回来了」"),
}


def ls(ref):
    out = subprocess.run(["git", "ls-tree", "-r", "--name-only", ref, "--", "src/main/java"],
                         capture_output=True, text=True, encoding="utf-8").stdout
    return [line for line in out.splitlines() if line.endswith(".java")]


def simple_name(path):
    return path.rsplit("/", 1)[-1][:-len(".java")]


def module_of(path):
    q = path.replace("src/main/java/", "")
    if q.startswith("com/github/tartaricacid/simplebedrockmodel/"):
        return "simplebedrockmodel"
    if q.startswith("com/github/tartaricacid/touhoulittlemaid/geckolib3/"):
        return "geckolib3"
    parts = q.split("/")
    if q.startswith("com/github/tartaricacid/touhoulittlemaid/compat/"):
        return "compat/" + parts[5]
    if q.startswith("cn/sh1rocu/touhoulittlemaid/mixin/compat/"):
        return "compat/" + parts[5]
    if q.startswith("cn/sh1rocu/touhoulittlemaid/"):
        return "fork:" + parts[3]
    if q.startswith("com/github/tartaricacid/touhoulittlemaid/"):
        return parts[4]
    return parts[0]


def classify():
    old, new = ls(OLD_REF), ls(NEW_REF)
    new_names = collections.Counter(simple_name(p) for p in new)
    new_paths = set(new)

    filtered = collections.Counter()
    replaced = collections.Counter()
    residue = []
    for path in old:
        if path in new_paths:
            continue
        name = simple_name(path)
        rest = path.replace("src/main/java/", "")
        replacement = next((k for k in KNOWN_REPLACED if rest.startswith(k)), None)
        if new_names.get(name):
            filtered["同名类换了包"] += 1
        elif name.startswith("TileEntity") and new_names.get("BlockEntity" + name[len("TileEntity"):]):
            filtered["TileEntity→BlockEntity 改名"] += 1
        # TileEntity<X>Renderer → client/renderer/blockentity/<X>Renderer（前缀去掉，不是换成 BlockEntity）
        elif (name.startswith("TileEntity") and name.endswith("Renderer")
              and new_names.get(name[len("TileEntity"):])):
            filtered["TileEntity*Renderer→blockentity/*Renderer"] += 1
        elif name.startswith("Tile") and new_names.get("Block" + name[len("Tile"):]):
            filtered["Tile→Block 改名"] += 1
        elif replacement:
            replaced[replacement] += 1
        else:
            residue.append(path)
    return old, new, filtered, replaced, residue


LEDGER = "docs/tools/host_gap_ledger.tsv"
#: 判定取值。**「待定」是允许的中间态，但不许留到最后**——账本里还有待定就说明这件事没做完。
VERDICTS = {
    "替换",   # 26.1 里有承接者，功能还在，只是换了实现／位置
    "生态",   # 对应模组在 26.1.2 Fabric 上不存在，删掉是对的（审计 §2.2）
    "丢失",   # 没有承接者，相对行为基准是真的少了东西 → 这些才是要干的活
    "无关",   # 死代码 / 构建产物 / 与玩法无关，丢了也无所谓
    "待定",
}


def load_ledger():
    import os
    if not os.path.exists(LEDGER):
        return {}
    rows = {}
    with open(LEDGER, encoding="utf-8") as handle:
        for line_no, line in enumerate(handle, 1):
            line = line.rstrip("\n")
            if not line or line.startswith("#"):
                continue
            parts = line.split("\t")
            if len(parts) < 5:
                raise SystemExit("%s:%d 列数不足 5（path/lines/说明/承接者/判定[/备注]）" % (LEDGER, line_no))
            rows[parts[0]] = parts
    return rows


def check_ledger(residue):
    rows = load_ledger()
    live = {p.replace("src/main/java/", "") for p in residue}
    logged = set(rows)

    missing = sorted(live - logged)
    stale = sorted(logged - live)
    bad_verdict = sorted(p for p, r in rows.items() if r[4] not in VERDICTS)
    pending = sorted(p for p, r in rows.items() if r[4] == "待定")

    counts = collections.Counter(r[4] for r in rows.values())
    print("账本 %d 条 · 残余 %d 条" % (len(rows), len(live)))
    for verdict, count in counts.most_common():
        print("   %-4s %d" % (verdict, count))

    problems = 0
    if missing:
        problems += 1
        print("\n★ 残余里有、账本没登记（%d 条）——判定没做完：" % len(missing))
        for path in missing[:20]:
            print("    " + path)
        if len(missing) > 20:
            print("    …… 另 %d 条" % (len(missing) - 20))
    if stale:
        problems += 1
        print("\n★ 账本登记了、但已不在残余里（%d 条）——多半是补回来了或过滤规则变了，该删账本行：" % len(stale))
        for path in stale[:20]:
            print("    " + path)
    if bad_verdict:
        problems += 1
        print("\n★ 判定取值非法（只能是 %s）：" % "/".join(sorted(VERDICTS)))
        for path in bad_verdict[:20]:
            print("    %s → %r" % (path, rows[path][4]))
    if pending:
        print("\n· 仍为「待定」%d 条（允许的中间态，但不该留到最后）" % len(pending))

    lost = sorted(p for p, r in rows.items() if r[4] == "丢失")
    if lost:
        print("\n=== 判定为「丢失」的 %d 条 —— 这些才是要干的活 ===" % len(lost))
        for path in lost:
            print("  %-72s %s" % (path, rows[path][2]))

    return 1 if problems else 0


def main():
    old, new, filtered, replaced, residue = classify()
    buckets = collections.defaultdict(list)
    for path in residue:
        buckets[module_of(path)].append(path)

    if len(sys.argv) > 1 and sys.argv[1] == "--ledger":
        return check_ledger(residue)

    if len(sys.argv) > 1:
        wanted = sys.argv[1]
        for path in sorted(buckets.get(wanted, [])):
            print(path)
        if wanted not in buckets:
            print("没有这个桶。可选：" + ", ".join(sorted(buckets)))
        return 0

    print("%s: %d 个 java 文件 → %s: %d 个" % (OLD_REF, len(old), NEW_REF, len(new)))
    print("路径级消失: %d" % (len(old) - sum(1 for p in old if p in set(new))))
    for reason, count in filtered.most_common():
        print("  已过滤·改名 [%s]: %d" % (reason, count))
    for key, count in replaced.most_common():
        target, why = KNOWN_REPLACED[key]
        print("  已过滤·整包替换 [%s → %s]: %d" % (key.rstrip("/").split("/")[-1], target, count))
        print("        取证：%s" % why)
    print("残余候选（无同名类 / 无改名对应 / 无已知整包替换）: %d\n" % len(residue))

    eco, adjudicate = [], []
    for module, paths in buckets.items():
        target = module.split("/", 1)[1] if module.startswith("compat/") else None
        (eco if target in ECOSYSTEM_IMPOSSIBLE else adjudicate).append((module, paths))

    print("--- A. 生态不允许（审计 §2.2 已裁决，属预期，不是回归）---")
    for module, paths in sorted(eco, key=lambda kv: -len(kv[1])):
        print("%4d  %s" % (len(paths), module))
    print("     小计 %d\n" % sum(len(p) for _, p in eco))

    print("--- B. 需人工逐条判定 ---")
    for module, paths in sorted(adjudicate, key=lambda kv: -len(kv[1])):
        print("%4d  %s" % (len(paths), module))
    print("     小计 %d" % sum(len(p) for _, p in adjudicate))
    print("\n展开某个桶：python docs/tools/host_gap.py <模块名>")
    return 0


if __name__ == "__main__":
    sys.exit(main())
