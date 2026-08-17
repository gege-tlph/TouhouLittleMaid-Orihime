# Touhou Little Maid 26.1.2 Fabric 当前状态

**本文是唯一活动状态账本，只写「现在什么是真的」。**
开放项在最前面且各自自带验收标准与下一步；已关闭的压成一行 + 提交号；
**不写会过时的数字**——那些跑 `python docs/tools/facts.py`。
移植范围、边界与测试台账在 [PORT_26X_AUDIT.md](PORT_26X_AUDIT.md)，不要在本文复制。

## 结论

当前树 = `origin/26.1`（MC 26.1.2 Fabric）+ 一层工程设施 + **审计 §3.A「服务器规则体系与配置所有权」已整块落地**
（配置事务写盘 → 世界规则本体与文件层 → 读点改道 → 网络层与配置菜单 → op/deop 重发 →
**个人配置只在物理客户端注册**，2026-08-17 补，此前这句「整块落地」不准确）
+ **§3.B「智能应战 / 威胁响应」已整簇落地**（统一目标策略 → 瞬态应战 → 每女仆响应策略与配置屏 → B1 远程应战）
+ **§3.C「AI 聊天 · 站点 · TTS/STT」已整块落地**（AI 配置店 → 站点层 →
聊天管理层与 agent 层含 C2 三笔结构性修复 → 网络层 / 试听 / §3.H 命令面 / 17 个 lang →
**AI 设置屏五页**）
+ **§3.E「寻路 · 工作任务 · 跟随手感」已整块落地**（农场站位 → 浅水泳姿 → 跟随手感与
`SMOOTH_FOLLOW`，2026-08-17；工作面由跨版本 wiki 的行为面指纹机械产出，不是抄交接文档）
+ **§3.F「渲染与标记」三项里两项已落地**（追踪标记搬 HUD、手办/坐垫记忆化；
YSM 那项依赖 O4 前置。顺带修掉宿主在罗盘/范围可视化上的四项回归与更早的抱姿回归）。
**除此之外，代码行为等同于代码宿主，不等同于我们 1.21.11 的行为。**
构建、JUnit、GameTest 三条链路均已实跑验证；**2026-08-14 起有了单人档实机验收**
（棋局簇、背包四型、世界规则单人侧、抱姿、TACZ 五项、**§3.B 威胁响应整簇**——见 O1，
专服/存档升级侧仍开放）。

---

# 开放项

**O1 · 待入世实测（2026-08-14 单人档验收后收窄到专服/存档侧）**

**已通过的单人档验收**（2026-08-14/15 用户实测；按「验收通过要连同环境一起记账」，
**以下仅覆盖单人档**——客户端与服务端共用同一份数据的特例，两侧数据可分叉的场景仍开放）：
抱姿 / 棋局簇（Shift 预览、残局右键、战利品表）/ 四种背包全功能与持久化 / 祭坛四配方 /
配置菜单两栏与保存 / 原版替换五开关（含岩浆怪崩溃点复验）/ REI 三项 / **图标缓存全项**。
实机还抓出并当场修掉一个崩溃（替换燃烧中的熔炉背包，`d1fcc58c4`）。
☑️ 饮用音效条件难凑未实测，**用户裁定视为通过、除非后续被报告 bug**——口径与实测项不同，故分开记。

图标缓存那一簇留下两条长期有效的事实：**F3+T 重载后图标暂时变 missing 属预期**
（包重载重填队列，下次开 GUI 自动重缓存）；**缓存屏不暂停，故单人档缓存期间世界在走**
（约 1 分钟，与开着模型 GUI 一致，非缺陷）。五轮排障的根因链见
`7a2d63ca9`/`fdd0d3763`/`be60bdb62` 提交信息，其中「`isPauseScreen()` 冻结 level 时间」
一条已进证伪表。

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

**§3.E 寻路与跟随手感（`051aa37bb`…`e1e67db8a`）：零实机，单人档就能验**

