# 迁移到 MC 26.1.2 / 26.2 的差异化审计

> 本文 2026-08-13 自 `port/1.21.11-fabric` 迁入本分支（隔离纪律要求 26.1.2 的文档只住这里）。
> 文中对 1.21.11 分支的引用一律**复述结论**，不跨分支指路。

**在建设计文档，随移植完成移入 `docs/archive/`，不常驻。** 状态与待办仍以
[CURRENT_STATUS.md](CURRENT_STATUS.md) 为唯一来源，本文只回答一个问题：

> 把本分支的特色带到 Minecraft 26.1.2 / 26.2 上，**要搬什么、搬得动吗、按什么顺序搬**。

本文的数字是 2026-08-13 的测量快照，§4 给了重算命令。**结论可以过时，方法不该过时**——
再次评估时先重跑测量，不要引用本文的数字。

## 0-. 已定决策（用户 2026-08-13）

| 决策 | 内容 |
|---|---|
| **目标版本** | **只做 26.1.2**。26.2 不开工（两者只差 88 文件，将来要跟随时另议） |
| 依赖档位 | **允许用 beta / alpha 依赖**（26.1.2 上 JEI 只有 beta、Sodium 只有 alpha、Patchouli 只有 beta） |
| 工作方式 | **继承 1.21.11 移植的全部工程纪律**：四方校验、先取证再解释、编译通过不等于成功、用户实测优先、每节点收尾含文档同步与实机验收 |
| 行为基准 | 仍是 `origin/1.21.1`。**26.1.2 移植的基准是"我们在 1.21.11 上已经验收过的行为"**，不是重新推导 |

## 0. 结论

1. **版本迁移不必重做**：行为基准所在仓库 `Sh1roCu/TouhouLittleMaid-Orihime`（本地远端 `origin`）
   已有 26.1.2 与 26.2 的 **Fabric** 分支，2026-07-30 仍在更新。
   1.21.1 → 1.21.11 那种 API 苦力活在新基上是白拿的。
2. **但差异化不能机械搬运**，两个独立原因：
   - **三条谱系没有共同祖先**（§1），`cherry-pick` / `rebase` 全部无效；
   - **宿主结构已重构**（§5），`EntityMaid` 被拆成多个 manager，我们的改动没有落点。
3. **量级**：89 笔差异化提交、396 个生产文件、12,711 增 / 18,365 删。其中
   **149 个文件新基上不存在（可直接搬）**，**247 个已存在（必须在新宿主上重做）**。
4. **生态齐备**（§2），唯二缺口是两个由我们自己维护的前置：**Yes Steve Model**（上游无 26.x）
   与 **Patchouli**（只有 26.1 beta，26.2 缺）。

## 1. 事实基线

| 分支 | Minecraft | Loader | 最后提交 | 形态 |
|---|---|---|---|---|
| `origin/1.21.1`（行为基准） | 1.21.1 | 0.18.4 | 2026-06-24 | Fabric |
| `origin/26.1` | **26.1.2** | 0.19.2 | 2026-07-30 | **Fabric** |
| `origin/26.2` | **26.2** | 0.19.2 | 2026-07-30 | **Fabric** |
| 本分支 `port/1.21.11-fabric` | 1.21.11 | 0.19.3 | — | Fabric |

- **`26.1` / `26.2` 是基准仓库自己的分支**，上游本体（`upstream`）最新只有 `1.21`，
  没有 26.x 也没有 1.21.1。这条与 [CURRENT_STATUS.md](CURRENT_STATUS.md) 的「上游功能回移审计」一致。
- **`git merge-base` 在 `origin/1.21.1` 与 `origin/26.1` / `origin/26.2` 之间为空**，
  与 `HEAD` 之间同样为空。**提交级操作全部失效**——这不是"冲突多"，是根本没有共同基。
- **26.1 与 26.2 之间只差 88 个文件 / 289 增 289 删量级**。一次移植吃两个版本，
  双版本维护几乎不额外收费。
- 两条 26.x 分支的最后一笔提交是「鞍抱起女仆再放下会瞬移」——**和我们 `c9763d19f` 修的是同一个缺陷**。
  说明基准侧仍在修上游缺陷，我们的「上游缺陷修复批」需要**逐条重新核对是否已被上游修掉**。

## 2. 生态可用性（2026-08-13 实查）

查询通道：Modrinth `/v2/project/{slug}/version?loaders=["fabric"]`、CurseForge 项目页、
`origin/26.2` 的 `build.gradle` 与 `gradle.properties`。

**判据统一为：该模组自己发布的、`game_versions` 含 `26.1.2` 且 `loaders` 含 `fabric` 的构件。**
「26.x 有版本」不够——多个模组的 26.x 只发 NeoForge。

### 2.1 目标版本上可用（26.1.2 · Fabric）

| 前置 / 兼容目标 | 版本 | 档位 | 备注 |
|---|---|---|---|
| Fabric API | `0.155.2+26.1.2` | release | |
| Forge Config API Port | `26.1.5` | release | **硬依赖活着** |
| Cloth Config | `26.1.154+fabric` | release | |
| JEI | `29.22.0.73` | **beta** | 已获准使用 beta |
| REI | `26.1.819+fabric` | release | |
| Jade | `26.1.8+fabric` | release | |
| Iris | `1.11.3+26.1-fabric` | release | |
| Sodium | `mc26.1.2-0.9.2-alpha.4` | **alpha** | 已获准 |
| Inventory Profiles Next / libIPN | `fabric-26.1-2.3.6` / `6.8.3` | release | |
| Farmer's Delight Refabricated | `26.1-3.6.15` | release | |
| Kaleidoscope Cookery Refabricated | `1.4.1.4-fabric+mc26.1.2` | release | **正是 issue #1 要的 1.4+** |
| Kaleidoscope Tavern Refabricated | `1.2.0.5-fabric+mc26.1.2` | release | 我们独有的兼容 |
| Carry On | `2.10.0` | release | |
| PatPat | `1.3.1+26.1+fabric` | release | |
| Patchouli | `26.1-94-beta` | **beta** | 我们维护的 fork 需跟进到此 |
| MrCrayfish's Furniture: Refurbished | `v1.0.23+26.1.2` | release | GitHub Releases 实查 |
| Traveler's Backpack（本体） | `26.1.2-11.2.9` | release | **但兼容走 Accessories，见 2.2** |

### 2.2 目标版本上不可用（逐条实查，含加载器粒度）

