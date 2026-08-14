# Touhou Little Maid 26.1.2 Fabric 当前状态

**本文是唯一活动状态账本，只写「现在什么是真的」。**
开放项在最前面且各自自带验收标准与下一步；已关闭的压成一行 + 提交号；
**不写会过时的数字**——那些跑 `python docs/tools/facts.py`。
移植范围、边界与测试台账在 [PORT_26X_AUDIT.md](PORT_26X_AUDIT.md)，不要在本文复制。

## 结论

当前树 = `origin/26.1`（MC 26.1.2 Fabric）+ 一层工程设施 + **审计 §3.A「服务器规则体系与配置所有权」已整块落地**
（配置事务写盘 → 世界规则本体与文件层 → 读点改道 → 网络层与配置菜单 → op/deop 重发）
+ **§3.F 的一处宿主回归已修**（扛女仆时玩家手臂摆抱姿）。
**除此之外，代码行为等同于代码宿主，不等同于我们 1.21.11 的行为。**
构建、JUnit、GameTest 三条链路均已实跑验证；**2026-08-14 起有了单人档实机验收**
（棋局簇、背包四型、世界规则单人侧、抱姿——见 O1，专服/存档升级侧仍开放）。

---

# 开放项

**O1 · 待入世实测（2026-08-14 单人档验收后收窄到专服/存档侧）**

**单人档实机验收已完成**（2026-08-14 用户实测，dev 客户端约 25 分钟；
按「验收通过要连同它的环境一起记账」：**以下通过仅覆盖单人档**——
客户端与服务端共用同一份数据的特例，两侧数据可分叉的场景仍开放）：

- ✅ 抱姿（F5 第三人称）、Shift 棋盘预览与提示行、残局道具右键载入、
  创造栏顺序与译文、四种背包穿脱/背上模型/GUI 全功能（工作台合成、末影箱、
  熔炉烧炼与火焰/箭头进度、储罐灌取与流体渲染/tooltip）、熔炉持久化重进、
  祭坛四配方、配置菜单两栏与保存生效（单人）、四张战利品表的 `/loot`
  （`random_board_state` + 三张背包表，tank 出品带岩浆 mB）
- ✅ **实机抓出并当场修掉一个崩溃**：替换燃烧中的熔炉背包必崩（`d1fcc58c4`，见已关闭表）
- ☑️ 饮用音效（女仆喂主人喝东西）：条件难凑本轮未实测，**用户裁定视为验收通过、
  除非后续被报告 bug**（2026-08-14）——口径与上面的实测项不同，特此分开记

- ✅ **原版替换五开关**（2026-08-14 第二轮实测，`6cbe559ba`+`e48a55f56`）：五开关逐个开合
  即时生效（史莱姆/岩浆怪油库里、经验球点符、图腾 1UP、附魔之瓶点符），全关回原版；
  **岩浆怪崩溃复现点不再崩**（资源修复经用户复验）
- ✅ **REI 三项**（2026-08-14 第二轮实测，`30d0d2795`）：折叠组、祭坛配方页 + 背包 GUI
  点击区、一键转移进合成格/熔炉格

**图标缓存：单人档实机验收通过（2026-08-15 用户全项确认，五轮排障闭环）**：

- ✅ 缓存屏弹出/回原屏/图标全量生成且**无错位**（第三次重推导的截图时序实机定案成立）；
  bedrock 与 gecko 图标全部清晰；真绿部件（大妖精）与半透明部件（冰翅/妖精羽）均正确
- 五轮排障入账（详见 `7a2d63ca9`/`fdd0d3763`/`be60bdb62` 提交信息）：
  ① 绿面 = origin 固有等值抠像伪影（冰翅 α=128 实查）→ 用户批准改**双背景差分抠像**；
  ② gecko 帧间位移被差分误读 → **同帧双幕**；③ gecko 特效（ysmGlow 法阵/gui 装饰板）
  入镜的根因 = **CacheScreen 继承 Screen 默认 isPauseScreen()=true，单人档暂停冻结
  level 时间，隐藏特效的 pre_parallel/条件动画永不推进** → 改不暂停 + gecko 20 tick
  安定等待（配套，勿单独删）；④ 显示端 10:1 最近邻抽样锯齿 → 按 guiScale 面积平均降采样
- F3+T 资源重载后图标会暂时变 missing，属预期：包重载会重填队列，下次开 GUI 自动重缓存
- ⚠️ 缓存屏不暂停后，单人档缓存期间世界在走（约 1 分钟）——行为与开着模型 GUI 一致，非缺陷

**缺陷采用批（`6f5d8f2a5`）可选实机抽验**：拉弓后换武器姿势应恢复；灭火剂应浇灭灵魂火；
主手空时吃工作餐副手物品不消失。均有基准同款修法背书，抽验非必须。
- **GIF 表情气泡**（`b70145a20`）：让女仆发表情气泡直到抽到 GIF 表情，验显示不空、逐帧在动

**仍开放（都需要专服/局域网或旧存档，单人档验不到）**：

| 待验 | 为什么这轮验不到 | 怎么验 |
|---|---|---|
| 世界规则从 stock 26.1 存档升级 | 需要真有旧值的 `touhou_little_maid-common.toml` | 用装过 stock 26.1 的存档进一次，看旧值有没有进 `serverconfig/touhou_little_maid-server.toml` |
| 配置菜单可见性（局域网客机 / 专服非 OP） | 单人档只验了单人身份 | 局域网客机与专服非 OP 各开一次菜单，看「玩法设置 / 高级设置」两栏在不在 |
| 专服保存 → `/tlm config reload` | 需要真专服 + 真客户端 | 改一项保存，确认提示出现且值未生效；跑 reload 后生效并同步 |

**O2 · 反向缺口：宿主迁移时丢掉的东西（判定已完成，进入实施排期）**

差异化清单只回答「我们多出来的搬齐了没」，回答不了「宿主丢了什么」。
账本 `docs/tools/host_gap_ledger.tsv`（222 条全登记），核对 `python docs/tools/host_gap.py --ledger`。
方法与完整结果见审计 §7.8。

**判定分布与「已补回 / 待补」分栏不在本文写死**——跑 `python docs/tools/host_gap.py --ledger` 看实时数
（本文此前写死过一次，两天内过时了两回，正是本文开头「不写会过时的数字」那条禁令针对的形态）。
动手补的过程中已改判三条：`ClientBoardStateTooltip` 待定→丢失并已补、`MaidGameRecordManager` 丢失→替换、
`IBackpackData` 替换→丢失——**判定的最终校验是真去补它**。

