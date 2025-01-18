package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record RequestCompassC2SPacket(UUID graveId) implements CustomPacketPayload {
    public static final Type<RequestCompassC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "request_grave_compass"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestCompassC2SPacket> STREAM_CODEC = StreamCodec.ofMember(RequestCompassC2SPacket::write, RequestCompassC2SPacket::new);

    @Override
    public @NotNull Type<RequestCompassC2SPacket> type() {
        return TYPE;
    }

    public RequestCompassC2SPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readUUID());
    }
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.graveId);
    }
}
