# TLM 1.21.11 → 26.1.2 缺陷修复移交清单（2026-08-15）

**目的**：把 `port/1.21.11-fabric` 分支上已定案的缺陷修复移交给 `port/26.1.2-fabric` 定案采用。
两个工作树同属一个仓库，文中所有提交号在 26.1.2 工作树内直接可查：

```
git -C C:\Users\steve\Desktop\MCV_Project\tlm-tsumugi-26.1.2 show <提交号>
```

标注约定：✅ = 已在 26.1.2 树上核实缺陷存在（本文写作时逐处 grep 过）；⚠ = 未核实，附自查命令。

---

## A. 静态分析批 `d07532df1`（本清单主项 · 三处全部 ✅ 已核实存在于 26.1.2）

SpotBugs HIGH 置信 200 条逐条人工复核后仅存的三条真缺陷，全部与 origin/1.21.1 逐字相同。
同型修法：`Math.abs(x) % n` → `Math.floorMod(x, n)`（恒返回 `[0,n)`，对其余输入只是换一个
同样合法的错峰/权重值，无行为依赖）。

| # | 26.1.2 位置（已核实） | 缺陷 | 后果 |
|---|---|---|---|
| A1 | `entity/passive/EntityMaid.java:222` | `Math.abs(this.getUUID().hashCode()) % saveIntervalTick`，hashCode 恰为 `Integer.MIN_VALUE` 时 abs 仍为负 | `gameTime % n == 负数` 永假 → **该 UUID 的女仆自动备份终生静默失效**（粘性，随 UUID 存续，非每次掷骰） |
| A2 | `api/game/chess/Position.java:1148` | `Math.abs(random.nextInt()) % value` 同型 | 国际象棋开局库加权选步退化为恒选首步（每次调用 1/2³²，瞬态） |
| A3 | `api/game/xqwlight/Position.java:1109` | 同型 | 中国象棋同上 |

修法原文（A1 另建议带上注释，防止将来被「简化」回去）：

```java
// floorMod 而非 abs+%：hashCode 恰为 Integer.MIN_VALUE 时 abs 仍为负，
// 下方 gameTime % n == 负数 永假，该女仆的备份将终生静默失效
int checkTick = Math.floorMod(this.getUUID().hashCode(), saveIntervalTick);
```

```java
// floorMod 而非 abs+%：nextInt() 恰为 Integer.MIN_VALUE 时 abs 仍为负，加权选步退化为恒选首步
value = Math.floorMod(random.nextInt(), value);
```

**验证判据**：装回 SpotBugs 的话，`RV_ABSOLUTE_VALUE_OF_RANDOM_INT` 与
`RV_ABSOLUTE_VALUE_OF_HASHCODE` 两类应整类清零（1.21.11 树实测 200→197，其余逐类计数不变）。

---

## B. 拉弓姿势标志位不清（`2e605d5c8` 第②部分 + `7cdfb3a1f` 其中一半）

继承缺陷（基准逐字同型）：`start` 置 `setSwingingArms(true)`，`stop` 从不清，
换武器后女仆保持拉弓姿势。基准里另外五个同类任务都清，唯独这几个不清。

| # | 26.1.2 现状 | 动作 |
|---|---|---|
| B1 | `MaidShootTargetTask` ✅ 仅 1 处 `setSwingingArms(true)`、零清位 | 在 `stop` 补 `setSwingingArms(false)`，对照 `2e605d5c8` |
| B2 | `MaidTridentTargetTask` ✅ 同上（1 处 true、零清位） | 同上 |
| B3 | `MaidShootTargetAnyItemTask` ⚠ 已含 2 处 `setSwingingArms(false)`，与 1.21.11 修前形态不同 | 对照 `7cdfb3a1f` 自查 stop 路径是否覆盖；已覆盖则无需动作 |

---

## C. 上游缺陷修复批（2026-07-29 · 5 提交 · 覆盖 8 项症状 · ⚠ 逐项自查）

全部为「与基准逐字相同的上游继承缺陷」，修复即有意行为分歧。26.1.2 若从同源移植，大概率同在。
自查方式：`git show <提交号>` 看改动文件名，在 26.1.2 树 grep 同文件同代码形状。