「丢失」归成五簇，**五簇与零散均已清零**（2026-08-14），剩余待补只有 TACZ 批（归 O6）：

| # | 簇 | 条数 | 后果 | 备注 |
|---|---|---|---|---|
| 1 | ~~**女仆背包四型**~~ | ~~15~~ | **已补完 4/4**（末影箱 `8efc995d4`、工作台 `60aa8dcc6`、熔炉 `40b8cfde0`、液体 `ff7ee685d`） | 见已关闭表 |
| 2 | ~~**棋局存档与记录层**~~ | ~~8~~ | **已补完**（`e778676cc`…`ba5b8af65`） | 见已关闭表 |
| 3 | ~~**REI 集成**~~ | ~~5~~ | **已补完**（`30d0d2795`：四件逐字 + Maker 重写走宿主 `ClientRecipeEvent.ALTAR_RECIPES`；REI 实证加载且 GameTest 28/0） | 见已关闭表 |
| 4 | ~~**原版替换功能**~~ | ~~4+2~~ | **已补完**（`6cbe559ba`，含两处改判：InitSpecialItemRender 替换→丢失、ReplaceableBakedModel 待定→丢失） | 见已关闭表 |
| 5 | ~~**模型图标缓存**~~ | ~~4+分支~~ | **已补完**（`e45ea33a6`：4 文件 + 5 处丢分支 + 队列接线 + 配置菜单 lang，契约测试三形态红测过） | 见已关闭表；⚠️ 零实机项在 O1 |
| — | ~~零散~~ | ~~2~~ | **已清零**：`GifTexture` 补回 `b70145a20`（宿主 FIXME 搁置还原）；`RenderFixer` **改判 丢失→无关**——origin 那个补丁是 1.21.1 时代 Carry On 附魔渲染 bug 的绕行，判据载体（`BufferSource.fixedBuffers`）在 26.1.2 submit 管线不存在，且行为基准在 render-state 重写时已放弃它、无等价承接（renderer 树零命中实查）。现存 Carry On 兼容 = tag + molang 纯数据层，与基准一致，依赖不接回（零消费者不留空壳）。若实机复现同类 bug → 修上游，属超基准新决策 | 至此**「丢失」待补只剩 TACZ 批（归 O6）**，实时数跑 `--ledger` |

⚠️ **还有一批「待定」**（实时数跑 `--ledger`）：子代理报「未找到」而我尚未复核，一律不写成结论
（上一轮子代理判定被逐条推翻过）。`--ledger` 会一直提示，忘不掉。

**优先三簇已复核完（2026-08-15，16 条全部改判「替换」，证据在账本各行）**：
箱子类型 5 条 → WirelessIO 改走 Fabric transfer `ItemStorage.SIDED`（覆盖为超集）；
任务数据 4 条 → 唯一内部注册项 `maid_attack_list` 由 `AttackListData` + Fabric attachment 全链路承接；
战利品 7 条 → fork 自制 GLM 管道被 Fabric loot v3 `LootTableEvents.MODIFY` 承接，
`loot_table_type` 全仓 JSON/源码零引用。
「唯一可能再藏整块玩法丢失的地方」**排除**——但代价是宿主取消了 `ILittleMaid` 的
`addChestType` / `registerTaskData` 两个第三方扩展点，第三方需求出现时再评估（账本行有锚点）。

**O3 · §3.A 剩余两项（世界规则那一层已闭合）**

配置三层的**世界规则那一层已完整**：本体、文件层、读点改道、网络层、配置菜单、
`/tlm config reload`、op/deop 重发，全部落地并有测试（见已关闭表三条）。剩下的两项都不属这一层：

| 缺口 | 现状 | 恢复锚点 |
|---|---|---|
| `AiServerRuleConfig` 那一店 | 未搬，整体属 §3.C。`ServerRuleConfig.get()` 无路由分支；两个包只装世界规则一半 | `ServerRuleConfig.get()`、`SyncServerRulesPacket` 两处 javadoc |
| `ExperimentalConfig.SMOOTH_FOLLOW` | 类与值都未建；配置项要与消费者（§3.E 跟随手感）同批落地 | `ServerRuleConfig.values()` 的 javadoc |

⚠️ 上表每一条在代码里都有对应注释，**不要只靠本表**——本表会过时，注释在改到时才会被看见。

**O6 · TACZ 兼容 + 远程应战批（2026-08-14 新开，两份清单已收齐）**

1.21.11 分支同日完成这轮并送来两份清单：专题版
[archive/PORT_TACZ_AND_RANGED_AI.md](archive/PORT_TACZ_AND_RANGED_AI.md)（TACZ+远程 AI 操作指引）
与全量版 [archive/PORT_TACZ_AND_RANGED_AI_FULL.md](archive/PORT_TACZ_AND_RANGED_AI_FULL.md)
（27 笔 67 文件按 A 恢复基准/B 超基准逐条决定/C 门禁 分三类 + 1.21.11 API 事实表）。
蒸馏结论已入审计 §2.2/§3.B，动手时**两份都过一遍**：

- **生态裁决已翻**：TaCZ Refabricated 有 26.1.2 分支构件（审计 §2.2 专门行），
  账本 14 行枪械条目改判「丢失」；⚠️ 上游明说 1.21.11 与 26.x 是**两套实现**，
  凡涉 TACZ 内部（mixin 注入点、数据组件）必须按 26.1.2 的 jar 重验；Fabric API 下限也按其
  fabric.mod.json 重读（1.21.11 那次升 API 就是被 TACZ 逼的）
- **远程应战批与 TACZ 无关、26.1.2 同样需要**（行为基准已前移，精确参数与陷阱全在审计
  §3.B 那一行：速度 0.5、站定距离 16、resolveImplementation 不加 isWeapon、
  canUseNonMeleeWeapon 陷阱、swingingArms 修复已随基准定案）
- **B 类超基准项逐条显式决定**（全量清单 B1–B7）：除上述外还有
  `GunRecognitionRange` 按枪种取值（基准无此文件，基准的扫描/交战半径本就不一致）、
  渲染期不读实体（`EntityMaidRenderState.backpackShowItem` 字段，26.1.2 渲染架构同源直接适用）、
  `GeoLocatorType` 的 TAC_PISTOL/RIFLE 取消注释、**菜单三个枪械滑块要包 `isModLoaded`**
  （⚠️ 26.1.2 现树的三个滑块是裸露的，TACZ 刀时对齐）、66 条枪械 lang×11 语言文件
  （`.desc` 按只有 TaCZ 重写，基准原文提到的 SBW 未恢复）
