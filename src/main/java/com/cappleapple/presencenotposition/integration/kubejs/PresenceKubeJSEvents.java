package com.cappleapple.presencenotposition.integration.kubejs;

import com.cappleapple.presencenotposition.api.LocationEvent;
import com.cappleapple.presencenotposition.location.LocationType;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.IEventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;
import java.util.EnumMap;
import java.util.Map;

final class PresenceKubeJSEvents {
    static final EventGroup GROUP = EventGroup.of("PresenceNotPosition");
    private static final Map<LocationType, EventHandler> ENTER = new EnumMap<>(LocationType.class);
    private static final Map<LocationType, EventHandler> EXIT = new EnumMap<>(LocationType.class);
    private static final EventHandler ENTERED = GROUP.server("entered", () -> LocationEventJS.class);
    private static final EventHandler EXITED = GROUP.server("exited", () -> LocationEventJS.class);

    static {
        ENTER.put(LocationType.STRUCTURE, GROUP.server("structureEntered", () -> LocationEventJS.class));
        EXIT.put(LocationType.STRUCTURE, GROUP.server("structureExited", () -> LocationEventJS.class));
        ENTER.put(LocationType.BIOME, GROUP.server("biomeEntered", () -> LocationEventJS.class));
        EXIT.put(LocationType.BIOME, GROUP.server("biomeExited", () -> LocationEventJS.class));
        ENTER.put(LocationType.DIMENSION, GROUP.server("dimensionEntered", () -> LocationEventJS.class));
        EXIT.put(LocationType.DIMENSION, GROUP.server("dimensionExited", () -> LocationEventJS.class));
    }

    private PresenceKubeJSEvents() {
    }

    static void post(LocationEvent event) {
        LocationEventJS wrapper = new LocationEventJS(event);
        EventHandler specific = (event.entered() ? ENTER : EXIT).get(event.type());
        if (specific != null && specific.hasListeners()) specific.post(ScriptType.SERVER, wrapper);
        EventHandler unified = event.entered() ? ENTERED : EXITED;
        if (unified.hasListeners()) unified.post(ScriptType.SERVER, wrapper);
    }

    static void listen(LocationType type, boolean entered, IEventHandler listener) {
        EventHandler handler = (entered ? ENTER : EXIT).get(type);
        if (handler == null) throw new IllegalArgumentException("No built-in event for " + type);
        handler.listenJava(ScriptType.SERVER, null, listener);
    }

    static void listenUnified(boolean entered, IEventHandler listener) {
        (entered ? ENTERED : EXITED).listenJava(ScriptType.SERVER, null, listener);
    }
}