判据侧已有 11 条 GameTest（四条缺陷逐个红测过），但**全是服务端判据**；下面三项它们照不到：

| 待验 | 怎么验 |
|---|---|
| 农场收割不再空转 | 圈一片成熟小麦让女仆干活：应走到作物**旁边**收割，而不是反复往作物上撞。甘蔗/可可/西瓜/南瓜各看一眼 |
| 浅水泳姿 | 一格深的水里走过去：**碰撞箱不该缩、模型不该切游泳**（这一半是渲染端，GameTest 按定义验不到）；两格深水里游泳、上岸、水下吃呼吸食物照旧 |
| 平滑跟随 | 配置菜单「实验性功能」栏开 `SmoothFollow`（默认关＝逐字保持上游行为）：主人走动时女仆跟得紧、主人站定后她能自己干活不被拽回、走远约 20 格才传送 |

**§3.F 渲染与标记（`3558c6410`…`7621c181f`）：零实机，单人档就能验**

判据只到接线一档（🟡，`ClientRenderContractTest` 三条）——**runGametest 是纯服务端，
客户端渲染在那里根本不加载**。四项都得目视：

| 待验 | 怎么验 |
|---|---|
| 追踪标记（主症状） | 手持仆从铃/狐狸卷轴，**开一个光影包**：屏幕上应出现 ▼ 与距离数字且朝女仆方向，**地面不得发白**；转身时标记左右上下跟着动，背对时消失；5 格内不显示 |
| 罗盘与范围可视化 | 手持河童罗盘：标签在上、▼ 在下**指向方块**（此前上下颠倒）；文字**被墙挡住**而不是穿墙 |
| 女仆脚下调试盒 | 看范围时：盒子是**半透明黄**且**正好套在女仆身上**（此前是青蓝、且低一格）；还应有一条 home→女仆的**红线** |
| 手办/坐垫卡顿 | 创造栏翻到手办与坐垫那一页、JEI 搜同名物品：不应再有可感掉帧（此前每帧全量重画） |

**§3.G 第三方兼容（`e5eb3913c` `2d24ece5a`）：零实机**

| 待验 | 怎么验 |
|---|---|
| 女仆不爬森罗家具 | 装森罗厨房/酒馆，在桌椅吧台旁让女仆干活：不该跳上去、也不该站在上面 |
| 酒馆座椅 | 空闲时女仆应自己去坐沙发/吧台凳；**沙发上不该背朝外**；切到工作/睡眠日程时应起身 |
| 葡萄 | 成熟葡萄会被收割；收下的**生葡萄不该被当日常工作餐吃掉**（治疗时可以吃）；女仆不再把葡萄架当垂直捷径 |

⚠️ 这三项都要**装上对应模组**才能验——它们是 compileOnly，`runGametest` 里根本不在。

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

**O3 已关闭**（2026-08-17，`51d0c60de`：`SMOOTH_FOLLOW` 随 §3.E 的消费者同批落地）。§3.A 配置三层至此全部闭合。

**O6 · TACZ 兼容 + 远程应战批 —— 代码面全部闭合（B1 于 2026-08-16 随 §3.B 落地），余一项**

TACZ 14 条与 B1–B7 已全部落地（见已关闭表）。本项只剩：

| 余项 | 归属 |
|---|---|
| 「持枪寻路异常」 | 维持降级：1.21.11 六组受控对照复现不出，等用户给复现现场再立案；半径耦合是真实性能面但非该症状成因，BFS 解耦属超基准候选未做 |
| **TACZ 上游已给出官方 API，四个 mixin 应退役** | 我们钉 `26.1.2_R1`（8-12），用**四个 mixin** 打其内部类。上游 PR #48 **已于 8-16 合并**（`26.1.2_R2` 随之发布，比本兼容刀晚一天）：新增 `AmmoSource` / `AmmoSourceProvider` / `AmmoSourceRegistry`（provider 模式，**首个非 null 生效、未匹配回落原实体**），并把开火 / 换弹 / 枪机 / 动画与状态锁那几条路径的 lambda 与私有逻辑**提取成具名 protected 入口**——恰好覆盖我们四个 mixin 各自的靶点。另附内置弹药查询（JEI/REI 无需额外模组）。**落地前仍须 `javap` 核对包名、符号与签名**：PR 描述不是字节码，且空壳化是版本迁移常见手法。换过去可把四处「对方一重构就断」的耦合减到零 |

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

