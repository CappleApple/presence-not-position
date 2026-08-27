package com.cappleapple.presencenotposition.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue DETECTION_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue STRUCTURE_EXIT_GRACE_TICKS;
    public static final ModConfigSpec.IntValue BIOME_STABILITY_TICKS;
    public static final ModConfigSpec.BooleanValue HOME_ENABLED;
    public static final ModConfigSpec.DoubleValue HOME_RADIUS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("detection");
        DETECTION_INTERVAL_TICKS = builder.defineInRange("intervalTicks", 5, 1, 100);
        STRUCTURE_EXIT_GRACE_TICKS = builder.defineInRange("structureExitGraceTicks", 15, 0, 200);
        BIOME_STABILITY_TICKS = builder.defineInRange("biomeStabilityTicks", 20, 0, 200);
        builder.pop();
        builder.push("home");
        HOME_ENABLED = builder.comment("Detect home around the player's current respawn bed.")
            .define("enabled", true);
        HOME_RADIUS = builder.comment("Spherical radius in blocks from the center of the current respawn bed.",
            "The bed must still exist in the player's current dimension. Does not load distant chunks.")
            .defineInRange("radius", 64.0, 1.0, 1024.0);
        builder.pop();
        SPEC = builder.build();
    }

    private ServerConfig() {
    }
}
