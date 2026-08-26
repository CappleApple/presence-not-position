# Presence Not Position

Presence Not Position is a NeoForge 1.21.1 location-identity framework. The server authoritatively detects structure, biome, and dimension transitions and fires scripting events; each client independently resolves resource-pack presentations, title policy, and adaptive music.

With no presentation resources installed, every registry-backed location still gets a readable text title. `minecraft:ancient_city` becomes “Ancient City”, and localized registry names take precedence when they exist.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.244 or newer in the 1.21.1 line
- Java 21
- KubeJS is optional and is only needed for scripts

Install the mod on the server and participating clients. Client title and music settings never gate server detection or KubeJS events.

## Resource-pack layout

Definitions live below `assets/<pack_namespace>/presence_not_position/`:

```text
assets/within/presence_not_position/
├── structures/cataclysm/burning_arena/presentation.json
├── biomes/minecraft/deep_dark/presentation.json
├── dimensions/minecraft/the_nether/presentation.json
└── custom/deep_dark_warning/presentation.json
```

The target registry namespace remains a path segment for structures, biomes, and dimensions. Thus the first file targets `cataclysm:burning_arena`; the custom file targets `within:deep_dark_warning` because custom IDs use the asset namespace.

Normal resource-pack precedence applies when a pack replaces the same asset namespace and path. To override an existing definition, mirror its complete path.

## Presentation JSON

All sections are optional. A complete definition can look like this:

```json
{
  "title": {
    "text": "The Burning Arena",
    "subtitleTranslationKey": "location.within.burning_arena.subtitle",
    "visual": {
      "type": "texture",
      "texture": "within:presence_not_position/structures/cataclysm/burning_arena/title.png",
      "animation": {
        "frameCount": 12,
        "frameTime": 2,
        "loop": true,
        "layout": "vertical"
      }
    },
    "duration": 100,
    "fadeIn": 10,
    "fadeOut": 20,
    "priority": 200
  },
  "entrySound": {
    "id": "within:location.burning_arena_intro",
    "volume": 0.8,
    "pitch": 1.0
  },
  "music": {
    "folder": "within:music/structures/burning_arena",
    "dayFolder": "within:music/structures/burning_arena/day",
    "nightFolder": "within:music/structures/burning_arena/night",
    "volume": 0.8,
    "normalizeVolume": true,
    "normalizationTarget": -16.0,
    "selection": "shuffle",
    "avoidImmediateRepeat": true,
    "startAfterTitle": true,
    "startDelay": 2.0,
    "trackDelay": { "min": 20, "max": 60 },
    "fadeIn": 4.0,
    "fadeOut": 5.0,
    "transitionFadeIn": 3.0,
    "transitionFadeOut": 3.0,
    "transitionDelay": 1.0,
    "resume": true,
    "silenceLowerPriority": false
  }
}
```

Title durations and fades are ticks. Music delays and fades are seconds. Malformed files are logged and ignored; a missing visual falls back to text, and missing higher-priority music falls through to the next usable context.

Location entries detected together are presented as one simultaneous vertical stack, not as a sequential title queue. The fixed top-to-bottom order is dimension, biome, then structure. The highest available category occupies the top row at the largest size, so a biome is largest when no dimension entered in that batch and a structure is largest when it is the only entry. Multiple structures in the same batch remain grouped below the biome in deterministic priority and ID order.

The complete stack is compacted into the top third of the GUI. Category words such as “Dimension” and “Biome” are not added automatically; a resource pack can still provide an intentional subtitle when desired.

### Text and localization

Text title resolution is:

1. `structure.<namespace>.<path>`, `biome.<namespace>.<path>`, or `dimension.<namespace>.<path>` when that client has a translation
2. `title.translationKey`, then `title.text`
3. A Unicode-safe title-cased registry path

Optional subtitle fields are `subtitleTranslationKey` and `subtitle`.

### Static and animated title art

A static image uses:

```json
"visual": {
  "type": "texture",
  "texture": "within:titles/ancient_city.png"
}
```

Images retain their aspect ratio, use their real PNG dimensions, scale down for the current GUI, and remain centered.

Spritesheets use `type: "texture"`, `frameCount`, and `frameTime`. `layout` may be `vertical`, `horizontal`, or `auto`; auto prefers horizontal only when the width divides cleanly into frames and the height does not. Non-looping animations hold their last frame, independently of title duration.

Individual files use:

```json
"visual": {
  "type": "frames",
  "frames": [
    "within:titles/example/frame_00.png",
    "within:titles/example/frame_01.png",
    "within:titles/example/frame_02.png"
  ],
  "animation": { "frameTime": 2, "loop": false }
}
```

Entry stings are ordinary Minecraft sound events declared by a pack’s `sounds.json`; they are played separately from music.

## Adaptive music

All active contexts are retained. The winner is the first usable definition in this order:

```text
STRUCTURE > BIOME > DIMENSION
```

A higher context with no valid tracks does not interrupt lower music. A definition with `silenceLowerPriority: true` is deliberately usable without a folder and fades lower music to silence.

Music folders are OGG resource prefixes, not sound-event lists:

```text
assets/within/sounds/music/biomes/forest/
├── forest_01.ogg
├── forest_02.ogg
├── day/day_01.ogg
└── night/night_01.ogg
```

`within:music/biomes/forest` discovers the first two tracks, while the explicit day and night folders discover their subfolders. These streamed tracks do not need duplicate entries in `sounds.json`. Folder scans, texture metadata, and normalization tags are cached on resource reload.