- **「持枪寻路异常」降级**：基准分支六组受控对照（平地/地形×静止/慢走×四种武器）
  复现不出——枪每组都不比其它武器差、交战组最稳；等用户给复现现场再立案。
  半径耦合确认是真实性能面但非该症状成因，BFS 解耦属超基准候选未做
- 恢复锚点已埋：`ServerRuleConfig.values()` javadoc（枪械三键必须进认领清单，
  1.21.11 两次崩服实证「用对访问器 ≠ 读得到」）
- 1.21.11 API 事实表（AbstractArrow 挪包、getBaseDamage 删除、药水箭组件拷贝、
  不死系毒/再生免疫是原版规则等）在全量清单尾部，26.1.2 动到箭/远程时先查它
- 取证纪律两条随单收下：**创造模式玩家不被怪物索敌**（战斗类取证先证明触发条件成立）；
  **比值型聚合指标分母趋零会爆炸**（两数不自洽就回看原始序列）

**排序**：反向缺口五簇已清零，本批在零散两条与待定复核之后、§3.B/§3.C 大簇之前或同批，
届时由用户定夺。

**O4 · 前置项目未决**
- **YSM**：Fabric 26.1.2 上不存在任何实现（本体仅 NeoForge 且闭源，OpenYSM 无 26.x）。
  要保留该特色，须先把 `gege-tlph/OpenYSM-Updated` 移到 26.1.2——**独立项目，规模未评估**。
- **Patchouli**：官方有 26.1 beta，我们维护的 fork 需跟进。

**O5 · 公开发布链路尚未建立**
本分支还没有清洁分支、没有公开远端分支、没有 CI。`tree_equiv.py` 与 `git_hygiene.py`
里已经写好了目标 ref 名（`release/26.1.2-clean` / `fork/port/26.1.2-fabric`），
但**那两个 ref 还不存在**，相关门禁步骤现在必然跳过或报缺失——属预期，不是缺陷。

---

# 已关闭（一行结论 + 提交）

## 2026-08-15：缺陷修复移交清单定案采用（`6f5d8f2a5`）

1.21.11 分支移交的缺陷修复清单（原件归档
[archive/HANDOFF_DEFECT_FIXES_2026-08-15.md](archive/HANDOFF_DEFECT_FIXES_2026-08-15.md)）
逐项三树对照后采用：A 批 floorMod ×3、B 批拉弓姿势清位 ×3、C 批上游缺陷五处
（#1135/#1177/#938/#1058+#1059/#1158；#1053 熔炉刀已带入无需动作；
#1177/#938 按宿主管理器化结构适配进 `MaidItemManager`）、D 批入口时序对调
（首装首进程女仆名退化的根因）+ 两层楼死锁兜底（GUI 预览竞态核实本树本就正确）。
门禁：JUnit 65/0、GameTest 28/0（入口新时序下服务端启动实证）。

**延后项（动手时必查归档件）**：C2 AI 聊天层三笔（指令停调/TTS 语种漂移/识别提示冻结，
修法全是结构性的）+ TTS 语种诚实标注 → **§3.C 动手时必采**；#1139 返回容器形状校验 →
`RemainFoodEatenEvent`（账本待定行已注记）移植时随行。
**归档件 §E 是有意行为分歧全集——对照 origin 做基线等价审计时勿当漂移改回。**

## 2026-08-14：模型图标缓存整簇补回（`e45ea33a6`）——反向缺口第五簇清零

4 文件（CacheIconManager/CacheScreen/CacheIconTexture/IconCache）+ 5 处入口路由 +
三个 GUI 图标分支 + `IModelInfo.getCacheIconId` 消费链 + `MODEL_ICON_CACHE`
（initCommon 个人配置 + Cloth misc 段 + lang 两键双语；`cache_screen` 两键宿主本就留着——
又一处宿主删代码留资源，这次顺风）。

**取证推翻了两个「基准已验」假设**：

1. **基准分支这一簇是死代码**：origin/1.21.1 的队列填充接线（`MaidModels`/`ChairModels.addPack`
   登记 + `CustomPackLoader.clearCache`）在基准重构出 `AbstractClientModels` 时被静默丢失，
   队列恒空 → 缓存屏在 1.21.11 上从不弹出。故基准三件的「1.21.11 新纹理管线」注释**从未被
   运行期执行过**，不能当已验事实引用；本刀全部按 26.1.2 反编译源重推导，并补
   `CacheIconWiringContractTest`（构造点唯一路由·生产者接线·GUI 分支·配置所有权·lang 四键，
   三种缺陷形态红测：摘接线/全限定名绕过/配置挪世界规则侧 → 各自当场红）。
2. **基准的截图时序注释在 26.1.2 不成立**：渲染线程上 `Minecraft.execute` 是**内联执行**
   （`scheduleExecutables()` = `runningTask() || !isSameThread()`，帧循环里两者皆否），
   「execute 延到下一帧回读」照搬会把每个图标错位成前一个模型。重推导为显式帧计数：
   模型第 2 个 extract 帧内联发起截图（此刻主 RenderTarget 恰持有上一帧完整画面，
   拷贝命令先于本帧渲染命令入 GPU 命令流），回调经 `RenderSystem.executePendingTasks`
   在渲染线程执行——顺带满足 `registerAndLoad` 的渲染线程约束。

**26.1.2 实查漂移**：`TextureManager.register` 只入表不上传（基准裸 `register` 是从未踩响的
潜在炸弹）→ `registerAndLoad`；`byPath` 私有化且 `getTexture` 对未注册 id 会自动建
SimpleTexture 报错加载（不能当存在性探针）→ `CacheIconManager` 自持已注册集合；
`Window.getGuiScale()` double→int；`NativeImage.pixels` 私有化 → `getPixel/setPixel`；
`setIsYsmModel` 随 YSM 不存在于 26.1.2 Fabric 删去。

门禁：compileJava 0 错、JUnit 57/0（新增 5）、GameTest 28/0。⚠️ 零实机项见 O1。

## 2026-08-14：实机崩溃「岩浆怪替换开关一开即崩」（`e48a55f56`）

