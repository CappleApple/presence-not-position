package com.cappleapple.presencenotposition.integration.kubejs;

import com.cappleapple.presencenotposition.api.LocationEvent;
import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class LocationEventJS implements KubeEvent {
    private final LocationEvent event;

    LocationEventJS(LocationEvent event) {
        this.event = event;
    }

    public ServerPlayer getPlayer() { return this.event.player(); }
    public String getType() { return this.event.type().name(); }
    public ResourceLocation getId() { return this.event.id(); }
    public Component getDisplayName() { return this.event.displayName(); }
    public boolean isEntered() { return this.event.entered(); }
    public void cancelPresentation() { this.event.cancelPresentation(); }
    public void setTitle(Object title) { this.event.setTitle(component(title)); }
    public void setSubtitle(Object subtitle) { this.event.setSubtitle(component(subtitle)); }
    public void setSound(String sound) { this.event.setSound(sound); }
    public void setPriority(int priority) { this.event.setPriority(priority); }
    public void setDuration(int ticks) { this.event.setDuration(ticks); }

    private static Component component(Object value) {
        return value instanceof Component component ? component : Component.literal(String.valueOf(value));
    }
}
