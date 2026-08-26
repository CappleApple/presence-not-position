package com.cappleapple.presencenotposition.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public final class TrackSelector {
    private final List<ResourceLocation> tracks;
    private final MusicSelection mode;
    private final boolean avoidImmediateRepeat;
    private final RandomGenerator random;
    private final List<ResourceLocation> bag = new ArrayList<>();
    private int sequentialIndex;
    private ResourceLocation last;

    public TrackSelector(List<ResourceLocation> tracks, MusicSelection mode, boolean avoidImmediateRepeat, RandomGenerator random) {
        this.tracks = List.copyOf(tracks);
        this.mode = Objects.requireNonNull(mode);
        this.avoidImmediateRepeat = avoidImmediateRepeat;
        this.random = Objects.requireNonNull(random);
    }

    @Nullable
    public ResourceLocation next() {
        if (this.tracks.isEmpty()) return null;
        ResourceLocation selected = switch (this.mode) {
            case SEQUENTIAL -> sequential();
            case RANDOM -> random();
            case SHUFFLE -> shuffle();
        };
        this.last = selected;
        return selected;
    }

    private ResourceLocation sequential() {
        ResourceLocation selected = this.tracks.get(this.sequentialIndex % this.tracks.size());
        this.sequentialIndex++;
        return selected;
    }

    private ResourceLocation random() {
        if (!this.avoidImmediateRepeat || this.tracks.size() == 1) {
            return this.tracks.get(this.random.nextInt(this.tracks.size()));
        }
        ResourceLocation selected;
        do selected = this.tracks.get(this.random.nextInt(this.tracks.size()));
        while (selected.equals(this.last));
        return selected;
    }

    private ResourceLocation shuffle() {
        if (this.bag.isEmpty()) {
            this.bag.addAll(this.tracks);
            Collections.shuffle(this.bag, new java.util.Random(this.random.nextLong()));
            if (this.avoidImmediateRepeat && this.bag.size() > 1 && this.bag.getFirst().equals(this.last)) {
                Collections.swap(this.bag, 0, 1);
            }
        }
        return this.bag.removeFirst();
    }
}
