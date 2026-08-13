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
| Immersive Melodies · Simple Hats · Ponder · Improved Mobs · Just More Cakes · TACZ · SlashBlade · Superb Warfare | **无任何 26.x** | |
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

⚠️ **新基有一个同名不同物的 `MaidCombatManager`**：基准把 `EntityMaid` 的战斗逻辑抽进了
`entity.passive.MaidCombatManager`，与我们的 `entity.ai.combat.MaidCombatManager` **只是重名**。
移植时必须先读它，再决定我们的策略层挂在哪，**不要按名字合并**。

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

交叉验证：1.21.11 分支账本 2026-07-28 / 07-30 两节，及那条分支的站点配置拓扑设计文档
（结论已复述于本节，按隔离纪律不跨分支指路）。
**移植风险最高的一块**：它依赖我们自己的配置三层与网络包，必须排在 A 之后。

### D. 上游缺陷修复批（有意的行为分歧）

| 缺陷 | 证据 | 26.x 是否已修 |
|---|---|---|
| 炉子背包稀疏槽位错位 + 烧制进度归零 | `924d669b2` | **待核** |
| 非法配置项让世界起不来 | `69a5d0383` | **待核** |
| 换手进食丢副手物 / 截走忠诚三叉戟 | `e215f9fca` | **待核** |
| 女仆已回收时开界面崩客户端 | `c66ae62f4` | **待核** |
| 灭火剂漏灵魂火 | `52976d8b6` | **待核** |
| 鞍抱起女仆专服无效 + 放下瞬移 | `c9763d19f` | **✅ 基准已自行修掉**（26.x 最后一笔提交） |
| 远程攻击模式切换必然崩服 | `9d8aac120` | **待核** |

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

⚠️ [CURRENT_STATUS.md](CURRENT_STATUS.md) 的「过度限制 C 类候选」里，**第 2、3 两条正落在这块**
（`canPathReachExact` 更严、爬梯需额外批准）——**它们至今未逐条复核**。
移植是重新审视它们的时机，但**别在移植期顺手改行为**：先原样搬，改与不改另开一轮。

### F. 渲染与标记

| 内容 | 证据 | 宿主 |
|---|---|---|
| 追踪标记搬 HUD（光影下可见）+ 颜色补 Alpha | `1b1838ceb` `3a5b9954c` `3ec94fc28` | 已有 |
| YSM 挂件渲染 TLM 层（TLM 侧） | `14b380384` `9060014e3` | 已有 |
| 手办 / 坐垫物品渲染记忆化（创造栏与 JEI 卡顿） | `3f9f45c81` | 已有 |

⚠️ 三项都依赖 1.21.11 的 RenderState / SubmitNodeCollector 形态，**26.x 的渲染管线要重新读**。
YSM 那条还依赖 fork 侧的 `renderTlmLayers`，**双侧同时才有效**。

### G. 第三方兼容

| 内容 | 证据 | 宿主 |
|---|---|---|
| Kaleidoscope Tavern（我们独有） | 见 `compat/kaleidoscopetavern` | **新基无** |
| MrCrayfish's Furniture: Refurbished（我们独有） | `5ff13874a` `dd2ccc483` | **新基无** |
| 女仆不站上 Kaleidoscope 家具 | `c817ecec4` | 已有 |
| YSM 接管路径与数据层 | `5fdf46315` `d604795e0` `7ca21de41` | **新基无** |
| REI | 见 `compat/rei` | **新基无** |

交叉验证：1.21.11 分支的兼容状态账本，及其归档里的家具重制/仆从铃、YSM 兼容两份记录。
⚠️ 家具重制那条附带一个**注册表同步属性改动**（把配方序列化器标为 SYNCED），
代价是服务端有客户端缺失的序列化器时会明确断开。**26.x 上要重新判断这个代价是否仍必要**。

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
   5. §3.C AI（最大，依赖 A 与 H）→ 6. §3.D 上游缺陷（**逐条先核对新基是否已修**）→
   7. §3.E 寻路手感 → 8. §3.F 渲染 → 9. §3.G 兼容（YSM / Patchouli 前置就绪后）。
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
Immersive Melodies · Simple Hats · Ponder · Improved Mobs · Just More Cakes · TACZ · SlashBlade ·
Superb Warfare · Iron Chests · The One Probe · Embeddium · KubeJS · Aquaculture。