**O9 · 无线 IO 的绑定面比行为基准宽（2026-08-17 新开，待裁决）**

搬「家具重制」兼容时查出来的，**不限于那个模组**。基准用 `IChestType` 白名单决定
哪些方块算「箱子」，宿主把整套机制换成了 `ItemStorage.SIDED.find(...)`（O2 判为「覆盖为超集」）。
超集这一半是对的，但**基准那张白名单还承担着安全职责**——它有意排除了会**销毁物品**的
回收箱、以及一批带加工语义的机器（灶台/烤架/微波炉/冰柜/工作台/信箱）。
本树按 `ItemStorage.SIDED` 绑定，这些方块**只要暴露了物品存储就都能被绑上**。

| 待定 | 内容 |
|---|---|
| 是不是缺陷 | 玩家把无线 IO 绑到回收箱上，女仆会往里倒东西而东西会消失。基准明确禁止，本树允许 |
| 修法不止一种 | ① 在 `ItemStorage.SIDED` 之上加一层**黑名单**（与宿主架构最小冲突，但黑名单方向不安全：第三方新增的机器默认放行）② 恢复白名单语义（与宿主架构冲突，且要重开被取消的扩展点）③ 判定为可接受，写进「有意分歧」 |
| 为什么不擅自做 | 这已不是移植，是**超基准设计决策**；且 O2 当初把这条判成「覆盖为超集」时没算到安全那一半 |

⚠️ 家具重制的那两个兼容文件因此**判定为「无落点」**：`RefurbishedStorageChestType`
实现的 `IChestType` 在本树不存在，功能需求（无线 IO 能绑它的储物方块）已由超集满足；
它另一半的价值正是上面这条待裁决项。`RefurbishedFurnitureCompat.init()` 的注册表
`SYNCED` 改动同样搁置——审计早写了「26.x 上要重新判断这个代价是否仍必要」，
而判断它需要那个模组的 jar（只发 GitHub Releases，本机没有），**证据不足不下结论**。

**O8 · 行为面对账：只做完了配置面（2026-08-17 新开）**

审计 §7.6 的三面闭合（文件面 / 文档面 / 公开面）**覆盖不到共有文件里的启动期接线**——
个人配置那一层就是这么漏掉的（见已关闭表）。§7.9 新立了第四面并跑通配置面，
其余子系统**尚未对账**，按玩家可见程度排：网络包（发不发、发给谁）→ 存档与 attachment
（持久化什么、同步给谁）→ 资源与注册（生成什么文件、进哪张表）→ GUI（哪个屏在什么权限下出现）。

⚠️ 别读成「其余子系统有问题」——是**没查过**，与「查过没问题」不是一回事。
方法与两条重跑经验（正则要覆盖链式写法；活性判据须与结论正交）写在审计 §7.9。

---

# 已关闭（一行结论 + 提交）

## 2026-08-17：§3.G 第三方兼容（`e5eb3913c` `2d24ece5a`）——两项落地，两项另有判定

| 项 | 结论 |
|---|---|
| 女仆不站上森罗家具 | ✅ `e5eb3913c`。审计表标「宿主已有」，实为**文件在、内容缺**：本树只经零食台那条间接链覆盖了厨房桌子，酒馆家具与整张禁跳标签一条都没有。顺带修掉禁跳判据只挂在第二个分支、且只看脚下方块（女仆是为了**登上**桌椅才起跳的） |
| 森罗酒馆兼容 | ✅ `2d24ece5a`。7 个兼容文件 + **5 处核心接线**——后者是真发现，见下 |
| 家具重制兼容 | ⚠️ **判定为「无落点」**，见 O9 |
| YSM | ⬜ 依赖 O4 前置，Fabric 26.1.2 上该模组不存在 |

