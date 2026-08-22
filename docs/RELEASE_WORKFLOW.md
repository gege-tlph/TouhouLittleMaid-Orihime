# 内部开发与清洁发布规范

本文件是 26.1.2 Fabric 分支的发布操作手册。它把工程事实源、公开清洁树、CI、标签、Release
附件、摘要核验和回滚放在同一条可审计链上。它只存在于内部开发分支，不应进入公开清洁分支。

## 1. 不可变原则

项目维护两种视图：

| 视图 | ref / 远端 | 用途 |
|---|---|---|
| 内部开发视图 | 本地 `port/26.1.2-fabric` | 细粒度提交、迁移账本、审计证据、诊断注释与后续开发 |
| 公开清洁视图 | 本地 `release/26.1.2-clean` → `fork/port/26.1.2-fabric` | 面向玩家的源码、README、许可证、CI 与可复现构建 |
| 代码宿主 | `origin/26.1` | 26.1.2 的结构、命名和 API 形态参考 |
| 行为基准 | 本地 `port/1.21.11-fabric` | 已实机验收的玩法、数据、存档和视觉行为基准 |

永久规则：

1. 内部开发分支是工程事实源，不压缩历史，不为公开观感删除上下文。
2. 公开分支是可分发视图，不是下一轮功能开发现场。
3. 功能、依赖、构建或运行时修复不得只存在于公开分支；发现这种修补时先回写内部开发分支。
4. 公开历史正常追加有意义的发布提交，父提交是公开分支当前 tip，不以行为基准重建历史。
5. 默认普通快进推送。只有用户明确授权的历史重写才允许精确 `--force-with-lease`，禁止裸 `--force`。
6. 稳定标签不可移动；预发布标签也只有用户明确授权才可重写。

## 2. 文件边界

内部分支保留：

- `docs/`、`AGENTS.md`、`CLAUDE.md` 及迁移/审计/接手账本；
- Pxx、NODE、ROUND 等内部锚点与诊断注释；
- 细粒度开发提交、用户验收记录和恢复索引。

公开分支允许：

- 可编译源码、资源、许可证、Gradle wrapper、`build.gradle` 和发布所需配置；
- 面向贡献者的长期 API/行为契约注释；
- `.github/workflows/build.yml` 等公开 CI；
- 玩家实际需要的 README、语言和数据文件。

公开分支禁止：

- `docs/`、`AGENTS.md`、`CLAUDE.md`、`.codex-scratch/`；
- `run*/`、测试存档、日志、崩溃报告、截图、缓存和本地反编译文件；
- 本地依赖 JAR；
- Pxx/NODE/ROUND 等内部状态；
- `shadow`、dev 或其它容易误装的中间产物。

`.gitignore` 负责所有分支都不应提交的生成物；`.git/info/exclude` 只负责本机文件。
不能用 exclude 隐藏“只是不该进公开树”的内部文档，公开树必须靠清洁分支内容和审计保证。

## 3. 开发阶段门禁

在 `port/26.1.2-fabric` 或它的独立 worktree 开发。每个非平凡节点至少完成：

1. 代码宿主 `origin/26.1` 的 API 形态核对；
2. 行为基准 `port/1.21.11-fabric` 的玩法/数据/存档核对；
3. `compileJava`、完整 `build`、相应 JUnit、`runGametest`；
4. 需要客户端、专服或兼容模组的路径由用户实机验收；
5. `CURRENT_STATUS.md` / `HANDOFF.md` 同步后再提交。

日常门禁：

```powershell
powershell -File docs/tools/release-gate.ps1
```

同步到清洁树后才允许发布模式：

```powershell
powershell -File docs/tools/release-gate.ps1 -Release
```

门禁依次执行完整 `build`、`runGametest`、文档 lint、内部/公开树等价审计、git 卫生和
`git diff --check`。`-Release` 在同步前必然失败；不能把它提前运行的红灯当成脚本缺陷。
门禁通过也不能替代真实客户端视觉、交互、专服和第三方兼容验证。

## 4. 发布前保护

发布开始时读取并记录一次精确 SHA。26.1.2 的保护标签必须带版本段，以免和共享 git 仓库中的
1.21.11 标签混淆：

```powershell
$devSha = git rev-parse port/26.1.2-fabric
$publicSha = git rev-parse fork/port/26.1.2-fabric
$today = Get-Date -Format "yyyyMMdd"
git tag -a "backup/26.1.2/release/$today/dev" $devSha -m "26.1.2 发布前内部开发锚点"
git tag -a "backup/26.1.2/release/$today/public" $publicSha -m "26.1.2 发布前公开分支状态"
```