**重要更正**：「1.21.11 上放弃的兼容现在很多重新可用」这个前提**经实查不成立**。
逐条查到加载器粒度后，这批里**没有任何一个**在 26.1.2 的 Fabric 上可用；
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
而不是核对文件数对不对。

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

## 8. 剩余未决

1. **YSM 的 26.1.2**（§7.2）：`gege-tlph/OpenYSM-Updated` 要不要开 26.1.2 移植？
   这是独立仓库的独立项目，**规模未评估**——需要先单独量一轮，不能顺带做。
2. **「过度限制」C 类候选**（§3.E）在移植期原样搬还是借机复核？建议原样搬，另开一轮。
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

**A 配置所有权与持久化**（12）
- [ ] `AiConfigFileMigrationTest.java`
- [ ] `AiServerRuleAttackTest.java`
- [ ] `AiServerRuleMigrationTest.java`
- [x] `AtomicConfigFileWriterTest.java` —— 逐字搬入、零修改（`8c439de1a`）
- [ ] `ConfigPayloadCodecTest.java`
- [ ] `GlobalConfigMigrationTest.java`
- [ ] `MagmaCubeConfigInheritanceTest.java`
- [ ] `MaidConfigLayoutTest.java`
- [ ] `RuleStagingSessionTest.java`
- [x] `ServerRuleConfigTransactionTest.java` —— 搬入并加一条运维参数不进公开快照（`9354189d9`）
- [ ] `ServerRulesSaveAuthorityContractTest.java`
- [ ] `WorldRuleTestHarness.java`

**B 战斗 / 威胁响应**（3）
- [ ] `MaidCombatIntentGameTest.java`（GameTest）
- [ ] `MaidEmergencyCombatGameTest.java`（GameTest）
- [ ] `MaidTargetingPolicyGameTest.java`（GameTest）

**C AI 聊天·站点·TTS/STT**（24）
- [ ] `AvailableSitesTransactionTest.java`
- [ ] `ClientLocalChatContractTest.java`
- [ ] `DedicatedSiteFilesGameTest.java`（GameTest）
- [ ] `GameplayKnowledgeContractTest.java`
- [ ] `GuiLangKeyCoverageTest.java`
- [ ] `LLMSitePersistenceRegressionTest.java`
- [ ] `MaidChatFollowResolutionTest.java`
- [ ] `MaidChatHistoryNormalizationTest.java`
- [ ] `MaidContextsGameTest.java`（GameTest）
- [ ] `MaidControlToolsGameTest.java`（GameTest）
- [ ] `MaidFarmNavigationGameTest.java`（GameTest）
- [ ] `MaidFollowOwnerGameTest.java`（GameTest）
- [ ] `MaidTableFoodGameTest.java`（GameTest）
- [ ] `MissingTtsPartIsNotSilentContractTest.java`
- [ ] `ResponseChatSplitTest.java`
- [ ] `STTSiteCredentialsTest.java`
- [ ] `SettingsPopupWiringContractTest.java`
- [ ] `SiteCheckResultWiringContractTest.java`
- [ ] `SiteEditorLayoutTest.java`
- [ ] `SiteSecretRedactionTest.java`
- [ ] `SkillLoaderPriorityTest.java`
- [ ] `ToolDispatchWiringContractTest.java`
- [ ] `UseSkillToolContractTest.java`
- [ ] `VoicePreviewRequestValidationTest.java`

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
- [ ] `AiReloadWiringContractTest.java`
- [ ] `TlmCommandSmokeGameTest.java`（GameTest）

**J 通用门禁不变量**（2）
- [ ] `PayloadRegistrationInvariantTest.java`
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
