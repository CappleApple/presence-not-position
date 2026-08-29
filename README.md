# Presence Not Position

Presence Not Position is a NeoForge 1.21.1 location-identity framework. The server authoritatively detects structure, biome, dimension, and respawn-bed home transitions and fires scripting events; each client independently resolves resource-pack presentations, title policy, and adaptive music.

With no presentation resources installed, every registry-backed location still gets a readable text title. `minecraft:ancient_city` becomes “Ancient City”, and localized registry names take precedence when they exist.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.244 or newer in the 1.21.1 line
- Java 21
- KubeJS is optional and is only needed for scripts

Install the mod on the server and participating clients. Client title and music settings never gate server detection or KubeJS events.

When **Instanced Not Infinite** is installed, its temporary `instancednotinfinite:instances/<id>` dimensions show only automatic structure titles and entry sounds. Automatic dimension, biome, and home presentations are skipped. All location contexts remain synchronized, so structure, biome, dimension, and home music resolve normally. Explicit `/pnp title` requests and scripted custom presentations still work. Instanced Not Infinite is optional; without it, all locations retain their normal presentation behavior.

## Resource-pack layout

Definitions live below `assets/<pack_namespace>/presence_not_position/`:

```text
assets/within/presence_not_position/
├── structures/cataclysm/burning_arena/presentation.json
├── biomes/minecraft/deep_dark/presentation.json
├── dimensions/minecraft/the_nether/presentation.json
├── home/presentation.json
└── custom/deep_dark_warning/presentation.json
```

The target registry namespace remains a path segment for structures, biomes, and dimensions. Thus the first file targets `cataclysm:burning_arena`; the custom file targets `within:deep_dark_warning` because custom IDs use the asset namespace.

`home/presentation.json` targets the shared `HOME presencenotposition:home` context; each player enters it only around their own current respawn bed.

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

Location entries detected together are presented as one simultaneous vertical stack, not as a sequential title queue. The fixed top-to-bottom order is dimension, biome, structure, then home. The highest available category occupies the top row at the largest size, so a biome is largest when no dimension entered in that batch and a structure or home is largest when it is the only entry. Multiple structures in the same batch remain grouped below the biome in deterministic priority and ID order.

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

Entry stings are ordinary Minecraft sound events declared by a pack’s `sounds.json`; they are played separately from music. A simultaneous title batch plays only the topmost entry with an available sound: dimension before biome before structure before home (then custom entries). Rows without an available sound are skipped, script sound overrides are respected, and every title still appears. Within one category, the first available sound definition in the title stack's priority/ID order wins.

### Multiple music and sound paths

Music `folder`, `dayFolder`, and `nightFolder`, plus `entrySound.id`, accept either one string or an array. The plural names `folders`, `dayFolders`, `nightFolders`, and `ids` are also supported. For example, any structure, biome, dimension, or home presentation can use:

```json
{
  "entrySound": {
    "ids": ["within:location.home_intro", "another_pack:location.welcome"],
    "volume": 0.7,
    "pitch": 1.0
  },
  "music": {
    "folders": ["within:music/home", "shared:music/peaceful"],
    "dayFolders": ["within:music/home/day", "shared:music/peaceful/day"],
    "nightFolders": ["within:music/home/night", "shared:music/peaceful/night"],
    "selection": "shuffle",
    "startAfterTitle": true
  }
}
```

Existing definitions such as `"folder": "within:music/home"` still work. You can also change that value directly to `"folder": ["within:music/home", "shared:music/peaceful"]`. If singular and plural names are both present, their values are combined, singular first, with duplicates removed.

Music paths may use different resource namespaces. Tracks from all configured paths form one playlist; duplicate IDs from overlapping paths appear only once. For sequential playback, paths are visited in the listed order and tracks within each path are sorted by resource ID. Shuffle and random selection use the combined collection. Every configured day/night prefix is excluded from the generic playlist, and missing paths do not prevent other sources from playing or normal period/location fallback. Normal resource-pack precedence still determines which file supplies a repeated resource ID.

Entrance sound IDs form a pool of alternatives: one available ID is chosen randomly for the selected title row, using its configured volume and pitch. They do not play simultaneously. Missing sound events or events without playable files are skipped; if a row has no available variants, selection continues to the next row. A script's sound override replaces the entire pool with its requested ID. Custom presentations also support entry-sound pools.

These are Minecraft resource paths, not operating-system paths. Music prefixes omit `assets/<namespace>/sounds/` and `.ogg`; entry sounds remain sound-event IDs declared in `sounds.json`. Reload resource packs with `/pnp music reload` after editing definitions or adding files.

## Respawn-bed home

The server's `[home]` section in `presence-not-position-server.toml` controls detection:

```toml
[home]
enabled = true
radius = 64.0
```

The radius is a sphere measured from the center of the player's current respawn-bed block to the player's position, including vertical distance. The boundary is included. The bed must still exist in the same dimension; other nearby beds, world spawn, forced spawn coordinates without a bed, and respawn anchors do not count. Modded beds that implement NeoForge's bed detection are supported. Sampling does not load bed chunks. Changing or breaking the respawn bed, leaving the radius, or changing dimension updates the home context on the next detection poll.

Home defaults to a readable “Home” title. To supply its entrance title, sound, and background songs, add `assets/within/presence_not_position/home/presentation.json` to your resource pack:

```json
{
  "title": {
    "text": "Welcome Home",
    "subtitle": "Rest a while",
    "duration": 100
  },
  "entrySound": {
    "id": "minecraft:block.amethyst_block.chime",
    "volume": 0.7,
    "pitch": 1.0
  },
  "music": {
    "folder": "within:music/home",
    "dayFolder": "within:music/home/day",
    "nightFolder": "within:music/home/night",
    "startAfterTitle": true,
    "selection": "shuffle",
    "fadeIn": 3.0,
    "fadeOut": 3.0
  }
}
```

