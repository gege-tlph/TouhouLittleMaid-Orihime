# 接手现场（26.1.2 分支）

## 接手规则

0. **先确认你在哪个工作树。** 本目录 = 26.1.2；1.21.11 在另一个目录。
   两者共享同一个 `.git`，但**各有独立的 HEAD 与索引**，互不影响彼此的未提交改动。
1. 读 `CLAUDE.md`（隔离纪律 + 工程纪律 + 证伪表 + **跨版本知识库指针**），
   再读 [CURRENT_STATUS.md](CURRENT_STATUS.md)。
2. **还有第三个仓库**：跨版本差异化知识库在两个工作树**之外**
   （路径见 `CLAUDE.md` 的「跨版本知识库」一节）。它不属于任何 port 分支、
   不占本仓库文档预算、也**不会出现在任何 `git status` 里**——
   不主动去看就等于不存在，而它正是为「不必每次移植重算差异化账」而建的。
   **凡要枚举差异化，先跑它的指纹工具，别抄交接文档。**
3. 要用 MCP rig 就**先跑 `docs/tools/mcp-up.ps1` 等握手成功再开会话**——
   HTTP MCP 只在会话启动那一刻连一次。

## 环境陷阱：Gradle 发行包下不动

本分支的 wrapper 要 **Gradle 9.4.0**（1.21.11 分支是 9.5.0，本机已缓存），而
**`services.gradle.org` / `downloads.gradle.org` 在这台机器上取不到正文**——
`HEAD` 返回 307/200 看着正常，`GET` 立刻 `Unexpected end of file from server`，
wrapper 与 `curl` 各失败两次，不是抖动。

绕法（**不要改仓库里的 `distributionUrl`**，那会把地区性镜像写进将来要公开的分支）：

```powershell
$d = "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.4.0-bin\lcvyxq3t37f6mx9miaydrrgs"
curl.exe -L -o "$d\gradle-9.4.0-bin.zip" https://mirrors.cloud.tencent.com/gradle/gradle-9.4.0-bin.zip
```

放好后 wrapper 会直接解包。⚠️ **失败的下载会在缓存里留下 0 字节的 `.part` 与 `.lck`**，
换镜像重试前先删掉它们，否则一次抖动会被固化成"一直坏"。

## 环境陷阱：MCP rig（8765 专服 / 8766 客户端）

`docs/tools/mcp-up.ps1` 负责拉起并等到**握手成功**（端口在听不算数：MC 先绑端口、后注册
handler）。2026-08-16 实跑一遍，踩到并已固化的五条：

| 症状 | 真因 | 判据 / 做法 |
|---|---|---|
| 脚本干等 300 秒后报「没有完成 MCP 握手」 | `run/eula.txt` 是 `eula=false`，服务端打印一行 EULA 提示后**立即自行退出**，端口从未监听 | 已加前置闸，现在 1 秒报真因。**同意 EULA 是使用者本人的法律行为，脚本不代改** |
| 服务端启动完成（日志到 `Done`）但 8765 仍无人监听 | `run/mods/` 里没装提供 MCP 的 mod | 需要 `minecraft-fabric-mcp`，上游 `chapmanjw/minecraft-java-fabric-mcp-server`，**必须用与 MC 版本精确匹配的构件**（v1.1.0 起同时发 1.21.11 / 26.1.1 / 26.1.2 / 26.2） |
| 装了 mod 却崩在 `NoClassDefFoundError: net.minecraft.class_1255` | 装成了 1.21.11 那份。它的约束写作 `minecraft: ">=1.21"`——**开区间没有上界**，26.1.2 的 loader 照收不误，运行时才炸 | **装得上 ≠ 能用**。26.1.2 那份的约束是 `minecraft: "26.1.2"` 精确锁版 |
| `/summon` 报 `successCount: 1`，实体却查不到 | 目标区块未加载，实体不留存 | **`successCount` 只代表命令执行成功，不代表实体存在**。先 `forceload add`，注意它收的是**方块坐标**、一格之差就是另一个区块 |
| 无人在线时 rig 半瘫 | `server.properties` 的 `pause-when-empty-seconds` 默认 60，满 60 秒后整个世界停 tick | 本树已改为 `0`（1.21.11 那边早就是 0） |

