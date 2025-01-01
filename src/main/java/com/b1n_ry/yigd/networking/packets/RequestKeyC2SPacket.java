package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record RequestKeyC2SPacket(UUID graveId) implements CustomPacketPayload {
    public static final Type<RequestKeyC2SPacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "request_grave_key"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestKeyC2SPacket> CODEC = StreamCodec.ofMember(RequestKeyC2SPacket::write, RequestKeyC2SPacket::new);

    @Override
    public @NotNull Type<RequestKeyC2SPacket> type() {
        return ID;
    }

    public RequestKeyC2SPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readUUID());
    }
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.graveId);
    }
}
