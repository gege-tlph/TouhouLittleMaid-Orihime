# 从 1.21.11 分支移过来：TACZ 兼容 + 远程 AI 改动

**来源**：`port/1.21.11-fabric` 分支 2026-08-14 一整轮工作，提交区间 `373ad95f2..b0ca8ff47`（20 笔，73 文件，+2605 行）。
**用途**：迁 26.1.2 时照此清单逐项判断「照搬 / 重做 / 不需要」，而不是重新发现一遍。
**本文件只描述那边发生了什么与为什么**，不复制状态；26.1.2 的当前状态仍以本工作树的 `CURRENT_STATUS.md` 为准。

---

## 一、依赖：TACZ 在 26.1.2 有对应构件，但不是同一个

| | 1.21.11 分支用的 | 26.1.2 应该用的 |
|---|---|---|
| 仓库 | `q14433686-arch/TaCZ_Refabricated_Unofficial` | 同一仓库，分支 `26.1.2` |
| release | `1.21.11_R1`（2026-08-13） | `26.1.2_R1`（2026-08-12） |
| 构件 | `TACZ-Refabricated-1.21.11-1.1.8+fabric.1.21.11.R1.jar` | `TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R1.jar` |
| 硬依赖 | fabric-api ≥ **0.141.6**、forgeconfigapiport ≥ 21.11.1 | **需按 26.1.2 那份 fabric.mod.json 重新确认** |

**没有 Maven 坐标**（Modrinth 上的 `tacz-refabricated` 停在 1.21.1，是更早的 0.0.7 那条线）。
1.21.11 分支的做法是 `modCompileOnly files("libs/compile_only/<jar>")` + jar 不入库（55 MB、GPL-3.0，仓库是 MIT+CC），
缺文件时 `compileJava` 抛出带下载地址的 `GradleException`。同一做法可直接照搬。

⚠️ **上游 README 明说 1.21.11 与 26.x 是两套实现**：1.21.11 是混淆版本，26.x 不是，
「网络 / 资源加载 / GUI / 渲染」在两个分支里各写各的。所以**不能假定 26.1.2 那份 jar 的内部结构与 1.21.11 一致**，
下面凡涉及 TACZ 内部（mixin 注入点、数据组件）的部分都必须重新验证。

---

## 二、改动分组

### A. TACZ 兼容恢复（`478bc3bc2` 为主）

按 `origin/1.21.1` 逐字恢复，约 1.5k 行。包括：

- `compat/gun/tacz/**` 7 文件、`compat/gun/common/**` 5 文件
- `cn/sh1rocu/.../mixin/compat/tacz/**` 4 个 mixin + `util/compat/tacz/ItemHandlerUtil`
- `client/animation/gecko/condition/ConditionTAC.java`
- 资源：`tacz_gun_icon` 的贴图 / `models/item` / **1.21.11 新增的 `items/` 物品模型定义** / `data/tacz` 两个 interact_key 标签

**卓越前线（Superb Warfare）没有恢复**——它没有 Fabric 端。基准里 `GunCommonUtil`/`GunClientUtil`
是 TaCZ 与 SBW 两路分发，恢复时剥掉了 SBW 那一路（未安装时那些分支恒 false，删除与基准行为一致）。
26.1.2 若仍无 SBW，照此处理。

### B. 恢复必须同批做的注册面（缺一即「纸面接口」）

这是最容易漏的一组，**每一项都实证过**：

| 注册面 | 内容 |
|---|---|
| `build.gradle` 依赖 | `modCompileOnly files(...)` + 缺件守卫 |
| **`build.gradle` 源集排除** | 本分支最后两条活跃排除正好盖着 `mixin/compat/**` 与 `util/compat/**`，删掉才编译得到 |
| `touhou_little_maid_fabric.mixins.json` | 4 条 `compat.tacz.*`（放 common 数组，走 `MixinPlugin` 的 isModLoaded 门控） |
| `InitItems` | `TACZ_GUN_ICON` |
| `InitAttribute` + `EntityMaid` 属性表 | `MAID_GUN_ATTACK_SPEED` |
| `MaidConfig` | 三档识别距离（键名与默认值照基准，保 TOML 兼容） |
| **`ServerRuleConfig.values()` 认领清单** | 三个键必须在此，否则读口回落到未加载的裸 spec → **崩服** |
| `MenuIntegration` | 三条滑条，按 `isModLoaded` 动态显示 |
| **lang** | 17 个语言文件 66 条。漏了就在「工作安排」里显示裸键名 |
| 调用点 | 7 处（`TaskManager`、`AnimationManager` ×2、`MaidBaseAnimation` ×2、两个背部/手持渲染层、gecko 背包层） |