| 模组 | 26.x 现状 | 结论 |
|---|---|---|
| **Yes Steve Model（本体）** | 26.1.2 **仅 NeoForge**，且闭源 | Fabric 上不存在可对接目标 |
| **OpenYSM（开源实现）** | 无任何 26.x 发布 | **要保留 YSM 特色只能我们自己移植**，见 §7.2 |
| Accessories | **无任何 26.x** | 连带 Sophisticated Backpacks / Traveler's Backpack / ExtraContainer 三项兼容全部不可能 |
| Sophisticated Backpacks | 26.1.x / 26.2 有，**仅 NeoForge** | 双重不可能（自身无 Fabric + Accessories 缺位） |
| KubeJS | 26.1.2 有，**仅 NeoForge** | Fabric 侧不可能；且 2026-07-28 已裁决放弃 |
| Aquaculture | 26.1.x 有，**仅 NeoForge** | 与 1.21.11 结论一致 |
| EMI · Iron Chests · The One Probe · Embeddium | **无任何 26.x** | |
| Immersive Melodies · Simple Hats · Ponder · Improved Mobs · Just More Cakes · SlashBlade | **无任何 26.x** | |
| **TACZ（TaCZ Refabricated）** | `q14433686-arch/TaCZ_Refabricated_Unofficial` 有 **26.1.2 分支**，release `26.1.2_R1`（2026-08-12）——**2026-08-14 改判：可用**（1.21.11 分支同日用同仓库 `1.21.11_R1` 实测接通）。无 Maven 坐标（Modrinth 的 tacz-refabricated 停在 1.21.1），做法照 1.21.11：`modCompileOnly files(libs/compile_only/…)` + 缺件守卫，jar 不入库（55 MB、GPL-3.0） | ⚠️ 上游明说 **1.21.11 与 26.x 是两套实现**（网络/资源加载/GUI/渲染各写各的），凡涉 TACZ 内部（mixin 注入点、数据组件）必须按 26.1.2 那份 jar 重验；硬依赖下限也按其 fabric.mod.json 重读。移植清单见 `docs/archive/PORT_TACZ_AND_RANGED_AI.md`。**2026-08-15 已落地**（`167608ab9`：jar 重验出两处漂移——lambda `$8→$0`、canReload/hasInventoryAmmo 去 static；实查下限 fabric-api ≥0.155.2、loader ≥0.19.3，均已升） |
| Superb Warfare | **无 Fabric 端** | 基准 `GunCommonUtil/GunClientUtil` 是 TaCZ/SBW 两路分发，恢复时剥掉 SBW 一路（未安装时那些分支恒 false，删除与基准行为一致） |
| ProxLib | `0.2.4+26.1` 有 Fabric，**但无 26.1.2 标签** | 当初不做是我们自己的裁决，不是生态问题 |

> ⚠️ **两条判据教训（本轮各栽一次）**
> ① **基准仓库 `build.gradle` 里注释掉的依赖行 ≠ 该模组没有 26.x**——Carry On 与 PatPat 都有正式版，
> 只是基准还没接回去。判据取模组自己的发布页。
> ② **「有 26.x 版本」≠「Fabric 上有」**——KubeJS / Aquaculture / Sophisticated Backpacks 的 26.x
> 全是 NeoForge。**查依赖必须查到加载器粒度**，只看游戏版本会得出相反结论。

## 3. 差异化清单

每条给出：内容 → 证据提交 → 新基宿主状态 → 交叉验证出处。
「宿主状态」写的是同名文件在 `origin/26.2` 里存不存在，**存在 = 要在新结构上重做，不存在 = 可直接搬**。

### A. 服务器规则体系与配置所有权（我们独有，无基准约束）

| 内容 | 证据 | 宿主 |
|---|---|---|
| 世界规则 / 个人配置 / 实例级 AI 规则三层拆分 | `c5060a194` `5ee4c81d7` `c545d82ac` | 新增为主 |
| 配置事务写盘 + `.last-good` 恢复 | `270532410` | 新增 |
| 站点保存即生效（补回上游本有的行为） | `48fe6db66` | 新增 |
| op / deop 后重发服务器规则 | `a04b810f7` | 已有 |
| **个人配置只在物理客户端建立与注册**（`Type.CLIENT` + 显式 `-global.toml`，专服不生成它与 `-ai.toml`） | `c5060a194`；本树 2026-08-17 补 | 宿主把四类混在 `-common.toml` 里 |

⚠️ **最后一行是 2026-08-17 才补的，此前「§3.A 已整块落地」这句话是不准确的**：三层拆分的
本体早就到位，但个人配置那一层留在了无守卫的 `Type.COMMON`（写法照宿主），专服因此会凭空
生成两份客户端配置文件。分歧当时只写在三处 javadoc 里，账本一字未提。它躲过下方 §7.6
三面闭合的方式，正是 §7.6 自己写明的盲区——**共有文件里的启动期接线**，故新增 §7.9 第四面。

交叉验证：[CURRENT_STATUS.md](CURRENT_STATUS.md)「我们独有」与「配置文件归属」两表、
`archive/2026-07/CONFIG_UI_REDESIGN.md`、`archive/2026-07/CLOSED_2026-07-23_TO_07-27.md`。
**这套子系统没有行为基准，可自由重构**——这是 26.x 上最好搬的一块。

### B. 智能应战 / 威胁响应（我们独有）

| 内容 | 证据 | 宿主 |
|---|---|---|
| 统一目标策略 `MaidTargetingPolicy` | `7d2b15f19` | **新基无同名文件** |
| 瞬态应战 Activity 仲裁 | `f7ea486bf` | 新增 |
| 每女仆响应策略（关闭 / 自卫 / 护主）+ 持久化 | `604927e30` `9dd7783f2` | 新增 |
| 任意手持物近战 | `bb5f847d4` | 已有 |
| **远程应战批（基准 2026-08-14 轮新增，行为基准已前移）**：应战活动接入三条远程行为（两件本 fork 早写好却零调用点的 `MaidAttackStrafingAnyItemTask`/`MaidShootTargetAnyItemTask` + `GunShootTargetTask`）、`MaidEmergencyWalkToTarget` 持远程武器 + 16 格内看得见目标即站定不贴脸、`isHoldingUsableRangedWeapon` 唯一判据（vanilla=`ProjectileWeaponItem`+能解析实现+`getProjectile` 非空）、**弓弩解绑工作任务**（`resolveImplementation` 第一步只认 `IRangedAttackTask` **不额外要求 isWeapon**——多要求会让「弓兵任务+手持弩」落错实现、模组远程武器全哑，基准分支红测实证）、三个射击任务 `stop()` 清 `swingingArms`（**该缺陷 origin 也有，此修复超出 origin，但在我们的行为基准上已定案带上**）。**手感参数照抄勿改**：走位速度 **0.5** 非 0.6、保留主人距离刹车（基准分支改错过被用户实测退回）。陷阱：`canUseNonMeleeWeapon` 实为「当前任务是不是远程任务」，不看物品弹药。刻意不动：近战闸门（既有用例钉着，用户明确不改）。**本树进度（2026-08-16）：整簇已落地。** B2 弓弩解绑 `c6aa20b24`；B3/B4/B6/B7 随 TACZ 刀；框架层（统一目标策略 / 瞬态应战 / 每女仆响应策略 / 配置屏）`5fc0baf4f`；B1 应战接线与 `MaidRangedEmergencyGameTest` 6 例 `be9c15fff`。敌我判定门已装回 `entity.passive.MaidCombatManager` 的 `doHurtTarget` 与 `performRangedAttack`。⚠️ 我们的应战管理器命名为 `entity.ai.combat.MaidEmergencyCombatManager`（不叫 MaidCombatManager）——宿主的同名类占着 `getCombatManager` 访问器，见下方警告 | `2e605d5c8` 及后续（基准区间 `373ad95f2..4c5bfe402` 全量 27 笔）；配套 `MaidRangedEmergencyGameTest` 6 条；全量清单 `archive/PORT_TACZ_AND_RANGED_AI_FULL.md` | 新增；「持枪寻路异常」**基准分支六组受控对照复现不出**（枪每组都不比其它武器差、交战组最稳），降级为「等用户复现现场」；半径耦合（索敌半径流进 BFS 与传感器盒）确认是**真实性能面**但非该症状成因，解耦属超基准候选未做 |
| **女仆配置屏重做**：`MaidConfigLayout` 布局引擎 + 重写 `MaidConfigContainerGui`（较宿主版 +64/−139 行），响应策略的配置行就住在这屏里 | `1f1b72105` `9dd7783f2`；`MaidConfigLayoutTest`（§9 台账 A 组，未搬） | 宿主是旧版屏，**无 `MaidConfigLayout`**。⚠️ 玩家可见（用户 2026-08-14 实机点名）；**必须与本簇同刀**——先搬屏就是绑不上后端的纸面界面 |

