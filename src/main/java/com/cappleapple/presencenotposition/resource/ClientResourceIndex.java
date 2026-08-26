package com.cappleapple.presencenotposition.resource;

import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.music.NormalizationMetadata;
import com.cappleapple.presencenotposition.music.ResolvedMusic;
import com.cappleapple.presencenotposition.presentation.PresentationDefinition;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.ResourceLocation;

public final class ClientResourceIndex {
    public record Snapshot(
        Map<LocationContext, PresentationDefinition> definitions,
        Map<LocationContext, ResolvedMusic> music,
        Map<ResourceLocation, TextureSize> textureSizes,
        Map<ResourceLocation, NormalizationMetadata> normalization,
        long revision
    ) {
        public Snapshot {
            definitions = Map.copyOf(definitions);
            music = Map.copyOf(music);
            textureSizes = Map.copyOf(textureSizes);
            normalization = Map.copyOf(normalization);
        }
    }

    private static final AtomicLong REVISIONS = new AtomicLong();
    private static volatile Snapshot snapshot = new Snapshot(Map.of(), Map.of(), Map.of(), Map.of(), 0L);

    private ClientResourceIndex() {
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static Optional<PresentationDefinition> definition(LocationContext context) {
        return Optional.ofNullable(snapshot.definitions().get(context));
    }

    public static void replace(
        Map<LocationContext, PresentationDefinition> definitions,
        Map<LocationContext, ResolvedMusic> music,
        Map<ResourceLocation, TextureSize> textureSizes,
        Map<ResourceLocation, NormalizationMetadata> normalization
    ) {
        snapshot = new Snapshot(definitions, music, textureSizes, normalization, REVISIONS.incrementAndGet());
    }
}
