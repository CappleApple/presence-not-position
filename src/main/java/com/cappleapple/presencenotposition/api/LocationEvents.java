package com.cappleapple.presencenotposition.api;

import java.util.Objects;
import java.util.function.Consumer;

/** Optional integrations install a sink without making them classloading dependencies. */
public final class LocationEvents {
    private static volatile Consumer<LocationEvent> sink = ignored -> { };

    private LocationEvents() {
    }

    public static void installSink(Consumer<LocationEvent> newSink) {
        sink = Objects.requireNonNull(newSink);
    }

    public static void post(LocationEvent event) {
        sink.accept(event);
    }
}
