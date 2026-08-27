package com.cappleapple.presencenotposition.server;

import com.cappleapple.presencenotposition.PresenceNotPosition;
import com.cappleapple.presencenotposition.api.LocationEvent;
import com.cappleapple.presencenotposition.api.LocationEvents;
import com.cappleapple.presencenotposition.config.ServerConfig;
import com.cappleapple.presencenotposition.detection.LocationSample;
import com.cappleapple.presencenotposition.detection.PlayerLocationState;
import com.cappleapple.presencenotposition.integration.InstancedNotInfiniteCompatibility;
import com.cappleapple.presencenotposition.location.LocationTransition;
import com.cappleapple.presencenotposition.network.ContextPayload;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ServerLocationTracker {
    private static final Map<UUID, PlayerLocationState> STATES = new HashMap<>();

    private ServerLocationTracker() {
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int interval = ServerConfig.DETECTION_INTERVAL_TICKS.get();
        if (player.tickCount % interval != Math.floorMod(player.getId(), interval)) return;
        PlayerLocationState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new PlayerLocationState(
            ServerConfig.STRUCTURE_EXIT_GRACE_TICKS.get(), ServerConfig.BIOME_STABILITY_TICKS.get()));
        long tick = player.serverLevel().getGameTime();
        for (LocationTransition transition : state.sample(sample(player), tick)) dispatch(player, transition);
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }

    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Force an immediate authoritative rebuild instead of waiting for the staggered poll.
            PlayerLocationState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new PlayerLocationState(
                ServerConfig.STRUCTURE_EXIT_GRACE_TICKS.get(), ServerConfig.BIOME_STABILITY_TICKS.get()));
            for (LocationTransition transition : state.sample(sample(player), player.serverLevel().getGameTime())) dispatch(player, transition);
        }
    }

    public static PlayerLocationState state(ServerPlayer player) {
        return STATES.get(player.getUUID());
    }

    private static LocationSample sample(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ResourceLocation dimension = level.dimension().location();
        Holder<?> biomeHolder = level.getBiome(player.blockPosition());
        ResourceLocation biome = biomeHolder.unwrapKey().orElseThrow().location();
        Set<ResourceLocation> structures = new HashSet<>();
        Map<Structure, LongSet> referenced = level.structureManager().getAllStructuresAt(player.blockPosition());
        var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        referenced.keySet().forEach(structure -> {
            StructureStart start = level.structureManager().getStructureWithPieceAt(player.blockPosition(), structure);
            if (start.isValid()) {
                ResourceLocation id = registry.getKey(structure);
                if (id != null) structures.add(id);
            }
        });
        return new LocationSample(dimension, biome, structures,
            ServerConfig.HOME_ENABLED.get() ? HomeDetector.findHome(player, ServerConfig.HOME_RADIUS.get()) : null);
    }

    private static void dispatch(ServerPlayer player, LocationTransition transition) {
        PacketDistributor.sendToPlayer(player, new ContextPayload(transition.entered(), transition.context()));
        LocationEvent event = new LocationEvent(player, transition.context(), transition.entered());
        try {
            LocationEvents.post(event);
        } catch (RuntimeException exception) {
            PresenceNotPosition.LOGGER.error("Location integration failed for {}", transition.context(), exception);
        }
        if (transition.entered() && !event.presentationCancelled()
            && !InstancedNotInfiniteCompatibility.suppressAutomaticTitle(transition.context())) {
            PresentationService.show(player, transition.context(), event.override());
        }
        ServerCommands.debugTransition(player, transition);
    }
}