`dayFolder` or `nightFolder` wins for its period when it contains tracks; `folder` is the fallback. Day/night changes require two seconds of stable observed state and use the same crossfade pipeline as structure, biome, and dimension changes. Fixed-time and modded dimensions use the level’s effective `isDay()` state.

Selection values are `sequential`, `random`, and `shuffle` (default). Shuffle exhausts a randomized bag before refilling it and avoids a cycle-boundary repeat when more than one track exists. `trackDelay` accepts either a number or `{ "min": n, "max": n }`; zero provides continuous playback.

Normal starts use `fadeIn`/`fadeOut`; winner changes use `transitionFadeIn`/`transitionFadeOut`. Both streams overlap during a crossfade. `transitionDelay` requires a winner to remain valid before committing; defaults are 0.5 seconds for structures, 2 seconds for biomes, and immediate for dimensions. With `resume: true`, a still-active lower context is kept silent at its current streaming position and resumed when possible.

### Volume normalization

Normalization is performed from cached OGG Vorbis metadata without decoding tracks during playback. The resolver understands:

- `PNP_LOUDNESS_LUFS=<value>` for an explicit measured loudness
- `R128_TRACK_GAIN=<Q7.8 value>`
- `REPLAYGAIN_TRACK_GAIN=<dB> dB`

When measured loudness is available, gain targets `normalizationTarget` (default `-16.0` LUFS). Gain is safety-clamped. Missing or malformed metadata uses unity gain and never prevents playback.

### Vanilla music

`vanillaMusicBehavior` supports:

- `REPLACE`: prevent new vanilla music starts and smoothly reduce any current vanilla music to silence while a valid PNP context—including explicit silence—is active
- `DUCK`: smoothly reduce vanilla music to `vanillaMusicDuckVolume`
- `ALLOW`: leave vanilla music untouched

The implementation adjusts live sound channels and never rewrites the user’s Minecraft volume options. PNP streams remain in the normal MUSIC category, so Minecraft’s master and music sliders still apply.

## Client configuration

NeoForge creates `config/presence-not-position-client.toml`:

```toml
[structures]
enabled = true
showMode = "COOLDOWN"
cooldownSeconds = 300

[biomes]
enabled = true
showMode = "COOLDOWN"
cooldownSeconds = 600

[dimensions]
enabled = true
showMode = "ONCE"
cooldownSeconds = 0

[titleLayout]
x = 0
y = 0
spacing = 2

[custom]
enabled = true

[music]
enabled = true
masterVolume = 1.0
structureMusic = true
biomeMusic = true
dimensionMusic = true
vanillaMusicBehavior = "REPLACE"
vanillaMusicDuckVolume = 0.15
```

Title layout coordinates are signed offsets from the GUI's top center in scaled pixels. `x = 0` horizontally centers every title, while `y = 0` places the top of the first title row at the top edge; positive values move right/down and negative values move left/up. `spacing` controls the empty pixels between rows when titles stack; extremely dense stacks reduce it only as needed to retain their compact height.

`ALWAYS` shows every legitimate re-entry, `COOLDOWN` is tracked per registry ID, and `ONCE` shows each ID once. History is cosmetic client state in `config/presence-not-position-history.json` and persists across restarts. Title and music category toggles are independent.

Server detection defaults are a five-tick staggered sampling interval, a 15-tick per-structure exit grace, and 20 ticks of biome stability. They are configurable in the generated server config.

## KubeJS

KubeJS support is optional, server-only, and discovered without making KubeJS a required mod dependency.

```js
PresenceNotPosition.structureEntered(event => {
  if (event.id == 'minecraft:ancient_city') {
    event.setSubtitle('The sculk is listening.')
    event.setPriority(500)
    event.setDuration(120)
    event.setSound('within:location.ancient_city_intro')
  }
})

PresenceNotPosition.structureExited(event => {})
PresenceNotPosition.biomeEntered(event => {})
PresenceNotPosition.biomeExited(event => {})
PresenceNotPosition.dimensionEntered(event => {})
PresenceNotPosition.dimensionExited(event => {})
PresenceNotPosition.entered(event => {})
PresenceNotPosition.exited(event => {})
```

Event properties are `player`, `type`, `id`, `displayName`, and `entered`. `cancelPresentation()` suppresses only the generated client title request; it does not cancel detection, context synchronization, music fallback, or other server listeners.

Show a resource-defined custom presentation:

```js
PresenceNotPosition.show(player, 'within:deep_dark_warning')
```

Or send overrides:

```js
PresenceNotPosition.show(player, {
  id: 'within:something_stirs',
  title: 'Something Stirs',
  subtitle: 'You are not alone.',
  sound: 'within:location.something_stirs',
  priority: 1000,
  duration: 100,
  respectClientPolicy: true
})
```

For custom requests only, `respectClientPolicy: false` bypasses the client’s custom-presentation toggle. Built-in structure, biome, and dimension titles always respect their category policy.

## Debug commands

Server-aware commands:

```text
/pnp current
/pnp debug
/pnp title <structure|biome|dimension|custom> <namespace:id>
```

`debug` and `title` require permission level 2. Client music/history commands are:

```text
/pnp music current
/pnp music next
/pnp music stop
/pnp music reload
/pnp history
/pnp clearhistory
```

`music reload` reloads resource packs, rebuilding definitions, track lists, texture dimensions, and normalization metadata without restarting the game.

## Building and testing

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21'
.\gradlew.bat test build
.\gradlew.bat runGameTestServer
.\gradlew.bat runServer
```

The test suite covers independent overlapping structures, piece-gap grace, biome stability, dimension rebuilds, all presentation policies, animation semantics, music specificity/fallback/silence, day/night sets, track selectors, delay parsing, registry-path resolution, and cached normalization metadata.
