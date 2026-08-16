# Touhou Little Maid 26.1.2 Fabric 当前状态

**本文是唯一活动状态账本，只写「现在什么是真的」。**
开放项在最前面且各自自带验收标准与下一步；已关闭的压成一行 + 提交号；
**不写会过时的数字**——那些跑 `python docs/tools/facts.py`。
移植范围、边界与测试台账在 [PORT_26X_AUDIT.md](PORT_26X_AUDIT.md)，不要在本文复制。

## 结论

当前树 = `origin/26.1`（MC 26.1.2 Fabric）+ 一层工程设施 + **审计 §3.A「服务器规则体系与配置所有权」已整块落地**
（配置事务写盘 → 世界规则本体与文件层 → 读点改道 → 网络层与配置菜单 → op/deop 重发）
+ **§3.B「智能应战 / 威胁响应」已整簇落地**（统一目标策略 → 瞬态应战 → 每女仆响应策略与配置屏 → B1 远程应战）
+ **§3.C「AI 聊天 · 站点 · TTS/STT」已整块落地**（AI 配置店 → 站点层 →
聊天管理层与 agent 层含 C2 三笔结构性修复 → 网络层 / 试听 / §3.H 命令面 / 17 个 lang →
**AI 设置屏五页**）
+ **§3.F 的一处宿主回归已修**（扛女仆时玩家手臂摆抱姿）。
**除此之外，代码行为等同于代码宿主，不等同于我们 1.21.11 的行为。**
构建、JUnit、GameTest 三条链路均已实跑验证；**2026-08-14 起有了单人档实机验收**
（棋局簇、背包四型、世界规则单人侧、抱姿、TACZ 五项、**§3.B 威胁响应整簇**——见 O1，
专服/存档升级侧仍开放）。

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

**TACZ 兼容刀（`167608ab9`）：单人档实机验收通过（2026-08-15 用户实测，五项全过）**：

- ✅ 枪械工作模式（开火 / 背包扣弹换弹 / 走位）；持枪与换弹动画；背部枪械渲染；
  敌我免伤（子弹不伤主人 + **爆炸不炸女仆**——`ServerExplosionMixin` 重设计触发点实证生效，
  四个 tacz mixin 对真 jar 零错误织入，两处漂移修正同获实证）；
  配置菜单三滑块（isModLoaded 显示 + 保存生效）
- 测试环境：dev 客户端单人档，TACZ jar 放 `run/mods/`（`libs/compile_only` 那份的拷贝，
  均不入库）；运行期前置 FCAP 升 26.1.5（`7a1c0bce`，TACZ 硬下限）
- 按「验收连同环境记账」：枪械三键是世界规则，**专服上的同步 / 菜单可见性 / reload
  未验**——随下表既有专服三项一并开放
- ⚠️ **弓弩解绑（`c6aa20b24`）本身不构成实机验收项**：`performRangedAttack` 的调用方
  全在远程任务的 brain 行为里（实查），非远程任务女仆在 B1（应战活动）落地前没人调它——
  「农场女仆手持弓被打能还击」要等 §3.B。B2 的判定面由 `RangedResolveGameTest` 三例钉住，
  弓手/弩手行为逐字不变（当前任务优先）

**§3.B 威胁响应大簇（`5fc0baf4f` `be9c15fff`）：单人档实机验收通过**（2026-08-16 用户实测，
dev 客户端存档「新的世界」，全程无崩溃、无 mixin 失败、无封包处理失败、无 AI 超时）：

- ✅ 三档响应策略（关闭＝挨打不还手 / 自卫＝只护自己 / 护主＝主人被打或主人打敌对生物也上）
- ✅ **远程站定（B1 主症状）**：手持上了箭的弓/弩被打时站定射击不再贴脸；没箭时照旧走过去打近战
- ✅ 非远程工作模式手持弓也能还击（弓弩解绑 + B1 合流后的效果）
- ✅ 应战不改常驻任务与日程；玩家指令（切任务/切日程/开关 home/坐下）立刻打断应战
- ✅ 敌我判定（已驯服宠物含横扫波及不受伤）；配置屏滚轮翻页
- ✅ 近战武器耐久正常下降（此前永不损耗，本簇顺带修掉的宿主回归）

⚠️ **按「验收通过要连同环境一起记账」：以上仅覆盖单人档**——它是「客户端与服务端共用同一份
数据」的特例。本簇在专服上才会分叉的部分另行验证如下。

