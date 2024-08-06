package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record RequestKeyC2SPacket(UUID graveId) implements CustomPayload {
    public static final Id<RequestKeyC2SPacket> ID = new Id<>(Identifier.of(Yigd.MOD_ID, "request_grave_key"));
    public static final PacketCodec<RegistryByteBuf, RequestKeyC2SPacket> CODEC = PacketCodec.of(RequestKeyC2SPacket::write, RequestKeyC2SPacket::new);

    @Override
    public Id<RequestKeyC2SPacket> getId() {
        return ID;
    }

    public RequestKeyC2SPacket(RegistryByteBuf buf) {
        this(buf.readUuid());
    }
    public void write(RegistryByteBuf buf) {
        buf.writeUuid(this.graveId);
    }
}