实测轮用户复现：开关一开渲染线程 NPE（model==null）。日志第一现场在启动期——
加载器对缺失模型文件只打一行 ERROR 继续跑，用到才崩。根因是上一刀我的取证错误
（把基准 grep 输出误读成本树的，详见证伪表新条目），4 个 yukkuri 资源实为两树都缺。
自基准取回并验字节；**新闸 `BedrockModelResourceInvariantTest`**（登记闸家族第三枚）：
注册常量↔模型文件机械对账，红测过（挪走一个 json 当场红）。**用户复验通过**（岩浆怪开关不再崩且正常变身）。

## 2026-08-14：原版替换补回（`6cbe559ba`）——反向缺口第四簇清零（4+2 条）

五开关整簇：油库里史莱姆/岩浆怪、点符经验球、1UP 图腾、点符附魔之瓶。
**两处账本改判**：InitSpecialItemRender 原判「替换」被实查证伪（所谓承接者是椅子/手办
special renderer，同名家族误判）；ReplaceableBakedModel 待定→丢失（两开关唯一消费者）。
基准三渲染器已是 render-state 形态近乎原样搬入；四处 26.1.2 漂移全部实查
（CameraRenderState→state.level、BlockStateModel→block.dispatch、
BlockModelWrapper→CuboidItemModelWrapper、entityCutoutNoCull→entityCutout 命名反转语义不变）。
VanillaConfig 按交接裁决进 CommonConfig；inheritMagmaCubeFromSlime 迁移随行
（四例 JUnit，接线哨兵红测过）。~~宿主半吊子清点：yukkuri 资源留了、常量没留~~（**此句错误，`e48a55f56` 纠正**：
yukkuri 4 个资源宿主同样删了——「留了」是把基准 grep 输出误读成本树的，见证伪表新条目）；
点符/1UP 资源全删。JUnit 51/0、GameTest 28/0。零实机项进 O1。

## 2026-08-14：REI 集成补回（`30d0d2795`）——反向缺口第三簇清零

四件逐字照基准（容器槽位布局两树逐行同构，转移处理器槽位号原样可用），
Maker 重写走宿主 `ClientRecipeEvent.ALTAR_RECIPES`（真实配方 id，宿主 JEI 插件同表为参照）。
依赖解开宿主注释行（版本经 1.21.11 交接实测）；implementation 让开发客户端可实测；
⚠️ Xaero 前科当面验证：gametest 日志实证 REI+architectury 已加载且 28 例全过。
一处漂移：`Item.getName()` 无参版已删 → `getName(ItemStack)`。
**新通用闸 `EntrypointRegistrationInvariantTest`**（与 mixin 登记闸成对）：每个 entrypoint
类必须真实存在——第三方拉起的入口（REI/JEI/GameTest）坏了纯静默，红测过。
零实机项进 O1。

## 2026-08-14：SpotBugs（手动报告态）+ ArchUnit（进 JUnit 门）落地

自 1.21.11 分支照搬的门禁工具副本（隔离纪律允许共享），按本分支适配：

- **ArchUnit 两条规则进 `test` 门**（`ArchitectureRulesTest`）：①认领的世界规则不得裸读
  ②经读口读的键必须在认领清单里——字节码判据，间接引用与静态导入的绕过形态无所遁形，
  与既有源码扫描契约互为补充。适配两处：认领清单暂只世界规则一份（AI 店 §3.C 落地时
  加回另一半，锚点在 `claimedWorldRuleFields`）；引导走 `ServerConfig.init()`。
  **双红测通过**：两条规则各注入一个违规（裸读认领键 / 经读口读未认领键）→ 各自当场红并
  精确定位违规行 → 还原回绿。每条规则自带双下限活性断言（字段识别与调用识别分开看守）。
- **SpotBugs 手动报告态**：`gradlew spotbugsMain` → `build/reports/spotbugs/main.html`，
  HIGH 置信 + MAX 力度，**不进默认构建不进发布门**（新闸先红测再纳入的纪律）。
  首报 64 类告警备查（`BC_IMPOSSIBLE_CAST` 46、`NP_NULL_ON_SOME_PATH` 10、
  `RV_ABSOLUTE_VALUE_OF_RANDOM_INT` 6——修不修逐条另议，多数属宿主/基准代码，行为对基准优先）。
- 已否决规则记录在测试 javadoc：「客户端类不得被服务端引用」在本仓库不适用
  （基准靠 @Environment 与 isClientSide 分流，不靠包边界，1.21.11 分支实测 1000+ 处基线）。
- PIT 变异测试在 1.21.11 分支评估过、用户裁决不采用（minion 不继承 workingDir/loom
  classpath，对扫源码型契约测试天然无效）——本分支同构，别再试，理由记在 build.gradle。

## 2026-08-14：实机崩溃「替换燃烧中的熔炉背包」（`d1fcc58c4`）

单人档验收中用户复现：熔炉背包燃烧时手持工作台背包右键女仆替换，客户端必崩
（`Failed to handle packet ServerboundInteractPacket` + 渲染线程 NPE）。

**根因是宿主自 NeoForge 移植 `util/transfer` 时引入的**：`RootCommitJournal.createSnapshot()`
按 NeoForge 语义返回 null，但 `SnapshotJournal` 把它嫁接在 **Fabric 的 `SnapshotParticipant`** 上，
后者对快照做非空断言——`VanillaContainerWrapper` 的整条取放路径**在宿主树里从来没工作过**
（宿主零消费者，纸面组件），熔炉/储罐两刀的 `onTakeOff` 是第一个踩上去的。
修法：null → 非空哨兵（顺带修掉 null 快照下「每次根提交重复注册回调」的隐藏缺陷）。

**1.21.11 分支无此缺陷**（实查：那边没有 `util/transfer` 包，丢物走 Forge 形态 `InvWrapper`）。

红测：GameTest `takeOffDropPathExtractsThroughVanillaContainerWrapper` 走 onTakeOff 同路，
修前与实机同一 NPE 当场红，修后回绿；**用户实机复验通过**（物品正常掉出）。
JUnit 44/0、GameTest 28/0。

## 2026-08-14：液体背包补回（`ff7ee685d`）——背包四型 15 条全簇清零

沿用熔炉刀的附件机制，本刀的量全在流体侧。**三处 26.1.2/Fabric 8.0.x 真实漂移**（实查）：

- `SingleFluidStorage.writeData/readData` 实例方法 → 静态 `SingleVariantStorage.writeValue/readValue`
  （仍写 `variant`+`amount`，与基准存档兼容，GameTest 往返钉着）
- `FluidVariantRendering.getSprite` 已删（Fabric 流体渲染并进原版 FluidModel）→
  `ModelManager.getFluidStateModelSet().get(state).stillMaterial().sprite()`，
  与 JEI 29.5 Fabric 版同款（其 FluidHelper 字节码实查——`MaidFluidRender` 本就抄自 JEI）
