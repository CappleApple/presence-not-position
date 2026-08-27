package com.cappleapple.presencenotposition.resource;

import com.cappleapple.presencenotposition.PresenceNotPosition;
import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.location.LocationType;
import com.cappleapple.presencenotposition.music.MusicNormalizationService;
import com.cappleapple.presencenotposition.music.MusicTrackSet;
import com.cappleapple.presencenotposition.music.NormalizationMetadata;
import com.cappleapple.presencenotposition.music.ResolvedMusic;
import com.cappleapple.presencenotposition.presentation.PresentationDefinition;
import com.cappleapple.presencenotposition.presentation.VisualDefinition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public final class PresentationResourceLoader extends SimplePreparableReloadListener<PresentationResourceLoader.Prepared> {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String DIRECTORY = "presence_not_position";

    @Override
    protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, JsonElement> json = new HashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(manager, DIRECTORY, GSON, json);
        Map<LocationContext, PresentationDefinition> definitions = parseDefinitions(json);
        Map<ResourceLocation, Resource> soundFiles = Sound.SOUND_LISTER.listMatchingResources(manager);
        Set<ResourceLocation> allTracks = soundFiles.keySet().stream()
            .map(Sound.SOUND_LISTER::fileToId)
            .collect(Collectors.toUnmodifiableSet());
        Map<LocationContext, ResolvedMusic> music = resolveMusic(definitions, allTracks);
        Map<ResourceLocation, TextureSize> textureSizes = readTextureSizes(manager, definitions);
        Map<ResourceLocation, NormalizationMetadata> normalization = readNormalization(soundFiles, music);
        return new Prepared(definitions, music, textureSizes, normalization);
    }

    @Override
    protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        ClientResourceIndex.replace(prepared.definitions, prepared.music, prepared.textureSizes, prepared.normalization);
        PresenceNotPosition.LOGGER.info(
            "Loaded {} location definitions and {} resolved music contexts with {} discovered tracks",
            prepared.definitions.size(), prepared.music.size(),
            prepared.music.values().stream().map(ResolvedMusic::tracks).flatMap(set ->
                java.util.stream.Stream.of(set.generic(), set.day(), set.night())).mapToInt(List::size).sum()
        );
    }

    private static Map<LocationContext, PresentationDefinition> parseDefinitions(Map<ResourceLocation, JsonElement> json) {
        Map<LocationContext, PresentationDefinition> result = new LinkedHashMap<>();
        json.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try {
                LocationContext context = target(entry.getKey());
                PresentationDefinition previous = result.put(context, DefinitionParser.parse(context.type(), entry.getValue().getAsJsonObject()));
                if (previous != null) {
                    PresenceNotPosition.LOGGER.warn("Multiple resources target {}; using {}", context, entry.getKey());
                }
            } catch (RuntimeException exception) {
                PresenceNotPosition.LOGGER.error("Invalid Presence Not Position definition {}", entry.getKey(), exception);
            }
        });
        return Map.copyOf(result);
    }

    static LocationContext target(ResourceLocation resourceId) {
        String[] parts = resourceId.getPath().split("/");
        if (resourceId.getPath().equals("home/presentation")) return LocationContext.HOME;
        if (parts.length < 3 || !"presentation".equals(parts[parts.length - 1])) {
            throw new IllegalArgumentException("Expected <type>/.../presentation.json");
        }
        LocationType type = java.util.Arrays.stream(LocationType.values())
            .filter(value -> value.resourceDirectory().equals(parts[0]))
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown location type directory " + parts[0]));
        String namespace;
        String path;
        if (type == LocationType.CUSTOM) {
            namespace = resourceId.getNamespace();
            path = String.join("/", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1));
        } else {
            if (parts.length < 4) throw new IllegalArgumentException("Built-in locations require registry namespace and path");
            namespace = parts[1];
            path = String.join("/", java.util.Arrays.copyOfRange(parts, 2, parts.length - 1));
        }
        return new LocationContext(type, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static Map<LocationContext, ResolvedMusic> resolveMusic(
        Map<LocationContext, PresentationDefinition> definitions,
        Set<ResourceLocation> allTracks
    ) {
        Map<LocationContext, ResolvedMusic> result = new HashMap<>();
        definitions.forEach((context, definition) -> {
            if (definition.music() == null) return;
            List<ResourceLocation> day = matching(definition.music().dayFolder(), allTracks, List.of());
            List<ResourceLocation> night = matching(definition.music().nightFolder(), allTracks, List.of());
            List<ResourceLocation> generic = matching(
                definition.music().folder(), allTracks,
                java.util.stream.Stream.of(definition.music().dayFolder(), definition.music().nightFolder())
                    .filter(java.util.Objects::nonNull).toList()
            );
            MusicTrackSet tracks = new MusicTrackSet(generic, day, night);
            if (tracks.isEmpty() && definition.music().hasFolder() && !definition.music().silenceLowerPriority()) {
                PresenceNotPosition.LOGGER.warn("No OGG tracks found for {}; lower-priority music will remain eligible", context);
            }
            result.put(context, new ResolvedMusic(definition.music(), tracks));
        });
        return Map.copyOf(result);
    }

    private static List<ResourceLocation> matching(
        ResourceLocation folder,
        Set<ResourceLocation> allTracks,
        List<ResourceLocation> excludedPrefixes
    ) {
        if (folder == null) return List.of();
        return allTracks.stream()
            .filter(track -> isUnder(track, folder))
            .filter(track -> excludedPrefixes.stream().noneMatch(prefix -> isUnder(track, prefix)))
            .sorted()
            .toList();
    }

    private static boolean isUnder(ResourceLocation track, ResourceLocation folder) {
        return track.getNamespace().equals(folder.getNamespace())
            && (track.getPath().equals(folder.getPath()) || track.getPath().startsWith(folder.getPath() + "/"));
    }

    private static Map<ResourceLocation, TextureSize> readTextureSizes(
        ResourceManager manager,
        Map<LocationContext, PresentationDefinition> definitions
    ) {
        Set<ResourceLocation> textures = definitions.values().stream()
            .map(PresentationDefinition::title)
            .map(title -> title.visual())
            .flatMap(visual -> visual.type() == VisualDefinition.Type.FRAMES
                ? visual.frames().stream()
                : java.util.stream.Stream.ofNullable(visual.texture()))
            .collect(Collectors.toSet());
        Map<ResourceLocation, TextureSize> result = new HashMap<>();
        for (ResourceLocation texture : textures) {
            manager.getResource(texture).ifPresentOrElse(resource -> {
                try (InputStream input = resource.open(); NativeImage image = NativeImage.read(input)) {
                    result.put(texture, new TextureSize(image.getWidth(), image.getHeight()));
                } catch (IOException | RuntimeException exception) {
                    PresenceNotPosition.LOGGER.warn("Unable to read title texture {}", texture, exception);
                }
            }, () -> PresenceNotPosition.LOGGER.warn("Missing title texture {}; text fallback will be used", texture));
        }
        return Map.copyOf(result);
    }

    private static Map<ResourceLocation, NormalizationMetadata> readNormalization(
        Map<ResourceLocation, Resource> soundFiles,
        Map<LocationContext, ResolvedMusic> music
    ) {
        Set<ResourceLocation> referenced = music.values().stream().map(ResolvedMusic::tracks)
            .flatMap(set -> java.util.stream.Stream.of(set.generic(), set.day(), set.night()))
            .flatMap(List::stream).collect(Collectors.toSet());
        Map<ResourceLocation, Resource> byTrack = new HashMap<>();
        soundFiles.forEach((file, resource) -> byTrack.put(Sound.SOUND_LISTER.fileToId(file), resource));
        Map<ResourceLocation, NormalizationMetadata> result = new HashMap<>();
        for (ResourceLocation track : referenced) {
            Resource resource = byTrack.get(track);
            if (resource == null) continue;
            try (InputStream input = resource.open()) {
                result.put(track, MusicNormalizationService.read(input));
            } catch (IOException exception) {
                PresenceNotPosition.LOGGER.debug("Could not inspect normalization metadata for {}", track, exception);
            }
        }
        return Map.copyOf(result);
    }

    protected record Prepared(
        Map<LocationContext, PresentationDefinition> definitions,
        Map<LocationContext, ResolvedMusic> music,
        Map<ResourceLocation, TextureSize> textureSizes,
        Map<ResourceLocation, NormalizationMetadata> normalization
    ) { }
}