Place songs under `assets/within/sounds/music/home/` (and its `day/` and `night/` subfolders). Home supports the same text, localization, title art, entry-sound, timing, normalization, and playlist fields as other locations. No songs are bundled. Its optional default-name translation key is `home.presencenotposition.home`. Its client `[home]` policy controls entrance titles independently from `[music].homeMusic`; title history uses the single `presencenotposition:home` ID even after changing beds.

When home has usable music it takes precedence over structures, biomes, and dimensions. With no home playlist, normal location music continues; `silenceLowerPriority` can explicitly request silence instead.

## Adaptive music

All active contexts are retained. The winner is the first usable definition in this order:

```text
HOME > STRUCTURE > BIOME > DIMENSION
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

Normal starts use `fadeIn`/`fadeOut`; winner changes use `transitionFadeIn`/`transitionFadeOut`. Both streams overlap during a crossfade unless the outgoing category has a nonzero client music cooldown. `transitionDelay` requires a winner to remain valid before committing; defaults are 0.5 seconds for structures and home, 2 seconds for biomes, and immediate for dimensions. With `resume: true`, a still-active lower context is kept silent at its current streaming position and resumed when possible.

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

### Jukebox and boss music

PNP music is muted while an audible RECORDS sound (including jukebox discs) or non-vanilla MUSIC sound is playing. This includes boss themes played through Minecraft's music manager or directly through its sound engine; the vanilla dragon theme also takes priority. Other mods' MUSIC sounds are conservatively treated as priority music even when they are not boss themes. Vanilla background replacement/ducking only changes vanilla background music, and other mods' music-selection hooks keep running.

Detection checks live sound channels, their volume sliders, and positional attenuation. A stopped, silent, or out-of-range jukebox does not keep PNP muted. All PNP streams, including outgoing crossfades, are muted together. Existing tracks continue advancing silently; new tracks and music transitions wait. When the external music ends, PNP becomes audible again and continues normal playlist timing. Entrance stings and titles are unaffected.

For a boss mod that plays its theme in another category, add the exact sound-event ID to `[music].additionalPrioritySounds`, for example `additionalPrioritySounds = ["example:boss_theme"]`. Ordinary hostile sound effects do not mute PNP. Music played outside Minecraft's sound engine cannot be detected by this mechanism.

## Client configuration

NeoForge creates `config/presence-not-position-client.toml`:

```toml
[structures]
enabled = true
showMode = "COOLDOWN"
cooldownSeconds = 300
musicCooldownSeconds = 0

[biomes]
enabled = true
showMode = "COOLDOWN"
cooldownSeconds = 600
musicCooldownSeconds = 0

[dimensions]
enabled = true
showMode = "ONCE"
cooldownSeconds = 0
musicCooldownSeconds = 0

[home]
enabled = true
showMode = "COOLDOWN"
cooldownSeconds = 300
musicCooldownSeconds = 0

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
homeMusic = true
additionalPrioritySounds = []
vanillaMusicBehavior = "REPLACE"
vanillaMusicDuckVolume = 0.15
```

Title layout coordinates are signed offsets from the GUI's top center in scaled pixels. `x = 0` horizontally centers every title, while `y = 0` places the top of the first title row at the top edge; positive values move right/down and negative values move left/up. `spacing` controls the empty pixels between rows when titles stack; extremely dense stacks reduce it only as needed to retain their compact height.

`ALWAYS` shows every legitimate re-entry, `COOLDOWN` is tracked per registry ID, and `ONCE` shows each ID once. History is cosmetic client state in `config/presence-not-position-history.json` and persists across restarts. Title and music category toggles are independent.

Each category's `musicCooldownSeconds` adds a forced gap after its music finishes, separate from the title `cooldownSeconds`. Natural track completion waits for the resource pack's `trackDelay` **plus** this extra gap. On a transition, a nonzero outgoing cooldown makes the music fade out fully and then wait before any location music starts or resumes. Location/day-night changes, resource reloads, and `/pnp music next` cannot bypass an active forced gap. All four defaults are `0`, keeping normal crossfades and resource-pack timing; there is no extra wait before the first track. Disconnecting clears the session's cooldown. This does not change background-music priority (home > structure > biome > dimension) or entry-sting timing.

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
PresenceNotPosition.homeEntered(event => {})
PresenceNotPosition.homeExited(event => {})
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

For custom requests only, `respectClientPolicy: false` bypasses the client’s custom-presentation toggle. Built-in structure, biome, dimension, and home titles always respect their category policy.

## Debug commands

Server-aware commands:

```text
/pnp debug
/pnp title <structure|biome|dimension|home|custom> <namespace:id>
```

`/pnp title home presencenotposition:home` previews the home presentation (subject to client policy). `debug` and `title` require permission level 2. Client music/history commands are:

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

The test suite covers independent overlapping structures, piece-gap grace, biome stability, dimension rebuilds, home entry/exit and bed changes, optional Instanced Not Infinite title suppression, all presentation policies, animation semantics, music specificity/fallback/silence, interruption audibility and reversible muting, day/night sets, track selectors, combined folder discovery, entry-sound pools, delay parsing, resource-path resolution, and cached normalization metadata. GameTests also verify real respawn-bed sampling, radius boundaries, bed removal, dimension checks, avoiding chunk loads, and server-safe audio-definition parsing. GameTests use the `presencenotposition` namespace so optional mods do not add their own tests to the run; the empty test template is a development fixture and is excluded from the release JAR.

The home context uses network protocol version 2; update the mod on both server and clients together.