- 桶↔储罐的背包插入 Forge `ItemHandlerHelper` → 宿主 `ItemsUtil.insertItemStacked`

同步三通道照基准：`BACKPACK_FLUID` 实体数据（流体 id）+ 容器 data slot（int 低位）+
`SyncFluidAmountPackage`（精确 long，变化发主人、开 GUI 发打开者）。
`TANK_BACKPACK_TAG` 数据组件**宿主删功能时留了下来**，直接用（又一处宿主半吊子删除，这次是顺风）。
槽位空图标走宿主 back_show 同款 idiom（gui/sprites png + blocks.json 双保险）。
获取路径随刀齐：祭坛配方 + `chest/tank_backpack`（岩浆 9/4/3 桶，产物与基准 hash 相同）注入下界要塞。

GameTest 四条 + 红测（摘登记行三条当场红）。JUnit 44/0、GameTest 27/0。
⚠️ 零实机：GUI 流体渲染与 tooltip、背上模型、穿脱携带流体，见 O1。

## 2026-08-14：熔炉背包补回，IBackpackData 机制立起（`40b8cfde0`）

背包四型第三种。与前两种不同，熔炉带持久化、tick 驱动的数据对象，本刀把这层机制
按宿主形态立起来（液体背包直接沿用）：

- **`BackpackStateData` 附件：persistent 但有意不 syncWith**——GUI 进度条走容器
  `addDataSlots(ContainerData)`（26.1.2 javap 证仍在，与基准同一条路），物品走菜单槽位同步；
  烧炼进度每 tick 在变，挂 `syncWith(all)` 会对所有追踪者每 tick 重发
- **附件值是可变 holder**：codec 编码时从 runtime 拉活状态（存档那刻快照，无须保存前刷新钩子）；
  解码只得待恢复 NBT，由 `MaidBackpackManager` 惰性绑定（解码时拿不到 maid/level）。
  tag 内部格式与基准逐字相同（含 #1053 稀疏槽位修复），基准存档可互认
- serverTick 挂 `manager.tick()`（baseTick 服务端分支），节奏同基准 aiStep
- 三处 26.1.2 漂移（对照原版 `AbstractFurnaceBlockEntity` 反编译源实查）：`assemble` 单参、
  燃料残留改 `Item.getCraftingRemainder()` 返回 `ItemStackTemplate`（shrink 后空则 create，
  真实燃料下行为等价）、`canInsertItem` 挪进 `MaidItemManager`
- 获取路径随刀齐：祭坛配方 + `furnace_or_crafting_table_backpack` 战利品表
  （datagen 产物与基准 hash 相同）注入 SIMPLE_DUNGEON

GameTest 四条：注册三点 / 附件惰性绑定 / 210 tick 烧熟牛肉全链路 / 稀疏槽位存取往返。
红测：摘 BackpackManager 登记行 → 四条当场红 → 还原回绿。JUnit 44/0、GameTest 23/0。
⚠️ 零实机：GUI 火焰/箭头、背上模型、穿脱丢物需入世验证（见 O1）。

## 2026-08-14：上一会话 15 刀全量审计（审计结论与修复分开，一缺陷一提交）

四方校验逐文件过完 15 笔提交（世界规则五刀 / 抱姿三 mixin / 喂饮音效 / 棋局六刀 / 背包两刀）。

**已审无缺陷的块**：世界规则体系（PlayerListMixin 的 op/deop 描述符与「单参 op 委托三参」经
javap+字节码复验；网络层的 AI 店删减全部带恢复锚点；ConfigFileMigration 时序正确）、
抱姿三 mixin（两个靶点描述符 26.1.2 javap 逐字复验，角度与基准/origin 全同）、
喂饮音效（与基准逐字一致，「宿主已实现牛奶解毒」的断言复核属实）、
GomokuCodec/BoardStateTooltip/棋谱数据层与三份预设 json（与基准逐字或仅有已记录的 API 漂移）、
三个棋盘方块的右键分支（逐字同基准）、CraftingTableBackpackContainer（对照 26.1.2 原版
CraftingMenu：`setRecipeUsed` 布尔重载仍在用、`assemble` 单参、`setRemoteSlot` 模式同构）、
两背包屏（`guiBlit` 8 参内建 256 假设 vs 两张 GUI 贴图实测都是 256×256，语义保持；
Curios 分支与宿主全部兄弟屏一致，是宿主自己的 Trinkets 集成）、全部资源与基准 hash 相同。

**抓出并已修的缺陷（按严重程度）**：

| 提交 | 缺陷 |
|---|---|
| `d78f1ba29` | **最大缺陷**：`be630452f` 提交信息声称「ItemBoardState 的锚点由本刀闭合」，但那刀只加了组件/渲染器/映射三个文件，从未碰 ItemBoardState——`ofGomoku` 三个工厂全仓零调用者，整条预览链是死代码。补回 `getTooltipImage` + Shift 提示行 + `show_picture`/`gomoku.default` 四条 lang |
| `d3bc262b9` | 两种新背包**生存模式完全不可获取**：补祭坛配方×2（配比逐字照基准）+ `chest/ender_chest_backpack` 战利品表 + STRONGHOLD_CORRIDOR 注入；熔炉/液体那两张表因引用未搬物品顺延，配比已抄进账本 |
| `a08bf30e8` | zh_cn 四条手写译文与基准不符（「五子棋残局」还与棋谱预设名撞名）+ 丢了 `crafting_table_backpack.tip` |
| `59821b556` | 创造栏两背包顺序与基准相反（无理由分歧，实现先后的副产物） |
| `615375eb0` | `8efc995d4` 误入库的 0 字节 `textures/item/ender_chest_backpack.json` |

**门禁**：JUnit 44 例 0 失败（常驻探针在报告里）；`build/gametest/report.xml` 19 例 0 失败；
读点契约做了活体红测（把一个读点改回 `MaidConfig.BOW_RANGE.get()` → 两层当场红 → 还原）。

## 2026-08-14：棋局簇补完（`e778676cc` `c47fd7572` `bbd3b6f45` `be630452f` `ba5b8af65` `af7372306`）

反向缺口五簇里的第二簇。宿主删得很干净——`board_state` 在 `origin/26.1` 的源码与资源里
**命中为 0**，不是「注册还在类没了」那种半吊子，所以没有残留物要清理。