⚠️ **新基有一个同名不同物的 `MaidCombatManager`**：基准把 `EntityMaid` 的战斗逻辑抽进了
`entity.passive.MaidCombatManager`，与我们的 `entity.ai.combat.MaidCombatManager` **只是重名**。
移植时必须先读它，再决定我们的策略层挂在哪，**不要按名字合并**。
**2026-08-16 的处理结果**：宿主那个不仅重名，还占着 `getCombatManager()` 这个访问器名，
故我们的类落地为 `entity.ai.combat.MaidEmergencyCombatManager`（alias `emergencyCombatManager`，
经 `@MaidManagerDef` 生成 `getEmergencyCombatManager()`）。两者职责正交：宿主那个是战斗**执行**层
（盾牌、横扫、伤害事件、耐久），我们这个是威胁响应的**瞬态状态机**。

交叉验证：`archive/2026-07/CLOSED_2026-07-23_TO_07-27.md`（T1–T5）、
`archive/2026-07/POST_BETA_OVERRESTRICTION_AUDIT.md`。

### C. AI 聊天 · 站点 · TTS / STT（最大的一块）

80 个生产文件、5,488 增行，其中 58 个在新基上已存在。关键条目：

| 内容 | 证据 |
|---|---|
| 站点密钥留在服务端、默认不下发 | `feffe7736` `82745b102` |
| 站点「检查配置」与音色试听 | `12fb3e331` `cd435f4a3` |
| 世界默认 → 每女仆「跟随 / 覆盖」继承链 | `d62beb7bf` |
| 动作判定与执行搬出对话通道（先做后说） | `812571ef6` `1e4fb5afb` `979343d2d` `14ab7edb1` |
| 待合成文本改由不带历史的独立请求产出 | `ee8e9095c` |
| 渲染产物不再写进对话状态 | `893831008` |
| 本地提示不走 `ChatListener`（渲染线程冻结） | `644250596` |
| 站点文件损坏不再整批放弃 | `7707ae918` `feea39a10` `f0750d651` |
| 服务端按需下发 STT 凭据 / Velocity 常开 | `ea1c10ecb` `f51fc214f` |
| 开发环境对话驱动命令 | `4b0683905` |
| 15 种社区语言补齐 | `4fc61407d` |
| **AI 设置屏五页重做**（玩家可见，用户 2026-08-14 实机点名）：hub 侧边栏做权限边界并把 AI 配置收拢一处、用量页 `AIChatSettingsUsageScreen`（宿主无）、STT 由每女仆选站改**全局单选**（基准删了宿主还有的 `AIChatSettingsSTTSiteScreen`）、hub 共享暂存「任何一页保存都提交全部改动」契约（证伪表「三处写下契约代码没做」那条的主角，枚举测试钉着） | `92d473a15` `b4eb00235` `5ee4c81d7` |

交叉验证：1.21.11 分支账本 2026-07-28 / 07-30 两节，及那条分支的站点配置拓扑设计文档
（结论已复述于本节，按隔离纪律不跨分支指路）。
**移植风险最高的一块**：它依赖我们自己的配置三层与网络包，必须排在 A 之后。

### D. 上游缺陷修复批（有意的行为分歧）

⚠️ **下表 2026-08-17 逐条实查更新。此前它写「7 条全部待核」，那是过期的**——
`6f5d8f2a5`（缺陷采用批）与几把功能刀早已把其中五条带进来了。
照过期的表干活会重复移植，甚至把修复叠在已有修复上。

| 缺陷 | 基准证据 | 26.1.2 现状（2026-08-17 实查） |
|---|---|---|
| 炉子背包稀疏槽位错位 + 烧制进度归零 | `924d669b2` | **✅ 已落地**，随熔炉背包刀 `40b8cfde0` 带入 |
| 换手进食丢副手物 / 截走忠诚三叉戟 | `e215f9fca` | **✅ 已落地** `6f5d8f2a5`（按宿主管理器化结构适配进 `MaidItemManager`） |
| 女仆已回收时开界面崩客户端 | `c66ae62f4` | **✅ 已落地** `6f5d8f2a5`（#1058/#1059：只解引用一次并安全降级，把控制权交还既有 null 守卫） |
| 灭火剂漏灵魂火 | `52976d8b6` | **✅ 已落地** `6f5d8f2a5`（#1158：改判 `BaseFireBlock`，同时覆盖灵魂火与继承该父类的模组火） |
| 远程攻击模式切换必然崩服 | `9d8aac120` | **✅ 等价修复已在，且更彻底**——基准改的是 `configRange.get()` → 读口；本树宿主的 `targetConditionsTest` 已收**已解析的 int**，四个调用点全走 `ServerRuleConfig.get`（`GunRecognitionRangeTest` 与读点契约钉着） |
| 鞍抱起女仆专服无效 + 放下瞬移 | `c9763d19f` | **✅ 基准已自行修掉**（26.x 最后一笔提交） |
| 非法配置项让世界起不来 | `69a5d0383` | **⚠️ 半落地**：正则那半的守卫在（`MaidMealRegConfigEvent` 捕 `PatternSyntaxException` 并跳过该项）；另一半在 `RemainFoodEatenEvent`，**本树无该文件**——那个功能本身尚未移植，故当前**没有暴露面**。随该功能移植时必须同批带上，否则会退回「手改坏一个字符世界就打不开」 |

**结论：本类实际已基本闭合**，只剩「随 `RemainFoodEatenEvent` 移植时补一道守卫」这一个锚点。
§6 的建议顺序把 §3.D 排在下一项，那是按过期的表排的。

交叉验证：`archive/2026-07/CLOSED_2026-07-29_BATCHES.md`（含上游 issue 号与红测记录）。
**这批是唯一「明知与基准不同仍保留」的集合**，移植时逐条先核对新基现状，
**已被上游修掉的不要重复搬**，否则会与新实现打架。

### E. 寻路 · 工作任务 · 跟随手感

25 个文件、439 增行，**全部在新基上已存在**——这块没有一个新文件，
是最典型的「特色藏在共有文件的小改动里，diff 搬不过去」。

| 内容 | 证据 |
|---|---|
| `canPathReachExact`：只认精确节点，用于农场站位 | `2dc01a31d` |
| 主人跟随降级为兜底任务 | `51623bbad` |
| 农场站位目标限制在女仆 home 内 | `c6b1d59f1` |
| 农场收割 / 跟随手感 / 主人指令攻击 | `609546e1a` |
| 浅水泳姿稳定 | `57c7aafea` |

✅ **已整块落地（2026-08-17，`051aa37bb`…`e1e67db8a`）**，详情见
[CURRENT_STATUS.md](CURRENT_STATUS.md) 已关闭表。方法上有三条留给后来者：

1. **工作面是机械产出的**：先跑 wiki 的行为面指纹（Brain 活动→行为→优先级 的四树差集，
   方法见 §7.9 第四面），再逐文件做四树对照（A = 我们的旧基 − 上游旧基，B = 我们的新基 − 上游新基）。
   指纹照出的第一条就是「跟随优先级基准 3→4，本树仍是 3」——**同文件、同类、同方法，
   只有一个数字不同**，上面三种枚举全都照不出它。
2. **上表那 5 笔证据提交是下界**：机械枚举 `git log origin/1.21.1..port/1.21.11-fabric -- <路径>`
   当场多出 `348cd0056`（follow QA）。按提交主题分类的清单永远只是下界。
3. ⚠️ **空 diff 不是证据**：四树对照时 `git diff <ref>:<path>` 在某一侧文件不存在会失败，
   输出与「无差异」完全一样（`EntityMaidRenderState` 就栽在这里，它在 `client/renderer/entity/state/`）。
   逐条先用 `git cat-file -e` 证明四棵树都有那个路径。