| 上游 issue | 提交 | 根因与修法要点 |
|---|---|---|
| #1053 炉子背包稀疏槽位错位（+顺带：烧制进度重进世界归零） | `924d669b2` | `storeAsItemList`/`fromItemList` 不带槽位索引、读取用 `addItem` 压紧，**输入空时燃料被读进输入槽当原料烧掉**。修法**纯增量**：照旧写 `Items` 另写同序槽位索引，缺索引回落旧行为——**旧存档原样可读，移植时不得改成破坏性格式** |
| #1135 非法正则崩配置 / #1139 返回容器配置形状 | `69a5d0383` | 一个 `[` 让世界起不来且 last-good 回滚无效（文件是合法 TOML，重读仍是同一条坏正则）；三种坏形状在服务端 tick 内抛。均改为逐项隔离 + 记名日志 |
| #1177 换手进食丢物 | `e215f9fca` | 四个换餐任务按 `NATIVE_HANDS` 取「第一只空手」，主手为空时换的是主手，而收尾硬编码副手 → 副手无关物被扫走。判据改 `getUsedItemHand()`，无需新增持久化状态 |
| #938 截走玩家忠诚三叉戟 | `e215f9fca` | `isNoPhysics()` 即「忠诚三叉戟返程中」，原版 `tryPickup`/`playerTouch` 均按所有者设防，女仆原先照单全收 |
| #1058 / #1059 女仆已回收时开界面崩客户端 | `c66ae62f4` | `AbstractMaidContainerGui` 构造函数二次解引用 `menu.getMaid()`；既有 null 守卫在抛出点下游够不着。改为只解引用一次并安全降级 |
| #1158 灭火剂漏灵魂火 | `52976d8b6` | `Blocks.FIRE` → `BaseFireBlock`（两种火的共同父类） |

上游回报材料（按上游 1.21.1 NeoForge API 转写的根因+补丁，供对照）：
`C:\Users\steve\Desktop\MCV_Project\TLM-NeoForge-Upstream-Audit-2026-07-29\UPSTREAM-SUBMISSIONS.md`

---

## C2. AI 聊天层继承缺陷修复（⚠ 整层自查——动作取决于 26.1.2 的 AI 层移植进度）

三条根因都在上游的架构形态里，修法全部是**结构性**的（数据层/通道层），不是提示词调参。
移植 AI 层时应把这三笔一起带上，否则缺陷会原样复活。

| 症状 | 提交 | 根因与修法要点 |
|---|---|---|
| 聊久了女仆只闲聊、不执行指令（「跟着我」答应得好听但不动） | `1e4fb5afb`（主）+ `979343d2d` `14ab7edb1` `016660a9f` | 长历史里没有一条工具调用正例，**示范胜过指令**，弱档位模型必然停调（A/B 实测：同历史 flash 0/2、pro 2/2）。修法=「要不要动手」搬出会漂移的通道：一条**不带历史/人设/工具**的微型请求判定是否动作指令，判为动作再用一条不带历史+完整工具的请求执行，且**先做、后说**（执行完成早于回话）。判定带 `<context>` 状态快照防空操作；旁路必须覆写 `refreshWaitingChatBubble` 为空，否则留下 90 秒孤儿气泡；旁路只挂动作类工具。已证伪的方向（**勿再试**）：补合成正例（无效）、截断历史（2 轮即失效） |
| 选了语音语种，说一两轮就变回聊天语言，要清空聊天记录才恢复 | `ee8e9095c` | 根因=**把结构化字段（译文段）塞进自由文本通道**：历史里只要出现 1 条单段助手回复，第二段就确定性消失（非概率漂移）。修法=待合成文本改由一次**不带历史/人设/工具**的独立翻译请求产出（`TtsTranslationCallback`），主对话恒定单段——漂移从「概率更低」变成「结构上不可能」 |
| 云端语音识别时网络一慢整个客户端冻住 | `644250596` | 根因**在原版不在 STT 链路**：`displayClientMessage → ChatListener → PlayerSocialManager.isBlocked → forceFetchBlockList` 在**渲染线程上同步 HTTPS 到 Mojang**（识别结果 `<玩家名> 文本` 恰好命中 `guessChatUUID` 的 `<...>` 解析；对 Mojang 不畅的玩家失败不缓存、每次都卡）。修法=本地提示改走 `ClientLocalChat.show`（照抄原版未被屏蔽分支，保留朗读器，不记聊天日志），**全部纯客户端提示调用点都要改**（1.21.11 树共 6 处）。实测：修前 1 次识别 9 个渲染线程阻塞样本（连续 4.5 秒），修后 3 次识别 0 个 |

