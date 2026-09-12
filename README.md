# Presence Not Position

Presence Not Position gives places an identity.

The server tracks when a player enters or leaves a structure, biome, dimension, or the area around their respawn bed. Clients can then turn those transitions into location titles, entry sounds, and adaptive music supplied by resource packs.

Built for Minecraft 1.21.1 / NeoForge. KubeJS support is optional.

## What you can do with it

- Show a title when a player enters an Ancient City, dungeon, biome, dimension, or their home area.
- Give locations custom title art or animated sprites.
- Play an entry sting for a location.
- Give structures, biomes, dimensions, and home their own music playlists.
- Use separate day/night playlists and crossfade between contexts.
- Let resource packs override presentation without changing server detection.
- React to location entry/exit from KubeJS.
- Trigger custom presentations from scripts or commands.

If no resource-pack definition exists, registry-backed locations still get a readable text title. For example, `minecraft:ancient_city` becomes **Ancient City**.

## Resource-pack definitions

Presentation files live below:

```text
assets/<namespace>/presence_not_position/
```

For example:

```text
assets/within/presence_not_position/
├── structures/cataclysm/burning_arena/presentation.json
├── biomes/minecraft/deep_dark/presentation.json
├── dimensions/minecraft/the_nether/presentation.json
├── home/presentation.json
└── custom/deep_dark_warning/presentation.json
```

A definition can provide any combination of a title, visual, entry sound, and music settings.

A compact example:

```json
{
  "title": {
    "text": "The Burning Arena",
    "subtitle": "No turning back",
    "duration": 100,
    "fadeIn": 10,
    "fadeOut": 20
  },
  "entrySound": {
    "id": "within:location.burning_arena_intro",
    "volume": 0.8
  },
  "music": {
    "folder": "within:music/structures/burning_arena",
    "selection": "shuffle",
    "fadeIn": 4.0,
    "fadeOut": 5.0
  }
}
```

Title timing is measured in ticks. Music fades/delays are in seconds.

### Title art

Titles can use normal text, a static texture, an animated spritesheet, or an explicit list of frames. Images keep their aspect ratio and scale to the current GUI.

Several location changes detected at the same time are shown as one stack rather than a queue. The normal order is:

```text
Dimension -> Biome -> Structure -> Home
```

Resource packs can still control title priority within a category.

## Home

`HOME` means the area around the player's current respawn bed, not world spawn.

The server config controls whether home detection is enabled and its radius. The bed must still exist in the same dimension. Respawn anchors and arbitrary forced-spawn coordinates are not treated as home.

A home presentation uses:

```text
assets/<namespace>/presence_not_position/home/presentation.json
```

so a pack can give the player's base its own welcome title, sting, and music without knowing the bed coordinates ahead of time.

## Adaptive music

Presence Not Position keeps all active location contexts and chooses the most specific usable playlist in this order:

```text
HOME > STRUCTURE > BIOME > DIMENSION
```

If a higher-priority context has no playable music, the lower one can continue. A definition can also explicitly silence lower-priority music.

Music definitions support:

- one or more folders;
- separate day and night folders;
- sequential, random, or shuffle selection;
- delay between tracks;
- fade/crossfade timing;
- resuming a still-active lower-priority track;
- per-location volume; and
- optional loudness normalization from Vorbis/ReplayGain metadata.

Tracks are ordinary `.ogg` resources. A folder such as:

```text
within:music/biomes/forest
```

maps to files below:

```text
assets/within/sounds/music/biomes/forest/
```

They do not need duplicate `sounds.json` entries.

### Vanilla and other music

Client config can `REPLACE`, `DUCK`, or `ALLOW` normal vanilla background music while a PNP playlist is active.

Jukebox records and other audible music take priority over PNP by default. Extra sound-event IDs can be listed for boss mods that play themes outside the normal music path.

## Client settings

NeoForge creates:

```text
config/presence-not-position-client.toml
```

Structures, biomes, dimensions, and home each have their own title policy. Available modes include:

- `ALWAYS`
- `COOLDOWN`
- `ONCE`

Title history is stored locally in `config/presence-not-position-history.json`.

Music, title layout, category toggles, per-category music cooldowns, vanilla-music behavior, and extra priority sounds are also configurable client-side. Server detection intervals and home radius live in the server config.

## KubeJS

KubeJS is optional. When present, scripts can listen for location entry/exit without turning it into a hard dependency.

```js
PresenceNotPosition.structureEntered(event => {
  if (event.id == 'minecraft:ancient_city') {
    event.setSubtitle('The sculk is listening.')
    event.setPriority(500)
  }
})
```

Available entry/exit hooks cover structures, biomes, dimensions, home, and the generic combined event.

Scripts can also show a resource-defined custom presentation:

```js
PresenceNotPosition.show(player, 'within:deep_dark_warning')
```

or provide temporary title/sound overrides in the request.

`cancelPresentation()` suppresses that automatic title request; it does not cancel the underlying location detection or active music context.

## Commands

Server/debug commands:

```text
/pnp debug
/pnp title <structure|biome|dimension|home|custom> <namespace:id>
```

Client commands:

```text
/pnp music current
/pnp music next
/pnp music stop
/pnp music reload
/pnp history
/pnp clearhistory
```

`/pnp music reload` reloads resource packs and rebuilds presentation/music data without restarting the game.

## Instanced Not Infinite

Instanced Not Infinite is optional. When it is installed, its temporary dungeon dimensions avoid showing redundant automatic dimension/biome/home titles while still allowing the dungeon's structure presentation and normal music context to work. Explicit scripted/custom presentations are unaffected.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.244 or a compatible 21.1 build
- Java 21
- KubeJS only if you want scripting support

Install the mod on the server and participating clients.

## Building

```powershell
.\gradlew.bat test build
.\gradlew.bat runGameTestServer
.\gradlew.bat runServer
```

Update server and clients together when the network protocol changes.
