# Touhou Little Maid: Tsumugi — Minecraft 1.21.11 Fabric

**简体中文** | [English](README_en.md)

[![CurseForge](https://img.shields.io/curseforge/dt/1636073?logo=curseforge&logoColor=white&label=CurseForge&color=F16436)](https://www.curseforge.com/minecraft/mc-mods/touhou-little-maid-tsumugi)
[![Release](https://img.shields.io/github/v/release/gege-tlph/TouhouLittleMaid-Tsumugi?logo=github&label=Release)](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/releases/latest)
[![GitHub downloads](https://img.shields.io/github/downloads/gege-tlph/TouhouLittleMaid-Tsumugi/total?logo=github&label=Downloads)](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A)](https://www.minecraft.net/)
[![Mod Loader](https://img.shields.io/badge/Mod%20Loader-Fabric-DBD0B4)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT%20%2B%20CC%20BY--NC--SA%204.0-blue)](LICENSE-MIT)

> [!IMPORTANT]
> 本仓库是 [Sh1roCu/TouhouLittleMaid-Orihime](https://github.com/Sh1roCu/TouhouLittleMaid-Orihime)
> 的非官方后续移植，目标平台为 **Minecraft 1.21.11 + Fabric**，独立维护。
> 以 Orihime 的 Minecraft 1.21.1 Fabric 实现为行为基准，本项目不代表原模组官方版本。
> 其他 Minecraft 版本请使用
> [上游发行版](https://github.com/TartaricAcid/TouhouLittleMaid/releases)。

[Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid)（东方小女仆）是以东方 Project
为主题的女仆模组：你可以召唤女仆陪伴、让她们种地打鱼做饭、整理仓库、跟着你战斗、装扮和聊天。玩法资料请参阅
[Touhou Little Maid Wiki](http://page.cfpa.team/TouhouLittleMaid/)。

## 下载

| 渠道 | 链接 |
|---|---|
| CurseForge | [touhou-little-maid-tsumugi](https://www.curseforge.com/minecraft/mc-mods/touhou-little-maid-tsumugi) |
| GitHub Releases | [Releases](../../releases) |

两个渠道发布的是同一个构建。

## 关于本分支

上游本体面向 Minecraft 1.21.1，Orihime 把它移植到了 1.21.1 的 Fabric。本分支在此基础上
迁移到 **Minecraft 1.21.11** 上游功能和本分支已经没有差异。

除了版本更新，本分支还做了：

- **换用 Yes Steve Model 的女仆也能正常穿戴。** 背包、手持物品、主副手、头饰、背旗都会跟着
  模型的骨骼动。
- **女仆的追踪标记不会被光影吃掉。** 开着 Iris / Sodium 时也能稳定看到。
- **兼容 MrCrayfish's Furniture Mod: Refurbished。** 它的抽屉、柜子、板条箱等储物家具可以接入
  女仆的无线物品传输，桌子会被识别成放食物的台面。

AI 聊天与配置系统上的改动较多，单列一节说明。

## 本分支新增的玩法

上游没有、本分支加的功能：

### 威胁响应

以前女仆只有在执行「战斗」任务时才会打架，平时被打了也只会站着挨揍。现在每只女仆有一个
**威胁响应**，开关在她的配置界面里切换：

| 档位 | 行为 |
|---|---|
| **关闭** | 原版行为 |
| **自卫** | 女仆空闲状态下被攻击时会还手 |
| **护主** | 你被攻击时她会主动参战 |

应战是**临时状态**，不会覆盖现在的工作安排
配套的几件事：

- **通过AI能力或者规则下达，她可以攻击和平生物。** 平时不会主动攻击和平中立和有主生物。
- **手上拿着什么都能近战**，响应威胁时女仆空手也会进行攻击。

### 配置界面重写

全局配置界面重写，AI 相关的设置完全解耦、收进 AI 配置界面，各自归位；OP 现在可以直接
在全局配置菜单里改单人存档或服务器的配置文件，不必再去翻文件。

AI 配置界面也重写了，好用得多：站点可以就地「检查配置」告诉你是连不上还是没填对，
音色能直接试听，密钥全程留在服务端，改完保存立即生效。

命令也按同样的边界分开。`/tlm config` 是本分支新增的——上游只有 `/tlm ai_chat`，改了配置
文件没有任何命令能让它生效，只能重进世界或重启服务器：

| 命令 | 作用 |
|---|---|
| `/tlm config reload` | 重载世界规则|
| `/tlm ai_chat reload` | 重载 AI 的站点与技能 |
| `/tlm ai_chat status` | 打印 AI 服务状态 |
| `/tlm ai_chat sites` | 列出服务端配置的LMM/TTS |

## 本分支已修复的上游缺陷

以下都是从上游继承下来、本分支已修复：

- **炉子背包会把东西挪位置，甚至烧掉燃料。** 取出中间的物品后，剩下的会被挤到前面去；
  如果输入格空着，放进去的燃料会被当成原料烧成别的东西。另外，烧制进度以前每次重进世界都会归零。
- **手改配置写错一个字符，世界就再也打不开。** 而且备份回滚也救不回来——文件本身格式是对的，
  重读一遍还是同一条坏设置。现在坏的那一项会被单独跳过并写进日志，其余设置照常生效。
- **女仆吃东西时会把副手的物品弄丢。** 她主手空着的时候会用主手拿食物，收尾却总去清副手，
  于是副手里跟吃饭无关的东西被扫进背包，背包满了就掉在地上。
- **女仆会截走玩家扔出的忠诚三叉戟。** 附了忠诚的三叉戟在飞回你手上的途中会被女仆捡走。
- **女仆刚好消失时打开她的界面会让客户端崩溃。**
- **灭火剂灭不了灵魂火。**
- **聊久了之后，女仆只会闲聊、不再执行指令。** 你说「跟着我」「坐下」，她答应得很好听，
  然后什么也不做。现在动作的判断和执行走一条不带聊天记录的独立通道，而且在她开口**之前**
  完成——先做，再说，不会再答应一件没做的事。
- **选了别的语音语种，女仆说一两轮就变回聊天语言。** 得清空聊天记录才能恢复。现在要朗读的
  文本由一次单独的、不看聊天记录的请求产出，不再受历史影响。
- **用云端语音识别时，网络一慢整个客户端就冻住。** 卡到识别结果回来为止。

## 兼容性

| 组件 | 要求 |
|---|---|
| Minecraft | 1.21.11 |
| Java | 21 |
| Fabric Loader | 0.18.5 或更高版本 |
| Fabric API | 0.141.1+1.21.11 或更高的 1.21.11 版本 |
| Forge Config API Port | 21.11.0+（**必装**） |
| 安装位置 | 客户端与服务端 |

## 安装

1. 安装适用于 Minecraft 1.21.11 的 Fabric Loader。
2. 从 [CurseForge](https://www.curseforge.com/minecraft/mc-mods/touhou-little-maid-tsumugi)
   或 [Releases](../../releases) 下载名称中**不含** `sources` 或 `shadow` 的
   `touhoulittlemaid-fabric-*.jar`。
3. 把本模组、[Fabric API](https://modrinth.com/mod/fabric-api) 和
   [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port)
   一起放入客户端和服务端的 `mods` 目录。

推荐但非必需：[Mod Menu](https://modrinth.com/mod/modmenu) +
[Cloth Config API](https://modrinth.com/mod/cloth-config)（游戏内配置界面）、
[JEI](https://modrinth.com/mod/jei) 或 [REI](https://modrinth.com/mod/rei)（查看祭坛配方）。

表中版本是当前发布所验证的组合。使用其他兼容版本时若遇到问题，请先用这组版本复现。

## 可选模组兼容

不影响运行

### 需要从本组织获取

下列模组的 **Minecraft 1.21.11 Fabric 版本由本组织移植并维护**，上游没有对应版本，
请从下面的仓库下载，不要去上游找：

| 模组 | 用途 | 下载 |
|---|---|---|
| **OpenYSM-Updated** | 自己实现的车万女仆的兼容 | [gege-tlph/OpenYSM-Updated](https://github.com/gege-tlph/OpenYSM-Updated/releases) |
| **Patchouli** | 帕秋莉手册 | [gege-tlph/Patchouli](https://github.com/gege-tlph/Patchouli/releases) |
| **Maid Restaurant**（附属模组） | 女仆厨房 | [gege-tlph/MaidRestaurant](https://github.com/gege-tlph/MaidRestaurant/releases) |

> [!NOTE]
> OpenYSM 上游虽有 1.21.11 版本，但其中的女仆兼容模块在 Fabric 上是空实现，装了也不会生效。
> 本组织的分支把它做成了真的实现，**要让女仆用自定义模型必须用这个分支**。

### 其余可选兼容

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

## 暂时没有兼容的模组

上游版本里有一批第三方兼容，本分支暂时没有。绝大多数是**那一侧还没有可以对接的 Minecraft
1.21.11 Fabric 目标**，不是我们不想做：

| 模组 | 情况 |
|---|---|
| EMI | Fabric 版最新到 1.21.1 |
| Accessories | Fabric 版最新到 1.21.10 |
| Immersive Melodies | Fabric 版最新到 1.21.1 |
| Simple Hats | Fabric 版最新到 1.21.1 |
| Ponder | Fabric 版最新到 1.20.1 |
| Improved Mobs | Fabric 版最新到 1.21.1 |
| Just More Cakes | Fabric 版最新到 1.21.1 |
| TACZ | 本体只有 Forge；Fabric 移植版最新到 1.21.1 |
| KubeJS | 上游已放弃该兼容 |
| Iron Chests | 没有 Fabric 版 |
| Aquaculture | 没有 Fabric 版（官方只发 NeoForge） |
| Superb Warfare | 没有 Fabric 版 |
| SlashBlade | 没有 Fabric 版 |
| The One Probe | 没有 Fabric 版 |
| Sophisticated Backpacks · Traveler's Backpack · ExtraContainer | 这三个的兼容都是经 **Accessories** 的槽位接口做的。Traveler's Backpack 自己有 1.21.11 版本，但 Accessories 停在 1.21.10 |
| ProxLib | 暂不考虑兼容 |
| Embeddium | Fabric 上由 Sodium / Iris 取代，这两个都已兼容 |

**这不是永久决定。** 等它们出了 1.21.11 的 Fabric 版本，需要的话我们会把兼容重新接上；
如果某个模组确实需要而作者没有跟进，我们也会像对 Yes Steve Model 和 Patchouli 那样，
自己移植一份维护版本再对接。

## 反馈问题

本分支仍在积极开发中，遇到问题请提交 [Issue](../../issues/new/choose)。issue 区有四种模板：

| 模板 | 用于 |
|---|---|
| 缺陷报告 | 行为不对、崩溃、渲染或存档问题 |
| 模组兼容 | 与别的模组一起用时出问题，或希望新增兼容 |
| 功能建议 | 希望新增或改进玩法、界面、命令 |
| 使用问题 | 安装、配置、AI 聊天与语音服务的用法 |

无论用哪个模板，都请附上 `logs/latest.log`、模组列表和复现步骤；若与 AI 功能相关，
请说明用的是云端服务还是系统语音，并**先给密钥打码**。

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