其它两条长期有效：

- **不必重开会话**：HTTP MCP 的连接窗口只有开头约 6 秒，但**专服在跑就能直接对
  `http://127.0.0.1:8765/mcp` 说 JSON-RPC**（initialize → notifications/initialized → tools/call），
  实测可用；`/mcp` 面板手动重连同样有效。所以 rig 可以由会话内部自己拉起。
- **读数据不要靠 `command_execute` 的返回**：它只捕获 tellraw / feedback，`/data get` 的回显拿不到，
  `save-all` 的正常回显还会被当成 `error`。读实体与 attachment 用
  `entity_get_nbt` / `data_attachment_get` 这类专用工具。

⚠️ **rig 跑着的时候禁止再跑任何 gradle 任务**——两个 gradle 同写 `build/` 崩过一次。

## 分支与工作树

| 角色 | ref / 路径 |
|---|---|
| 内部开发 | `port/26.1.2-fabric`（本工作树） |
| 代码宿主 | `origin/26.1`（`Sh1roCu/TouhouLittleMaid-Orihime` 的 MC 26.1.2 Fabric 分支） |
| **行为基准** | `port/1.21.11-fabric`（另一个工作树，**不要在这里 checkout 它**） |
| 计划中的清洁分支 | `release/26.1.2-clean`（**尚不存在**） |
| 计划中的公开分支 | `fork/port/26.1.2-fabric`（**尚不存在**） |
| 标签规范 | `v<版本>+mc26.1.2` |
| 备份规范 | `backup/26.1.2/<原因>/<YYYYMMDD>/<dev\|public>`——**必须带 `26.1.2` 段**，否则与 1.21.11 的备份标签混在同一个 `git tag` 视图里分不清 |

完整发布顺序、清洁树边界、CI、精确租约、附件与回滚见
[RELEASE_WORKFLOW.md](RELEASE_WORKFLOW.md)。正式 CI 文件为 `.github/workflows/build.yml`；
`26.1-snapshot.yml` 只产出开发快照 artifact，不创建正式 Release。

⚠️ 本分支建立时已 `git branch --unset-upstream`：`origin` 指向基准仓库，**我们没有写权限**，
留着跟踪只会让 `git push` 指错地方。将来接公开分支时把 upstream 设到 `fork`。

## 两个工作树的硬约束

| 约束 | 原因 |
|---|---|
| **不得同时跑 Gradle** | 两边 `build/` 与 loom 缓存互相污染；1.21.11 上实证过一次服务端读到半成品 `.class` 的 `ClassNotFoundException` |
| **rig 不得同时起** | MCP 端口 8765/8766 全局唯一；`mcp-up.ps1` 会报占用并打印占用者命令行 |
| **文档不交叉引用** | 两边账本各自独立；要引用对面的结论就复述结论 |
| **`.git/info/exclude` 是共享的** | `.mcp.json`、`AGENTS.md` 在两边都被忽略；**分支专属的忽略要写进该分支的 `.gitignore`** |
| **`git gc` / `worktree prune` 是全局操作** | 跑之前确认另一边没在工作 |

## 与 1.21.11 分支的关系

- **无共同祖先**，`cherry-pick` / `rebase` / `format-patch` 全部无效。
- 1.21.11 分支继续维护并发布，直到 26.1.2 稳定且完成实机验收。
- 要搬什么、边界在哪、有没有漏 —— 全在 [PORT_26X_AUDIT.md](PORT_26X_AUDIT.md)。

## 下一步

见 [CURRENT_STATUS.md](CURRENT_STATUS.md) 顶部开放项。准备首次 26.1.2 发布时，先按
[RELEASE_WORKFLOW.md](RELEASE_WORKFLOW.md) 建立清洁分支与公开分支；在二者不存在期间，
`release-gate.ps1 -Release` 应当失败，不能把这条预期红灯改成跳过。