⚠️ [CURRENT_STATUS.md](CURRENT_STATUS.md) 的「过度限制 C 类候选」里第 2、3 两条落在这块
（`canPathReachExact` 更严、爬梯需额外批准）——**本轮按纪律原样搬，未复核**。改与不改另开一轮。

### F. 渲染与标记

| 内容 | 证据 | 26.1.2 |
|---|---|---|
| 追踪标记搬 HUD（光影下可见）+ 颜色补 Alpha | `1b1838ceb` `3a5b9954c` `3ec94fc28` | ✅ 已落地（`3558c6410`） |
| 手办 / 坐垫物品渲染记忆化（创造栏与 JEI 卡顿） | `3f9f45c81` | ✅ 已落地（`5172a81b9`） |
| 河童罗盘 / 女仆范围可视化四项 | `cb7837b77` | ✅ 已落地（`7621c181f`），**审计原表漏列**，机械枚举补上 |
| YSM 挂件渲染 TLM 层（TLM 侧） | `14b380384` `9060014e3` | ⬜ 依赖 O4 前置（Fabric 26.1.2 上 YSM 不存在），且需 fork 侧 `renderTlmLayers` 双侧同时才有效 |

**两条根因的前提都在 26.1.2 反编译源上逐行复核过**，不是按补丁名推——
`LevelRenderer.addLateDebugPass` 仍在 `clearDepthTexture`；`SpecialModelWrapper` 仍把
`extractArgument` 的返回值 `appendModelIdentityElement`。两条都成立，所以两个缺陷在本树原样存在。

⚠️ **一条判定教训**：罗盘那笔的主题写着「移植回归」（我们自己在 1.21.11 造的），
按 §7.5「勿重做」本该跳过——但宿主从 1.21.1 迁到 26.1 是**另一次独立迁移**，
四项缺陷被独立重现。**「我们自己造成的回归」不等于「新宿主没有它」**，仍须逐条四树对照。

**2026-08-13 追加（发现方式不同，单列）**：扛女仆时玩家手臂摆抱姿
（`client.HumanoidModelMixin`）。这一条**不是我们的差异化**——`origin/1.21.1` 上就有，
1.21.11 只是按 render-state 重构改写过（`ICarryMaidRenderState` + `HumanoidRenderState`）。
代码宿主 `origin/26.1` 迁移期把它整段注释、`@Mixin` 靶点改指 `Dummy`、留 `FIXME`，
也没登记进 `mixins.json`，于是相对行为基准是一处回归。
由 `MixinRegistrationInvariantTest` 首跑照出，非人工阅读发现。
**已修（`79e7f7914`）**，但尚无运行期凭据——客户端 mixin 在纯服务端的 `runGametest` 里不加载。

⚠️ **这一条的意义超出它本身**：宿主在迁移期用「注释掉 + 靶点改指 `Dummy` + `FIXME`」的手法
搁置过不止一处（`compat.improvedmobs.EventHandlerMixin` 同款，只是它**登记了**所以闸门抓不到）。
**§3 各类在核对时，除了问「我们的东西搬了没」，还要问「宿主有没有把原本就有的东西搁置掉」**——
后者不会出现在任何差异化清单里，因为它本来就不是差异化。

### G. 第三方兼容

| 内容 | 证据 | 26.1.2 |
|---|---|---|
| Kaleidoscope Tavern（我们独有） | 见 `compat/kaleidoscopetavern` | ✅ 已落地（`2d24ece5a`） |
| 女仆不站上 Kaleidoscope 家具 | `c817ecec4` | ✅ 已落地（`e5eb3913c`）——审计原标「宿主已有」，实为**文件在、内容缺** |
| REI | 见 `compat/rei` | ✅ 已随反向缺口第三簇补回（`30d0d2795`） |
| MrCrayfish's Furniture: Refurbished（我们独有） | `5ff13874a` `dd2ccc483` | ⚠️ **判定为「无落点」**，见 CURRENT_STATUS 的 O9 |
| YSM 接管路径与数据层 | `5fdf46315` `d604795e0` `7ca21de41` | ⬜ 依赖 O4 前置（Fabric 26.1.2 上该模组不存在），且需 fork 侧同时就位 |

**一条对排期有效的教训**：酒馆兼容的 7 个文件只是一半——它依赖两个**我们自己加的核心扩展点**
（`IExtraMaidBrain#canClimbBlock`、`MaidMealManager#addWorkMealExclusion`，上游两棵树都是 0），
而那两处此前没搬。**「搬兼容」的实际工作量常常在核心那一侧**，按兼容包的文件数估工时会失准。

⚠️ 家具重制那条附带的**注册表 SYNCED 改动**仍未判断（代价是服务端有客户端缺失的序列化器时
会明确断开）。判断它需要那个模组的 jar——它只发 GitHub Releases，本机没有，**证据不足不下结论**。

### H. 命令

`/tlm config` 全套（基准只有 `/tlm ai_chat`）、`ai_chat reload` 收拢、
`ai_chat status` / `sites`：`28f180fa6` `48fe6db66` `a2a3d608f`。4 个文件、365 增行。

### I. 玩法微调（有意分歧）

桌上食物奖励 1~3 点 + 冷却、偷吃持有目标限时让位：`6d5ea732b` `588e9e6f3` `94883be7e` `3cb53e74e`。
交叉验证：`archive/2026-07/TABLE_FOOD_DESIGN.md`。

### K. 祭坛配方的客户端同步（我们独有）

1.21.11 起**客户端不再持有 `RecipeManager`**，自定义 `RecipeType` 不会同步过去。
我们为此加了 `SyncAltarRecipesPackage` + `ClientAltarRecipeCache`，JEI / REI / Patchouli
三个展示面都靠它。证据 `a74d27f2c`；**新基没有这两个文件**。

⚠️ 26.1.2 仍有同一条原版限制，因此这条**必须带过去**；同时要先读新基在没有这套同步的情况下
是怎么显示祭坛配方的——它可能有另一套做法，两者不能叠加。

交叉验证：[CURRENT_STATUS.md](CURRENT_STATUS.md)「我们独有」第 5 项。
**这一条是三面交叉验证抓出来的**：按提交前缀分类时它被漏掉了（见 §4 的方法缺陷）。

### J. 测试与门禁设施

47 个 GameTest / 单元测试文件属于差异化的一部分：注册完整性不变量、payload 不变量、
reload 接线契约、GUI lang 键覆盖、上游缺陷七条红测。
**它们比功能代码更该先搬**——移植期正是最容易静默丢功能的时候。
⚠️ 新增 GameTest 类必须同时登记 `fabric.mod.json` 的 `fabric-gametest` entrypoint，否则永不执行。

## 4. 量化与重算方法

测量口径：以 `origin/1.21.1` 为基准，取 `origin/1.21.1..HEAD` 全部 386 笔提交，
剔除迁移期提交（`feat(client)` / `feat(datagen)` / Node / Phase / Renderer Knot / Batch 等）
与移植回归修复（`fix(port)` / `fix(mixin)` / `fix(fidelity)` / `fix(client): restore` /
标题含「移植」「TODO 审计」等），**剩余 89 笔判为差异化**，取其触及的 `src/` 文件并集，
排除 `src/main/generated`（datagen 产物，可重新生成）。

