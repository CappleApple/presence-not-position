# Changelog

## 1.0.3 - 2026-08-27

### Added

- Per-category client music cooldowns, defaulting to zero, with additional forced gaps after tracks and transition fade-outs.

### Changed

- Simultaneous entry stings now play only the topmost sound in dimension, biome, structure, then custom title order.

### Fixed

- Automatic dimension titles and entry sounds no longer trigger for Instanced Not Infinite's temporary dimensions; biome and structure titles, location events, and music are preserved.
- Pending title waits no longer overflow music-start deadlines or get bypassed by the next-track path.

## 1.0.2 - 2026-08-26

### Added

- Client title-layout settings for the title stack's top-center-relative X/Y offsets and spacing between simultaneous titles.

## 1.0.1 - 2026-08-25

### Changed

- Simultaneous dimension, biome, and structure titles now use tighter spacing and remain within the top third of the GUI without automatic category-label subtitles.

## 1.0.0 - 2026-08-24

### Added

- Server-authoritative structure, biome, and dimension transition detection with overlap support and stability handling.
- Resource-pack title definitions with text, localization, static textures, frame animation, entry sounds, simultaneous dimension-biome-structure stacking, cooldowns, and persistent history.
- Adaptive structure, biome, dimension, day, and night music with cached folder discovery, selection modes, delays, crossfades, resume, explicit silence, and metadata-based loudness normalization.
- Independent client title/music settings and runtime-managed vanilla music replacement, ducking, or allowance.
- Optional server-side KubeJS events and custom presentation API.
- Server and client debug commands plus automated unit and GameTest coverage.

### Fixed

- `/pnp title` now formats its confirmation with valid chat-component arguments instead of throwing after sending the presentation.
