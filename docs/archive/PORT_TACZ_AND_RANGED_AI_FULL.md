# 本会话在基线上改了什么 —— 交给 26.1.2 差异化移植

> 归档说明（26.1.2 树，2026-08-14）：本文是 1.21.11 分支 2026-08-14 全量会话清单，
> 是 [PORT_TACZ_AND_RANGED_AI.md](PORT_TACZ_AND_RANGED_AI.md) 的超集（那份是 TACZ/远程 AI 专题）。
> 蒸馏结论已进 `PORT_26X_AUDIT.md` §3.B 与 `CURRENT_STATUS.md` O6，动手时以专题清单为操作指引、
> 本文为全量对账，两份都过一遍再开刀。

**来源**：`port/1.21.11-fabric` 分支，区间 `373ad95f2..4c5bfe402`，**27 笔提交**，触及 `src/` 下 **67 个文件**。
**日期**：2026-08-14。**行为基准**恒为 `origin/1.21.1`（Sh1roCu 非官方 Fabric 移植分支）。

> **怎么用这份文件**：下面按「这条改动相对基准是什么性质」分成三类。
> **A 类**是基准本来就有、我们只是在 1.21.11 上恢复/迁移——26.1.2 若基准同样有，照做即可；
> **B 类是我们独有、超出基准的行为改动**——26.1.2 必须**逐条显式决定要不要带**，不能默认继承；
> **C 类**不影响游戏行为，只影响工程门禁。
>
> 更详细的 TACZ 与远程 AI 移植笔记已经在 26.1.2 工作树里：
> `tlm-tsumugi-26.1.2/docs/PORT_TACZ_AND_RANGED_AI.md`（提交 `a73982a7a`）。本文件是**全量会话清单**，那份是专题。

---

## A 类 · 恢复基准既有功能（26.1.2 照基准做即可）

### A1. TACZ 枪械兼容整体恢复

- 基准 `compat/gun/` 下有 **两套**枪械兼容：`tacz/` 与 `swarfare/`（Superb Warfare）。
  **我们只恢复了 `tacz/`，`swarfare/` 仍未恢复**（该模组无 1.21.11 Fabric 版）。26.1.2 要单独判断。
- 依赖形态：`modCompileOnly files("libs/compile_only/TACZ-Refabricated-...jar")`，**jar 不入库**，
  `compileJava` 前置检查缺文件时抛 `GradleException` 并打印下载地址。
  上游为 `q14433686-arch/TaCZ_Refabricated_Unofficial`（GPL-3.0，只发 GitHub Releases，无 Maven）。
- 恢复必须做的三处登记（**任何一处漏了都不报错、功能静默不执行**）：
  1. `build.gradle` 的源集排除行——本轮把**最后两条活跃排除**注释掉了
     （`mixin/compat/**` 与 `util/compat/**`）；
  2. `fabric.mod.json` 的 entrypoint；
  3. `*.mixins.json` 条目（`required:true`，与排除集不一致＝加载必崩）。
- ⚠️ **本轮在这里栽过一次**：文件 `git checkout` 回来后 `compileJava` 全绿，但落点仍在排除目录里，
  class 从未产出 → 无 TACZ 启动直接 `ClassNotFoundException` 崩在 PREPARE。
  **恢复文件的第一步是查落点在不在排除里，不是查编不编得过。**

### A2. Fabric API 0.141.4 → 0.141.6+1.21.11

TACZ 的硬性下限。26.1.2 自有版本线，只需知道**这次升级是被 TACZ 逼的**，不是我们想升。

---

## B 类 · 我们独有、超出基准的行为改动（**逐条显式决定**）

### B1. 威胁响应改为使用手里的远程武器（最大的一条）

**基准的行为**：应战（EMERGENCY_COMBAT）活动**只有近战**。女仆哪怕手持弓/弩/枪，
遇敌也会走过去肉搏。

**关键事实**：基准里 `MaidShootTargetAnyItemTask` 与 `MaidAttackStrafingAnyItemTask` 两个类
**存在但零消费者**（全仓只有它们自己引用自己，是死代码）。我们把它们接进了应战活动。

**我们改了什么**：
- `MaidBrain` 的应战活动新增三项：`MaidAttackStrafingAnyItemTask`（速度 **0.5**，
  范围 = `MaidCombatManager.LOCAL_PROTECTION_RANGE` = 16）、`MaidShootTargetAnyItemTask(2, 20, ...)`、
  `GunShootTargetTask()`；
- 两个 AnyItem 任务的判据从 `Predicate<ItemStack>` **改成 `Predicate<EntityMaid>`**
  （因为「能不能开火」不只看物品，还要看有没有弹药、有没有对应实现）；
- 新增共享判据 `MaidCombatManager.isHoldingUsableRangedWeapon(maid)`：
  vanilla 远程 = `ProjectileWeaponItem` **且** 能解析出实现 **且** `maid.getProjectile(stack)` 非空；
  或 `GunCommonUtil.hasUsableGun(maid)`；
