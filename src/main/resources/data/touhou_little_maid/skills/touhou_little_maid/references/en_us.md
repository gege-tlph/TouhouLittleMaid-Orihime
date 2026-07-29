Touhou Little Maid is a mod about maids, Touhou-inspired combat and items, altar-based progression, support tools, and
many decorative or utility systems.

Progression and spawning:
A maid is normally obtained through the multiblock altar rather than an ordinary crafting recipe. Build the altar, check
the exact recipe in JEI, place the required items on the altar structure, and spend Power points to craft the maid cake
box. Open the cake box in stages, then use a cake to tame the maid. A tamed maid behaves like a controllable companion
with her own GUI, inventory, schedules, tasks, and interactions.

Power points and altar:
Power points are one of the core resources of the mod. They are mainly obtained by killing hostile maid fairies and can
also appear as loot. Holding a gohei shows the current power amount on screen. Power is capped and is required for altar
crafting. If altar crafting stops because power is insufficient, it can be resumed after gaining more power and
interacting again. The altar is built with the gohei and uses item placement on pillar tops; empty-hand crouch
interaction removes altar items.

Gohei and danmaku:
The gohei is both a melee weapon and a key progression tool. It is used to build or activate the altar and is also tied
to danmaku combat. Maids holding suitable gohei can use danmaku in combat modes. The gohei supports some vanilla
enchantments and also several mod-added enchantments.

World content:
The mod adds maid fairies that spawn at night in biomes and attack with danmaku. Their drops are important crafting
materials. Spawn rates can be configured and even disabled. There is also a chance for a maid to generate inside
pillager outpost cages, allowing the player to rescue and tame her.

Maid basic behavior:
By default, maids follow the player and actively pick up nearby items, arrows, experience, and power points. Crouch
right-clicking a maid with an empty hand can switch her to a sitting or standby-style state. Crouch right-click can also
be used for several direct interactions, such as applying potions or golden apples. Right-clicking a maid with a glass
bottle can convert stored experience into Bottles o' Enchanting. Maids can render skulls, be named directly with blank
name tags, attract animals when holding temptation items, be carried with saddles, and are avoided by creepers.
Owner following is responsive: only genuine combat, meaning an active temporary threat response or a live attack
target, keeps a maid from coming to her owner. Queued work, block targets, and item use no longer hold her back, and
she returns to what she was doing afterwards. An optional experimental setting replaces the classic home-radius follow
with a movement-aware natural leash; it is off by default.
In a single shallow water layer, a maid keeps her standing dimensions and wades. She switches to the compact swimming
pose only when the water has sufficient consecutive depth, navigation actually requests swimming, and she is not on
the ground, sitting, or riding.

Home mode, tasks, and schedules:
Home mode records a position and keeps the maid working or moving inside a nearby configured range instead of freely
following the player. The Kappa Compass can define work, idle, and sleep areas, and those positions can be written to
the maid. The maid GUI allows switching tasks and schedules. Schedules decide whether the maid works, rests, or sleeps
at different times.
For farming, the crop remains the interaction target while the maid chooses a reachable, occupiable neighboring stand
node that is inside Home and within interaction distance. If an edge crop has no such legal neighbor, she skips that
target for the current attempt instead of standing in the crop or crossing the Home boundary. Harvesting itself uses a
forgiving distance check from that stand node, so she reaps from beside a crop rather than re-pathing onto it.

