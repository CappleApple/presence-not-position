package com.cappleapple.presencenotposition.network;

import com.cappleapple.presencenotposition.PresenceNotPosition;
import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.location.LocationType;
import com.cappleapple.presencenotposition.presentation.PresentationOverride;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PresentationPayload(LocationContext context, PresentationOverride override) implements CustomPacketPayload {
    public static final Type<PresentationPayload> TYPE = new Type<>(PresenceNotPosition.id("presentation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PresentationPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PresentationPayload decode(RegistryFriendlyByteBuf buffer) {
            LocationContext context = new LocationContext(buffer.readEnum(LocationType.class), ResourceLocation.STREAM_CODEC.decode(buffer));
            Component title = FriendlyByteBuf.readNullable(buffer, ComponentSerialization.TRUSTED_STREAM_CODEC);
            Component subtitle = FriendlyByteBuf.readNullable(buffer, ComponentSerialization.TRUSTED_STREAM_CODEC);
            ResourceLocation sound = FriendlyByteBuf.readNullable(buffer, ResourceLocation.STREAM_CODEC);
            Integer priority = buffer.readBoolean() ? buffer.readVarInt() : null;
            Integer duration = buffer.readBoolean() ? buffer.readVarInt() : null;
            return new PresentationPayload(context, new PresentationOverride(title, subtitle, sound, priority, duration, buffer.readBoolean()));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PresentationPayload payload) {
            buffer.writeEnum(payload.context.type());
            ResourceLocation.STREAM_CODEC.encode(buffer, payload.context.id());
            FriendlyByteBuf.writeNullable(buffer, payload.override.title(), ComponentSerialization.TRUSTED_STREAM_CODEC);
            FriendlyByteBuf.writeNullable(buffer, payload.override.subtitle(), ComponentSerialization.TRUSTED_STREAM_CODEC);
            FriendlyByteBuf.writeNullable(buffer, payload.override.sound(), ResourceLocation.STREAM_CODEC);
            buffer.writeBoolean(payload.override.priority() != null);
            if (payload.override.priority() != null) buffer.writeVarInt(payload.override.priority());
            buffer.writeBoolean(payload.override.durationTicks() != null);
            if (payload.override.durationTicks() != null) buffer.writeVarInt(payload.override.durationTicks());
            buffer.writeBoolean(payload.override.respectClientPolicy());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