**专服侧（2026-08-16 用 MCP rig 实测，真 dedicated server）**：

- ✅ **`ConfigData` 的 `combat_response_policy` 三态全部验证通过**（判据是 rig 读回的是
  `ConfigData[...]` 对象而非原始 SNBT，即确实经 codec 解码）：
  ① 默认值 `PROTECT_OWNER` **不写入 NBT**（`optionalFieldOf(name, default)` 的标准行为），
  读回靠缺键回落——旧档兼容因此成立，且默认策略不占存档空间；
  ② 非默认值 `"self_defense"` 写入后解码为 `SELF_DEFENSE`；
  ③ 坏值 `"future_invalid_value"` 回落 `PROTECT_OWNER` **且其余字段完好**
  （509 / ALL / 1.0）——宽容解码不会把整份配置打回默认。
- ⬜ 配置屏经 `MaidSubConfigPackage` 的往返：**需要真客户端连入**，rig 是服务端侧工具，验不到。
- ⚠️ **应战触发未能在 rig 上复现，证据不足以定因，不作结论**。已排除：伤害确实生效
  （女仆 20→18）、距离 1.66 格在近战范围内、策略为 `SELF_DEFENSE`、僵尸 `NoAI` 满血不自燃
  （判据干净）。未排除：`/damage ... by <entity>` 造出的 `DamageSource` 与真实攻击是否等价、
  未驯服女仆是否影响、传感器是否扫得到 `NoAI` 实体。
  **下一步取证**：应战状态（`EMERGENCY_COMBAT_ACTIVE` / `ATTACK_TARGET`）有意不持久化，
  rig 读不到，必须插 `[TLM-QA-*]` 诊断桩才能定案——而插桩要编译，**与 rig 同时跑 gradle 是禁止的**，
  须先停 rig。⚠️ 单人档已由用户实测三档策略全部生效，故这条**不是**「功能不工作」的证据。

**§3.C AI 大簇（`fbb360d39`…`f74380bc1`）：零实机，全部待验**

代码面四刀已闭合（GUI 五页见 O7），但**没有一条实机凭据**——AI 聊天要真站点密钥才能验：

| 待验 | 怎么验 |
|---|---|
| AI 配置文件三分（`-ai-server.toml` 实例级 / `-ai.toml` 个人 / `-common.toml` 不再含 `[ai]`） | 用装过 stock 26.1 的存档启一次，看 `[ai]` 旧值有没有各自迁到位、`-common.toml` 里那节是否已被剥掉 |
| 站点密钥不下发 | 客户端开站点编辑屏，密钥框应显示哨兵而非明文；只改地址保存后密钥不丢 |
| 站点文件损坏的降级 | 手工把 `sites/llm.json` 改坏再进世界：应只降级 LLM 一家，且服务不消失 |
| `/tlm ai_chat status` / `sites` / `reload` | 专服上逐条跑，看回执有无裸 lang 键 |
| `AiChatDevCommand` 驱动一轮对话 | **不需要真客户端**，rig 可直接验 C2① 的动作旁路真的会执行工具 |
| C2① 长对话后仍执行指令 | 聊满几十轮后说「跟着我」，应真的跟上来而不是只答应 |
| C2② 切语种后 TTS 不漂回 | 选一个与聊天语言不同的语音语种，连说三轮以上 |
| C2③ 云端识别不冻结客户端 | 网络不畅时连续识别，渲染线程不应卡住 |

**AI 设置屏五页（O7，`f01ae9bce`…`fcbd03296`）：单人档实机验收通过**
（2026-08-17 用户实测，dev 客户端单人档；启动无 mixin 失败，日志 ERROR 全为 dev 环境噪音
——Realms 鉴权 / user properties / Controlify 可选靶点 / Jade 的 REI compat）：

- ✅ 底部概要三态（跟随默认 / 已覆盖 / 已回落）且显示站点**名字**不是内部 id；
  点过弹出项后未动过的那一项仍是「跟随默认」（旧实现「补一次默认就再也回不去」已不复现）