### C. 1.21.11 特有的移植改写（**26.1.2 上很可能不同，需逐条重验**）

| 面 | 1.21.11 的写法 | 备注 |
|---|---|---|
| 物品渲染 | `ItemRenderer.renderStatic` → `ItemModelResolver` + `ItemStackRenderState.submit(...)` | TaCZ 自己实现了原版 `ItemModel`（`TaczDynamicItemModel`），走标准通道即可，不必碰它的渲染器 |
| gecko 定位点 | `ILocationModel` 骨骼链 → `IGeoLocatorSource` + `GeoLocatorType` 定位组 | 需把 `GeoLocatorType` 里注释掉的 `TAC_PISTOL`/`TAC_RIFLE` 放回，并在 `LocationModelLocatorSource` 补两支（YSM 模型走那条） |
| 动画 | `ILoopType`/`AnimationBuilder` → 枚举 `LoopType` + `getCodedController()` | 基准的 `shouldResetTick + adjustTick(0)` → `indicateReload()` |
| 条件动画 | 静态表 `ConditionManager.getTAC(modelId)` → 随 `GeckoContainer` 走的每模型实例字段 | 需给 `ConditionManager` 加 `tac` 字段并在 `addTest` 里喂 |
| 手臂姿势 | `ModelRendererWrapper` → `BedrockPart`；内层动画改渲染状态驱动 | `setRotateAngleX/Y` 对应 `xRot/yRot`；`onHoldGun` 改收主手 `ItemStack` |
| 活动范围 API | `hasRestriction`/`getRestrictCenter`/`getRestrictRadius` → `hasHome`/`getHomePosition`/`getHomeRadius` | |
| brain 行为签名 | `StartAttacking.create(pred, finder)` → `(level, e) -> ...` 两参形式 | `StopAttackingIfTargetInvalid` 同 |
| 友伤判据 | `getOwnerUUID()` 已删 → `getOwnerReference().getUUID()` | **不能换成 `getOwner() != null`**：后者要求主人在线，离线时女仆会重新吃子弹伤害 |
| 箭的类 | `net.minecraft.world.entity.projectile.arrow.AbstractArrow` | 1.21.11 挪进了 `projectile.arrow` 子包 |

### D. 远程 AI 改动（**与 TACZ 无关，是本 fork 自己的系统，26.1.2 同样需要**）

用户报告「女仆手拿着弓/弩/枪还要上去手打敌人」。根因：`MaidBrain.registerEmergencyCombatGoals()`
只注册了灭火、举盾、走向目标、`MaidMeleeAttack` 四条，**应战活动里没有任何远程行为**，
而应战会覆盖工作活动。改动（`2e605d5c8` + 后续修正）：

1. 应战活动接入三条远程行为：`MaidAttackStrafingAnyItemTask`、`MaidShootTargetAnyItemTask`
   （这两个是本 fork 早就写好、却一直**零调用点**的件）、`GunShootTargetTask`（未装 TaCZ 时进入条件恒 false）
2. `MaidEmergencyWalkToTarget`：手持可用远程武器且目标在保护半径内就站定，不再贴脸
3. `MaidCombatManager.isHoldingUsableRangedWeapon`：三个消费者共用的唯一判据
4. `IRangedAttackTask.resolveImplementation`：**弓弩解绑工作任务**（用户裁决）——
   按手里的武器找开火实现，当前任务优先。此前 `EntityMaid.performRangedAttack` 只认当前工作任务，
   任务不对就是空操作，于是「农场女仆手持弓有箭」打不响而手持枪却能打（枪走 TaCZ 自己的链）
