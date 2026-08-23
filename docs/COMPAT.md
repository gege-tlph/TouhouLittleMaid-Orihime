# 第三方兼容状态与边界

本文件是 26.1.2 分支的兼容唯一活动账本。历史审计和逐轮证据留在内部状态账本，不在这里重复。
兼容结论必须以当前 26.1.2 Fabric 构建为准；“有 26.x 版本”不等于“有 Fabric 版本”。

## 永久原则

- 目标模组未安装时不得加载其类、注册其入口或改变核心行为。
- 优先使用 vanilla 数据组件、标准 tag 和公开父类；必须引用第三方类型时才建立按 mod id 门控的 typed adapter。
- 兼容依赖保持 compile-only 或 `isModLoaded` 软边界；不得把可选模组写进 `fabric.mod.json` 的硬依赖。
- 网络只使用 vanilla 协议和标准 Fabric namespaced payload，不改变 vanilla 包结构。
- 恢复任何兼容模块都必须同时检查依赖、entrypoint、两份 mixins JSON、无目标模组启动和目标模组实机路径。

## 活动兼容

| 模块 | 当前边界 | 验证状态 |
|---|---|---|
| JEI | Fabric entrypoint；配方与变体展示 | 代码已接通，随发布门禁验证 |
| REI | `rei_client` entrypoint；祭坛配方摘要与侧栏 | 代码已接通，GameTest/客户端路径按门禁验证 |
| Patchouli | `26.1-94-beta` Fabric 构件覆盖 Minecraft 26.1.2；提供本项目内置手册运行时 | 构件适用；本项目 Gradle/API 接入仍待完成，不能宣称手册功能已验收 |
| Sodium / Iris | 仅可选客户端兼容，不进入服务端运行集 | 已有共存实机证据；新版本组合仍需用户回归 |
| TACZ Refabricated | `26.1.2_R2`；枪械攻击、弹药来源、持枪渲染与远程应战。编译期 jar 不入库 | R2 代码已落地；R1 旧验收不能替代 R2 实机复验 |
| Farmer's Delight Refabricated | compile-only typed adapter，目标模组缺席时核心不触碰其类型 | 兼容边界已接通，按真实 mod jar 验收 |
| Kaleidoscope Cookery | 标准食物/作物由核心覆盖，特殊三段水稻走 adapter | 已接通；初始化必须延迟到服务端启动 |
| Kaleidoscope Tavern | 桌台、坐具、葡萄与工作餐排除；不自动饮酒或接入酿造机器 | 已接通，玩家路径仍按兼容组合回归 |
| Carry On | 黑名单与携带行为 | 目标版本组合需专服/客户端回归 |
| Traveler's Backpack | 仅保留可编译的软适配，不把 Accessories 当成硬前置 | 26.1.2 构件可得性需逐次确认 |

## 无落点或暂不支持

以下项目在当前 26.1.2 Fabric 生态没有可用接入点，不能在 Release 中宣称兼容：

- YSM / OpenYSM：当前没有 26.1.2 Fabric 构件；若恢复，必须先完成独立 fork 移植，再同时恢复模型与挂件两侧。
- KubeJS、Aquaculture、Sophisticated Backpacks、Accessories：当前目标构件为 NeoForge-only 或缺少 Fabric 依赖。
- EMI、The One Probe、Iron Chests、Ponder、Immersive Melodies、Improved Mobs、Simple Hats、Embeddium：没有当前目标的可测 Fabric 闭包。
- Refurbished Furniture：当前树中判定为无落点，不得只凭类名或旧版白名单宣称支持。

这些项目不是“永远不做”的承诺；只有拿到精确的 26.1.2 Fabric 构件和可复现测试路径后，才能重新进入活动兼容表。

## 兼容恢复闸门

恢复或新增模块时，同一变更必须检查：

1. `build.gradle` 的依赖范围、版本和缺件行为；
2. `fabric.mod.json` 的 Fabric entrypoint；
3. `touhou_little_maid.mixins.json` 与 `touhou_little_maid_fabric.mixins.json` 的登记和条件加载；
4. 无目标模组启动、带目标模组启动、单人和专服路径；
5. 对应交互、同步、渲染、存档与资源重载行为。

编译和启动都正常但入口漏登记，功能仍可能从未执行；因此入口和 mixin 登记属于静默故障面，不能用 compile==0 代替验证。

## 配置与数据边界

- 兼容菜单只显示当前构建确有消费者的配置项。
- 可选模组专属配置按 `FabricLoader.isModLoaded` 动态显示，服务端规则仍由服务端裁决。
- 不擅自更改旧配置键名、默认值、序列化格式或存档 tag；确有迁移时先加读取兼容和回归测试。
- 第三方 jar 的许可证与本项目不同，必须留在本地 `libs/compile_only/` 或由 CI 临时下载，不能打进公开源码树。
