package com.cappleapple.presencenotposition.api;

import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.location.LocationType;
import com.cappleapple.presencenotposition.presentation.PresentationOverride;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class LocationEvent {
    private final ServerPlayer player;
    private final LocationContext context;
    private final boolean entered;
    private Component title;
    private Component subtitle;
    private ResourceLocation sound;
    private Integer priority;
    private Integer duration;
    private boolean presentationCancelled;

    public LocationEvent(ServerPlayer player, LocationContext context, boolean entered) {
        this.player = player;
        this.context = context;
        this.entered = entered;
    }

    public ServerPlayer player() { return this.player; }
    public LocationType type() { return this.context.type(); }
    public ResourceLocation id() { return this.context.id(); }
    public LocationContext context() { return this.context; }
    public boolean entered() { return this.entered; }
    public Component displayName() { return LocationNames.defaultComponent(this.context); }

    public void cancelPresentation() { this.presentationCancelled = true; }
    public boolean presentationCancelled() { return this.presentationCancelled; }
    public void setTitle(Component title) { this.title = title; }
    public void setTitle(String title) { this.title = Component.literal(title); }
    public void setSubtitle(Component subtitle) { this.subtitle = subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = Component.literal(subtitle); }
    public void setSound(ResourceLocation sound) { this.sound = sound; }
    public void setSound(String sound) { this.sound = requireId(sound); }
    public void setPriority(int priority) { this.priority = priority; }
    public void setDuration(int ticks) { this.duration = Math.max(1, ticks); }

    public PresentationOverride override() {
        return new PresentationOverride(this.title, this.subtitle, this.sound, this.priority, this.duration, true);
    }

    @Nullable public Component title() { return this.title; }
    @Nullable public Component subtitle() { return this.subtitle; }
    @Nullable public ResourceLocation sound() { return this.sound; }
    @Nullable public Integer priority() { return this.priority; }
    @Nullable public Integer duration() { return this.duration; }

    private static ResourceLocation requireId(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) throw new IllegalArgumentException("Invalid resource location: " + value);
        return id;
    }
}