- ✅ 🌐 在系统 TTS 下出黄色警告、换站点后消失；系统站点编辑屏两行说明在
- ✅ 🔊 弹层首项「跟随默认」；每行 ▶ 只出声不改选择；在途时其余 ▶ 置灰
- ✅ 三处弹出列表：全候选 / 当前项高亮 / 空间不足向上弹 / 滚轮归弹层 / 点外面只关弹层
- ✅ 试听出声，失败红字在屏内
- ✅ 检查配置：判词 + 悬停原因 + 8 秒还原；与「添加模型」不重叠；系统 TTS 站点上无保存/检查两颗
- ✅ STT 全局单选：侧栏无「语音输入站点」标签；四行来源各显示状态；齿轮进表单、返回回本页

**☑️ 两项口径不同，单列**（用户 2026-08-17 裁定**延后复测、目前视为通过**；
与上面的 ✅ 实测项不是一回事，故分开记——同一族做法见饮用音效那条）：

| 待复测 | 复测判据（写细一点，下一轮照着走即可） |
|---|---|
| **跨页共享暂存** | ① LLM 页翻开关**不保存** → 切语音输入页点保存 → **完全退出设置屏再进** → 该开关应已是新值；② 反向也走一遍（语音输入页改代理不保存 → 去 TTS 页保存）；③ 保存按钮的「*」应在提交后熄灭 |
| **密钥三态** | ① 已配密钥的站点只改 URL → 保存 → 重进：密钥**仍在**（灰字「已配置」）；② 点「清除」→ 保存 → 重进：灰字应为「未配置」；③ 填新密钥 → 保存 → 重进：灰字「已配置」且服务可用 |

⚠️ **这两项恰好是「误判为通过」代价最高的两条**：前者是本刀契约的主角
（同一条契约我曾写下三遍而实现没做），后者一旦判错就是**静默丢掉一把已配好的密钥**。
源码接线各有契约测试钉着，但**值有没有真上服务器、密钥有没有真留住，只有实机能证**。

⚠️ **按「验收通过要连同它的环境一起记账」：以上全部仅覆盖单人档。**

**仍开放（都需要专服/局域网或旧存档，单人档验不到）**：

| 待验 | 为什么这轮验不到 | 怎么验 |
|---|---|---|
| 世界规则从 stock 26.1 存档升级 | 需要真有旧值的 `touhou_little_maid-common.toml` | 用装过 stock 26.1 的存档进一次，看旧值有没有进 `serverconfig/touhou_little_maid-server.toml` |
| 配置菜单可见性（局域网客机 / 专服非 OP） | 单人档只验了单人身份 | 局域网客机与专服非 OP 各开一次菜单，看「玩法设置 / 高级设置」两栏在不在 |
| 专服保存 → `/tlm config reload` | 需要真专服 + 真客户端 | 改一项保存，确认提示出现且值未生效；跑 reload 后生效并同步 |
| **AI 设置屏的侧栏权限边界**（O7 唯一未验项） | **单人档按定义测不出来**（不是「没顾上」）：`GameModeUtil.canEditSite` 的**第一个分支** `isSinglePlayer(player) → return true` 在任何权限判定之前就短路，`/deop` 改不动它，于是 `insufficientPermissions` 恒为 false，那条分支不可达 | **最便宜的路径是局域网客机**（非房主）：`isLanHost` 为假、`isDedicatedServer` 也为假 → `canEditSite` 返回 false。判据：侧栏**只剩「语音输入设置」一栏**，且落点就在那一页——不该出现「高亮的标签在侧栏里根本找不到」。专服非 OP 同理 |

**O2 · 反向缺口：宿主迁移时丢掉的东西（判定已完成，进入实施排期）**

差异化清单只回答「我们多出来的搬齐了没」，回答不了「宿主丢了什么」。
账本 `docs/tools/host_gap_ledger.tsv`（222 条全登记），核对 `python docs/tools/host_gap.py --ledger`。
方法与完整结果见审计 §7.8。

**判定分布与「已补回 / 待补」分栏不在本文写死**——跑 `python docs/tools/host_gap.py --ledger` 看实时数
（本文此前写死过一次，两天内过时了两回，正是本文开头「不写会过时的数字」那条禁令针对的形态）。
动手补的过程中已改判三条：`ClientBoardStateTooltip` 待定→丢失并已补、`MaidGameRecordManager` 丢失→替换、
`IBackpackData` 替换→丢失——**判定的最终校验是真去补它**。

「丢失」归成五簇，**五簇、零散与 TACZ 批全部清零**（TACZ 批 2026-08-15，`167608ab9`）——
「丢失」58 条已全部补回，实时数跑 `--ledger` 核对：