> ⚠️ **这个口径有一处已知缺陷，不要单独使用**：它按 conventional 前缀筛选，
> 而本分支有 **39 笔提交没有前缀**（`Add typed Kaleidoscope Tavern compatibility`、
> `Complete semantic audit fixes and altar recipe sync` 等），**被整批丢掉**。
> §3.K 就是三面交叉验证才补回来的。下表的行数因此是**下界，不是全集**；
> 完整性以 §7.6 的三面闭合为准。

| 领域 | 生产文件 | 增行 | 删行 | 新基已有 |
|---|---|---|---|---|
| AI 聊天 · 站点 · TTS/STT | 80 | 5,488 | 1,740 | 58 |
| 配置所有权 / 持久化 | 20 | 2,594 | 265 | 5 |
| 其它（实体 / 物品 / init / mixin） | 141 | 1,673 | 10,740 | 101 |
| 战斗 / 威胁响应 | 5 | 820 | 44 | **0** |
| 第三方兼容 | 80 | 708 | 5,226 | 26 |
| 渲染 / 标记 / YSM | 17 | 534 | 46 | 12 |
| 寻路 · 工作任务 · 跟随 | 25 | 439 | 103 | **25** |
| 命令 | 4 | 365 | 92 | 2 |
| 网络包 | 7 | 49 | 40 | 5 |
| GUI（非 AI） | 17 | 41 | 69 | 13 |
| **合计（不含测试 47 个文件）** | **396** | **12,711** | **18,365** | **247** |

「删行」偏大主要来自归档第三方兼容与移除上游模块，**那部分在新基上多半不必重做**——
新基有它自己的兼容集合，删什么要重新判断。

重算（判定规则写在脚本注释里，改判据请改脚本而不是改本表）：

```powershell
git fetch origin
git rev-list --count origin/1.21.1..HEAD          # 提交总数
git merge-base origin/1.21.1 origin/26.2          # 应为空
git diff --shortstat origin/26.1 origin/26.2 -- src
```

## 5. 结构性障碍（决定「重做」而非「搬运」）

1. **无共同祖先**（§1）。`cherry-pick` / `rebase` / `format-patch` 全部无效。
2. **`EntityMaid` 已被拆解**：基准 → 26.2 是 294 增 / 2,345 删，逻辑迁进多个 manager。
   我们对它的 572 增 / 380 删**没有落点**，必须按新结构重新分配。
3. **新增 `modules/maid-manager-codegen` 代码生成模块**：manager 类的写法变了，
   我们新增的 manager 要按它的约定写，否则风格与生成物冲突。
4. **26.2 带 `patches/` 与 `rewrite.yml`**（OpenRewrite）：基准自己用自动化做跨版本迁移，
   **值得先读懂再动手**，可能省掉大量手工改写。
5. **datagen 产物不要手搬**：`src/main/generated` 在新基上重新生成。

## 6. 建议顺序

0. **探路轮**（先做，用来校准工期）：只搬服务器规则体系的最小闭环（§3.A 前两行）+ 一条 GameTest，
   量出真实工时与冲突形态，再排整体计划。
1. §3.J 测试与门禁设施 → 2. §3.A 配置三层 → 3. §3.B 威胁响应 → 4. §3.H 命令 →
   5. ~~§3.C AI（最大，依赖 A 与 H）~~ **已整块落地（2026-08-17，含 GUI 五页）** →
   6. ~~§3.D 上游缺陷~~ **2026-08-17 逐条实查：实际已基本闭合**（七条里五条早已随
   `6f5d8f2a5` 与功能刀落地、一条基准自行修掉、一条半落地且当前无暴露面）——
   **原先「全部待核」的表是过期的，照它干活会重复移植** →
   7. ~~§3.E 寻路手感~~ **已整块落地（2026-08-17）** →
   8. ~~§3.F 渲染~~ **三项里两项已落地（2026-08-17）**，YSM 那项依赖 O4 前置 →
   9. ~~§3.G 兼容~~ **可做的都已落地（2026-08-17）**：森罗家具避让、酒馆兼容、REI；
   家具重制判定为「无落点」，YSM 等 O4。

   **§3 的可做项至此全部走完。** ②**O8 行为面对账**已于 2026-08-18 五面跑完（见 §7.9），
   只产出一个真缺口且已修。剩下两条（由用户定夺）：
   ① **实机验收**——§3.E/§3.F/§3.G 三节已过单人档，§3.C 那一大批仍欠真站点密钥，专服侧整体仍开放；
   ③ **O9 裁决**（无线 IO 绑定面比基准宽）与 O4 前置项目（YSM 的 26.1.2 移植，规模未评估）；
   另有一个便宜的待裁决项 `create:automation_ignore`，见 `docs/CURRENT_STATUS.md` 的 O8。
2. 1.21.11 分支继续维护，直到 26.x 稳定并完成实机验收。

## 7. 移植边界

用户 2026-08-13 要求：**先明确边界，不要有任何遗漏**。本节是范围声明，§7.6 给完整性论证。

### 7.1 范围内 —— 必须带到 26.1.2

§3 的 A–K 十一类**全部在内**，逐类对应关系见 §7.6 的三面矩阵。按可搬性分两档：

- **可直接搬（新基无同名文件，149 个）**：威胁响应全套、配置三层与事务写盘、AI 站点与继承链、
  `/tlm config` 命令族、Kaleidoscope Tavern 兼容、Refurbished 兼容、REI 集成、
  祭坛配方客户端同步、全部 GameTest 与契约测试。
- **必须在新宿主重做（新基已有同名文件，247 个）**：寻路与工作任务手感、`EntityMaid` 上的一切、
  渲染与标记、GUI 布局、网络包注册、上游缺陷修复批。

### 7.2 范围内但依赖前置项目（先做前置，否则该块作废）

| 特色 | 前置 | 现状 |
|---|---|---|
| YSM 自定义模型 + 挂件渲染 | Fabric 上要有 YSM 实现 | **26.1.2 上不存在**：本体仅 NeoForge 且闭源；OpenYSM 无 26.x 发布。**要保留就得我们把 `gege-tlph/OpenYSM-Updated` 移植到 26.1.2**，那是独立仓库的独立项目，规模未评估 |
| Patchouli 手册 | Patchouli 26.1.x | 官方有 `26.1-94-beta`，**我们维护的 fork 需跟到这一版** |

### 7.3 范围外 —— 生态不允许（§2.2 实查，非我们选择）

EMI · Accessories（连带 Sophisticated Backpacks / Traveler's Backpack / ExtraContainer 三项）·
Immersive Melodies · Simple Hats · Ponder · Improved Mobs · Just More Cakes · SlashBlade ·
Superb Warfare · Iron Chests · The One Probe · Embeddium · KubeJS · Aquaculture。

**2026-08-14 移出一项：TACZ**——TaCZ Refabricated 有 26.1.2 分支构件（§2.2 专门行），
兼容恢复回到范围内，账本 20 行枪械条目中 14 行已改判「丢失」（swarfare 6 行维持生态）。

**重要更正**：「1.21.11 上放弃的兼容现在很多重新可用」这个前提**经实查不成立**。
逐条查到加载器粒度后，这批里当时**没有任何一个**在 26.1.2 的 Fabric 上可用
（**2026-08-14 更新：TACZ 成为唯一例外**，经非官方 Refabricated 仓库的 26.1.2 分支）；
KubeJS / Aquaculture / Sophisticated Backpacks 确有 26.x，但**全是 NeoForge**。
真正"重新可用"的是那些我们**本来就已经兼容**的（Carry On、PatPat、Kaleidoscope Tavern、
Patchouli beta、Refurbished），它们在 1.21.11 上就没断过。

### 7.4 范围外 —— 我们自己的裁决（生态允许但不做）

- **ProxLib**：有 `0.2.4+26.1` 的 Fabric 版（无 26.1.2 标签），当初不做是我们的决定，维持。
- **26.2**：本次不开工。

