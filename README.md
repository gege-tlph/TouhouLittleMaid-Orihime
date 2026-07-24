# Touhou Little Maid: Tsumugi — Minecraft 1.21.11 Fabric

> [!IMPORTANT]
> **Touhou Little Maid: Tsumugi** 是 [Sh1roCu/TouhouLittleMaid-Orihime](https://github.com/Sh1roCu/TouhouLittleMaid-Orihime)
> 的非官方后续移植，目标平台为 **Minecraft 1.21.11 + Fabric**，独立维护。
> 本项目以 Orihime 的 Minecraft 1.21.1 Fabric 实现为行为基准，不代表原模组官方版本。

[Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid) 是以东方 Project 为主题的女仆模组。
玩法资料请参阅 [Touhou Little Maid Wiki](http://page.cfpa.team/TouhouLittleMaid/)。

## 关于本次移植

本 fork 将 Orihime 的 Minecraft 1.21.1 Fabric 版本完整迁移至 **Minecraft 1.21.11**，
使用 Java 21、Mojang 官方映射、Gradle 9.5 与 Fabric Loom 1.17。
开发分支为 [`port/1.21.11-fabric`](../../tree/port/1.21.11-fabric)，构建产物可从
[GitHub Actions](../../actions) 获取；Beta 与正式发布版本请从 [Releases](../../releases) 下载。

移植亮点：

- 将女仆、椅子及其他实体迁移至 1.21.11 的渲染状态提取/提交架构，并重写方块实体与特殊物品渲染链路。
- 保留 Bedrock 与 Gecko 动态模型、动画、模型包热重载、GIF 表情、雕像和车库套件预览；修复动态眼睛、尾巴、双面材质、视锥剔除与零面积 UV 亮线等问题。
- 适配 1.21.11 数据驱动物品模型，让御币、相机、灭火器、女仆信标、零食柜、胶片、照片和三维预览物品正常显示。
- 按新版本 API 迁移物品数据组件、NBT、方块实体持久化、SavedData、配方、战利品、进度与 Mixin 注入点，并保持旧存档所需的数据兼容。
- 使用标准 Fabric `CustomPayload` 完成网络同步；专用服务器通过配方摘要同步恢复祭坛配方显示，不修改原版数据包结构，可用于 Velocity 代理环境。
- 恢复女仆日程、睡眠、工作、进食、钓鱼、盾牌格挡、背包、无线物品传输、AI 聊天及语音等完整玩法链路。
- 增加每名女仆独立的威胁响应策略，并统一战斗目标安全规则、玩家命令仲裁与瞬态应战状态。
- 配置与 AI 站点采用事务写盘、last-good 恢复和专用服务器显式重载，避免失败保存破坏已有文件。
- 第三方适配采用可插拔设计：未安装对应模组时不加载兼容代码，配置界面隐藏无效选项，但配置文件仍保留稳定的默认结构。

## 安装

### 必要前置

| 组件 | 已验证版本 | 下载 |
|---|---:|---|
| Minecraft | 1.21.11 | [Minecraft 官网](https://www.minecraft.net/) |
| Java | 21 | [Eclipse Temurin 21](https://adoptium.net/temurin/releases/?version=21) |
| Fabric Loader | 0.19.3 | [Fabric Installer](https://fabricmc.net/use/installer/) |
| Fabric API | 0.141.4+1.21.11 | [Modrinth](https://modrinth.com/mod/fabric-api) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/fabric-api) |
| Forge Config API Port | 21.11.1 | [Modrinth](https://modrinth.com/mod/forge-config-api-port) · [CurseForge](https://www.curseforge.com/minecraft/mc-mods/forge-config-api-port-fabric) |

本模组本体请从 [GitHub Releases](../../releases) 下载。选择名称中不含 `sources` 或 `shadow`
的 `touhoulittlemaid-fabric-...jar`；客户端与服务器都需要安装本模组、Fabric API 和
Forge Config API Port。Java 21 是 Minecraft 1.21.11 客户端、服务器及源码构建所需的运行环境。

推荐安装：

- [Mod Menu](https://modrinth.com/mod/modmenu) 与 [Cloth Config API](https://modrinth.com/mod/cloth-config)：游戏内配置界面。
- [Patchouli 1.21.11 Fabric fork](https://github.com/gege-tlph/Patchouli/releases)：游戏内手册。
- [JEI](https://modrinth.com/mod/jei) 或 [REI](https://modrinth.com/mod/rei)：祭坛配方查看。

将本模组及必要前置放入客户端和服务器的 `mods` 目录。表中版本是当前正式构建和测试所用的
已验证组合；若使用其他兼容版本，遇到问题时请先用这组版本复现。

## 可选模组兼容

本移植包含或验证了以下可选兼容：

| 模组 | 兼容内容 |
|---|---|
| Patchouli | 《记忆中的幻想乡》手册、祭坛配方组件与界面跳转 |
| JEI / REI | 祭坛配方展示；REI 背包配方转移 |
| Jade | 女仆、祭坛、野餐垫、墓碑等信息显示 |
| Sodium / Iris | 动态模型与旗帜渲染、光影切换和资源重载 |
| Carry On | 搬运女仆时的模型与姿势兼容 |
| PatPat | 女仆及 Gecko 模型的拍打缩放效果 |
| Farmer's Delight Refabricated | 食物识别与女仆进食逻辑 |
| Kaleidoscope Cookery Refabricated | 作物和可食用方块识别 |
| Kaleidoscope Tavern Refabricated | 葡萄连续采收、工作餐保护、座椅和家具交互 |
| Inventory Profiles Next | 女仆背包界面排序 |

兼容项均为软依赖。未列出的旧版第三方集成不属于当前 1.21.11 移植的支持范围。

## 已知限制

- 真实外部 LLM provider 的“上下文查询 → 决策 → Tool 调用 → 服务端结果”全链路黑盒终验尚未完成；本版本不把提示词、Tool 或 Skill 的自动化测试等同于真实 provider 验收。
- 服务器提供 STT 的真实远程客户端与 Velocity 切服矩阵尚未全部关闭；STT 与 PatPat 按已记录条件提供支持。
- 一格浅水泳姿以及史莱姆、岩浆怪、经验球替换渲染器仍缺最后一轮真实客户端视觉矩阵；对应服务端状态、自动化与基础客户端入世门已通过。
- KubeJS、YSM 与 Aquaculture 仍在后续兼容计划中，不属于本版本已验证范围。More Delight 25.12.10 存在其自身初始化顺序问题。

## 从源码构建

需要 JDK 21。克隆仓库并切换到移植分支后运行：

```bash
./gradlew build
```

Windows PowerShell：

```powershell
.\gradlew.bat build
```

可分发的二进制位于 `build/libs/`。本项目使用 Mojang 官方映射。

## 扩展接口

附属模组可以在自己的 `fabric.mod.json` 中注册 `little_maid_extension` 入口：

```json
{
  "entrypoints": {
    "little_maid_extension": [
      "com.example.yourmod.YourMaidExtension"
    ]
  }
}
```

入口类需要实现 `ILittleMaid`。兼容代码应保持可选依赖边界，不应要求未安装的模组类参与基础加载。

## 来源与许可证

- 原模组：[TartaricAcid/TouhouLittleMaid](https://github.com/TartaricAcid/TouhouLittleMaid)
- Fabric 移植来源：[Sh1roCu/TouhouLittleMaid-Orihime](https://github.com/Sh1roCu/TouhouLittleMaid-Orihime)
- Minecraft 1.21.11 维护分支（本项目 Tsumugi）：[gege-tlph/TouhouLittleMaid-Tsumugi](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi)

代码采用 MIT License；美术与资源采用 CC BY-NC-SA 4.0。详情见仓库中的
[`LICENSE-MIT`](LICENSE-MIT) 与 [`LICENSE-CC`](LICENSE-CC)。