| # | 簇 | 条数 | 后果 | 备注 |
|---|---|---|---|---|
| 1 | ~~**女仆背包四型**~~ | ~~15~~ | **已补完 4/4**（末影箱 `8efc995d4`、工作台 `60aa8dcc6`、熔炉 `40b8cfde0`、液体 `ff7ee685d`） | 见已关闭表 |
| 2 | ~~**棋局存档与记录层**~~ | ~~8~~ | **已补完**（`e778676cc`…`ba5b8af65`） | 见已关闭表 |
| 3 | ~~**REI 集成**~~ | ~~5~~ | **已补完**（`30d0d2795`：四件逐字 + Maker 重写走宿主 `ClientRecipeEvent.ALTAR_RECIPES`；REI 实证加载且 GameTest 28/0） | 见已关闭表 |
| 4 | ~~**原版替换功能**~~ | ~~4+2~~ | **已补完**（`6cbe559ba`，含两处改判：InitSpecialItemRender 替换→丢失、ReplaceableBakedModel 待定→丢失） | 见已关闭表 |
| 5 | ~~**模型图标缓存**~~ | ~~4+分支~~ | **已补完**（`e45ea33a6`：4 文件 + 5 处丢分支 + 队列接线 + 配置菜单 lang，契约测试三形态红测过） | 见已关闭表；⚠️ 零实机项在 O1 |
| — | ~~零散~~ | ~~2~~ | **已清零**：`GifTexture` 补回 `b70145a20`（宿主 FIXME 搁置还原）；`RenderFixer` **改判 丢失→无关**——origin 那个补丁是 1.21.1 时代 Carry On 附魔渲染 bug 的绕行，判据载体（`BufferSource.fixedBuffers`）在 26.1.2 submit 管线不存在，且行为基准在 render-state 重写时已放弃它、无等价承接（renderer 树零命中实查）。现存 Carry On 兼容 = tag + molang 纯数据层，与基准一致，依赖不接回（零消费者不留空壳）。若实机复现同类 bug → 修上游，属超基准新决策 | — |
| — | ~~TACZ 批~~ | ~~14~~ | **已补完**（`167608ab9`，见已关闭表）——至此「丢失」58 条全部清零 | 零实机项在 O1 |

⚠️ **还有一批「待定」**（实时数跑 `--ledger`）：子代理报「未找到」而我尚未复核，一律不写成结论
（上一轮子代理判定被逐条推翻过）。`--ledger` 会一直提示，忘不掉。

**优先三簇已复核完（2026-08-15，16 条全部改判「替换」，证据在账本各行）**：
箱子类型 5 条 → WirelessIO 改走 Fabric transfer `ItemStorage.SIDED`（覆盖为超集）；
任务数据 4 条 → 唯一内部注册项 `maid_attack_list` 由 `AttackListData` + Fabric attachment 全链路承接；
战利品 7 条 → fork 自制 GLM 管道被 Fabric loot v3 `LootTableEvents.MODIFY` 承接，
`loot_table_type` 全仓 JSON/源码零引用。
「唯一可能再藏整块玩法丢失的地方」**排除**——但代价是宿主取消了 `ILittleMaid` 的
`addChestType` / `registerTaskData` 两个第三方扩展点，第三方需求出现时再评估（账本行有锚点）。

**O3 · §3.A 只剩一项（配置三层已全部闭合）**

配置三层**全部完整**：世界规则那一层（本体、文件层、读点改道、网络层、配置菜单、
`/tlm config reload`、op/deop 重发）与 AI 那一店（实例级 `AiServerRuleConfig` + 个人
`AiClientConfig`，随 §3.C 第一刀 `fbb360d39` 落地，四个恢复锚点全部认领）。剩下一项不属这一层：

| 缺口 | 现状 | 恢复锚点 |
|---|---|---|
| `ExperimentalConfig.SMOOTH_FOLLOW` | 类与值都未建；配置项要与消费者（§3.E 跟随手感）同批落地 | `ServerRuleConfig.values()` 的 javadoc |

⚠️ 上表每一条在代码里都有对应注释，**不要只靠本表**——本表会过时，注释在改到时才会被看见。

**O6 · TACZ 兼容 + 远程应战批 —— 代码面全部闭合（B1 于 2026-08-16 随 §3.B 落地），余一项**

TACZ 14 条与 B1–B7 已全部落地（见已关闭表）。本项只剩：