| 刀 | 内容 |
|---|---|
| `e778676cc` | `GomokuCodec` 残局编解码 + 6 条往返 JUnit（红测：黑白对调当场红 2 条） |
| `c47fd7572` | 三个残局道具 + 数据组件 + 三个棋盘方块的右键分支 + 方块实体存取 + 创造栏/lang/模型贴图 |
| `bbd3b6f45` | 数据包预设棋谱（三份 json + 重载监听器） |
| `be630452f` | 提示框棋盘预览（闭合上一刀留的锚点） |
| `ba5b8af65` | 随机残局战利品函数 |
| `af7372306` | 改判 `MaidGameRecordManager`：不是丢失，是被重构成 `MaidGameManager` |

**三处 26.1.2 的真实 API 漂移**（都已实查取证）：
- 提示框绘制 `renderImage(GuiGraphics)` → `extractImage(GuiGraphicsExtractor)`，
  与本版本的渲染状态抽取模型一致；`GuiGraphics` 类已不存在
- 战利品函数 `getType()` 返回 `LootItemFunctionType` → `codec()` 返回 `MapCodec`
- 重载监听器不再需要 `IdentifiableResourceReloadListener`，id 在注册那步传入

**两处「把复核交给编译器」**：基准的 `ItemBoardState` 留着一句
「TODO: 检查 appendHoverText 是否匹配父类」，javap 实查签名一致，故删 TODO 补 `@Override`；
`extractImage` 是接口 default 方法（写错只会静默不画），同样补 `@Override` 断言。

**接线的运行期证据是跨刀拿到的**：数据包重载监听器在 `bbd3b6f45` 只有构建绿，
到 `ba5b8af65` 的 GameTest 才拿到运行期凭据（红测摘掉注册 → 三类棋谱全为 0）。

**最后一环也已补齐**（`b4fc6feff`）：残局道具会出现在制图师村屋箱与要塞图书馆。
我一度把它留成待办，理由是「datagen 会产生大而危险的 diff」——**那是没验证就下的判断，两条都不成立**：
报错的是 `downloadAssets`（战利品表 datagen 不需要游戏资源，`-x downloadAssets` 即可跑通）；
跑通后虽动 183 个文件，但 `git diff --numstat --ignore-cr-at-eol` 为 **0**，全是换行差异、内容一字未变。

⚠️ 环境备忘：本机跑 datagen 必须 `./gradlew runDatagen -x downloadAssets`，
且提交前要把 183 个文件的 CRLF/LF churn 还原、只 stage 真正新增的产物。


## 2026-08-13：复原「扛女仆时玩家手臂摆抱姿」（`79e7f7914`）

上一刀的 mixin 登记闸门首跑照出的那处宿主回归。**不是我们漏搬**——`origin/1.21.1` 上就有，
宿主 `origin/26.1` 迁移期把它整段注释、靶点改指 `Dummy` 空壳、留 `FIXME` 且未登记。

按 26.1.2 的渲染状态形态复原，写法与 1.21.11 同源而非照抄 `origin/1.21.1`：
渲染状态重构后 `setupAnim` 只拿得到 `HumanoidRenderState`、拿不到实体，
故经 `ICarryMaidRenderState` 三件套传递（渲染状态存位 → 抽取时求值 → 摆姿势时读出）。
两个注入点的描述符都经 javap 26.1.2 实查；`AvatarRenderer` 在 26.1.2 是泛型，
但擦除取第一上界，描述符与基准一致。

`MixinRegistrationInvariantTest.HOST_PARKED` 随之**清空**。名单机制保留并改写说明：
往里加条目前先分清「宿主搁置」与「对基准的回归」，**后者的正确做法是修好并登记，不是加进名单**。

⚠️ **本刀没有运行期凭据**：三个都是客户端 mixin，而 `runGametest` 是纯服务端，
客户端 mixin 在那里根本不加载。凭据只到「已登记 + 靶点描述符存在 + 编译通过」，见 O1。


## 2026-08-13：op/deop 后重发规则快照 + mixin 登记闸门（`c8b0c4d70`）

§3.A 最后一条。`canEdit` 只在发包那一刻求值，`/op` 与 `/deop` 本身不触发重新同步，
先进服后被授予 OP 的玩家要重进才看得到玩法设置栏。**至此配置三层的世界规则那一层闭合。**

**注入点的取证**（基准踩过坑，本轮在 26.1.2 上重验，结论照旧）：
`javap` 实查 `PlayerList`，`sendPlayerPermissionLevel` 的调用方恰是四个——
`placeNewPlayer` / `respawn` / `op` / `deop`。挂它会在**每次加入与每次重生**都白发一个规则包，
故只挂 `op`(三参) 与 `deop`。单参 `op` 的方法体就是 `op(id, empty, empty)`，注入三参即覆盖两条路径。

**顺带补的通用闸门 `MixinRegistrationInvariantTest`**：把核心纪律第 3 条机械化——
每个 mixin 源文件都必须登记进 `mixins.json`。没登记是**纯静默**的（本仓库为此抓过 5 枚纸面接口）；
登记了但靶点不存在则由 `required: true` 在启动时报错，不归它管。
**它第一次跑就照出了 `HumanoidModelMixin`**（见 O1）。

GameTest 加 `playerListMixinIsWovenIn`：反射查织入产物。
`required: true` 只在**目标类被加载**时才会因注入失败而崩，「服务器起来了」本身不构成证据。


## 2026-08-13：世界规则网络层与配置菜单（`0e54b51ac`）

上一刀有意留下的功能缺口全部补齐：世界规则从「只能手改存档 TOML」回到**服务器权威 + 客户端可编辑**。

| 落地 | 说明 |
|---|---|
| `SyncServerRulesPacket`（S2C） | 两份快照分开：`runtimeRulesJson` 发给所有人（客户端侧读点据此看到服务器的值），`editableRulesJson` 只发给有编辑权的人（菜单的编辑基线）。进服即下发 |
| `SaveServerRulesPacket`（C2S） | **只带改动过的键**，两个管理员同时开菜单改不同字段不会互相回滚。服务端重做权限 / JSON / 键归属 / spec 校验，任一不过整批拒绝并回发权威快照 |
| `ServerRulesClientCache` + `Session` | 菜单编辑的是**文件值**而非运行期值——专服上两者可以不同 |
| Cloth 菜单 | 上一刀摘掉的 36 条以「玩法设置 / 高级设置」两栏装回，由 `canEdit()` 门控（无权限者看不到，而不是看得到点不动） |
| `/tlm config reload` | 新基**没有**这个命令，随本刀补入。专服上「保存只写文件」这条路要靠它激活，缺了它那条路是死的 |
| 断开连接复位 | 离开服务器时清缓存并把运行期快照退回本端文件值 |

