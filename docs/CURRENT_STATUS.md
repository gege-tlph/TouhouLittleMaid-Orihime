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
构建、JUnit、GameTest 三条链路均已实跑验证；**但全程没有入世实测**，凭据都来自自动化门。

---

# 开放项

**O1 · 待入世实测（累计 4 刀，全部零实机验收）**

至今所有凭据都来自自动化门（build / JUnit / GameTest / doc_lint），**没有任何一刀经过实机**。
其中**第 4 刀是唯一连运行期凭据都没有的**，优先级最高：

| 待验 | 为什么自动化门验不到 | 怎么验 |
|---|---|---|
| 扛女仆时玩家手臂摆抱姿（`79e7f7914`） | 三个都是**客户端 mixin**，而 `runGametest` 是纯服务端，客户端 mixin 在那里根本不加载。凭据只到「已登记 + 靶点描述符经 javap 实查存在 + 编译通过」 | 开发客户端里骑上女仆看第三人称。`required: true` + `defaultRequire: 1` 意味着描述符若不对会**直接崩**，所以「客户端能起来」本身就是强证据 |
| 世界规则从 stock 26.1 存档升级 | 需要一个真有旧值的 `touhou_little_maid-common.toml`，单元测试造不出「FCAP 已经建过世界文件」那个前置状态 | 用装过 stock 26.1 的存档进一次，看旧值有没有进 `serverconfig/touhou_little_maid-server.toml` |
| 配置菜单三种身份的可见性 | Cloth 菜单要真实客户端才能构造 | 单人 / 局域网客机 / 专服非 OP 各开一次菜单，看「玩法设置 / 高级设置」两栏在不在 |
| 专服保存 → `/tlm config reload` | 需要真专服 + 真客户端 | 改一项保存，确认提示出现且值未生效；跑 reload 后生效并同步 |

**O2 · §3.A 剩余两项（世界规则那一层已闭合）**

配置三层的**世界规则那一层已完整**：本体、文件层、读点改道、网络层、配置菜单、
`/tlm config reload`、op/deop 重发，全部落地并有测试（见已关闭表三条）。剩下的两项都不属这一层：

| 缺口 | 现状 | 恢复锚点 |
|---|---|---|
| `AiServerRuleConfig` 那一店 | 未搬，整体属 §3.C。`ServerRuleConfig.get()` 无路由分支；两个包只装世界规则一半 | `ServerRuleConfig.get()`、`SyncServerRulesPacket` 两处 javadoc |
| `ExperimentalConfig.SMOOTH_FOLLOW` | 类与值都未建；配置项要与消费者（§3.E 跟随手感）同批落地 | `ServerRuleConfig.values()` 的 javadoc |

⚠️ 上表每一条在代码里都有对应注释，**不要只靠本表**——本表会过时，注释在改到时才会被看见。

**O3 · 前置项目未决**
- **YSM**：Fabric 26.1.2 上不存在任何实现（本体仅 NeoForge 且闭源，OpenYSM 无 26.x）。
  要保留该特色，须先把 `gege-tlph/OpenYSM-Updated` 移到 26.1.2——**独立项目，规模未评估**。
- **Patchouli**：官方有 26.1 beta，我们维护的 fork 需跟进。

**O4 · 公开发布链路尚未建立**
本分支还没有清洁分支、没有公开远端分支、没有 CI。`tree_equiv.py` 与 `git_hygiene.py`
里已经写好了目标 ref 名（`release/26.1.2-clean` / `fork/port/26.1.2-fabric`），
但**那两个 ref 还不存在**，相关门禁步骤现在必然跳过或报缺失——属预期，不是缺陷。

---

# 已关闭（一行结论 + 提交）

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