**酒馆那刀的真发现**：它依赖两个**我们自己加的核心扩展点**，而它们此前没搬
（`origin/1.21.1` 与 `origin/26.1` 都是 0，实查）——`IExtraMaidBrain#canClimbBlock`
（由寻路消费，把葡萄架从女仆的主动攀爬里摘出去）与 `MaidMealManager#addWorkMealExclusion`
（由默认工作餐消费，让葡萄留着加工而不被日常吃掉；治疗餐有意不受影响）。
**「搬兼容」的实际工作量常常在核心那一侧**，按兼容包的文件数估工时会失准。

依赖 `kaleidoscope-tavern-refabricated:1.2.0.5-fabric+mc26.1.2`（compileOnly）。
全部第三方符号 **javap 逐个核对 26.1.2 的 jar**，不是照抄基准的 import。
⚠️ 一处差点误判：gradle 缓存里躺着基准那份 **1.21.11** 的同名 jar，
**在缓存目录里看到同名文件不等于它是你要的那个版本**——坐标可解析另行实证。

判据四条（`OptionalCompatWiringContractTest`，始终有效，均红测）：entrypoint 登记 /
每个扩展点实现都有 isModLoaded 守卫 / 两个新钩子真的被核心消费 / 第三方类型不许泄漏出兼容包。
另两条（`BlockTagDatagenContractTest`）：森罗家具 7 个 id 必须同时在两张标签里；
**TagBlock 源码里的每个 optional id 都必须出现在入库产物里**（「改了 datagen 却忘了重跑」是纯静默失败）。
⚠️ **不搬基准那条 `OptionalCompatInitializationGameTest`**：它整条被 `isModLoaded` 守着，
而模组是 compileOnly、不在 runGametest 运行时里——不执行却照样绿，正是「零覆盖恒绿」。
`little_maid_extension` 是**第十处静默注册面**。零实机项在 O1。

## 2026-08-17：§3.F 渲染与标记（`3558c6410` `5172a81b9` `f348b4ca6` `7621c181f`）

**审计 §6 的下一项。三项里两项落地，YSM 那项依赖 O4 前置未做。** 与 §3.E 同法：
先机械枚举我方在这一片的提交，再逐文件四树对照——**审计原列的证据表又是下界**，
当场多出 `cb7837b77`（河童罗盘）。**两条根因的前提都在 26.1.2 反编译源上逐行复核过，
不是按补丁名推**（「兼容补丁携带着它对宿主代码形状的假设」）。

| 刀 | 内容 |
|---|---|
| `3558c6410` | 追踪标记搬 HUD：世界内「置顶」= `addLateDebugPass` 里 `clearDepthTexture`，光影下地面整片发白 |
| `5172a81b9` | 手办/坐垫预览状态按数据记忆化：`SpecialModelWrapper` 把 `extractArgument` 的返回值追加进图标缓存 identity，而两个状态类都没有 `equals` |
| `f348b4ca6` | 三条接线判据 + 两张具名允许表（均红测） |
| `7621c181f` | 罗盘/范围可视化四项：缺红线、盒色参数序、盒子低一格、标签与 ▼ 颠倒且穿墙 |

**两处写法按新基改、行为不变**：真实 FOV 在 26.1.2 是 `Camera#getFov()` 且**公开**
（基准当年为 `GameRenderer.getFov` 加过 access widener，本树不需要；且 26.1.2 的
AW 文件已改名 `.classtweaker`）；`Gizmos.billboardText(String, Vec3, Style)` 直接可用。