**一处 26.1.2 API 漂移**：`ServerPlayer.displayClientMessage(Component, boolean)` 已不存在，
改用新基通用的 `sendSystemMessage(Component)`，玩家侧同样是一条聊天消息。

**一处文案与基准有意不同**：基准的 `config.reload_success` 写「存档配置与 AI 站点已重新加载」，
而它自己的 `ConfigCommand` 明写「与 AI 零瓜葛」——那是条陈旧文案。本分支按实际行为写。

**红测三种缺陷形态，全部照出**：客户端接收器漏注册 / 进服不下发 / 激活判定写死。
另有一条测试自身的缺陷被照出：读点契约的「命中数 ≥ 40」下限对**第一层**（禁止直接 `get()`）
是错的——那一层命中数**本就应当是 0**，拿它当活性判据就是「零覆盖恒绿」。
已改为按「走过了多少个源文件」判活性，两层各用各的下限。

## 2026-08-13：世界规则体系落地（`e1efc8b64` `d387a07b7` `9354189d9`）

**这是第一刀真正碰新基宿主结构的差异化**，量出的数才有代表性（探路轮那刀零依赖，量不出成本）。

**先答上一轮的两个问题**（结论与「它们在新基改名/合并了」的猜想都不同）：

| 问 | 实查结论 |
|---|---|
| `ExperimentalConfig` 去哪了 | **哪也没去，它是我们独有的**——`origin/1.21.1` 与 `origin/26.1` 都没有。唯一成员 `SMOOTH_FOLLOW` 的消费者属 §3.E |
| `VanillaConfig` 去哪了 | **上游在 26.1 删了**，连同整个原版替换功能（`yukkuri` 资源 11 → 0，只剩一条孤儿 lang 键）。不是改名也不是合并 |
| 顺带 | `MAID_TAMED_ITEM` / `MAID_TEMPTATION_ITEM` **由配置项改成了物品标签** `TagItem.*`，`values()` 少两项且无等价物 |

**决定「我们的类挂在哪」的那个事实**：新基把 SERVER spec **注册给了 Forge Config API Port**，
于是 FCAP 自己在管 `<world>/serverconfig/touhou_little_maid-server.toml`——正是 `ServerRuleConfig`
要独占的那个文件。故照 1.21.11 的做法**不注册 SERVER spec**，只把它当 spec 用（`correct` 播种、
`getSpec().test()` 校验）。副产物是一条机制性保证：规则值上的 `XXX.get()` 会抛
`Cannot get config value before config is loaded`（已对 FCAP 26.1.4 的 `ConfigValue.getRaw`
字节码取证）——**漏改道的读点当场炸，而不是静默读到实例级旧值**。后者才是危险形态。

| 量到的 | 结果 |
|---|---|
| 主体（`ServerRuleConfig` / 事务用例） | 语义逐条可搬，改动都在**接口而非逻辑**：去掉 AI 路由分支、`values()` 少三项、快照回退用 `getDefault()` |
| 真正的成本 | **在宿主的读写面上**：93 处读点 / 38 个文件改道，Cloth 菜单摘 36 条，两个 subconfig 拆 `initCommon` / `initServerRule` |
| 新基逼出来的新代码 | `ConfigFileMigration.migrateServerFileIfNeeded`（值原属 COMMON spec，注册那刻 `correct()` 会剥掉旧值）+ `prepareWorldFile` 不再「文件已存在即返回」（FCAP 建的那份只有四项） |
| 结论 | **「代码能不能搬」不是成本所在，「宿主有多少地方在读它」才是。**§7.1 那 247 个同名文件的估算应按此校准 |

**两次红测各照出一个真缺陷**（都不是测试本身的问题，是被测代码的）：

1. 契约测试第一版按 `Owner.FIELD.get()` 扫，对**把配置对象当参数传**（`targetConditionsTest`
   收 `ModConfigSpec.IntValue` 自己 `get()`）和**静态导入后写裸名字**两种形态零覆盖——
   四条攻击任务与两处静态导入正是这么漏改的，运行期一攻击就炸。
2. GameTest 第一版拿「世界文件存在」当接线判据，摘掉 `SERVER_STARTING` 钩子后**照样通过**：
   `runGametest` 的 run 目录多轮复用，读到的是上一轮遗留的文件。

**验收凭据**：`./gradlew build` 绿；JUnit 报告 17 例 0 失败（新增 9 例）；
`build/gametest/report.xml` 含
`touhou_little_maid:world_rule_game_test_world_rules_are_loaded_from_the_save_on_server_start`。

⚠️ **尚未入世实测**：本轮全部凭据来自自动化门，没有用户实机验收。世界规则在真实存档里的
表现（尤其是从 stock 26.1 升级过来的存档能不能继承旧值）需要一次入世确认。

## 2026-08-13：继承前提逐条修正（`d726e2915`）

`CLAUDE.md` 的方法论与证伪表自 1.21.11 原样搬入，**经验有效但前提是版本相关的**。逐条查证后就地改：

| 条目 | 查证结果 | 已做 |
|---|---|---|
| 核心纪律 2 | **方向反了**（原文禁止「为贴近 26.1 改行为」写于 `origin/26.1` 还只是架构参照的时期） | 改写成两问：代码长什么样照 `origin/26.1`，表现成什么样照 `port/1.21.11-fabric` |
| 核心纪律 3 | 「排除 = 隐形」前提不存在——新基 `sourceSets` **零条源码排除**（`build.gradle` 里的 `exclude` 全是依赖组与 shadowJar 的） | 三查降为两查（entrypoint / `mixins.json`），适用面改为「**我们新增的类**」；顺带删掉一条指向 1.21.11 分支 COMPAT 文档的路径，那份文档没随方法论迁进本分支 |
| 核心纪律 4 | frozen-node 是逐个解除排除时的排序法，本分支无适用对象 | 标注保留备查，未删 |
| 核心纪律 8 | 命名约定**是假的**（新基 `blockentity/BlockEntity*`）；另两半**经查仍成立**：`fabric.mod.json` 的 `depends` 恰为 `fabric-api`/`forgeconfigapiport`/`minecraft`，`build.gradle` 生效的 `include(` 为 0 | 只改命名那半 |
| 核心纪律 11 | 已自带「动到时复核」标注 | 不动 |