### 7.5 明确不带过去的

- `src/main/generated` 全部 datagen 产物 —— 在新基上重新生成，手搬必错。
- 1.21.11 专属的移植修复：`fix(port)` / `fix(mixin)` / `fix(client): restore` 那一批，
  它们修的是我们自己在 1.21.11 上造成的回归，新基没有这些回归。
- 已被基准自己修掉的上游缺陷（当前已知：鞍抱瞬移）——**逐条复核后才能划掉，不要凭这一条外推**。
- **1.21.11 的一切文档与账本**（见 §7.7 隔离要求）。

### 7.6 完整性论证（凭什么说没有遗漏）

**单靠提交分类不可靠，本轮已实证**：按 conventional 前缀分类会把 **39 笔无前缀提交整批丢掉**，
祭坛配方客户端同步（§3.K）就是这么漏的。因此完整性改由**三面闭合**保证：

| 面 | 枚举方式 | 性质 |
|---|---|---|
| **文件面** | `origin/26.2` 树里不存在、而我们有的文件（149 个） | **机械可重算**，给"我们凭空造的东西"一个完整下界 |
| **文档面** | 1.21.11 分支账本的「我们独有」5 项 +「有意的行为分歧」4 项 + 其兼容状态账本 + 归档的已关闭条目 | 覆盖藏在共有文件里的行为改动 |
| **公开面** | 公开分支 README 对玩家承诺的每一条特色（3 条改进 + 2 项新玩法 + 9 条上游缺陷修复） | 反向兜底：**承诺过的都不能丢** |

三面已逐条对齐，结果：**文档面的 5 项「我们独有」全部落在 §3 的 A / B / G / K 中**
（祭坛配方那项此前缺失，本轮补入）；**公开面 9 条上游缺陷全部有落点**（6 条在 §3.D、
3 条在 §3.C）；文件面的 149 个文件按目录归入 §3 各类。

⚠️ **本审计不保证"行为面"绝对完备**：共有文件里的小改动无法机械枚举（寻路那 25 个文件即是）。
补救是**移植时按功能验收，不按 diff 清点**——每类搬完跑对应 GameTest + 实机验收，
而不是核对文件数对不对。**这条警告 2026-08-17 被实证不够用**，故加第四面，见 §7.9。

### 7.7 工程隔离要求（用户 2026-08-13 追加）

**1.21.11 与 26.1.2 的代码、资源、文档、账本一律不得混放，本地也不行。**

| 维度 | 要求 |
|---|---|
| 分支 | 新建 `port/26.1.2-fabric`，**父提交取 `origin/26.1`**。两者与 1.21.11 分支无共同祖先，本来也合不了 |
| 工作目录 | **独立目录**，不复用 1.21.11 的工作树；避免 `build/` 与 loom 缓存互相污染 |
| 文档 | 26.1.2 的 `docs/` 只存在于新分支，自带 `CURRENT_STATUS` / `HANDOFF`；**两边账本不交叉引用** |
| 发布 | 公开仓库另开分支，标签 `v<版本>+mc26.1.2`，Release 与 1.21.11 分开 |
| 可共享的 | **只有方法论与门禁工具**（`AGENTS.md` 纪律、`docs/tools/` 脚本）——以**副本**方式带过去，不做跨分支引用 |
| 禁止 | 跨分支 cherry-pick（无共同祖先，技术上也不成立）· 同一工作树切换分支跑 Gradle |

⚠️ **本审计文档自身也受这条约束**：它现在在 1.21.11 分支上，属于「决定要不要开工」的产物。
**新分支建立后应迁往新分支**，1.21.11 这边只留一行指路。

## 7.8 反向缺口：**代码宿主自己在迁移时丢了什么**（2026-08-13 新开）

⚠️ **§3～§7 全部只回答一个问题：「我们多出来的部分搬齐了没」。**
它们回答不了另一个问题：**宿主把 `origin/1.21.1` 原本就有的东西丢了多少**。
那类缺口**不会出现在任何差异化清单里**——因为它本来就不是差异化。

工具：`docs/tools/host_gap.py`（可重跑，结论随 ref 更新自动重算）。

### 首轮结果（2026-08-13）

`origin/1.21.1` 1578 个 java 文件 → `origin/26.1` 1635 个。路径级消失 318 个，
扣掉「同名类换包」40 个与「`TileEntity*`→`BlockEntity*` 改名」16 个，**残余 262 个**。
其中 37 个属 §2.2 当时裁决的生态不允许（枪械 / EMI / Immersive Melodies / Ponder 等；**TACZ 14 行已于 2026-08-14 改判回「丢失」**，见 §2.2），
**剩 225 个需人工逐条判定**。

⚠️ **225 不等于 225 处回归。**已抽样确认至少三种「不是回归」的形态：
- **架构替换**：JS 动画（`CustomJsAnimationManger` / `EntityMaidWrapper` / `GlWrapper`，Nashorn）
  被 `client/animation/gecko/molang/` 那套绑定取代；
- **内部重构**：`geckolib3` 删了 25 个类，但该包在 26.1 仍有 **218 个**文件；
- **库合并**：`simplebedrockmodel` 8 个类（`build.gradle` 注释写着「又合并回来了」）。

### 判定结果（2026-08-14，222 条全部登记）

账本 `docs/tools/host_gap_ledger.tsv`，`host_gap.py --ledger` 双向核对，覆盖率 222/222。

| 判定 | 条数 | 含义 |
|---|---|---|
| 替换 | 76 | 功能还在，换了实现/位置，承接者已写进账本 |
| 生态 | 48 | 对应模组在 26.1.2 Fabric 上不存在（§2.2） |
| **丢失** | **42** | **没有承接者，相对行为基准真少了东西 —— 要干的活** |
| 待定 | 55 | 子代理报「未找到」而主模型尚未复核，**不写成结论** |
| 无关 | 1 | 1.21.1 上已无引用者的死代码 |

### 42 条「丢失」归成五簇（按玩家可见程度）

| # | 簇 | 条数 | 玩家侧后果 |
|---|---|---|---|
| 1 | **女仆背包四型**：工作台 / 末影箱 / 熔炉 / 液体 | 15 | 这四种背包**整个玩法没了**。含背包本体、容器、GUI 屏、数据、物品、流体渲染与工具、容量战利品函数、液体量同步包 |
| 2 | **棋局存档与记录层** | 8 | **棋还能下**（方块/方块实体/AI 都在），但棋局存不进物品、没有胜负记录、没有数据包棋谱、没有随机棋局战利品 |
| 3 | **REI 集成** | 5 | 祭坛配方在 REI 里查不到、不能一键填入女仆背包。⚠️ REI 在 26.1.2 上**有正式版**，是宿主把依赖注释了 |
| 4 | **原版替换功能** | 4 | 史莱姆/岩浆怪不再变 Yukkuri、经验球不再变点符；五个开关连同 `VanillaConfig` 整个没了 |
| 5 | **模型图标缓存** | 4 | 模型预览图标不再缓存；`MiscConfig.MODEL_ICON_CACHE` 开关一并消失 |
| — | 零散 | 6 | GIF 表情纹理、Carry On 抱起女仆的渲染修正等 |

**第 1、2 簇是最要紧的**：它们是玩家能直接察觉的完整玩法，且与生态无关——纯粹是宿主迁移时丢的。

### 已确认的真实丢失（首轮抽样，非全集）