---

## D. 其他已修继承缺陷 / 已同形项

| 项 | 提交 | 26.1.2 动作 |
|---|---|---|
| **首个进程女仆显示裸实体键名而非模型名**（资源重载/重启前一直如此）：Fabric 入口先执行 `CommonRegistry.onSetupEvent()` 扫描服务端模型表，之后 `commonSetup()` 才解包内置默认模型包到 `tlm_custom_pack`，首个进程 `SERVER_MAID_MODELS` 全程为空，`EntityMaid.getTypeName()` 退回裸键 | 2026-07-22 时序修复（入口改为 配置注册 → `commonSetup` 解包 → `CommonRegistry` 扫描；1.21.11 树现状 `TouhouLittleMaidFabric.java:57-58` 为正确顺序） | ✅ **缺陷已核实存在**：26.1.2 的 `cn/sh1rocu/touhoulittlemaid/TouhouLittleMaidFabric.java:65-66` 顺序是反的（先扫描后解包）——**对调这两行**。验法：把 `tlm_custom_pack` 完全移走做隔离首装，新召唤女仆应立即显示「博丽灵梦」而非键名 |
| 两层楼工作点死锁（home 三维球罩住楼上 + 找坐具要视线，叠层即死锁；三树逐字一致） | `d06fed0b2` | ⚠ 自查。修法=「找不到坐具且距工作中心 >2 格则走向中心」兜底，收敛条件 WALK_TARGET 空才运行 |
| GUI 预览跟随鼠标姿态数据竞争（动画任务在鼠标角度写入前提前启动） | `843f3fa70` | ✅ **无需动作**——26.1.2 的 `InventoryScreenMixin` 本就在 `extractEntityInInventoryFollowsMouse` RETURN 处启动，正是本次 1.21.11 修法的蓝本 |
| 系统 TTS 语种是无效开关（`TTSSystemClient.play` 丢弃 TTSConfig，语音全由操作系统决定；基准同） | 2026-07-30 批 | ⚠ 若 26.1.2 有同 UI：采取的是**诚实标注**而非行为修复（站点表单 hints + 聊天屏 🌐 按钮黄色悬停警告），自查是否需要同步 |

---

## E. 有意行为分歧（非缺陷修复 · 26.1.2 做基线等价审计时**勿当漂移改回**）

- 桌上食物奖励改为每次 1~3 点 + 冷却（超出基准）
- 偷吃持有目标限时让位（超出基准）
- Velocity 代理兼容常开（超出基准）
- **弓弩解绑工作任务**（`cdd4c6fa5`，射击实现按手里武器解析 `IRangedAttackTask.resolveImplementation`，当前任务优先；基准只认工作任务）
- 家具重制兼容：装载该模组时把 `minecraft:recipe_serializer` 标 `RegistryAttribute.SYNCED`（`dd2ccc483`；代价是服务端多出客户端缺失的序列化器时会明确断开而非默默错位）
- 以上加 C 部分整批，共同构成「明知与 origin/1.21.1 不同仍保留」的集合

---

*来源：`port/1.21.11-fabric` @ `dbe39d0fc`。A/B 部分的 26.1.2 现状核实于 2026-08-15（对 `tlm-tsumugi-26.1.2` 工作树逐处 grep）；⚠ 项移植前先按「三树对照」（本地 / origin/1.21.1 / 26.1.2）确认代码形状，勿按症状照抄。*