| 余项 | 归属 |
|---|---|
| 「持枪寻路异常」 | 维持降级：1.21.11 六组受控对照复现不出，等用户给复现现场再立案；半径耦合是真实性能面但非该症状成因，BFS 解耦属超基准候选未做 |

再动 §3.B 相关件时仍**两份归档清单都过一遍**：
[archive/PORT_TACZ_AND_RANGED_AI.md](archive/PORT_TACZ_AND_RANGED_AI.md)（专题）与
[archive/PORT_TACZ_AND_RANGED_AI_FULL.md](archive/PORT_TACZ_AND_RANGED_AI_FULL.md)
（全量，尾部有 1.21.11 API 事实表——26.1.2 动到箭/远程时先查它）。
取证纪律两条随单收下且长期有效：**创造模式玩家不被怪物索敌**（战斗类取证先证明触发条件成立）；
**比值型聚合指标分母趋零会爆炸**（两数不自洽就回看原始序列）。

**O4 · 前置项目未决**
- **YSM**：Fabric 26.1.2 上不存在任何实现（本体仅 NeoForge 且闭源，OpenYSM 无 26.x）。
  要保留该特色，须先把 `gege-tlph/OpenYSM-Updated` 移到 26.1.2——**独立项目，规模未评估**。
- **Patchouli**：官方有 26.1 beta，我们维护的 fork 需跟进。

**O5 · 公开发布链路尚未建立**
本分支还没有清洁分支、没有公开远端分支、没有 CI。`tree_equiv.py` 与 `git_hygiene.py`
里已经写好了目标 ref 名（`release/26.1.2-clean` / `fork/port/26.1.2-fabric`），
但**那两个 ref 还不存在**，相关门禁步骤现在必然跳过或报缺失——属预期，不是缺陷。

**O7 已关闭**（2026-08-17，见已关闭表）。§3.C 的代码面至此整块闭合，实机验收项在 O1。

---

# 已关闭（一行结论 + 提交）

## 2026-08-17：O7 · §3.C 的 AI 设置屏五页（`f01ae9bce` `5cc3f0848` `1aa2e46ac` `fcbd03296`）

**§3.C 代码面至此整块闭合。** 这一层**不能整取**：26.1.2 删掉了整个 `GuiGraphics` 类
（渲染改成 `GuiGraphicsExtractor` + `extractRenderState` 抽取模型），照搬基准那 18 个屏
得到 60+ 个「找不到符号」。正确方向是**把我们的差异逐个铺到宿主已按 26.1.2 写好的屏上**，
逐 hunk 分「我们的功能」与「1.21.11 API 漂移」，后者一律丢弃。

| 刀 | 内容 |
|---|---|
| `f01ae9bce` | 基座：新建 `SiteEditorLayout`（三屏共用布局常量，纯 int 故可 JUnit 断言）+ `FlatColorButton` 四项能力 + 三条侧栏组标题 |
| `5cc3f0848` | 三个站点编辑屏：密钥三态、「检查配置」按钮状态灯、TTS 语种诚实标注、STT 编辑屏解耦 |
| `1aa2e46ac` | 五页重做 + 删 `AIChatSettingsSTTSiteScreen`/`STTSiteButton` + 新建 `AIChatSettingsUsageScreen` |
| `fcbd03296` | 五条契约用例（全部红测）+ 两处 C2③ 漏网修复 |

**三条产品判据（长期有效，改这一层前先读）**：
① **侧栏分栏 = 权限边界，不是服务种类**——原先四个标签按 LLM/TTS/STT 排，
权限边界横切在中间且界面上完全看不见，没权限的玩家点进去才撞上一行红字。
② **「用量管理」的收录判据 = 只改变花多少钱、不改变功能**——按这条
`AUTO_GEN_SETTING_ENABLED` 不进那一栏。守不住它，这栏会变成 LLM 杂项的垃圾桶。
③ **不许有说谎的标签**——系统 TTS 站点不显示保存/检查两颗（检查必然停在「还没填地址」，
保存写回逐字相同的副本）；🌐 语种在系统 TTS 上是无效开关，故加黄色悬停警告。
**一个能设置却不起作用的选项，比没有这个选项更糟**；采取的是诚实标注而非行为修复
（原版朗读器接口给不出语种参数）。