规则：

- 备份是标签，不是分支；命名为 `backup/26.1.2/<原因>/<YYYYMMDD>/<dev|public>`；
- 保护标签只留本地，不推到公开 fork；
- `$publicSha` 同时是整个发布过程的 lease，不能中途重新取值掩盖并发更新；
- 版本号写在发布标签，不写入保护标签名；
- 建标签前先确认两个 ref 都能解析，禁止模糊 ref、通配符和未解析变量。

## 5. 同步到清洁分支

清洁操作使用独立 worktree，不要在内部工作树 checkout：

```powershell
git worktree add C:\tmp\tlm-release-26.1.2 release/26.1.2-clean
```

若清洁分支尚不存在，先以公开远端当前 tip 建立它；若公开远端也尚不存在，则必须先由用户
确认公开仓库和默认分支名称，再创建，不得猜测。

同步规则：

1. 根据上次发布锚点计算内部新增文件区间，只覆盖这一区间的功能改动；不要整树覆盖。
2. `docs/`、`AGENTS.md`、`CLAUDE.md`、`.codex-scratch/` 不进入公开树。
3. 清洁时若发现依赖、Gradle、资源、注册或运行时代码需要改，停止公开侧修补，先回内部开发分支验证。
4. `README.md`、公开注释和 CI 可以只在清洁树维护，但不得改变游戏行为。
5. `libs/compile_only/` 的第三方 JAR 不入库；公开 CI 必须提供等价的下载和摘要校验。
6. 每次同步前先确认公开远端 tip 已被本地清洁分支包含，不能覆盖网页端或其他维护者的新提交。

## 6. 注释与文档清洁

公开注释不是全部删除，而是改写为长期可读的契约。

应删除：

- Pxx/NODE/ROUND、审计批次、会话和代理叙事；
- 已被实现取代的 TODO；
- 内部路径、临时日志和只对一次迁移有意义的过程记录。

应保留并改写：

- 许可证、版权、来源和第三方归属；
- 存档格式、协议边界、线程要求和不可直观推导的原版行为；
- no-cull、客户端 RecipeManager、可选模组入口和 `isModLoaded` 边界等长期红线。

Java、Groovy 和资源清洁前后必须做忽略注释的 token 审计。真实 token 差异按代码改动重新审查，
不能用“只是公开树清理”掩盖行为变化。公开树至少扫描：

```powershell
rg -n "P[0-9]+_[A-Z]|ROUND[0-9]|NODE [A-Z0-9]" src README.md build.gradle
git diff --check
```

## 7. 公开树审计

发布前在清洁 worktree 执行：

```powershell
git status --short
git ls-files docs AGENTS.md CLAUDE.md .codex-scratch
git ls-files | rg "(^|/)(run|logs?|crash-reports?|\.gradle|build)(/|$)"
rg -n "P[0-9]+_[A-Z]|ROUND[0-9]|NODE [A-Z0-9]" src README.md build.gradle
```

期望：内部文档和临时目录无跟踪文件；内部标记无命中；工作树只包含已经解释的发布改动。
同时核对 `fabric.mod.json`、两份 mixins JSON、语言/配方/战利品/模型、`build.gradle` 的依赖与
entrypoint，以及网络 payload。任意无法由文档、公开注释或 CI 解释的差异必须停止发布并定案。

## 8. 发布提交

公开历史应在当前公开 tip 上追加一个或少数几个有意义的提交：

```powershell
$oldPublic = git rev-parse fork/port/26.1.2-fabric
$devSha = git rev-parse port/26.1.2-fabric
$tree = git write-tree
$newPublic = git commit-tree $tree -p $oldPublic `
  -m "feat: <玩家可理解的一句话变更>" `
  -m "Development source: $devSha. Verified by build, GameTest, compatibility gates, and clean-tree audit."
```

提交后必须证明旧历史完整保留且只追加了预期提交：

```powershell
git rev-list --count "$oldPublic..$newPublic"
git merge-base --is-ancestor $oldPublic $newPublic
git diff --exit-code <清洁树 tip> $newPublic
```

禁止 reset 内部开发分支到公开提交。发布标题和 Release 正文写玩家可见变化，不写内部根因日记。

## 9. 推送、CI、标签与 Release

默认普通快进：

```powershell
git push fork HEAD:refs/heads/port/26.1.2-fabric
```

被拒时先 fetch，检查公开分支新增提交，把发布提交重叠到新 tip 后再推。只有用户明确授权历史重写
才可使用精确租约：

```powershell
git push --force-with-lease="refs/heads/port/26.1.2-fabric:$publicSha" `
  fork HEAD:refs/heads/port/26.1.2-fabric
