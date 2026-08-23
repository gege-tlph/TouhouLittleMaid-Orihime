# Touhou Little Maid: Tsumugi

[简体中文](README.md) | **English**

[![Release](https://img.shields.io/github/v/release/gege-tlph/TouhouLittleMaid-Tsumugi?logo=github&label=Release)](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/releases)
[![GitHub downloads](https://img.shields.io/github/downloads/gege-tlph/TouhouLittleMaid-Tsumugi/total?logo=github&label=Downloads)](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/releases)
[![Mod Loader](https://img.shields.io/badge/Mod%20Loader-Fabric-DBD0B4)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT%20%2B%20CC%20BY--NC--SA%204.0-blue)](LICENSE-MIT)

> [!IMPORTANT]
> This repository is an unofficial continuation series of
> [Sh1roCu/TouhouLittleMaid-Orihime](https://github.com/Sh1roCu/TouhouLittleMaid-Orihime), maintained
> independently by gege-tlph and not an official release of the original mod. This branch targets
> **Minecraft 26.1.2 Fabric** and is currently a **Beta**.

[Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid) is a Touhou Project themed maid mod:
summon maids to keep you company, farm, fish, organize items, fight beside you, change outfits and chat.
See the [Touhou Little Maid Wiki](http://page.cfpa.team/TouhouLittleMaid/) for gameplay documentation.

## Supported versions

| Minecraft | Loader | Branch | Status |
|---|---|---|---|
| 26.1.2 | Fabric | [`port/26.1.2`](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/tree/port/26.1.2) | `v1.0.22-beta.1`, Beta |
| 1.21.11 | Fabric | [`port/1.21.11-fabric`](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/tree/port/1.21.11-fabric) | Stable maintenance |

Each Minecraft version is maintained on an independent branch. Match the artifact, dependencies and save data
to the target version; do not mix artifacts across Minecraft versions.

## Downloads

Download the Minecraft 26.1.2 `touhoulittlemaid-fabric-*.jar` whose name does not contain `sources`,
`shadow` or `dev` from
[GitHub Releases](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/releases).

This Beta is still collecting feedback from real clients, dedicated servers and third-party mod combinations.
Back up your world before updating.

## About this project

The 26.1.2 branch is based on Orihime's Fabric implementation for the same Minecraft version and uses this
project's verified 1.21.11 branch as its behavior baseline. Beyond adapting APIs, data and rendering for
Minecraft 26.1.2, this project includes:

- Maids can defend themselves or protect their owner while preserving their scheduled work.
- Maids keep their distance when using bows, crossbows or TACZ guns with ammunition.
- Reworked configuration and AI settings screens, server-rule sync, site checks, TTS previews and reload commands.
- Fixes for furnace backpacks, off-hand food loss, Loyalty tridents, extinguishers and maid-screen race conditions.
- Fixes for chairs, item previews, model markers, backpack screens and other cross-version rendering regressions.

AI chat and voice require players to provide their own services and credentials. This mod does not provide or
proxy any AI service.

## 26.1.2 branch: requirements

| Component | Requirement |
|---|---|
| Minecraft | 26.1.2 |
| Java | 25 |
| Fabric Loader | `>=0.19.0`; `>=0.19.3` with TACZ R2 |
| Fabric API | `>=0.149.0`; `>=0.155.2` with TACZ R2 |
| Forge Config API Port | `>=26.1.5`, required |
| Install on | Both client and server |

Recommended but optional: Mod Menu and Cloth Config for in-game configuration, JEI or REI for altar recipes,
and Patchouli for the *Memorizable Gensokyo* manual. If Patchouli is missing, the in-game warning opens the
[Patchouli Fabric download page](https://www.curseforge.com/minecraft/mc-mods/patchouli-fabric); the currently
verified version is `26.1-94-beta`.

## 26.1.2 branch: installation

1. Install Fabric Loader for Minecraft 26.1.2.
2. Install Fabric API and Forge Config API Port.
3. Download this mod's installable JAR and place it with the dependencies in the `mods` folder on client and server.
4. Install Patchouli Fabric for the in-game manual, or Cloth Config and Mod Menu for in-game configuration screens.

The table lists the base runtime minimums. Optional integrations may raise the minimum Fabric Loader or
Fabric API version.

## 26.1.2 branch: optional mod compatibility

| Mod | Compatibility |
|---|---|
| Patchouli | Built-in *Memorizable Gensokyo* manual, altar documentation and recipe pages |
| JEI / REI | Altar recipes and item variant display |
| Sodium / Iris | Dynamic models, markers and shader-environment rendering |
| TACZ Refabricated R2 | Maid gun combat, backpack ammunition, ranged response and gun rendering |
| Farmer's Delight Refabricated | Maids recognize and eat its food |
| Kaleidoscope Cookery Refabricated | Special food, rice harvesting and replanting |
| Kaleidoscope Tavern Refabricated | Tables, seating, grape harvesting and work-food boundaries |
| Carry On | Maid model, pose and blacklist behavior while carried |

TACZ compatibility targets the 26.1.2 R2 build from
[TaCZ Refabricated Unofficial](https://github.com/q14433686-arch/TaCZ_Refabricated_Unofficial/releases).
Third-party JARs are not bundled into this mod.

This Beta does not claim compatibility with KubeJS, Aquaculture, Accessories, Sophisticated Backpacks or
YSM/OpenYSM because no verified 26.1.2 Fabric integration path is currently available. Optional compatibility
code does not participate in base loading when its target mod is absent.

## Reporting issues

Choose the matching template under [Issues](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/issues).
State the Minecraft version, Fabric Loader, Fabric API and full mod list first, then attach `logs/latest.log` and
reproduction steps. Redact credentials before reporting an AI-related issue.

## Building from source (26.1.2 branch)

Check out `port/26.1.2` and install JDK 25. The build also requires the TACZ R2 and Patchouli Fabric compile-time
JARs in `libs/compile_only/`; they are compile inputs only and are not included in the final artifact.

```bash
./gradlew build
```

On Windows PowerShell use `./gradlew.bat build`. Artifacts are written to `build/libs/`; install only the remapped JAR.

## Extension API

Addon mods can register the `little_maid_extension` entrypoint in their own `fabric.mod.json`; the entrypoint class
implements `ILittleMaid`. Compatibility code should preserve optional dependency boundaries and must not require
classes from an absent mod during base loading.

## Sources and license

- Original mod: [TartaricAcid/TouhouLittleMaid](https://github.com/TartaricAcid/TouhouLittleMaid)
- Fabric port this continues from: [Sh1roCu/TouhouLittleMaid-Orihime](https://github.com/Sh1roCu/TouhouLittleMaid-Orihime)
- This project: [gege-tlph/TouhouLittleMaid-Tsumugi](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi)

Code is MIT licensed; art and assets are CC BY-NC-SA 4.0. See [`LICENSE-MIT`](LICENSE-MIT) and
[`LICENSE-CC`](LICENSE-CC).
