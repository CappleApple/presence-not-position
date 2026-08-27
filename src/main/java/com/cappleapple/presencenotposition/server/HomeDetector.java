package com.cappleapple.presencenotposition.server;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Reads the authoritative respawn bed without searching for other beds or loading chunks. */
public final class HomeDetector {
    private HomeDetector() { }

    @Nullable
    public static BlockPos findHome(ServerPlayer player, double radius) {
        BlockPos bed = player.getRespawnPosition();
        ServerLevel level = player.serverLevel();
        if (bed == null || !level.dimension().equals(player.getRespawnDimension())) return null;
        if (player.position().distanceToSqr(Vec3.atCenterOf(bed)) > radius * radius) return null;
        if (!level.getChunkSource().hasChunk(bed.getX() >> 4, bed.getZ() >> 4)
            || !level.getBlockState(bed).isBed(level, bed, player)) return null;
        return bed.immutable();
    }
}