**「任何一页保存都提交全部改动」配了按屏枚举的测试**——我曾在别处把这句契约写了三遍
而语音输入页根本不碰共享暂存，还无条件闪「已保存」。识别依据取「它覆写没覆写
`addFooterButtons`」这种结构量，不取按钮标签字面量，并自带「认出了几屏」的下限断言。

**两处 C2③ 漏网被新收紧的判据当场照出**（javap 实证，非推理）：
`LocalPlayer.sendSystemMessage` 在 26.1.2 的字节码就是
`getChatListener().handleSystemMessage(component, true)`——与已被禁用的
`displayClientMessage` **是同一条链，只是换了个名字**，而宿主迁移时正是机械改写成了它。
`AIChatScreen` 的玩家回显（格式 `<玩家名> 文本`，正好命中 `guessChatUUID`）与
`TTSPlayer2Client` 的两处错误提示都在其中，均已改走 `ClientLocalChat`。
判据已从「写了 displayClientMessage」升级为「谁把文本送进了 ChatListener」，扫描面加 `client/gui`。

**账本改判一条**：`MaidAIChatConfigButton` 待定 → **无关**。它在 `origin/1.21.1` 与行为基准
**都是零调用者的死代码**（全仓 git grep 三个 ref 实证），贴图的真实消费者另有其人。
交接清单曾把它列为「本刀新建三件」之一，**那是错的**。

门禁：compileJava 0 错；JUnit **180 例 0 失败**（45 个测试类，5 个新类逐个实见于
`build/test-results`）；GameTest **78 例 0 失败**；`--ledger` / `doc_lint` 各自单独跑退出 0。
⚠️ **零实机**——GUI 是玩家可见层，逐屏点检清单在 O1。

## 2026-08-16：§3.C AI 聊天大簇（服务端与链路侧四刀，GUI 五页见 2026-08-17 那节）

`fbb360d39` `4137ea764` `0d5124259` `f74380bc1`。**C2 三笔结构性缺陷修复全部落地。**

| 刀 | 内容 |
|---|---|
| `fbb360d39` | **AI 配置店**：`AIConfig` 按所有权拆成实例级 AI 规则（`AiServerRuleConfig`，服务器权威、spec 有意不注册）与个人配置（`AiClientConfig`）；`ServerRuleConfig.get()` 按键归属路由；两个规则包合流分拣；ArchUnit 认领清单改两店并集。删 `TTS_LANGUAGE`（语种是纯女仆属性）与 Cloth 的 LLM/TTS 两组（服务器权威值不许经客户端直写 TOML） |
| `4137ea764` | **站点层**：站点文件损坏不再整批放弃（那个 P0 的根因——表空后 `getSTTSite` 恒 null，玩家被告知「服务器不提供」）；密钥哨兵在编码结果的 tag 上统一处理，明文永不下行；专服不管理 STT 站点因而不生成 `stt.json` |
| `0d5124259` | **聊天管理层 + agent 层**，含 **C2①**（判定/执行搬出对话通道、先做后说）、**C2②**（待合成文本改由无历史的独立翻译请求产出）、**C2③**（本地提示走 `ClientLocalChat`） |
| `f74380bc1` | **网络层**（四个新包）、站点保存事务、试听链路、**§3.H 命令面**（`status`/`sites`/reload 收拢 + `AiChatDevCommand`）、17 个 lang 文件并入 |

**C2③ 的前提在 26.1.2 复核成立且更硬**（javap -c）：
`displayClientMessage → ChatListener.handleSystemMessage → guessChatUUID →
Minecraft.isBlocked → PlayerSocialManager.isBlocked → pendingBlockListRefresh.join()`
——在调用线程上**硬等** Mojang 屏蔽名单刷新。26.1.2 还给了个更贴切的落点：
`ChatComponent.addMessage` 已私有化，按来源拆成 `addServerSystemMessage`（原版走这条）与
`addClientSystemMessage`（本机自己打的提示），后者正是我们要的语义。

**两处按范围裁剪，各留恢复锚点**（不预留空壳）：`ChatClientInfo` 的 YSM 短路（§7.2 范围外）、
`table_food` 两个上下文（§3.I 未移植）。**知识文档同步裁剪**：`en_us.md`/`zh_cn.md` 各剔除
3 段（§3.E 跟随·浅水·农作站位、§3.I 桌上食物），契约测试的锚点表与数值事实表同步收窄——
知识文档是直接喂给模型当事实的，描述一个本构建做不到的行为比不描述贵得多。