⚠️ **一处自我更正**：我先把罗盘那两处 `setAlwaysOnTop` 写成「与行为基准同为已知未修」，
并据此给了具名豁免。**实查后推翻**：基准与 `origin/1.21.1` 的真实调用点都是 0。
教训进证伪表——**「这是我们自己造成的移植回归」不等于「新宿主没有它」**，
宿主那次是独立迁移，可以独立踩同一个坑。允许表随之清空，改为全仓禁止。

判据 3 条（JUnit 187→190），四种缺陷形态逐个红测：摘 HUD 登记 / 改回每帧新建状态 /
从允许表摘一项 / 注入一个真的 `setAlwaysOnTop` 调用——各红在正确断言上且无连带。
⚠️ **零实机**，逐项目视清单在 O1。

## 2026-08-17：§3.E 寻路 · 工作任务 · 跟随手感整块落地（`051aa37bb` `81b325700` `51d0c60de` `912f59522` `e1e67db8a`）

**审计 §6 的下一项，同时闭合 O3。** 这一层没有一个新文件，全部藏在共有文件里的小改动——
文件面 / 提交面 / 公开面三种枚举**一个都照不出**，所以工作面是**机械产出**的：
先跑跨版本 wiki 的行为面指纹（Brain 活动→行为→优先级的四树差集），再逐文件做四树对照。
**审计原列的 5 笔证据提交是下界**：机械枚举当场多出 `348cd0056`（follow QA）。

| 刀 | 内容 |
|---|---|
| `051aa37bb` | 农场站位：`canPathReachExact`（只认精确节点）+ `getWalkTargetPos` 钩子 + 在 home 内/交互距离内取最近可达邻格 + 收割判据改按真实位置带一格容差 |
| `81b325700` | 浅水泳姿三处：深度判据 `hasSwimmableDepth`、切水中寻路时按它决定 `setWantToSwim`、渲染态 `isSwimming` 改读服务端 |
| `51d0c60de` | 跟随：优先级 3→4、平滑跟随（牵引圈 5/12、迟滞回程 4、传送 20、站定 40 tick）、`SMOOTH_FOLLOW` 世界规则与 Cloth 分组、WALK_TARGET 改带 1 tick 有效期 |
| `912f59522` `e1e67db8a` | 优先级理由重写 + 补仓内判据并收紧到真正的差值 |

**两处「不搬」的判定**：基准的 `getTargetCloseEnoughDistance` 钩子**零覆写**，默认返回 0 与上游
字面量 0 等价，搬来就是零消费者空壳；`getWalkTargetPos` 的第二个消费者在偷吃桌上食物那条链
（§3.I 未移植），随那批落地。

**一处基准的陈旧注释被照出**：跟随优先级常量在基准上一度是 50，配文「工作行为用 5-20，
所以跟随必须排在其后」；实测「女仆叫不动」后改回 4，**那段 javadoc 没跟着改**。
我第一刀原样抄了它——4 小于 5，方向是反的。已按实际数值重写（判据见证伪表新条目）。

**一处会腐的判据顺带修掉，并连带修了它自己引出的假阳性**：
`ServerRuleReadRoutingContractTest` 的 owner 清单是手抄的四个类，新增 `ExperimentalConfig`
时那个键当场零覆盖（它的下限断言正确判红），已改成按 config 包目录推导。**但扩宽 owner 清单
把扫描面也一起撑大了**——`RenderConfig`（纯客户端偏好，一条世界规则都不拥有）被算成看管对象，
它的通配静态导入成了假阳性。改法是拆成两张表：「能反射到的全部配置字段」只服务那条非自证的
下限断言，「真正属于世界规则的」才进扫描。⚠️ **「能反射到」与「归它管」是两件事**，
共用一张表就会在扩面时静默串味。同轮给静态导入那条补了与结论正交的活性断言（走过 1752 个源文件）。

判据 11 例（GameTest 78→89）。**五条缺陷逐个红测，各红在正确断言上且无连带**：
农场导航目标回退（6 例红「站进作物列」）/ 收割判据改回过严（6 例红「拒绝所选站位」）/
摘浅水深度判据（1 例）/ 跟随 WALK_TARGET 改回不过期（1 例）/ 优先级改回 3（1 例红「自愈须先于跟随」）。
⚠️ **零实机**，逐项清单在 O1。

