# Touhou Little Maid: Tsumugi — Minecraft 1.21.11 Fabric

> [!IMPORTANT]
> 本仓库是 [Sh1roCu/TouhouLittleMaid-Orihime](https://github.com/Sh1roCu/TouhouLittleMaid-Orihime)
> 的非官方后续移植，目标平台为 **Minecraft 1.21.11 + Fabric**，独立维护。
> 以 Orihime 的 Minecraft 1.21.1 Fabric 实现为行为基准，本项目不代表原模组官方版本。
> 其他 Minecraft 版本请使用
> [上游发行版](https://github.com/TartaricAcid/TouhouLittleMaid/releases)。

[Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid)（东方小女仆）是以东方 Project
为主题的女仆模组：你可以召唤女仆陪伴、让她们种地打鱼做饭、整理仓库、跟着你战斗，也可以给她们换模型、
换背包、装扮和聊天。玩法资料请参阅
[Touhou Little Maid Wiki](http://page.cfpa.team/TouhouLittleMaid/)。

English: An unofficial Fabric 1.21.11 port of Touhou Little Maid, continuing from the Orihime 1.21.1
Fabric port.

## 关于本分支

上游本体面向 Minecraft 1.21.1，Orihime 把它移植到了 1.21.1 的 Fabric。本分支在此基础上
迁移到 **Minecraft 1.21.11** 并继续维护——女仆的全部玩法链路（日程、睡眠、工作、进食、钓鱼、
背包、无线物品传输、战斗、AI 聊天与语音）都已在新版本上跑通。

除了版本更新，本分支还做了几件玩家能直接感受到的事：

- **换用 Yes Steve Model 的女仆也能正常穿戴。** 背包、手持物品、主副手、头饰、背旗都会跟着
  模型的骨骼动。
- **女仆的追踪标记不会被光影吃掉。** 开着 Iris / Sodium 时也能稳定看到。
- **AI 聊天的密钥留在服务端。** 服主配好之后玩家不必也不能看到密钥；改完点保存立即生效，
  不需要再敲任何命令。每只女仆可以单独选用不同的服务，也可以直接跟随世界默认。
- **兼容 MrCrayfish's Furniture Mod: Refurbished。** 它的抽屉、柜子、板条箱等储物家具可以接入
  女仆的无线物品传输，桌子会被识别成放食物的台面。

## 本分支修掉的上游缺陷

以下都是从上游继承下来、任何版本的使用者都会遇到的问题，本分支已修复：

- **炉子背包会把东西挪位置，甚至烧掉燃料。** 取出中间的物品后，剩下的会被挤到前面去；
  如果输入格空着，放进去的燃料会被当成原料烧成别的东西。另外，烧制进度以前每次重进世界都会归零。
- **手改配置写错一个字符，世界就再也打不开。** 而且备份回滚也救不回来——文件本身格式是对的，
  重读一遍还是同一条坏设置。现在坏的那一项会被单独跳过并写进日志，其余设置照常生效。
- **女仆吃东西时会把副手的物品弄丢。** 她主手空着的时候会用主手拿食物，收尾却总去清副手，
  于是副手里跟吃饭无关的东西被扫进背包，背包满了就掉在地上。
- **女仆会截走玩家扔出的忠诚三叉戟。** 附了忠诚的三叉戟在飞回你手上的途中会被女仆捡走。
- **女仆刚好消失时打开她的界面会让客户端崩溃。**
- **灭火剂灭不了灵魂火。**

## 兼容性

| 组件 | 要求 |
|---|---|
| Minecraft | 1.21.11 |
| Java | 21 |
| Fabric Loader | 0.19.3 或更高版本 |
| Fabric API | 0.141.4+1.21.11 或更高的 1.21.11 版本 |
| Forge Config API Port | 21.11.1（**必装**，缺少则无法启动） |
| 安装位置 | 客户端与服务端 |

## 安装

1. 安装适用于 Minecraft 1.21.11 的 Fabric Loader。
2. 从 [Releases](../../releases) 下载名称中**不含** `sources` 或 `shadow` 的
   `touhoulittlemaid-fabric-*.jar`。
3. 把本模组、[Fabric API](https://modrinth.com/mod/fabric-api) 和
   [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port)
   一起放入客户端和服务端的 `mods` 目录。

推荐但非必需：[Mod Menu](https://modrinth.com/mod/modmenu) +
[Cloth Config API](https://modrinth.com/mod/cloth-config)（游戏内配置界面）、
[JEI](https://modrinth.com/mod/jei) 或 [REI](https://modrinth.com/mod/rei)（查看祭坛配方）。

表中版本是当前发布所验证的组合。使用其他兼容版本时若遇到问题，请先用这组版本复现。

## 可选模组兼容

装了才生效，不装不影响运行。

### 需要从本组织获取

下列模组的 **Minecraft 1.21.11 Fabric 版本由本组织移植并维护**，上游没有对应版本，
请从下面的仓库下载，不要去上游找：

| 模组 | 用途 | 下载 |
|---|---|---|
| **OpenYSM-Updated** | 给女仆换用自定义模型与动画，挂件跟随骨骼 | [gege-tlph/OpenYSM-Updated](https://github.com/gege-tlph/OpenYSM-Updated/releases) |
| **Patchouli** | 游戏内手册《记忆中的幻想乡》 | [gege-tlph/Patchouli](https://github.com/gege-tlph/Patchouli/releases) |
| **Maid Restaurant**（附属模组） | 厨师女仆烹饪、服务女仆取餐送到桌位 | [gege-tlph/MaidRestaurant](https://github.com/gege-tlph/MaidRestaurant/releases) |

> [!NOTE]
> OpenYSM 上游虽有 1.21.11 版本，但其中的女仆兼容模块在 Fabric 上是空实现，装了也不会生效。
> 本组织的分支把它做成了真的实现，**要让女仆用上自定义模型必须用这个分支**。

### 其余可选兼容

从各自的官方渠道安装即可：

| 模组 | 兼容内容 |
|---|---|
| MrCrayfish's Furniture: Refurbished | 储物家具接入无线物品传输，桌子可放食物 |
| JEI / REI | 祭坛配方展示；REI 支持背包配方转移 |
| Jade | 查看女仆、祭坛、野餐垫、墓碑等信息 |
| Sodium / Iris | 动态模型与背旗渲染、光影热切换 |
| Farmer's Delight Refabricated | 女仆识别并食用其食物 |
| Kaleidoscope Cookery Refabricated | 分份食物与水稻的收获补种 |
| Kaleidoscope Tavern Refabricated | 桌台、坐具、葡萄连续采收 |
| Rustic Delight | 女仆识别并食用其食物 |
| Carry On | 搬运女仆时的模型与姿势 |
| PatPat | 摸头效果 |
| Inventory Profiles Next | 女仆背包界面排序 |

## 已知问题

本分支仍在积极开发中，遇到问题请提交
[Issue](../../issues)，并附上 `logs/latest.log`、模组列表和复现步骤；
若与 AI 功能相关，请说明用的是云端服务还是系统语音。

AI 聊天与语音需要你自己准备服务与密钥，本模组不提供也不代理任何服务。

## 从源码构建

需要 JDK 21：

```bash
./gradlew build
```

Windows PowerShell 用 `.\gradlew.bat build`。产物在 `build/libs/`，使用 Mojang 官方映射。

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

入口类需要实现 `ILittleMaid`。兼容代码应保持可选依赖边界，不要求未安装的模组类参与基础加载。

## 来源与许可证

- 原模组：[TartaricAcid/TouhouLittleMaid](https://github.com/TartaricAcid/TouhouLittleMaid)
- Fabric 移植来源：[Sh1roCu/TouhouLittleMaid-Orihime](https://github.com/Sh1roCu/TouhouLittleMaid-Orihime)
- 本分支：[gege-tlph/TouhouLittleMaid-Tsumugi](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi)

代码采用 MIT License，美术与资源采用 CC BY-NC-SA 4.0，详见
[`LICENSE-MIT`](LICENSE-MIT) 与 [`LICENSE-CC`](LICENSE-CC)。
