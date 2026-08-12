# 接手现场（26.1.2 分支）

## 接手规则

0. **先确认你在哪个工作树。** 本目录 = 26.1.2；1.21.11 在另一个目录。
   两者共享同一个 `.git`，但**各有独立的 HEAD 与索引**，互不影响彼此的未提交改动。
1. 读 `CLAUDE.md`（隔离纪律 + 工程纪律 + 证伪表），再读 [CURRENT_STATUS.md](CURRENT_STATUS.md)。
2. 要用 MCP rig 就**先跑 `docs/tools/mcp-up.ps1` 等握手成功再开会话**——
   HTTP MCP 只在会话启动那一刻连一次。

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

见 [CURRENT_STATUS.md](CURRENT_STATUS.md) 的 O1–O4。最短路径是 **O2 先跑一次 `build`**
（证明照搬来的工程设施真的可用），再进 **O1 探路轮**。