**四处静默故障被闸门当场照出**（都是「编译打包启动全正常、功能从不执行」那一族）：
① 四个新 payload 一个都没登记进 `NetworkHandler`（J 组 `PayloadRegistrationInvariantTest`
此前也没搬，一并补上并红测）② `MaidAIChatManager` 漏了 `@MaidManagerDef` 导致
`EntityMaid.aiChatManager` 整个不存在 ③ 三个新 GameTest 类未登记 `fabric-gametest`
entrypoint ④ `SiteSecretRedactionTest` 的字段集取自它本该看管的那张表（自证式断言，
删掉 `SECRET_ID` 原有 7 条用例一条都不红）——补了一条独立取证的断言。

门禁：compileJava 0 错；JUnit 75 → **168** 例 0 失败（新增 19 个测试类）；
GameTest 64 → **78** 例 0 失败（新增 4 类，用例名逐个实见于 `report.xml`）。
七轮红测各红在正确断言上（读口路由 / ArchUnit 认领 / 迁移次序 / 脱敏字段表 / 技能优先级 /
站点降级 / C2 三笔 / payload 登记）。

⚠️ **全部凭据来自自动化门，尚无实机验收**：AI 聊天要真站点密钥才能验，专服两侧分叉
（站点表、AI 配置文件）单人档按定义测不出来。实机项见 O1。

## 2026-08-16：§3.B 威胁响应大簇整簇落地（`5fc0baf4f` `be9c15fff`）

审计 §3.B 的框架层与 B1 一并闭合，`origin/26.1` 上**无同名文件**，属可直接搬那一档，
但全部按宿主结构重做（manager codegen / attachment / ActivityData 声明式活动）。

| 刀 | 内容 |
|---|---|
| `5fc0baf4f` | 统一目标策略 `MaidTargetingPolicy` + 瞬态应战 `MaidEmergencyCombatManager` + 每女仆响应策略（关闭/自卫/护主，落 `ConfigData` attachment）+ 配置屏重做（`MaidConfigLayout` + 可滚动列表，响应策略作首行）+ 周边七处应战护栏 |
| `be9c15fff` | B1：应战活动接三条远程行为（走位 0.5 / 射击 (2,20) / `GunShootTargetTask`）+ `MaidEmergencyWalkToTarget` 站定 + 唯一判据 `isHoldingUsableRangedWeapon` + 两件 AnyItem 任务判据改 `Predicate<EntityMaid>` |

**宿主同名冲突按管理器约定化解**：新基已有同名不同物的 `entity.passive.MaidCombatManager`
（战斗执行层，且占着 `getCombatManager` 访问器），故我们的类命名
`entity.ai.combat.MaidEmergencyCombatManager`、alias `emergencyCombatManager`——
**不按名字合并**（审计早有警示，本轮照办）。

**顺带修一处宿主回归**（本簇新测试照出，与威胁响应无关）：女仆近战武器**永不损耗**。
宿主 `doHurtTarget` 调 `Item.hurtEnemy` / `Item.postHurtEnemy`，而 26.1.2 这两个方法的
字节码就是一行 `return`（javap -c 实证），耐久实际由 WEAPON 组件驱动的
`ItemStack.postHurtEnemy` 承担。已改调 `ItemStack` 版。

**四处 26.1.2 实查漂移**：`Brain.addActivityWithConditions` 已无 → `ActivityData.create(
activity, pairs, conditions)` 声明式；`ServerLevel.setDayTime` 已删（日程改
`EnvironmentAttribute` 驱动）；`Mob.setTarget` 与 `getTarget` **双重**经 `asValidTarget`
过滤（创造/旁观玩家一律滤成 null）；`Item.postHurtEnemy` 空壳化（见上）。

门禁：compileJava 0 错、JUnit 75/0（新增 `MaidConfigLayoutTest` 4 例）、
GameTest 64/0（新增 33 例，四个类的 `fabric-gametest` entrypoint 均已登记）。
**三轮红测各红在正确断言上**：① 摘 `EMERGENCY_COMBAT_ACTIVE` 出 `MaidBrain` 记忆列表
→ 19 条红在「应战进不去」；② 摘 `restoringPersistentState` 分支 → 精确 1 条红在
「NBT 恢复被误判为玩家指令」；③ 判据换成 `canUseNonMeleeWeapon` → 精确 2 条红，
分别是「非远程任务应判为可用远程武器」与「没有箭时不应判为可用远程武器」。

