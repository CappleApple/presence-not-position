package com.cappleapple.presencenotposition.integration.kubejs;

import com.cappleapple.presencenotposition.api.LocationEvents;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.ClassFilter;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

/** Discovered by KubeJS only when KubeJS is installed. */
public final class PresenceKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void init() {
        LocationEvents.installSink(PresenceKubeJSEvents::post);
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(PresenceKubeJSEvents.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("PresenceNotPosition", PresenceKubeJSBindings.INSTANCE);
    }

    @Override
    public void registerClasses(ClassFilter filter) {
        filter.allow("com.cappleapple.presencenotposition.integration.kubejs");
        filter.allow("com.cappleapple.presencenotposition.api");
    }
}