Work modes and practical task types:
Maid task switching in the GUI can enable many practical roles. The Patchouli manual explicitly points at farming,
fishing, feeding players, breeding animals, shearing, milk collection, extinguishing, combat roles, and other work
modes. Exact behavior depends on equipment, current mode, and environment.
Existing combat tasks and temporary threat responses use one server-side target policy for search, continued tracking,
melee, ranged attacks, and sweep damage. For targets the maid picks herself, owned entities cannot become targets and
an attack-list override cannot bypass that protection; neutral mobs must be genuinely angry at the maid or owner, while
conditionally hostile mobs such as piglins, spiders, goats, and llamas are judged by their actual current target. An
explicit owner order given through a skill or chat command is obeyed even against peaceful or non-angry neutral
targets; only the immutable hard-safety set of players, tamed pets, allies, other maids, and protected or ignored types
can still refuse it.
Temporary threat response has three server rules: Off, Self Defense, and Protect Owner. Protect Owner is the current
default. It reacts to successful damage against the maid, and while her living owner is loaded in the same dimension
within the local 16-block protection area, it can also react to successful owner damage or confirmed owner attacks.
If that owner is unavailable or outside the local area, protection temporarily behaves as self-defense without changing
the selected rule. The rule can be changed in the maid configuration screen; it is saved with the maid and remains
selected after a client reconnect or dedicated-server restart.
One successful direct owner hit is enough for a genuinely hostile target. A normally peaceful, unowned target requires
two separate successful direct attacks by the owner against the same entity within 100 game ticks. Neutral mobs still
wait for real anger. A projectile fired directly by the owner counts; indirect fire, poison, falling, thorns, or an
attack made by the owner's pet does not. Old hit evidence is cleared by an explicit player control and is not replayed
when the owner comes back into range.
Once established, the response temporarily takes execution priority over work, leisure, or rest without changing the
maid's permanent work task. Breathing, escaping hazards, extinguishing fire, and necessary healing still take priority.
The target is sticky for about 40 game ticks and is abandoned after about 60 game ticks of recorded unreachability.
An explicit follow, sit, schedule, or work-task command stops the response, and the maid resumes the activity calculated
from her current schedule rather than restoring an old path. Temporary-response melee can use the tool, ordinary item,
projectile weapon, or empty hand currently held in the maid's main hand. Damage, attack speed, enchantments, and
durability follow the vanilla held-item rules, and no weapon is swapped in from inventory automatically. Permanent
melee work and ranged work such as bows or crossbows retain their own weapon and ammunition requirements.

Backpacks and storage:
Backpacks are equipped by right-clicking the maid while holding a backpack. They expand inventory and come in multiple
sizes. Some special backpacks add extra functions such as crafting table, furnace, or ender chest behavior. Shears can
remove a backpack. The last special display slot in the top row can render banners, weapons, or plants on or above the
maid.

Baubles and support items:
The mod adds several maid baubles. Ultramarine Orb Elixir can prevent death a limited number of times. Protection
baubles cancel specific damage types. Nimble Fabric makes projectile hits behave like enderman-style dodging. Item
Magnet increases pickup range. Mute Bauble silences maid chatter. Gap enables wireless item transfer between marked
chests and the maid after configuration.

Meals, favorability, and joy:
Maids can eat while injured or working to heal or gain favorability. Put food in their main hand or off hand. During
idle periods they may seek picnic mats and eat there, which greatly improves favorability if food is prepared.
Favorability increases maid stats such as health and attack. It also grows through sleeping or enjoying leisure blocks
like keyboard, computer, bookshelf, and gomoku. Death reduces favorability.
Maids may also eat table food, meaning food already served on blocks such as cakes and plated dishes. A per-maid
toggle in the maid configuration screen controls this; it defaults to on and is saved with the maid. A successful
bite always grants 1 to 3 favorability regardless of the dish's nutrition, rarity, or price, and then starts a shared
cooldown of 3600 game ticks during which no further table food is eaten. Plating food onto blocks is a separate
action and is affected by neither the toggle nor the cooldown. While seeking or eating table food a maid holds the
shared work target for at most 200 game ticks before work reclaims it, so a maid who cannot reach a dish does not
stay stuck on it.

Customization:
Maid skins are fully customizable and can be changed in game. Third-party model packs can be added through the game
directory. Custom sound resource packs can also replace or extend maid voices. Special name-based easter eggs can switch
models, including player-skin-based or resource-pack-defined special models.

Recovery and recall:
Film drops when a maid dies and can be used to resurrect her, typically through the altar or shrine. Servant Bell
recalls a specifically bound maid and can also help locate her if she is in an unloaded chunk. Trumpet recalls all
loaded maids, but cannot solve unloaded-chunk recall limitations. Red and white fox scrolls are used to find maids or
maid tombstones.

Other systems and items:
The mod also includes camera and photo systems for storing and redeploying maids, brooms for maid-assisted travel,
custom chairs and chair displayers, chisels and statues, garage kits, gomoku and other joy systems, shrine lamps, picnic
baskets, shrine resurrection, model switchers, favorability tools, extinguishers, and `/tlm` commands for things like
power points, maid counts, and model-pack reloads.

If the player asks about how to start, how to get or tame a maid, what Power points are, how the altar works, how maid
tasks or backpacks work, what support items do, or what a specific utility object from the manual is for, answer from
this knowledge body.
This Skill is only for gameplay knowledge. Nearby entities, equipment, position, and current maid state must come from
the latest game context query; supported follow, sit, schedule, or work-task changes should use the matching direct
state tool.