**单人档实机验收 2026-08-16 通过**（用户实测八项全过，含 B1 主症状「持弓站定射击 / 没箭回落近战」；
全程无崩溃、无 mixin 失败、无封包处理失败、无 AI 超时）。按「验收连同环境记账」：
**专服侧仍开放**——`ConfigData` attachment 同步与配置屏发包往返在单人档里两侧是同一份数据，
按定义测不出来，详见 O1。

## 2026-08-15：TACZ 兼容整刀 + 弓弩解绑（`167608ab9` `c6aa20b24`）——「丢失」58 条全部清零

账本 14 条 TACZ 件全部补回。依赖 TaCZ Refabricated 26.1.2_R1（compileOnly + 缺件守卫，
jar 不入库）；fabric-api 0.149.1→0.155.2、loader 0.19.2→0.19.3（其 fabric.mod.json 实查下限，
与 1.21.11 同型「升 API 被 TACZ 逼的」）。

**26.1.2 jar 重验（上游明说与 1.21.11 是两套实现）**：18 个引用类全在、六个注入锚点
javap -c 逐个证实；两处真实漂移——`GunAnimationStateContext` 目标 lambda `$8→$0`、
`AbstractGunItem.canReload/hasInventoryAmmo` 去 static 化。`tacz$getItemHandler`
描述符两版一致。

**宿主残件盘点让实际工作面比交接清单小得多**：分发器两件是宿主留的空壳 stub 且
调用点全活着（TaskManager/AnimationManager/ConditionManager.tac）、五个 tacz 资源与
66 条枪械 lang 宿主全留着（只按 B7 把 `.desc`/`.condition.has_gun` 六语言去 SBW 字样）、
InitItems/InitAttribute/三键声明也在。真正新建的是 tacz 内层 7 件 + common/ai 3 件 +
4 mixin + GunRecognitionRange + 事件基础设施。

**宿主写法适配（行为不变）**：IMaid→EntityMaid；IGeoLocatorSource→`GeoModelState`；
背包位移走 RENDER_DATA_CACHE；库存按 Fabric transfer 遍历——⚠️ `ItemUtil.getStack` 是
拷贝而非 Forge 活引用，弹药盒改完必须 `setStackInSlot` 回写；`targetConditionsTest`
宿主收已解析 int，调用点经 `ServerRuleConfig.get` 解析。

**配置所有权**：三键 initCommon→initServerRule（世界规则，TOML 键名不变），入
`values()` 认领（JUnit 钉子红测过：摘键当场红）；`ConfigFileMigration` 按 `values()`
全枚举自动随迁；Cloth 三滑块从个人配置段裸读写移入 server.combat 段走 session +
isModLoaded（**两处不可并存**，否则菜单写 TOML 绕过服务器权威通道）。

**事件基础设施随刀补齐（HEAD 原缺）**：`LivingAttackEvent` 四触发点（hurtServer×2 /
hurtClient×2，javap 复验）+ `ExplosionEvents` + `ServerExplosionMixin`（26.1.2 爆炸
重构后 DETONATE 触发点重设计：WrapOperation 包 `hurtEntities` 的 getEntities 取表，
列表可变即免疫语义）。⚠️ **回传情报（告知用户转达 1.21.11 分支，勿跨分支写文档）**：
1.21.11 迁移期把 origin 的 `ExplosionMixin` 触发点丢了——事件类在、零触发点，
「女仆免疫子弹爆炸」在那边从不生效。

**弓弩解绑（B2，`c6aa20b24`）**：`resolveImplementation` 当前任务优先且不加 isWeapon；
`MaidCombatManager.performRangedAttack` 改用它，敌我判定门（MaidTargetingPolicy）
留 §3.B 锚点。红测：注入 isWeapon 要求 → 「当前任务胜出」红报「实得 crossbow_attack」。

门禁：compileJava 0 错、JUnit 71/0（新增 GunRecognitionRangeTest 6 例）、
GameTest 31/0（新增 RangedResolveGameTest 3 例，entrypoint 已登记；服务端启动实证
新 mixin 织入）。**单人档实机验收 2026-08-15 通过**（用户五项实测，见 O1；
运行期前置 FCAP 26.1.4→26.1.5 随验收补上，`7a1c0bce`）；专服侧仍开放（O1 表）。

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