- `MaidEmergencyWalkToTarget` 加一条 `rangedStandoff` 分支：持可用远程武器 + 看得见目标 + 16 格内
  → 擦掉 WALK_TARGET（站定输出，不再贴脸）。

**26.1.2 注意**：
- ⚠️ **`maid.canUseNonMeleeWeapon(stack)` 顾名思义是「这把远程武器能用吗」，实际被覆写成
  `getTask() instanceof IRangedAttackTask`**——不看物品也不看弹药。拿它当弹药判据必错。
  真正的弹药判据是 `maid.getProjectile(stack)`（会翻手持 + 背包）。
- ⚠️ 别把近战一起堵死：既有用例 `usableBowCanMeleeOnlyThroughEmergencyLayer` 钉着
  「敌人贴脸时端着弓也得还手」。该拦的是「主动走过去」，不是「近战本身」。
- 走位手感必须对齐基准：速度 **0.5**（不是 0.6），并保留基准的**主人距离刹车**
  （`!hasHome()` 且主人距离 ≥ homeRadius → `stopInPlace()`）。这两条我们改错过一次，被用户实测退回。

### B2. 弓/弩解绑工作任务（用户 2026-08-14 定案）

**基准的行为**：只有当前工作任务是弓兵/弩兵时才会开火。
**我们改成**：按**手里的武器**找开火实现，与枪械那条路对齐。

- 新增 `IRangedAttackTask.resolveImplementation(maid, weapon)`：
  先看当前任务是不是 `IRangedAttackTask`（**不额外要求 `isWeapon`**），否则遍历
  `TaskManager.getTaskIndex()` 找 `isWeapon` 命中的那个；
- `EntityMaid.performRangedAttack` 改用它，不再是 `getTask() instanceof IRangedAttackTask`。

⚠️ **踩过的坑**：`resolveImplementation` 第一步若额外要求 `isWeapon`，会让「弓兵任务 + 手持弩」
落到弩的实现上、让模组远程武器**完全哑火**。红测证据：注入该缺陷后测试报「实得：TaskCrossBowAttack」。

### B3. 枪械识别距离按枪种取值（用户裁决「方案 2」，只动兼容层）

**基准的行为**：`TaskGunAttack.searchRadius` **恒取 `MAID_GUN_LONG_DISTANCE`（64）**，
而 `canSee` 却按枪种取 NEAR(32)/MEDIUM(48)/LONG(64)——**扫描半径与交战半径不是一个数**。

**我们改成**：新增 `compat/gun/common/GunRecognitionRange`（**基准无此文件**）作为唯一映射口：
狙击→LONG，霰弹/手枪/冲锋枪→NEAR，其余→MEDIUM；`searchRadius` 经 `ServerRuleConfig` 读它。

⚠️ **这个半径会顺着 `EntityMaid.searchRadius()` 流进两处**：`MaidNearestLivingEntitySensor`
的扫描盒、以及 `MaidPathFindingBFS` 的寻路半径。**持枪 = 所有找方块行为都用 64 格做 BFS**。
2026-08-14 实机六组对照证明这**不是**用户所报「寻路异常」的成因，但它是真实的性能面，
26.1.2 可以考虑把 BFS 半径与索敌半径解耦（我们没做，属超出基准）。

### B4. 姿势泄漏修复（基准同样有此缺陷）

`MaidShootTargetTask.stop()` 与 `MaidTridentTargetTask.stop()` 在基准里**只置位不清位**
（`start()` 里 `setSwingingArms(true)`，`stop()` 没有对应的 `false`），
于是拉过弓之后换任何武器都保持拉弓姿势。另外五个同类任务都是置位+清位。
**我们补了 `setSwingingArms(false)`。这是超出基准的修复，26.1.2 要显式决定带不带。**

### B5. 三个枪械距离键必须进 `ServerRuleConfig.values()` 认领清单

**这是本树独有的配置所有权重构带来的，26.1.2 若也有该重构就必须同步做。**
`ServerRuleConfig.get()` 对**未认领**的键会回落到裸 spec，而裸 spec 在集成服务端 tick 期没加载
→ `IllegalStateException` **崩服**。

⚠️ 本轮**因此崩了两次**：第一次是直接搬基准的裸 `.get()`；第二次是改用了读口但键不在清单里，
**只是栈深了一层**。「用对访问器」≠「读得到」。
**这是本仓库第 N 处静默注册面**，与 mixins.json / entrypoint / `MaidBrain` memory 列表同族。

### B6. 渲染期不得读实体（1.21.11 渲染架构的硬要求）

`LayerMaidBackItem` / `GeckoLayerMaidBackItem` 的枪械渲染路径改为读
`EntityMaidRenderState.backpackShowItem`（新增字段，提取阶段写入、`backItem.clear()` 时清）。
26.1.2 的渲染架构若同源，这条直接适用。

### B7. 其它

- `GeoLocatorType` 取消注释 `TAC_PISTOL`("PistolLocator") / `TAC_RIFLE`("RifleLocator")；
- `compat/cloth/MenuIntegration` 的三个枪械滑块包在 `isModLoaded(TacCompat.TACZ_ID)` 里
  （**未装 TACZ 时整组隐藏**）；