## 2026-08-17：个人配置搬回 `-global.toml`，专服不再生成它（`1c2a952a6` `09227b1d2`）

**§3.A 补上了缺的那一层。** 行为基准把个人偏好整段包在 `EnvType.CLIENT` 里、以
`Type.CLIENT` + 显式 `-global.toml` 注册；本树迁移时留在了无守卫的 `Type.COMMON`（写法照宿主），
于是**专服凭空生成两份客户端配置文件**，里面装着服务端一行都不读的聊天气泡、图标缓存、
原版替换五开关、渲染提示、麦克风与语音识别来源。**载体的存在会被读成语义**——判据与
`AvailableSites#managesSttSites` 不生成 `stt.json` 同源。

配置面全量对账 87 个键名：**归属等价 57 · 归属不同 26 · 单侧 4**——方法、完整结果与
「为什么三面闭合照不到它」全在审计 §7.9（不计预算，别在此复制）。

**搬运不是机械复制，两处实查拦下**：① `ENABLE_MAID_CURIOS` 是宿主新增键，经
`CuriosCompat.isLoadedOrEnable()` 被 `CuriosEvent`/`ExtraContainerManager`/`MaidContainerCache`
读，三处都在服务端跑——**故意留在 `-common.toml`**，那个文件因此不废弃，只是瘦身成
「真两侧共用的那一小撮」；② `MaidParticleManager.tick()` 挂在 `baseTick` 上两侧都跑却缺
`isClientSide` 短路（`1c2a952a6` 先修，是本刀的先决条件）。`STTSite` 那处无需改动。
**一处次序不能错**：`inheritMagmaCubeFromSlime` 作用在迁移源而非目标，必须早于
`migrateGlobalFileIfNeeded`；反了会让玩家旧选择被默认值静默吃掉且无任何报错。

门禁：compileJava 0 错；JUnit **46 类 187 例 0 失败**；GameTest **78 例 0 失败**。
**七种缺陷形态逐个红测**（摘守卫 / 改回 COMMON / 删文件名实参 / 把有服务端消费点的键塞进
CLIENT spec / 次序反转 / 摘短路 / 漏登记一个键），各红在正确断言上。

**两侧实机验收均已通过（2026-08-17）**：
- **专服**（`runGametest` 就是 `server()`，最便宜的专服）：`-global.toml` **不存在**；
  `-common.toml` 瘦身到只剩 `[maid] EnableMaidCurios`（107 字节，备份档 1505–3369）；
  残留 `-ai.toml` 打出「无人读」INFO（证明 else 分支真走到）；无配置未加载异常、无 NPE。
- **客户端**（dev 客户端脱离启动，日志首行属本轮，零 mixin 失败）：`-global.toml` 新建、
  **21 个值迁移**（恰等于 `GeneralConfig.values()` 的项数），三个非默认探针
  （`EnableModelIconCache`/`ReplaceSlimeModel`/`ReplaceMagmaCubeModel`）全部带 `true` 过来
  ——spec 默认是 false，静默失败会变回 false。

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

**「任何一页保存都提交全部改动」配了按屏枚举的测试**——我曾把这句契约写了三遍而语音输入页
根本不碰共享暂存。识别依据取结构量（覆写没覆写 `addFooterButtons`）而非按钮标签字面量，
并自带「认出了几屏」的下限断言。

**两处 C2③ 漏网被新收紧的判据当场照出**（javap 实证）：`LocalPlayer.sendSystemMessage`
的字节码就是 `getChatListener().handleSystemMessage(...)`——与被禁用的 `displayClientMessage`
**是同一条链、只是换了名字**，而宿主迁移时正是机械改写成了它。判据已从「写了某个方法名」
升级为「谁把文本送进了 ChatListener」，扫描面加 `client/gui`。

