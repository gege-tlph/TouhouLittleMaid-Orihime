# Touhou Little Maid: Tsumugi

**简体中文** | [English](README_en.md)

[![Release](https://img.shields.io/github/v/release/gege-tlph/TouhouLittleMaid-Tsumugi?logo=github&label=Release)](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/releases)
[![GitHub downloads](https://img.shields.io/github/downloads/gege-tlph/TouhouLittleMaid-Tsumugi/total?logo=github&label=Downloads)](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/releases)
[![CurseForge](https://img.shields.io/curseforge/dt/1636073?logo=curseforge&logoColor=white&label=CurseForge&color=F16436)](https://www.curseforge.com/minecraft/mc-mods/touhou-little-maid-tsumugi)
[![Mod Loader](https://img.shields.io/badge/Mod%20Loader-Fabric-DBD0B4)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT%20%2B%20CC%20BY--NC--SA%204.0-blue)](LICENSE-MIT)

> [!IMPORTANT]
> 本仓库是 [Sh1roCu/TouhouLittleMaid-Orihime](https://github.com/Sh1roCu/TouhouLittleMaid-Orihime)
> 的非官方后续移植系列，由 gege-tlph 独立维护，不代表原模组官方版本。本分支面向
> **Minecraft 26.1.2 Fabric**，当前版本为 **Beta**。

[Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid)（东方小女仆）是以东方 Project
为主题的女仆模组：你可以召唤女仆陪伴，让她们种地、钓鱼、整理物品、跟随战斗、换装并与你聊天。
玩法资料请参阅 [Touhou Little Maid Wiki](http://page.cfpa.team/TouhouLittleMaid/)。

## 支持的版本

| Minecraft | 加载器 | 分支 | 状态 |
|---|---|---|---|
| 26.1.2 | Fabric | [`port/26.1.2-fabric`](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/tree/port/26.1.2-fabric) | `v1.0.22-beta.1`，Beta |
| 1.21.11 | Fabric | [`port/1.21.11-fabric`](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/tree/port/1.21.11-fabric) | 稳定维护 |

每个 Minecraft 版本使用独立分支维护。构件、依赖和存档均应按目标版本核对，不要跨版本混装。

## 下载

从 [GitHub Releases](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/releases) 下载与
Minecraft 26.1.2 对应、名称中不含 `sources`、`shadow` 或 `dev` 的
`touhoulittlemaid-fabric-*.jar`。

也可以查看 [CurseForge 项目页](https://www.curseforge.com/minecraft/mc-mods/touhou-little-maid-tsumugi)。
项目页面目前仍在审核中，审核通过后会逐步显示 26.1.2 构件。

本 Beta 仍在收集真实客户端、专用服务器和第三方模组组合的反馈。更新前请备份世界。

## 关于本项目

26.1.2 分支基于 Orihime 的同版本 Fabric 实现，并以本项目已验证的 1.21.11 分支作为行为基准。
除适配 Minecraft 26.1.2 的 API、数据和渲染体系外，本项目还包含：

- 女仆空闲时可以自卫或保护主人，临时应战不会覆盖原有工作日程。
- 女仆手持弓、弩或 TACZ 枪械并有弹药时会保持距离射击。
- 重写配置与 AI 设置界面，支持服务器规则同步、站点检查、TTS 试听和独立重载命令。
- 修复炉子背包、进食副手物品、忠诚三叉戟、灭火剂、女仆界面竞态等继承问题。
- 修复坐垫、物品预览、模型标记、背包界面和多种跨版本渲染回归。

AI 聊天与语音需要玩家自行准备服务和密钥，本模组不提供或代理任何 AI 服务。

## 本分支新增的玩法

### 威胁响应

女仆在空闲时可以根据配置自卫或保护主人。应战是临时状态，不会覆盖原有的工作安排：

| 档位 | 行为 |
|---|---|
| **关闭** | 保持原有行为 |
| **自卫** | 女仆被攻击时还手 |
| **护主** | 主人被攻击时主动参战 |

女仆手持弓、弩或 TACZ 枪械且有弹药时会保持距离射击，不再拿着远程武器直接冲入近战。

### 配置与 AI 界面

全局配置和 AI 配置界面已重新整理。服务器规则会同步到客户端，站点可以直接检查，TTS
可以试听，修改后的设置也可以通过独立命令重新加载。

AI 聊天与语音仍需要玩家自行提供服务地址和凭据；本模组不会提供或代理第三方 AI 服务。

## 本分支已修复的问题

- 炉子背包的物品顺序、燃料处理和烧制进度异常。
- 女仆进食时误清理副手物品。
- 女仆截走玩家投出的忠诚三叉戟。
- 灭火剂无法处理部分灵魂火方块。
- 女仆消失时打开界面导致客户端崩溃。
- Patchouli 手册内容、配方引用和缺失提示不完整。
- 坐垫、物品预览、模型标记、背包界面和持枪渲染回归。
- 多项跨版本资源包和客户端资源重载问题。

## 暂未验证的兼容

以下模组目前没有经过 26.1.2 Fabric 的完整验证，本 Beta 不宣称兼容：

| 模组 | 状态 |
|---|---|
| KubeJS | 暂无经过验证的 26.1.2 Fabric 接入点 |
| Aquaculture | 暂无可用的 26.1.2 Fabric 版本 |
| Accessories | 暂无经过验证的 26.1.2 Fabric 接入点 |
| Sophisticated Backpacks | 依赖的槽位兼容路径尚未验证 |
| YSM / OpenYSM | 暂无经过验证的 26.1.2 Fabric 女仆兼容模块 |

这不是永久排除。等目标模组提供可用版本，或出现可靠的 Fabric 接入路径后，会重新评估兼容性。

## 26.1.2 分支：兼容性要求

| 组件 | 要求 |
|---|---|
| Minecraft | 26.1.2 |
| Java | 25 |
| Fabric Loader | `>=0.19.0`；安装 TACZ R2 时 `>=0.19.3` |
| Fabric API | `>=0.149.0`；安装 TACZ R2 时 `>=0.155.2` |
| Forge Config API Port | `>=26.1.5`，必装 |
| 安装位置 | 客户端与服务端 |

推荐但非必需：Mod Menu 与 Cloth Config（游戏内配置）、JEI 或 REI（查看祭坛配方）、
Patchouli（《幻想乡秘话》手册）。缺少 Patchouli 时，游戏内提示会打开
[Patchouli Fabric 下载页](https://www.curseforge.com/minecraft/mc-mods/patchouli-fabric)；
当前验证版本为 `26.1-94-beta`。

## 26.1.2 分支：安装

1. 安装适用于 Minecraft 26.1.2 的 Fabric Loader。
2. 安装 Fabric API 与 Forge Config API Port。
3. 下载本模组的可安装 JAR，与依赖一起放入客户端和服务端的 `mods` 目录。
4. 需要游戏内手册时安装 Patchouli Fabric；需要游戏内配置界面时安装 Cloth Config 与 Mod Menu。

表中下限是基础运行要求。可选兼容模组可能提高 Fabric Loader 或 Fabric API 的最低版本。

## 26.1.2 分支：可选模组兼容

| 模组 | 兼容内容 |
|---|---|
| Patchouli | 内置《幻想乡秘话》手册、祭坛说明与配方页面 |
| JEI / REI | 祭坛配方与物品变体展示 |
| Sodium / Iris | 动态模型、标记与光影环境渲染 |
| TACZ Refabricated R2 | 女仆持枪作战、背包取弹、远程应战与持枪渲染 |
| Farmer's Delight Refabricated | 女仆识别并食用其食物 |
| Kaleidoscope Cookery Refabricated | 特殊食物、水稻收获与补种 |
| Kaleidoscope Tavern Refabricated | 桌台、坐具、葡萄采收与工作餐边界 |
| Carry On | 搬运女仆时的模型、姿势与黑名单 |

TACZ 兼容针对
[TaCZ Refabricated Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases)
的 26.1.2 R2 构件。第三方 JAR 不会打包进本模组。

KubeJS、Aquaculture、Accessories、Sophisticated Backpacks、YSM/OpenYSM 等目前没有经过验证的
26.1.2 Fabric 接入点，本 Beta 不宣称兼容。可选模组未安装时，其兼容代码不会参与基础加载。

## 反馈问题

请在 [Issues](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/issues) 中选择对应模板。
先说明 Minecraft 版本、Fabric Loader、Fabric API 和完整模组列表，再附上 `logs/latest.log` 与复现步骤；
涉及 AI 功能时请先打码密钥。

请优先使用对应的 Issue 模板，并附上完整模组列表、复现步骤、`logs/latest.log` 或崩溃报告。
Beta 阶段尤其欢迎报告客户端、专用服务器和第三方模组组合中的兼容问题。

## 从源码构建（26.1.2 分支）

确认 checkout 的分支是 `port/26.1.2-fabric`，并安装 JDK 25。构建还需要将 TACZ R2 与 Patchouli Fabric 的
编译期 JAR 放入 `libs/compile_only/`；它们只参与编译，不会进入最终产物。

```bash
./gradlew build
```

Windows PowerShell 使用 `./gradlew.bat build`。产物位于 `build/libs/`，安装时只使用 remap JAR。

## 扩展接口

附属模组可以在自己的 `fabric.mod.json` 中注册 `little_maid_extension` entrypoint，入口类实现
`ILittleMaid`。兼容代码应保持可选依赖边界，不要求未安装模组的类参与基础加载。

## 来源与许可证

- 原模组：[TartaricAcid/TouhouLittleMaid](https://github.com/TartaricAcid/TouhouLittleMaid)
- Fabric 移植来源：[Sh1roCu/TouhouLittleMaid-Orihime](https://github.com/Sh1roCu/TouhouLittleMaid-Orihime)
- 本项目：[gege-tlph/TouhouLittleMaid-Tsumugi](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi)

代码采用 MIT License，美术与资源采用 CC BY-NC-SA 4.0，详见 [`LICENSE-MIT`](LICENSE-MIT) 与
[`LICENSE-CC`](LICENSE-CC)。