⚠️ 顺带更正上一版此表的一处数字：本分支 JEI 是 **`29.5.0.28-fabric`**（`gradle.properties`），不是 29.22。

**教训已入证伪表**：复制方法论，继承的不只是经验，还有**它的前提**。前提失效的那条会**主动把人引向错误方向**
（第 2 条尤甚——照它做等于拒绝新基的正确写法）。判据是逐条问「这条依赖的前提在新语境还成立吗」，
而不是只改显眼的版本号；改法是逐条改并注明理由，**别整份重写**。

## 2026-08-13：分支建立与工程设施带入

| 项 | 结论 |
|---|---|
| 工作树 | `git worktree add -b port/26.1.2-fabric … origin/26.1`；已解除对 `origin/26.1` 的跟踪，避免误推到基准仓库 |
| 方法论 | `CLAUDE.md` 重写：隔离纪律 + **原样继承**最终阶段工程纪律与全部证伪表 + 方法论（四方校验的「行为基准」改为 1.21.11 分支） |
| 门禁工具 | `docs/tools/` 七个脚本全部带入，分支常量已改（`release/26.1.2-clean` / `fork/port/26.1.2-fabric` / `mc26.1.2`），残留引用扫描为零 |
| 测试基础设施 | `build.gradle` 补入 JUnit 5 + Mockito + `useJUnitPlatform()`，以及 `gametest` 与 `clientDedicated` 两个运行配置 |
| **`.gitignore` 陷阱** | 新基第 26 行忽略了测试源码目录——**Gradle 会编译但 git 不跟踪**，与 1.21.11 分支踩过的是同一个坑。已删除 |
| 本机设施 | `.mcp.json`、`AGENTS.md` 已复制（二者由跨工作树共享的 `.git/info/exclude` 忽略，不会误提交） |
| 审计文档 | `PORT_26X_AUDIT.md` 自 1.21.11 分支迁入，并追加 §9 测试搬运台账（55 个测试类逐个登记） |

**构建与测试层已实跑验证**（2026-08-13）：

- `./gradlew build` **绿**（`compileJava` 只有 deprecation/unchecked 提示；产出 dev jar 与 remap jar）。
- **`:test` 第一次跑报的是 `NO-SOURCE`**——任务存在、被 `check` 依赖，但 Gradle 直接跳过，
  屏幕上与「通过」一模一样。这正是本仓库栽过多次的纸面接口形状，**不算验证**。
  加入 `BuildInfrastructureSmokeTest` 后重跑：`tests=3 failures=0`，测试层确认在执行。
  该用例是**常驻探针**：它一旦不出现在报告里，就说明测试层又被跳过了。
- ⚠️ **测试的 `workingDir` 是 `build/test-working`**（随配置从 1.21.11 分支带来），
  所以**读文件的用例必须 `Path.of("..", "..")` 回到项目根**。第一次写的两条断言就栽在这里，
  症状是「找不到文件」而非断言不成立。搬 41 个用例过来时逐个注意。
- 观察到但不属于我们的：`shadowJar` 报 `META-INF/services` 重复覆盖，
  新基自带该 services 文件，与本次改动无关。

## 2026-08-13：O1 探路轮完成（`8c439de1a`）

**搬的是配置事务写盘**（`AtomicConfigFileWriter` 160 行 + 其 5 条 JUnit 用例）。
选它打头是因为它零内部依赖、纯 JDK、不碰任何 MC API——用来把「一刀的成本里
有多少是我们的代码、有多少是环境」这两项分开量。

| 量到的 | 结果 |
|---|---|
| 主类与其单元测试 | **逐字搬入、零修改**，API 漂移成本为 0 |
| GameTest API | `gametest.v1` 在 26.1.2 与 1.21.11 **写法相同**，探针一次编过 |
| 真正的阻力 | **全部来自环境与新基，不是我们的代码**（见下三条） |

**三处阻力（都已解决并记账）**：

1. **Gradle 发行包在本机取不到**——见 [HANDOFF.md](HANDOFF.md) 的环境陷阱一节。
2. **`:test` 报 `NO-SOURCE`** 与「通过」在输出上无从分辨，靠常驻探针照出来。
3. **新基的 Xaero 小地图/世界地图崩 GameTest 服务端**：它们以 `runtimeOnly` 声明、
   没有 client-only 标记，被加载进服务端后在 `serverStarting` 抛
   `Registry is not frozen yet!`，`runGametest` 从一开始就跑不起来。
   loom 未拆分环境源集时没有「只给客户端」的作用域，故整体停用（`build.gradle` 有注释说明来历）。
   **要在 dev 客户端看地图可临时取消注释，但不要提交。**

**GameTest 全链路已打通**，判据不是「任务跑绿」而是报告里出现了那一条：
`touhou_little_maid:config_write_game_test_atomic_write_leaves_no_partial_image`
（同批还有原版自带的 `minecraft:always_pass`，故日志显示 2 个用例）。

⚠️ **这一轮的结论不能外推**：这一刀被选中正因为它零依赖。审计 §7.1 里那
**247 个新基已有同名文件的** 才是真正的成本所在，它们要在新宿主结构上重做。

---

# 勿重做（已排除的可能性）

- **跨分支 cherry-pick / rebase**：`origin/1.21.1` 与 `origin/26.x` **无共同祖先**，
  提交级操作在技术上就不成立。差异化只能按行为重做。
- **把 1.21.11 的 `fix(port)` / `fix(client): restore` 那一批搬过来**：它们修的是我们自己在
  1.21.11 上造成的回归，新基没有这些回归。
- **手搬 `src/main/generated`**：datagen 产物，在新基上重新生成。
- **指望「有 26.x 版本」就等于 Fabric 上能用**：KubeJS / Aquaculture / Sophisticated Backpacks
  的 26.x 全是 NeoForge。依赖可用性必须查到**加载器粒度**。

---

# 架构结论（本分支特有）

- **新基把 `EntityMaid` 拆成了多个 manager**（基准 → 26.x 是 294 增 / 2,345 删），
  并新增 `modules/maid-manager-codegen` 代码生成模块。我们的 manager 要按它的约定写。
- **新基有一个同名不同物的 `MaidCombatManager`**（在 `entity.passive`），
  与我们威胁响应用的 `entity.ai.combat.MaidCombatManager` 只是重名，**不要按名字合并**。
- **新基带 `patches/` 与 `rewrite.yml`**（OpenRewrite）：宿主自己用自动化做跨版本迁移，
  动手前值得先读懂，可能省掉大量手工改写。
