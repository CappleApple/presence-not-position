package com.cappleapple.presencenotposition.client;

import com.cappleapple.presencenotposition.PresenceNotPosition;
import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.location.LocationType;
import com.cappleapple.presencenotposition.presentation.HistoryEntry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

public final class PresentationHistory {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("presence-not-position-history.json");
    private static final Map<LocationContext, HistoryEntry> ENTRIES = new HashMap<>();
    private static boolean loaded;
    private static boolean dirty;
    private static int saveCountdown;

    private PresentationHistory() {
    }

    @Nullable
    public static HistoryEntry get(LocationContext context) {
        ensureLoaded();
        return ENTRIES.get(context);
    }

    public static void record(LocationContext context, long nowSeconds) {
        ensureLoaded();
        ENTRIES.compute(context, (ignored, old) -> old == null ? HistoryEntry.first(nowSeconds) : old.shown(nowSeconds));
        dirty = true;
        saveCountdown = 100;
    }

    public static int size() {
        ensureLoaded();
        return ENTRIES.size();
    }

    public static void clear() {
        ensureLoaded();
        ENTRIES.clear();
        dirty = true;
        saveNow();
    }

    public static void tick() {
        if (dirty && saveCountdown-- <= 0) saveNow();
    }

    public static void saveNow() {
        if (!dirty) return;
        JsonObject root = new JsonObject();
        for (LocationType type : LocationType.values()) {
            if (type == LocationType.CUSTOM) continue;
            JsonObject category = new JsonObject();
            ENTRIES.entrySet().stream().filter(entry -> entry.getKey().type() == type)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> category.add(entry.getKey().id().toString(), GSON.toJsonTree(entry.getValue())));
            root.add(type.resourceDirectory(), category);
        }
        try {
            Files.createDirectories(PATH.getParent());
            Path temporary = PATH.resolveSibling(PATH.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(root, writer);
            }
            try {
                Files.move(temporary, PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                Files.move(temporary, PATH, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException exception) {
            PresenceNotPosition.LOGGER.warn("Could not save client presentation history", exception);
        }
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        if (!Files.isRegularFile(PATH)) return;
        try (Reader reader = Files.newBufferedReader(PATH)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) return;
            for (LocationType type : LocationType.values()) {
                if (type == LocationType.CUSTOM || !root.has(type.resourceDirectory())) continue;
                JsonObject category = root.getAsJsonObject(type.resourceDirectory());
                category.entrySet().forEach(entry -> {
                    ResourceLocation id = ResourceLocation.tryParse(entry.getKey());
                    if (id != null) ENTRIES.put(new LocationContext(type, id), GSON.fromJson(entry.getValue(), HistoryEntry.class));
                });
            }
        } catch (IOException | RuntimeException exception) {
            PresenceNotPosition.LOGGER.warn("Could not load client presentation history; starting with an empty history", exception);
            ENTRIES.clear();
        }
    }
}
