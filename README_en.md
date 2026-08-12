# Touhou Little Maid: Tsumugi — Minecraft 1.21.11 Fabric

[简体中文](README.md) | **English**

[![CurseForge](https://img.shields.io/curseforge/dt/1636073?logo=curseforge&logoColor=white&label=CurseForge&color=F16436)](https://www.curseforge.com/minecraft/mc-mods/touhou-little-maid-tsumugi)
[![Release](https://img.shields.io/github/v/release/gege-tlph/TouhouLittleMaid-Tsumugi?logo=github&label=Release)](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/releases/latest)
[![GitHub downloads](https://img.shields.io/github/downloads/gege-tlph/TouhouLittleMaid-Tsumugi/total?logo=github&label=Downloads)](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A)](https://www.minecraft.net/)
[![Mod Loader](https://img.shields.io/badge/Mod%20Loader-Fabric-DBD0B4)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT%20%2B%20CC%20BY--NC--SA%204.0-blue)](LICENSE-MIT)

> [!IMPORTANT]
> This repository is an unofficial continuation of
> [Sh1roCu/TouhouLittleMaid-Orihime](https://github.com/Sh1roCu/TouhouLittleMaid-Orihime),
> targeting **Minecraft 1.21.11 + Fabric** and maintained independently.
> Orihime's Minecraft 1.21.1 Fabric implementation is the behavior baseline for this port,
> and this project does not represent an official release of the original mod.
> For other Minecraft versions, use the
> [upstream releases](https://github.com/TartaricAcid/TouhouLittleMaid/releases).

[Touhou Little Maid](https://github.com/TartaricAcid/TouhouLittleMaid) is a Touhou Project themed
maid mod: summon maids to keep you company, have them farm, fish, cook, tidy up storage, fight
alongside you, dress them up and chat with them. For gameplay documentation, see the
[Touhou Little Maid Wiki](http://page.cfpa.team/TouhouLittleMaid/).

## Downloads

| Source | Link |
|---|---|
| CurseForge | [touhou-little-maid-tsumugi](https://www.curseforge.com/minecraft/mc-mods/touhou-little-maid-tsumugi) |
| GitHub Releases | [Releases](../../releases) |

Both channels ship the same build.

## About this fork

The upstream mod targets Minecraft 1.21.1, and Orihime ported it to Fabric on 1.21.1. This fork
takes it from there to **Minecraft 1.21.11**; there is no longer any feature gap between upstream
and this fork.

Beyond the version bump, this fork adds:

- **Maids using Yes Steve Model wear their gear correctly.** Backpack, held items, main and off
  hand, headwear and back banner all follow the custom model's bones.
- **Maid tracking markers survive shaders.** They stay visible with Iris / Sodium enabled.
- **Compatibility with MrCrayfish's Furniture Mod: Refurbished.** Its drawers, cabinets, crates and
  other storage furniture can join maid wireless item transfer, and its tables are recognized as
  surfaces you can place food on.

AI chat and the config system changed enough to get their own section below.

## Gameplay added by this fork

Features that upstream does not have:

### Threat response

Maids used to fight only while running the "combat" task; the rest of the time they just stood
there and took the hit. Every maid now has a **threat response** setting, toggled in her config
screen:

| Mode | Behavior |
|---|---|
| **Off** | Vanilla behavior |
| **Self-defense** | An idle maid fights back when she is attacked |
| **Guard** | She joins in when *you* are attacked |

Fighting back is a **temporary state** and does not override her current work assignment.
Along with it:

- **She can attack peaceful mobs when told to, via an AI ability or a rule.** She will not attack
  peaceful, neutral or owned mobs on her own.
- **She can melee with whatever is in her hands** — while responding to a threat, an empty-handed
  maid still attacks.

### Rewritten config screens

The global config screen was rewritten: AI settings are fully decoupled and moved into the AI
config screen, each in its own place. Operators can now edit the singleplayer world's or the
server's config file straight from the global config menu instead of digging through files.

The AI config screen was rewritten too, and is far more usable: each site has a "check config"
action that tells you whether it is unreachable or simply misconfigured, voices can be previewed
in place, credentials never leave the server, and saving takes effect immediately.

Commands follow the same split. `/tlm config` is new in this fork — upstream only has
`/tlm ai_chat`, so after editing a config file no command could apply it and you had to re-enter
the world or restart the server:

| Command | Purpose |
|---|---|
| `/tlm config reload` | Reload world rules |
| `/tlm ai_chat reload` | Reload AI sites and skills |
| `/tlm ai_chat status` | Print AI service status |
| `/tlm ai_chat sites` | List the LLM / TTS sites configured on the server |

## Upstream bugs fixed in this fork

All of these were inherited from upstream and are fixed here:

- **The furnace backpack shuffled items around and even burned fuel.** Taking an item out of the
  middle compacted the rest toward the front, and with an empty input slot the fuel you inserted
  was smelted as if it were an ingredient. Smelting progress also used to reset every time you
  re-entered the world.
- **A single typo in a hand-edited config made the world unopenable** — and restoring a backup
  didn't help, because the file itself was syntactically fine and re-reading it produced the same
  bad setting. Now the offending entry alone is skipped and logged, and the rest still applies.
- **Maids lost off-hand items while eating.** With an empty main hand she would hold the food in
  her main hand, but cleanup always cleared the off hand, so unrelated off-hand items were swept
  into her backpack — and dropped on the ground once it was full.
- **Maids intercepted the player's Loyalty trident** as it flew back to your hand.
- **Opening a maid's screen at the exact moment she despawned crashed the client.**
- **Fire extinguisher couldn't put out soul fire.**
- **After a long conversation, maids only chatted and stopped following orders.** You'd say "follow
  me" or "sit down", she'd agree pleasantly, and then do nothing. Action decisions and execution
  now run on a separate channel that carries no chat history, and complete **before** she speaks —
  act first, then answer, so she can no longer agree to something she didn't do.
- **Picking a different voice language reverted to the chat language after a round or two**, and
  only clearing the chat history brought it back. The text to be spoken is now produced by a
  separate request that doesn't see the chat history, so history can no longer influence it.
- **Cloud speech-to-text froze the whole client whenever the network was slow**, until the
  recognition result came back.

## Requirements

| Component | Requirement |
|---|---|
| Minecraft | 1.21.11 |
| Java | 21 |
| Fabric Loader | 0.18.5 or newer |
| Fabric API | 0.141.1+1.21.11 or a newer 1.21.11 build |
| Forge Config API Port | 21.11.0+ (**required**) |
| Install on | Both client and server |

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Download the `touhoulittlemaid-fabric-*.jar` whose name does **not** contain `sources` or
   `shadow`, from
   [CurseForge](https://www.curseforge.com/minecraft/mc-mods/touhou-little-maid-tsumugi)
   or [Releases](../../releases).
3. Put this mod, [Fabric API](https://modrinth.com/mod/fabric-api) and
   [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port)
   into the `mods` folder of both the client and the server.

Recommended but not required: [Mod Menu](https://modrinth.com/mod/modmenu) +
[Cloth Config API](https://modrinth.com/mod/cloth-config) (in-game config screens),
[JEI](https://modrinth.com/mod/jei) or [REI](https://modrinth.com/mod/rei) (altar recipe lookup).

The versions in the table are the combination the current release was verified against. If you hit
a problem on other compatible versions, please reproduce it on this combination first.

## Optional mod compatibility

None of these affect the mod's ability to run.

### Must be obtained from this organization

The **Minecraft 1.21.11 Fabric builds of the mods below are ported and maintained by this
organization**; upstream has no matching version. Download them from these repositories rather than
looking upstream:

| Mod | Purpose | Download |
|---|---|---|
| **OpenYSM-Updated** | Our own implementation of maid compatibility | [gege-tlph/OpenYSM-Updated](https://github.com/gege-tlph/OpenYSM-Updated/releases) |
| **Patchouli** | The Patchouli guide book | [gege-tlph/Patchouli](https://github.com/gege-tlph/Patchouli/releases) |
| **Maid Restaurant** (addon) | Maid kitchen | [gege-tlph/MaidRestaurant](https://github.com/gege-tlph/MaidRestaurant/releases) |

> [!NOTE]
> Upstream OpenYSM does have a 1.21.11 build, but its maid compatibility module is an empty
> implementation on Fabric and does nothing once installed. Our fork turns it into a real one, so
> **custom maid models require our fork**.

### Other optional compatibility

| Mod | What it covers |
|---|---|
| MrCrayfish's Furniture: Refurbished | Storage furniture joins wireless item transfer; tables hold food |
| JEI / REI | Altar recipe display; REI also supports recipe transfer from the backpack |
| Jade | Info display for maids, altars, picnic mats, gravestones and more |
| Sodium / Iris | Dynamic model and back-banner rendering, shader hot-swapping |
| Farmer's Delight Refabricated | Maids recognize and eat its food |
| Kaleidoscope Cookery Refabricated | Portioned food, and rice harvest-and-replant |
| Kaleidoscope Tavern Refabricated | Tables, seating, continuous grape harvesting |
| Rustic Delight | Maids recognize and eat its food |
| Carry On | Model and pose while a maid is being carried |
| PatPat | Head-patting effect |
| Inventory Profiles Next | Sorting in the maid backpack screen |

## Mods not yet compatible

Upstream ships a batch of third-party integrations this fork does not have yet. In nearly every
case **there is no Minecraft 1.21.11 Fabric build on the other side to integrate with** — it isn't
that we don't want to:

| Mod | Status |
|---|---|
| EMI | Fabric build stops at 1.21.1 |
| Accessories | Fabric build stops at 1.21.10 |
| Immersive Melodies | Fabric build stops at 1.21.1 |
| Simple Hats | Fabric build stops at 1.21.1 |
| Ponder | Fabric build stops at 1.20.1 |
| Improved Mobs | Fabric build stops at 1.21.1 |
| Just More Cakes | Fabric build stops at 1.21.1 |
| TACZ | The mod itself is Forge-only; the Fabric port stops at 1.21.1 |
| KubeJS | Upstream dropped this integration |
| Iron Chests | No Fabric build |
| Aquaculture | No Fabric build (officially NeoForge only) |
| Superb Warfare | No Fabric build |
| SlashBlade | No Fabric build |
| The One Probe | No Fabric build |
| Sophisticated Backpacks · Traveler's Backpack · ExtraContainer | All three integrate through **Accessories**' slot API. Traveler's Backpack itself has a 1.21.11 build, but Accessories stops at 1.21.10 |
| ProxLib | Not currently planned |
| Embeddium | Superseded by Sodium / Iris on Fabric, both already compatible |

**None of this is permanent.** Once a 1.21.11 Fabric build appears, we will reconnect the
integration if it's wanted; and if a mod is genuinely needed but its author hasn't moved on, we may
port and maintain a build ourselves — as we already do for Yes Steve Model and Patchouli.

## Reporting problems

This fork is under active development. Please open an [issue](../../issues/new/choose); four
templates are available:

| Template | Use it for |
|---|---|
| Bug report | Wrong behavior, crashes, rendering or save issues |
| Mod compatibility | Something breaks alongside another mod, or a request for new compatibility |
| Feature request | New or improved gameplay, UI or commands |
| Question | Installation, configuration, AI chat and voice service usage |

Whichever template you use, please attach `logs/latest.log`, your mod list and the steps to
reproduce. For anything AI-related, say whether you use a cloud service or system speech, and
**redact your API keys first**.

AI chat and voice require your own service and credentials; this mod neither provides nor proxies
any service.

## Building from source

Requires JDK 21:

```bash
./gradlew build
```

On Windows PowerShell use `.\gradlew.bat build`. Artifacts land in `build/libs/`, built against
Mojang official mappings.

## Extension API

Addon mods can register a `little_maid_extension` entrypoint in their own `fabric.mod.json`:

```json
{
  "entrypoints": {
    "little_maid_extension": [
      "com.example.yourmod.YourMaidExtension"
    ]
  }
}
```

The entrypoint class must implement `ILittleMaid`. Compatibility code should keep its optional
dependency boundary and must not require classes from an absent mod during basic loading.

## Credits and license

- Original mod: [TartaricAcid/TouhouLittleMaid](https://github.com/TartaricAcid/TouhouLittleMaid)
- Fabric port this one continues from: [Sh1roCu/TouhouLittleMaid-Orihime](https://github.com/Sh1roCu/TouhouLittleMaid-Orihime)
- This fork: [gege-tlph/TouhouLittleMaid-Tsumugi](https://github.com/gege-tlph/TouhouLittleMaid-Tsumugi)

Code is licensed under the MIT License; art and assets under CC BY-NC-SA 4.0. See
[`LICENSE-MIT`](LICENSE-MIT) and [`LICENSE-CC`](LICENSE-CC).
