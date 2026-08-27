package com.cappleapple.presencenotposition.config;

import com.cappleapple.presencenotposition.location.LocationType;
import com.cappleapple.presencenotposition.presentation.ShowMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public enum VanillaMusicBehavior { REPLACE, DUCK, ALLOW }

    public record TitleCategory(
        ModConfigSpec.BooleanValue enabled,
        ModConfigSpec.EnumValue<ShowMode> showMode,
        ModConfigSpec.IntValue cooldownSeconds
    ) { }

    public static final ModConfigSpec SPEC;
    private static final Map<LocationType, TitleCategory> TITLES = new EnumMap<>(LocationType.class);
    private static final Map<LocationType, ModConfigSpec.IntValue> MUSIC_COOLDOWNS = new EnumMap<>(LocationType.class);
    public static final ModConfigSpec.IntValue TITLE_X;
    public static final ModConfigSpec.IntValue TITLE_Y;
    public static final ModConfigSpec.IntValue TITLE_SPACING;
    public static final ModConfigSpec.BooleanValue CUSTOM_PRESENTATIONS;
    public static final ModConfigSpec.BooleanValue MUSIC_ENABLED;
    public static final ModConfigSpec.DoubleValue MUSIC_MASTER_VOLUME;
    public static final ModConfigSpec.BooleanValue STRUCTURE_MUSIC;
    public static final ModConfigSpec.BooleanValue BIOME_MUSIC;
    public static final ModConfigSpec.BooleanValue DIMENSION_MUSIC;
    public static final ModConfigSpec.BooleanValue HOME_MUSIC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ADDITIONAL_PRIORITY_SOUNDS;
    public static final ModConfigSpec.EnumValue<VanillaMusicBehavior> VANILLA_MUSIC_BEHAVIOR;
    public static final ModConfigSpec.DoubleValue VANILLA_MUSIC_DUCK_VOLUME;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        addTitleCategory(builder, LocationType.STRUCTURE, true, ShowMode.COOLDOWN, 300);
        addTitleCategory(builder, LocationType.BIOME, true, ShowMode.COOLDOWN, 600);
        addTitleCategory(builder, LocationType.DIMENSION, true, ShowMode.ONCE, 0);
        addTitleCategory(builder, LocationType.HOME, true, ShowMode.COOLDOWN, 300);
        builder.push("titleLayout");
        TITLE_X = builder.comment(
            "Signed horizontal offset from the top-center origin in scaled GUI pixels. 0 is centered."
        ).defineInRange("x", 0, -Short.MAX_VALUE, Short.MAX_VALUE);
        TITLE_Y = builder.comment(
            "Signed vertical offset from the top-center origin to the top of the first title row, in scaled GUI pixels."
        ).defineInRange("y", 0, -Short.MAX_VALUE, Short.MAX_VALUE);
        TITLE_SPACING = builder.comment(
            "Empty scaled GUI pixels between simultaneously stacked title rows."
        ).defineInRange("spacing", 2, 0, Short.MAX_VALUE);
        builder.pop();
        builder.push("custom");
        CUSTOM_PRESENTATIONS = builder.comment("Applies when a scripted custom presentation respects client policy.")
            .define("enabled", true);
        builder.pop();

        builder.push("music");
        MUSIC_ENABLED = builder.define("enabled", true);
        MUSIC_MASTER_VOLUME = builder.defineInRange("masterVolume", 1.0, 0.0, 2.0);
        STRUCTURE_MUSIC = builder.define("structureMusic", true);
        BIOME_MUSIC = builder.define("biomeMusic", true);
        DIMENSION_MUSIC = builder.define("dimensionMusic", true);
        HOME_MUSIC = builder.define("homeMusic", true);
        ADDITIONAL_PRIORITY_SOUNDS = builder.comment(
            "Extra sound-event IDs that mute location music, for boss mods using non-music sound categories.",
            "Audible RECORDS and non-vanilla MUSIC sounds are detected automatically."
        ).defineListAllowEmpty("additionalPrioritySounds", List.of(),
            () -> "example:boss_theme", value -> value instanceof String id && ResourceLocation.tryParse(id) != null);
        VANILLA_MUSIC_BEHAVIOR = builder.defineEnum("vanillaMusicBehavior", VanillaMusicBehavior.REPLACE);
        VANILLA_MUSIC_DUCK_VOLUME = builder.defineInRange("vanillaMusicDuckVolume", 0.15, 0.0, 1.0);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }

    private static void addTitleCategory(ModConfigSpec.Builder builder, LocationType type, boolean enabled, ShowMode mode, int cooldown) {
        builder.push(type.resourceDirectory());
        TITLES.put(type, new TitleCategory(
            builder.define("enabled", enabled),
            builder.defineEnum("showMode", mode),
            builder.defineInRange("cooldownSeconds", cooldown, 0, Integer.MAX_VALUE)
        ));
        MUSIC_COOLDOWNS.put(type, builder.comment(
            "Additional seconds of silence after this category's music ends, on top of resource-pack track delay.",
            "Applies across location changes and skips. Nonzero values also wait after transition fade-out."
        ).defineInRange("musicCooldownSeconds", 0, 0, Integer.MAX_VALUE));
        builder.pop();
    }

    public static TitleCategory titles(LocationType type) {
        return TITLES.get(type);
    }

    public static int musicCooldownSeconds(LocationType type) {
        ModConfigSpec.IntValue value = MUSIC_COOLDOWNS.get(type);
        return value == null ? 0 : value.get();
    }

    public static boolean musicEnabled(LocationType type) {
        return switch (type) {
            case STRUCTURE -> STRUCTURE_MUSIC.get();
            case BIOME -> BIOME_MUSIC.get();
            case DIMENSION -> DIMENSION_MUSIC.get();
            case HOME -> HOME_MUSIC.get();
            case CUSTOM -> false;
        };
    }
}