- 补齐 **66 条**枪械任务 lang，落在 **11 个**语言文件里（仓库共 17 个，其余 6 个基准本就没有这些键）。
  `.desc` / `.condition.has_gun` 按**只有 TaCZ** 重写
  ——基准原文提到 Superb Warfare，而我们没恢复它。

---

## C 类 · 工程与门禁（不影响游戏行为）

| 项 | 状态 | 说明 |
|---|---|---|
| **ArchUnit 1.4.1** | 采用，**进 `test` 即进门禁** | `ArchitectureRulesTest` 两条规则：世界规则不得裸读 / 经读口读的键必须被认领。**按字节码全仓查**，两条都做过红测 |
| **SpotBugs 6.5.10** | 采用，report-only | `gradlew spotbugsMain`，HIGH+MAX 全 main 198 条；抽样核实过一条真缺陷（`Position.bookMove()` 的 `Math.abs(Integer.MIN_VALUE)`） |
| **PIT 变异测试** | **评估后摘除** | 卡在它的前置检查两重障碍；且它只能评价真正执行产品代码的测试，而本仓库多数契约测试是扫源码文本的。**别再试一遍** |
| 新增契约测试 | 4 个 | `ServerRuleReadContractTest` / `GunRecognitionRangeTest` / `MenuTranslationKeyContractTest` / `MaidRangedEmergencyGameTest`（6 个 GameTest） |

⚠️ **新增 GameTest 类必须登记 `fabric.mod.json` 的 `fabric-gametest` entrypoint**，否则永不执行。

⚠️ **ArchUnit 那条「认领清单」判据要取并集**：`ServerRuleConfig.get` 会把 AI 规则委派给
`AiServerRuleConfig`（**两个店**），只取前者会误报 `AIConfig.LLM_ENABLED` 一批。

---

## 1.21.11 API 事实（对 26.1.2 有参考价值的）

| 事实 | 影响 |
|---|---|
| `AbstractArrow` 移到 `net.minecraft.world.entity.projectile.arrow` 包 | import 要改 |
| `AbstractArrow.getBaseDamage()` **已删**（`setBaseDamage` 保留），新增 `setBaseDamageFromMob(float)` | 基准的 `setBaseDamage(getBaseDamage() * multiplier)` 无法直译，我们按 vanilla 箭基础伤害 2.0 重算 |
| `ProjectileUtil.getMobArrow` → `ArrowItem.createArrow` → `stack.copyWithCount(1)` | **药水箭的 `POTION_CONTENTS` 会被拷进箭实体**，之后 `shrink` 原栈是安全的 |
| `LivingEntity.canBeAffected`：带 `EntityTypeTags.IGNORES_POISON_AND_REGEN` 的实体（不死系）**对中毒与再生免疫** | 「毒箭射僵尸没反应」是**原版规则**，不是缺陷 |
| 客户端不再持有 `RecipeManager` | 自定义 `RecipeType` 不同步客户端 |
| `Mob` 装备 NBT 从 `HandItems`/`ArmorItems` 改为 `equipment:{mainhand:...}` | 写测试/命令时会踩 |
| `TamableAnimal` 的所有者 NBT 键是 **`Owner`**（int 数组） | 同上 |

---

## 未决项（本会话没能关闭）

**O3 · 持枪时寻路异常**（用户 2026-08-14 实机报告）。
用 rig 跑了**六组受控条件**（平地/自然地形 × 主人静止/慢走 × 空手/弓/弩/枪，每组同起点同路线只换一个变量），
判据是路程/净位移比与每步 yaw 变化：

| 场景 | 条件 | 打转比 | yaw |
|---|---|---|---|
| 平地·静止 | 空手 / 弩 / 枪 | 1.2 / 1.1 / **1.0** | 10 / 5 / **0**°/s |
| 自然地形·慢走 | 空手 | 4.5 | 80° |
| 自然地形·慢走 | 枪 | **2.8** | **51°** |
| 平地·交战 | 枪 | **1.1** | **1.0°** |

**枪在每一组里都不比其它武器差**，交战组还是最稳的。自然地形上的高 yaw 是跟随共性（空手最严重）。
**结论：症状复现不出来，等用户给复现现场**（哪把枪 / 有无敌对生物 / 家模式 / 是否骑乘）。
唯一与枪相关的真实信号：`gun_attack` 下触发过 **1 次** `Maid's AI taking too long`（79.75 ms，阈值 50 ms），未能复现。

---

## 两条本会话新学到的取证纪律（26.1.2 同样适用）

1. **创造模式的玩家不会被怪物索敌。** 我连开三轮「场上有僵尸」的战斗取证，
   女仆一次都没进交战态；换生存态后同一场景立刻开火。
   **凡取证依赖「某个 AI 会不会被触发」，先证明触发条件本身在场景里成立。**
2. **比值型聚合指标在分母趋零时会爆炸。** 一轮里报出「打转比 13.6」，
   回看原始序列她跟得好好的，只是起步落后、首末位置恰好接近。**两个数不自洽就别信聚合，回看原始序列。**
