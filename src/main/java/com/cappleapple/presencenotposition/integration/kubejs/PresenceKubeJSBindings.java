package com.cappleapple.presencenotposition.integration.kubejs;

import com.cappleapple.presencenotposition.api.PresenceApi;
import com.cappleapple.presencenotposition.location.LocationType;
import com.cappleapple.presencenotposition.presentation.PresentationOverride;
import dev.latvian.mods.kubejs.event.IEventHandler;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class PresenceKubeJSBindings {
    static final PresenceKubeJSBindings INSTANCE = new PresenceKubeJSBindings();

    private PresenceKubeJSBindings() {
    }

    public void structureEntered(IEventHandler listener) { PresenceKubeJSEvents.listen(LocationType.STRUCTURE, true, listener); }
    public void structureExited(IEventHandler listener) { PresenceKubeJSEvents.listen(LocationType.STRUCTURE, false, listener); }
    public void biomeEntered(IEventHandler listener) { PresenceKubeJSEvents.listen(LocationType.BIOME, true, listener); }
    public void biomeExited(IEventHandler listener) { PresenceKubeJSEvents.listen(LocationType.BIOME, false, listener); }
    public void dimensionEntered(IEventHandler listener) { PresenceKubeJSEvents.listen(LocationType.DIMENSION, true, listener); }
    public void dimensionExited(IEventHandler listener) { PresenceKubeJSEvents.listen(LocationType.DIMENSION, false, listener); }
    public void entered(IEventHandler listener) { PresenceKubeJSEvents.listenUnified(true, listener); }
    public void exited(IEventHandler listener) { PresenceKubeJSEvents.listenUnified(false, listener); }

    public void show(ServerPlayer player, Object value) {
        if (value instanceof CharSequence sequence) {
            PresenceApi.show(player, id(sequence.toString()));
            return;
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("PresenceNotPosition.show expects a resource ID or object");
        }
        ResourceLocation customId = id(required(map, "id"));
        Component title = map.containsKey("title") ? component(map.get("title")) : null;
        Component subtitle = map.containsKey("subtitle") ? component(map.get("subtitle")) : null;
        ResourceLocation sound = map.containsKey("sound") ? id(String.valueOf(map.get("sound"))) : null;
        Integer priority = map.containsKey("priority") ? number(map.get("priority")).intValue() : null;
        Integer duration = map.containsKey("duration") ? Math.max(1, number(map.get("duration")).intValue()) : null;
        boolean respect = !map.containsKey("respectClientPolicy") || Boolean.parseBoolean(String.valueOf(map.get("respectClientPolicy")));
        PresenceApi.show(player, customId, new PresentationOverride(title, subtitle, sound, priority, duration, respect));
    }

    private static String required(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value == null) throw new IllegalArgumentException("Missing required field '" + key + "'");
        return String.valueOf(value);
    }

    private static Number number(Object value) {
        if (value instanceof Number number) return number;
        return Double.parseDouble(String.valueOf(value));
    }

    private static Component component(Object value) {
        return value instanceof Component component ? component : Component.literal(String.valueOf(value));
    }

    private static ResourceLocation id(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) throw new IllegalArgumentException("Invalid resource location: " + value);
        return id;
    }
}