```

CI 顺序固定为：

1. 默认分支完整构建成功；
2. 确认 TACZ R2 编译期依赖按 SHA-256 下载，且不进入 JAR；
3. 确认 artifact 只有一个可安装 remap JAR，没有 sources、shadow 或 dev 构件；
4. 下载 artifact，记录文件大小和 SHA-256；
5. branch CI 全绿后才创建新标签；
6. 等标签/Release workflow 全绿后再核对 Release 页面。

发布标签格式为 `v<版本>+mc26.1.2`。版本号有递增、热修号或复用旧号等多个合理选择时，先向用户确认。
稳定标签不可移动；删除 Release、标签或改写公开历史都必须得到明确授权。

Release 至少说明 Minecraft 26.1.2、Fabric Loader、Fabric API、Forge Config API Port、版本性质、
安装 JAR、上游/许可证归属、CI run 和 SHA-256。玩家附件只上传可安装 remap JAR；sources 可以作为
明确标注的开发附件，shadow/dev 一律不上传。

## 10. CI 约定

`.github/workflows/build.yml` 是公开分支的正式构建入口，使用 Java 25，触发 `port/26.1.2-fabric`
的 push、pull request 和手动运行。它会：

- 下载 `TACZ-Refabricated-26.1.2-1.1.8+fabric.26.1.2.R2.jar`；
- 校验摘要 `fcfdfe6e6356ae5f33c7a6a439ed656f2b9f1034d09cd850a2666c7399febf6f`；
- 运行 `./gradlew build --no-daemon`；
- 拒绝零个或多个可安装候选，只上传单个 remap JAR。

`.github/workflows/26.1-snapshot.yml` 只用于开发快照 artifact，不创建正式 Release；快照不能代替
branch CI，也不能作为稳定标签的依据。

## 11. 发布后核验

```powershell
git ls-remote fork refs/heads/port/26.1.2-fabric
git ls-remote fork refs/tags/<tag>
gh release view <tag> --repo <公开仓库>
```

核对：默认分支、标签和 Release target 指向预期 SHA；附件可下载且摘要一致；公开树没有内部文件；
内部开发分支、文档、worktree 和保护标签仍在；清洁期间发现的功能/依赖改动已回写内部分支；
`HANDOFF.md` 记录内部 SHA、公开提交、标签、CI run 和摘要，`CURRENT_STATUS.md` 只记录功能状态。

## 12. 回滚

首选向前修复：在内部开发分支修复、重跑门禁，再追加新的公开发布提交。错误 Release 先标记 draft 或
问题说明，是否删除由用户决定。只有误提交凭据/内部文档等不可接受内容才回退历史：从
`backup/26.1.2/<原因>/<日期>/public` 解析旧 SHA，确认远端仍是错误提交，再经用户授权使用精确
`force-with-lease`。保留失败 CI 与根因记录，修复后重新走完整门禁。

## 13. 当前 26.1.2 映射

- 内部开发：`port/26.1.2-fabric`；工作树为当前项目目录。
- 计划中的清洁分支：`release/26.1.2-clean`，尚未建立时 `-Release` 必须保持红灯。
- 计划中的公开分支：`fork/port/26.1.2-fabric`，尚未发布前不得伪造公开 SHA、标签或 Release 记录。
- 行为基准：`port/1.21.11-fabric`；代码宿主：`origin/26.1`。
- 构建目标：Minecraft 26.1.2、Java 25、Fabric Loader 0.19.3、Fabric API 0.155.2+26.1.2。
- 编译期 TACZ：26.1.2_R2，本地 jar 不入库，公开 CI 负责下载与摘要校验。

首次 26.1.2 发布必须在清洁分支和公开仓库实际建立后，补写内部 SHA、公开提交 SHA、标签、CI run、
安装 JAR 大小与 SHA-256；在此之前不要把“计划中的映射”写成已发布事实。

## 14. 本地 git 规范

目标分支的保护 ref 使用 `backup/26.1.2/<原因>/<YYYYMMDD>/<dev|public>`；共享仓库中旧版的
`backup/<原因>/<YYYYMMDD>/...` 标签属于 1.21.11 视图，不能拿来作为 26.1.2 的发布锚点。

构建会编译或打包的源码、资源和测试必须被跟踪。`git_hygiene.py` 会检查未跟踪源文件、备份 ref、
清洁分支是否包含公开远端 tip、其他 worktree 状态和 git 中断残留。发布前务必查看
`git diff --cached --stat`，避免把旧的暂存删除或别的 worktree 改动带进发布提交。
