# Touhou Little Maid 26.1.2 Fabric 当前状态

**本文是唯一活动状态账本，只写「现在什么是真的」。**
开放项在最前面且各自自带验收标准与下一步；已关闭的压成一行 + 提交号；
**不写会过时的数字**——那些跑 `python docs/tools/facts.py`。
移植范围、边界与测试台账在 [PORT_26X_AUDIT.md](PORT_26X_AUDIT.md)，不要在本文复制。

## 结论

当前树 = `origin/26.1`（MC 26.1.2 Fabric）+ 一层工程设施 + **一刀差异化（配置事务写盘）**。
**除这一刀外，代码行为等同于代码宿主，不等同于我们 1.21.11 的行为。**
构建、JUnit、GameTest 三条链路均已实跑验证。

---

# 开放项

**O1 · 下一刀未定（探路轮已完成，见已关闭表）**
探路轮量出的结论是「零依赖切片可逐字搬」，**不能外推到那 247 个需要重做的文件**。
下一刀建议取 `ServerRuleConfig` 本体——它才第一次真正碰到新基的宿主结构
（依赖四个 subconfig 与一个事件类），量出来的数才有代表性。

**O2 · 前置项目未决**
- **YSM**：Fabric 26.1.2 上不存在任何实现（本体仅 NeoForge 且闭源，OpenYSM 无 26.x）。
  要保留该特色，须先把 `gege-tlph/OpenYSM-Updated` 移到 26.1.2——**独立项目，规模未评估**。
- **Patchouli**：官方有 26.1 beta，我们维护的 fork 需跟进。

**O3 · 公开发布链路尚未建立**
本分支还没有清洁分支、没有公开远端分支、没有 CI。`tree_equiv.py` 与 `git_hygiene.py`
里已经写好了目标 ref 名（`release/26.1.2-clean` / `fork/port/26.1.2-fabric`），
但**那两个 ref 还不存在**，相关门禁步骤现在必然跳过或报缺失——属预期，不是缺陷。

---

# 已关闭（一行结论 + 提交）

## 2026-08-13：继承前提逐条修正（`d726e2915`）

`CLAUDE.md` 的方法论与证伪表自 1.21.11 原样搬入，**经验有效但前提是版本相关的**。逐条查证后就地改：

| 条目 | 查证结果 | 已做 |
|---|---|---|
| 核心纪律 2 | **方向反了**（原文禁止「为贴近 26.1 改行为」写于 `origin/26.1` 还只是架构参照的时期） | 改写成两问：代码长什么样照 `origin/26.1`，表现成什么样照 `port/1.21.11-fabric` |
| 核心纪律 3 | 「排除 = 隐形」前提不存在——新基 `sourceSets` **零条源码排除**（`build.gradle` 里的 `exclude` 全是依赖组与 shadowJar 的） | 三查降为两查（entrypoint / `mixins.json`），适用面改为「**我们新增的类**」；删掉没跟过来的 `docs/COMPAT.md` 指路 |
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