| 丢了什么 | 证据 | 玩家侧后果 |
|---|---|---|
| **4 种女仆背包**：工作台 / 末影箱 / 熔炉 / 液体 | `entity/backpack/` 在 `origin/1.21.1` 与行为基准上都是 **9 个类型**，`origin/26.1` 只剩 5 个（Empty/Small/Middle/Big）；配套的 `data/FurnaceBackpackData`、`data/TankBackpackData` 与四个 `*BackpackContainerScreen` 一并消失 | 这四种背包**整个玩法没了** |
| 扛女仆时玩家手臂抱姿 | 见 §3.F | 已修 `79e7f7914` |
| 女仆喂主人喝东西的音效 | `TaskFeedOwner.feed()` 被注释 | 已修 `6dfb3357e` |

### 方法教训

首轮是派子代理做机械扫描 + 主模型逐条复核完成的，**复核推翻了子代理的多处结论**：
子代理报「278 个真正删除」，实为 262（它的改名过滤漏了 `TileEntity→BlockEntity` 约定）；
它给每条都写了「Impact: 整个 XX 系统缺失」这类断言，而抽样发现相当一部分是架构替换。
**子代理适合产出候选与证据，不适合下判定**——这条本仓库证伪表早有记载，本轮再次应验。

## 7.9 行为面对账：三面闭合的第四面（2026-08-17 新开）

**开这一面的直接原因**：用户问「1.21.11 整改过的混乱配置文件，会不会被误判阻塞」。
一查就发现个人配置那一层没搬（详见 §3.A 的警告框），而 §7.6 的三面**全部无信号**——
文件两树都有、不在「我们独有」清单、README 不承诺配置文件名。它没被丢，
**靠的是有人留了三处 javadoc**；靠人不靠闸，就是迟早会丢。

### 判据：枚举基准的「可观察行为契约」，逐条与本树对账

不是比 diff（无共同祖先，比不了），也不是比文件（共有文件天然无信号），
而是问：**这个子系统对外表现出哪些「谁在什么条件下会看到什么」的承诺？**
配置面的契约就是五项：**文件名 / 生成条件 / 注册类型 / 环境守卫 / 每个键的所有权**。

对账要机械可重算，不能靠读。配置面用的两支脚本形态（一次性，未入库；
判据写在下方，重跑时按这个形状重写即可）：

1. 解析两树各 spec 类的 build 方法 → 它调了哪些 `init*` → 每个 `init*` 里的
   `builder.…define*("键名")` → 得到「键 → spec → 文件 → 守卫」，两树对齐。
2. 对「要迁进 CLIENT-only spec」的每个字段，扫**全仓**源码找消费点，逐个分侧。

⚠️ 两条经验，重跑时直接复用：
- **正则要覆盖链式写法**：`RenderConfig` 是 `builder.translation(..).define(..)`，
  只匹配 `builder.define` 会让它 10 个键**一个都扫不到**，而结果看起来像「没有差异」。
- **活性判据要与结论正交**：脚本自报「扫了几个文件 / 认出几个 spec / 抓到几个键」。
  「没有差异」与「解析器坏了」在输出上完全一样。

### 配置面结果（2026-08-17）

87 个键名：**归属等价 57**（世界规则与实例级 AI 规则两层逐条对齐，只是方法名不同）·
**归属不同 26**（全是个人配置那一族，已补）· **单侧 4**：
`EnableMaidCurios` 仅本树（宿主新增，实查有三处服务端读点，**故意留在 `-common.toml`**）、
`MaidTamedItem`/`MaidTemptationItem` 仅基准（宿主改用物品标签，有意差异，不补）、
`SmoothFollow` 仅基准（配置项要与消费者 §3.E 同批落地）。
站点 JSON 目录与 `AtomicConfigFileWriter` 两树逐字相同。

### 长期看管

`ConfigBootstrapOrderContractTest`（次序 / 守卫块内 / 注册类型与文件名 / 迁移表完整性）+
`ClientOnlyConfigReadContractTest`（按成因扫全仓，允许表要求**具名写明是谁把它拉起来的**）+
`MagmaCubeConfigInheritanceTest` 的键集对账。**七种缺陷形态逐个红测过。**

### 五个面的结果（2026-08-18 全部跑完）

配置面是第一个，其余四面于 2026-08-17/18 跑完。**脚本全部入库 `docs/tools/o8/`（10 支），可重跑**；
每支的判据、活性自报与**已知盲区**写在各自文件头，别只看结论。

| 面 | 脚本 | 契约 | 结论 |
|---|---|---|---|
| 配置 | （一次性，形态见上） | 文件名 / 生成条件 / 注册类型 / 环境守卫 / 键所有权 | 已闭合，见上 |
| 网络包 | `net_face` `net_gap` `net_guard` `net_recipients` | 线上 id / 方向 / 注册完整性 / 收件人策略 / C2S 守卫 | 无回归：id 差 5 条逐条定案、方向差 0、注册异常 0、策略差 1（宿主改进）、守卫差 0 |
| 存档与 attachment | `attach_face` `nbt_face` `nbt_keys` | id / persistent / syncWith / copyOnDeath / initializer；NBT 键全集 | 无数据丢失：13 个 attachment 无一项能力少于基准 |
| 资源 | `res_face` `lang_face` | 携带哪些资源文件；玩家可见文本 | 基名对差 12 条（11 条改名/拆分/生态）；**lang 键差 0** |
| GUI | `gui_face` | 菜单注册 id / 屏绑定完整性 / `openMenu` 守卫 / 屏可达性 | 无回归：id 差 0、14/14 绑定、守卫无一处变弱、可达性差 0 |

**产出的唯一真缺口**：进食后归还容器那条链被宿主整条丢掉，配置键与配置菜单却都留着（详见
`docs/CURRENT_STATUS.md` 的 O8 与提交 `d9db0d754`）。它是这一族的典型形态——
**「载体还在、行为没了」**，而文件面 / 文档面 / 公开面三面对它全部无信号，正是第四面存在的理由。

⚠️ 「无回归」的适用范围是**这五个面各自的契约**，不等于该子系统整体无问题；
各脚本文件头写明了自己的盲区（例如 lang 只认字面量键、GUI 面的守卫按词类归类而非语义求值）。

## 8. 剩余未决

1. **YSM 的 26.1.2**（§7.2）：`gege-tlph/OpenYSM-Updated` 要不要开 26.1.2 移植？
   这是独立仓库的独立项目，**规模未评估**——需要先单独量一轮，不能顺带做。
2. **「过度限制」C 类候选**（§3.E）：移植期已按建议**原样搬**（2026-08-17），复核仍未做，另开一轮。
3. **隔离粒度**（§7.7）：独立 worktree（共享 `.git`）够不够"干净分离"，还是要独立 clone？
4. **祭坛配方**：新基在没有我们那套同步的情况下怎么显示配方？动手前必须读，避免两套叠加。

## 9. 测试搬运台账

**这张表是「不遗漏」的可检验凭据**：1.21.11 分支上的测试类逐个登记，搬一个勾一个。
测试**随各自功能一起搬**（它们引用差异化代码，功能没到就编译不过），但**不得静默漏掉**。

⚠️ 每搬入一个 GameTest 类，**必须同时登记 `fabric.mod.json` 的 `fabric-gametest` entrypoint**，
否则编译打包启动全正常、用例一次都不跑——1.21.11 上实证过。

⚠️ **测试的 `workingDir` 是 `build/test-working`**：凡读文件的用例都要 `Path.of("..", "..")`
回到项目根。搬用例时若照抄了相对路径却漏了这一层，症状是「找不到文件」而不是断言不成立，
容易被误读成"这条断言在新基上不成立"。

⚠️ **判据是「报告里出现了那个用例」**，不是「任务跑绿」：空的 `:test` 报 `NO-SOURCE` 同样是绿的。
`BuildInfrastructureSmokeTest` 作为常驻探针留在树里，就是为了让"测试层被跳过"这件事可见。

