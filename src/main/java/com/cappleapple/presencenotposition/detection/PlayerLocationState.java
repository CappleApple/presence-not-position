package com.cappleapple.presencenotposition.detection;

import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.location.LocationTransition;
import com.cappleapple.presencenotposition.location.LocationType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Pure transition state machine; world sampling and side effects deliberately live elsewhere. */
public final class PlayerLocationState {
    private final int structureExitGraceTicks;
    private final int biomeStabilityTicks;
    private final Map<ResourceLocation, Long> structureLastSeen = new HashMap<>();
    private ResourceLocation dimension;
    private ResourceLocation biome;
    private ResourceLocation biomeCandidate;
    private long biomeCandidateSince;
    private BlockPos home;

    public PlayerLocationState(int structureExitGraceTicks, int biomeStabilityTicks) {
        if (structureExitGraceTicks < 0 || biomeStabilityTicks < 0) {
            throw new IllegalArgumentException("stability values must be non-negative");
        }
        this.structureExitGraceTicks = structureExitGraceTicks;
        this.biomeStabilityTicks = biomeStabilityTicks;
    }

    public List<LocationTransition> sample(LocationSample sample, long gameTick) {
        Objects.requireNonNull(sample, "sample");
        List<LocationTransition> transitions = new ArrayList<>();
        if (!Objects.equals(this.dimension, sample.dimension())) {
            exitOldDimension(transitions);
            this.dimension = sample.dimension();
            transitions.add(LocationTransition.enter(context(LocationType.DIMENSION, this.dimension)));
            this.biome = sample.biome();
            this.biomeCandidate = null;
            transitions.add(LocationTransition.enter(context(LocationType.BIOME, this.biome)));
            for (ResourceLocation structure : sorted(sample.structures())) {
                this.structureLastSeen.put(structure, gameTick);
                transitions.add(LocationTransition.enter(context(LocationType.STRUCTURE, structure)));
            }
            updateHome(sample.home(), transitions);
            return List.copyOf(transitions);
        }

        updateBiome(sample.biome(), gameTick, transitions);
        updateStructures(sample.structures(), gameTick, transitions);
        updateHome(sample.home(), transitions);
        return List.copyOf(transitions);
    }

    private void exitOldDimension(List<LocationTransition> transitions) {
        updateHome(null, transitions);
        for (ResourceLocation structure : sorted(this.structureLastSeen.keySet())) {
            transitions.add(LocationTransition.exit(context(LocationType.STRUCTURE, structure)));
        }
        this.structureLastSeen.clear();
        if (this.biome != null) transitions.add(LocationTransition.exit(context(LocationType.BIOME, this.biome)));
        if (this.dimension != null) transitions.add(LocationTransition.exit(context(LocationType.DIMENSION, this.dimension)));
        this.biome = null;
        this.biomeCandidate = null;
    }

    private void updateBiome(ResourceLocation observed, long gameTick, List<LocationTransition> transitions) {
        if (Objects.equals(observed, this.biome)) {
            this.biomeCandidate = null;
            return;
        }
        if (!Objects.equals(observed, this.biomeCandidate)) {
            this.biomeCandidate = observed;
            this.biomeCandidateSince = gameTick;
            if (this.biomeStabilityTicks > 0) return;
        }
        if (gameTick - this.biomeCandidateSince < this.biomeStabilityTicks) return;
        if (this.biome != null) transitions.add(LocationTransition.exit(context(LocationType.BIOME, this.biome)));
        this.biome = observed;
        this.biomeCandidate = null;
        transitions.add(LocationTransition.enter(context(LocationType.BIOME, this.biome)));
    }

    private void updateStructures(Set<ResourceLocation> observed, long gameTick, List<LocationTransition> transitions) {
        for (ResourceLocation id : sorted(observed)) {
            if (!this.structureLastSeen.containsKey(id)) {
                transitions.add(LocationTransition.enter(context(LocationType.STRUCTURE, id)));
            }
            this.structureLastSeen.put(id, gameTick);
        }

        List<ResourceLocation> exits = this.structureLastSeen.entrySet().stream()
            .filter(entry -> !observed.contains(entry.getKey()))
            .filter(entry -> gameTick - entry.getValue() >= this.structureExitGraceTicks)
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
        for (ResourceLocation id : exits) {
            this.structureLastSeen.remove(id);
            transitions.add(LocationTransition.exit(context(LocationType.STRUCTURE, id)));
        }
    }

    private void updateHome(@Nullable BlockPos observed, List<LocationTransition> transitions) {
        if (Objects.equals(this.home, observed)) return;
        if (this.home != null) transitions.add(LocationTransition.exit(LocationContext.HOME));
        this.home = observed;
        if (observed != null) transitions.add(LocationTransition.enter(LocationContext.HOME));
    }

    @Nullable
    public BlockPos home() {
        return this.home;
    }

    public ResourceLocation dimension() {
        return this.dimension;
    }

    public ResourceLocation biome() {
        return this.biome;
    }

    public Set<ResourceLocation> structures() {
        return Collections.unmodifiableSet(this.structureLastSeen.keySet());
    }

    private static LocationContext context(LocationType type, ResourceLocation id) {
        return new LocationContext(type, Objects.requireNonNull(id, "sample location id"));
    }

    private static List<ResourceLocation> sorted(Set<ResourceLocation> ids) {
        return ids.stream().sorted().toList();
    }
}