**账本改判一条**：`MaidAIChatConfigButton` 待定 → **无关**，它在两个基准上都是零调用者的死代码；
交接清单把它列为「本刀新建三件」之一是错的。

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

**C2③ 的前提在 26.1.2 复核成立且更硬**（javap -c）：`displayClientMessage` 那条链会在
调用线程上**硬等** Mojang 屏蔽名单刷新。26.1.2 给了更贴切的落点——`ChatComponent.addMessage`
已按来源拆成 `addServerSystemMessage` 与 `addClientSystemMessage`，后者正是本机提示的语义。

**两处按范围裁剪，各留恢复锚点**（不预留空壳）：`ChatClientInfo` 的 YSM 短路（§7.2 范围外）、
`table_food` 两个上下文（§3.I 未移植）。**知识文档同步裁剪**（`en_us.md`/`zh_cn.md` 各剔除 3 段）——
它是直接喂给模型当事实的，**描述一个本构建做不到的行为比不描述贵得多**。

**四处静默故障被闸门当场照出**：四个新 payload 没登记进 `NetworkHandler` /
`MaidAIChatManager` 漏 `@MaidManagerDef` / 三个新 GameTest 类未登记 entrypoint /
`SiteSecretRedactionTest` 的字段集取自它本该看管的那张表（自证式断言）。
门禁：JUnit 168 例 0 失败、GameTest 78 例 0 失败，七轮红测各红在正确断言上。

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

门禁 JUnit 75/0、GameTest 64/0（新增 33 例，四个类的 entrypoint 均已登记）。
三轮红测各红在正确断言上（摘应战记忆 / 摘 NBT 恢复分支 / 换远程武器判据）。

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
`MaidCombatManager.performRangedAttack` 改用它。红测：注入 isWeapon 要求即当场红。

门禁 JUnit 71/0、GameTest 31/0。**单人档实机验收 2026-08-15 通过**（用户五项实测，见 O1；
运行期前置 FCAP 26.1.4→26.1.5 随验收补上，`7a1c0bce`）；专服侧仍开放（O1 表）。

## 2026-08-15：缺陷修复移交清单定案采用（`6f5d8f2a5`）

1.21.11 分支移交的缺陷修复清单（原件归档
[archive/HANDOFF_DEFECT_FIXES_2026-08-15.md](archive/HANDOFF_DEFECT_FIXES_2026-08-15.md)）
逐项三树对照后采用：A 批 floorMod ×3、B 批拉弓姿势清位 ×3、C 批上游缺陷五处
（#1135/#1177/#938/#1058+#1059/#1158；#1053 熔炉刀已带入无需动作；
#1177/#938 按宿主管理器化结构适配进 `MaidItemManager`）、D 批入口时序对调
（首装首进程女仆名退化的根因）+ 两层楼死锁兜底（GUI 预览竞态核实本树本就正确）。
门禁：JUnit 65/0、GameTest 28/0（入口新时序下服务端启动实证）。

**延后项**：C2 三笔与 TTS 语种诚实标注已随 §3.C 采纳；#1139 返回容器形状校验随
`RemainFoodEatenEvent` 移植时随行。**归档件 §E 是有意行为分歧全集——做基线等价审计时勿当漂移改回。**

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

背包四型第三种，本刀把「持久化 + tick 驱动的背包数据」这层机制按宿主形态立起来
（液体背包直接沿用）。仍然生效的三条设计事实：
**`BackpackStateData` 附件 persistent 但有意不 syncWith**（烧炼进度每 tick 在变，
挂 `syncWith(all)` 会对所有追踪者每 tick 重发；GUI 进度条走容器 `addDataSlots`）；
**附件值是可变 holder**，codec 编码时从 runtime 拉活状态，解码后由 `MaidBackpackManager`
惰性绑定，tag 内部格式与基准逐字相同（含 #1053 稀疏槽位修复），**基准存档可互认**；
三处 26.1.2 漂移已实查（`assemble` 单参 / 燃料残留改 `getCraftingRemainder` 返回
`ItemStackTemplate` / `canInsertItem` 挪进 `MaidItemManager`）。
GameTest 四条 + 红测（摘 BackpackManager 登记行当场红）。⚠️ 零实机项见 O1。

