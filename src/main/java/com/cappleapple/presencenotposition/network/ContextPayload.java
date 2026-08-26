package com.cappleapple.presencenotposition.network;

import com.cappleapple.presencenotposition.PresenceNotPosition;
import com.cappleapple.presencenotposition.location.LocationContext;
import com.cappleapple.presencenotposition.location.LocationType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ContextPayload(boolean entered, LocationContext context) implements CustomPacketPayload {
    public static final Type<ContextPayload> TYPE = new Type<>(PresenceNotPosition.id("location_context"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ContextPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ContextPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ContextPayload(buffer.readBoolean(), new LocationContext(
                buffer.readEnum(LocationType.class), ResourceLocation.STREAM_CODEC.decode(buffer)));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ContextPayload payload) {
            buffer.writeBoolean(payload.entered);
            buffer.writeEnum(payload.context.type());
            ResourceLocation.STREAM_CODEC.encode(buffer, payload.context.id());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