5. `TaskGunAttack.searchRadius` 按枪种取值（`GunRecognitionRange` 唯一映射），不再恒取 LONG(64)
6. 顺带修：`MaidShootTargetTask` / `MaidTridentTargetTask` / `MaidShootTargetAnyItemTask`
   都是 `start()` 置 `swingingArms` 而 `stop()` 不清 → 拉过弓后换武器仍保持拉弓姿势

**刻意没动的**：近战闸门仍是 `emergency || !isHoldingUsableProjectileWeapon(maid)`。
既有用例 `usableBowCanMeleeOnlyThroughEmergencyLayer` 钉着「敌人贴脸时端着弓也得还手」，
2026-08-14 用户明确表示这条不改。

### E. 新增测试（26.1.2 可直接照搬，判据与版本无关）

| 测试 | 钉住什么 | 是否红测过 |
|---|---|---|
| `ServerRuleReadContractTest` | 任务读的每个世界规则都在认领清单里；任务里不得裸读 | 是 |
| `MaidRangedEmergencyGameTest`（6 条） | 站定不贴脸 / 没弹药回落近战 / 近战武器不受影响 / 非远程任务也开得出火 / 当前远程任务胜出 | 是 |
| `GunRecognitionRangeTest`（5 条） | 枪种 → 识别距离档位 | — |
| `MenuTranslationKeyContractTest` | 菜单条目必须有翻译键 | 是 |

新增 GameTest 类**必须登记 `fabric.mod.json` 的 `fabric-gametest` entrypoint**，否则永不执行。

---

## 三、这一轮踩过的坑（26.1.2 上同样会踩）

1. **从基准 `git checkout` 回来的文件若落在 `sourceSets` 排除里，`compileJava` 的「0 错误」不含任何信息**——
   文件对 javac 是隐形的。恢复任何路径后先比对排除清单。
2. **配置键必须进 `ServerRuleConfig.values()`**——用对读口还不够。读口对未认领的键回落到裸 spec，
   而裸 spec 在集成服务端 tick 期没加载，一读就崩服。同族：`MaidBrain` 的 memory 列表、mixins.json、entrypoint。
3. **`EntityMaid.canUseNonMeleeWeapon` 被覆写成「当前工作任务是不是远程任务」**，既不看物品也不看弹药。
   拿它当「手持可用远程武器」用会得到相反结果。真正的弹药判据是 `maid.getProjectile(stack)`（翻手持+背包）。
4. **改行为前先看有没有既有用例钉着它**。第一版把「应战放行近战」一并堵了，被既有用例判红——
   该拦的其实是「主动走过去」而不是「近战本身」。
5. **枚举型断言必须自带下限断言**。菜单翻译守卫第一版只扫 `tr("字面量")`，而绝大多数条目的键名是参数，
   只认出 6 个键——是下限断言照出来的，否则它会以「0 个缺失」的姿态永远绿。
6. **含反斜杠的内容用 Write 工具落盘**，别过 bash heredoc（转义会被吃掉）。

---

## 四、未决项：跟着一起迁

**O3 · 持枪时寻路异常**（用户实机报告，根因未定）。已证的代码事实：

- `MaidPathFindingBFS` 的半径取 `maid.searchRadius()` → 当前任务的值。默认是 home 半径，
  而远程任务覆写成 48~64。于是拿远程武器时，**所有走 `MaidMoveToBlockTask` 的行为**
  （偷吃、找坐具、农场）都以那个半径做 BFS。
- `MaidNearestLivingEntitySensor` 扫的是 `maid.searchDimension()`，同样来自当前任务 → 同一个大盒子。
- 拿到 `ATTACK_TARGET` 后 `MaidFollowOwnerTask.hasCompetingGoal` 直接让位，**两种跟随实现共用这个守卫**，
  所以「原版跟随与平滑跟随都一样」是这条链预测的结果，不是跟随实现的问题。

**注意这三条都在核心侧，弩兵（64）与弹幕（64）与枪械同档**，不是 TACZ 独有。
26.1.2 上大概率原样存在。判据：拿弩兵女仆复现一次——**弩兵也异常就说明根在核心**。

修法候选：① BFS 半径与索敌半径解耦（前者本就不该吃后者）；② 已做的按枪种取值只收窄了枪械那一路。