## 2026-08-14：上一会话 15 刀全量审计（审计结论与修复分开，一缺陷一提交）

四方校验逐文件过完 15 笔提交。抓出并已修五个缺陷，其中最大的一个进了证伪表：
`be630452f` 的提交信息声称「ItemBoardState 的锚点由本刀闭合」，而那刀根本没碰那个文件，
整条棋盘预览链是死代码（`d78f1ba29` 补回）。其余四条：两种新背包生存模式不可获取
（`d3bc262b9` 补祭坛配方与战利品表）、zh_cn 四条译文与基准不符（`a08bf30e8`）、
创造栏两背包顺序相反（`59821b556`）、误入库的 0 字节 json（`615375eb0`）。
其余各块（世界规则、抱姿三 mixin、喂饮音效、棋局全簇、两背包容器与屏）逐字或仅有已记录的
API 漂移，资源与基准 hash 相同。

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

按 26.1.2 的渲染状态形态复原：`setupAnim` 只拿得到 `HumanoidRenderState`、拿不到实体，
故经 `ICarryMaidRenderState` 三件套传递（存位 → 抽取时求值 → 摆姿势时读出）；
两个注入点描述符经 javap 实查。`MixinRegistrationInvariantTest.HOST_PARKED` 随之清空——
往那张名单里加条目前先分清「宿主搁置」与「对基准的回归」，**后者要修好并登记，不是加进名单**。
⚠️ 无运行期凭据：客户端 mixin 在纯服务端的 `runGametest` 里不加载，见 O1。


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

**量到的结论（对后续排期有效）**：主体语义逐条可搬，改动都在接口而非逻辑；
**真正的成本在宿主的读写面上**——93 处读点 / 38 个文件改道，Cloth 菜单摘 36 条。
**「代码能不能搬」不是成本所在，「宿主有多少地方在读它」才是**，§7.1 那 247 个同名文件的估算按此校准。
新基逼出来的新代码：`ConfigFileMigration.migrateServerFileIfNeeded` + `prepareWorldFile` 不再「文件已存在即返回」。

两次红测各照出一个真缺陷（都在被测代码而非测试）：契约测试第一版按 `Owner.FIELD.get()` 扫，
对「把配置对象当参数传」与「静态导入后写裸名字」两种形态零覆盖（四条攻击任务正是这么漏改的，
运行期一攻击就炸）；GameTest 第一版拿「世界文件存在」当接线判据，而 `runGametest` 的 run 目录
多轮复用，读到的是上一轮遗留的文件。

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

工作树、方法论、`docs/tools/` 七个门禁脚本、JUnit/Mockito/`runGametest`/`clientDedicated`、
审计文档与 §9 台账一并带入。仍然生效的两条环境事实：
**测试的 `workingDir` 是 `build/test-working`**，读文件的用例必须 `Path.of("..", "..")` 回项目根
（症状是「找不到文件」而非断言不成立）；新基 `.gitignore` 曾忽略测试源码目录
（**Gradle 编译但 git 不跟踪**），已删除。`shadowJar` 的 `META-INF/services` 重复覆盖警告是新基自带，与我们无关。

## 2026-08-13：O1 探路轮完成（`8c439de1a`）

配置事务写盘逐字搬入、零 API 漂移；阻力全在环境侧。仍然生效的两条：
**Xaero 小地图/世界地图已整体停用**（`runtimeOnly` 无 client-only 标记，进服务端即
`Registry is not frozen yet!`，`runGametest` 起不来；`build.gradle` 有注释，临时取消注释可看地图但**不要提交**）；
**`:test` 报 `NO-SOURCE` 与「通过」输出无从分辨**，靠 `BuildInfrastructureSmokeTest` 这枚常驻探针照出。

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
