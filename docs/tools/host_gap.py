# -*- coding: utf-8 -*-
"""反向缺口扫描：origin/1.21.1 有、origin/26.1 没有的类。

**这个工具回答的问题与 PORT_26X_AUDIT 的差异化清单相反。**
差异化清单回答「我们多出来的部分搬齐了没」；本工具回答「**代码宿主自己在迁移时丢了什么**」。
后者不会出现在任何差异化清单里——因为它本来就不是差异化。

用法：
    PYTHONIOENCODING=utf-8 python docs/tools/host_gap.py            # 汇总
    PYTHONIOENCODING=utf-8 python docs/tools/host_gap.py <模块名>    # 展开某个桶

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


def main():
    old, new, filtered, replaced, residue = classify()
    buckets = collections.defaultdict(list)
    for path in residue:
        buckets[module_of(path)].append(path)

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