共 41 个 JUnit + 14 个 GameTest = 55 个测试类

本分支另有**不在这 55 个里**的新增用例（1.21.11 上没有对应物，故不占台账条目）：
`ServerRuleReadRoutingContractTest`（读点唯一性，3 例）、`WorldRuleGameTest`（世界规则全链路，GameTest）。
- [x] `HubSharedStagingContractTest.java` —— O7 新增：hub「任何一页保存都提交全部改动」这条
  **全称契约**的按屏枚举测试。识别依据取「覆写没覆写 `addFooterButtons`」（结构量），
  不取按钮标签字面量；自带「认出了几屏」的下限断言，否则识别依据一变就静默零覆盖（`fcbd03296`）

**A 配置所有权与持久化**（12）
- [x] `AiConfigFileMigrationTest.java` —— 本树源链只有 -common.toml，回落用例改由服务端侧覆盖（`fbb360d39`）
- [x] `AiServerRuleAttackTest.java` —— 改用就地引导 + 列表型世界规则验同一条 4096 防线（`fbb360d39`）
- [x] `AiServerRuleMigrationTest.java` —— 加一条「坏源跳过而非整批放弃」（`fbb360d39`）
- [x] `AtomicConfigFileWriterTest.java` —— 逐字搬入、零修改（`8c439de1a`）
- [x] `ConfigPayloadCodecTest.java` —— 搬入并加一条 Sync 包往返（`0e54b51ac`）
- [ ] `GlobalConfigMigrationTest.java`
- [ ] `MagmaCubeConfigInheritanceTest.java`
- [ ] `MaidConfigLayoutTest.java`
- [x] `RuleStagingSessionTest.java` —— 断言对象由 AI 值改为世界规则值，语义不变（`0e54b51ac`）
- [x] `ServerRuleConfigTransactionTest.java` —— 搬入并加一条运维参数不进公开快照（`9354189d9`）
- [x] `ServerRulesSaveAuthorityContractTest.java` —— 去掉 AI 店那一半，加一条「激活与否按服务器形态判定」（`0e54b51ac`）
- [x] `WorldRuleTestHarness.java` —— 两个店一起搭（`0d5124259`）

**B 战斗 / 威胁响应**（3）
- [ ] `MaidCombatIntentGameTest.java`（GameTest）
- [ ] `MaidEmergencyCombatGameTest.java`（GameTest）
- [ ] `MaidTargetingPolicyGameTest.java`（GameTest）

**C AI 聊天·站点·TTS/STT**（24）
- [x] `AvailableSitesTransactionTest.java` —— 引导改 AiClientConfig/AiServerRuleConfig（`4137ea764`）
- [x] `ClientLocalChatContractTest.java` —— 判据改 addClientSystemMessage（26.1.2 按来源拆了入口）（`0d5124259`）
- [x] `DedicatedSiteFilesGameTest.java`（GameTest） —— 逐字搬入，entrypoint 已登记（`4137ea764`）
- [x] `GameplayKnowledgeContractTest.java` —— 锚点表与数值事实表按已落地范围收窄（`0d5124259`）
- [x] `GuiLangKeyCoverageTest.java` —— 首跑报两条假阳性（拼接前缀被当完整键），已改「模板键与以 . 结尾的片段一律跳过」，并写明它给的是**下界**（`fcbd03296`）
- [x] `LLMSitePersistenceRegressionTest.java` —— 逐字搬入（`4137ea764`）
- [x] `MaidChatFollowResolutionTest.java` —— 逐字搬入（`0d5124259`）
- [x] `MaidChatHistoryNormalizationTest.java` —— 逐字搬入（`0d5124259`）
- [x] `MaidContextsGameTest.java`（GameTest） —— setDayTime 已删 → 改断言上下文与真实日程一致；桌上食物三条留锚点（`0d5124259`）
- [x] `MaidControlToolsGameTest.java`（GameTest） —— 背包走 Fabric transfer；首跑红 5 条照出两个工具类缺三项加固（`0d5124259`）
- [ ] `MaidFarmNavigationGameTest.java`（GameTest）—— ⚠️ **归类有误，实属 §3.E（在 entity/ai/brain/task，依赖寻路手感，非 AI 聊天）**
- [ ] `MaidFollowOwnerGameTest.java`（GameTest）—— ⚠️ **归类有误，实属 §3.E（同上）**
- [ ] `MaidTableFoodGameTest.java`（GameTest）—— ⚠️ **归类有误，实属 §3.I（桌上食物，非 AI 聊天）**
- [x] `MissingTtsPartIsNotSilentContractTest.java` —— 逐字搬入（`0d5124259`）
- [x] `ResponseChatSplitTest.java` —— 逐字搬入（`0d5124259`）
- [x] `STTSiteCredentialsTest.java` —— 逐字搬入（`4137ea764`）
- [x] `SettingsPopupWiringContractTest.java` —— 顺序断言缩到方法体（大括号配对截取）（`fcbd03296`）
- [x] `SiteCheckResultWiringContractTest.java` —— 按「谁发 CheckSiteConfigPackage」枚举（`fcbd03296`）
- [x] `SiteEditorLayoutTest.java` —— 纯 int 常量，无客户端可跑；红测装回「照抄邻居宽度 96」（`fcbd03296`）
- [x] `SiteSecretRedactionTest.java` —— 补一条独立取证的字段表断言——原用例是自证式的（`4137ea764`）
- [x] `SkillLoaderPriorityTest.java` —— 逐字搬入（`4137ea764`）
- [x] `ToolDispatchWiringContractTest.java` —— 逐字搬入（`0d5124259`）
- [x] `UseSkillToolContractTest.java` —— 逐字搬入（`0d5124259`）
- [x] `VoicePreviewRequestValidationTest.java` —— 逐字搬入（`f74380bc1`）

**E 寻路·任务·跟随**（2）
- [ ] `MaidSwimmingGameTest.java`（GameTest）
- [ ] `ScheduleGameTest.java`（GameTest）

**F 渲染与标记**（2）
- [ ] `GeckoMaidLayerContractTest.java`
- [ ] `VanillaReplaceRendererWiringContractTest.java`

**G 第三方兼容**（4）
- [ ] `OptionalCompatInitializationGameTest.java`（GameTest）
- [ ] `PatchouliBookRecipeGuardTest.java`
- [ ] `RefurbishedFurnitureCompatContractTest.java`
- [ ] `YsmStandaloneIsolationContractTest.java`

**H 命令**（2）
- [x] `AiReloadWiringContractTest.java` —— 逐字搬入（`f74380bc1`）
- [x] `TlmCommandSmokeGameTest.java`（GameTest） —— 逐字搬入，entrypoint 已登记（`f74380bc1`）

**J 通用门禁不变量**（2）
- [x] `PayloadRegistrationInvariantTest.java` —— 四个新 payload 漏登记正是它抓的形态（`f74380bc1`）
- [ ] `RegistrationInvariantTest.java`

**Z 其它**（4）
- [ ] `ClientAltarRecipeCacheSourceContractTest.java`
- [ ] `GameModeUtilPermissionTest.java`
- [ ] `MaidSubConfigPayloadCodecTest.java`
- [ ] `MaidUpstreamBugFixGameTest.java`（GameTest）


**核对方法**（每轮收尾跑一次，防止台账与现实脱节）：

```powershell
# 本分支已有的测试类数
(Get-ChildItem -Recurse src/test -Filter *.java).Count
(Get-ChildItem -Recurse src/main -Filter *GameTest.java).Count
```
