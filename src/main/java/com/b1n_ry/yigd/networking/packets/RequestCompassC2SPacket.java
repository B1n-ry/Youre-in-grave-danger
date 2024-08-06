package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record RequestCompassC2SPacket(UUID graveId) implements CustomPayload {
    public static final Id<RequestCompassC2SPacket> ID = new Id<>(Identifier.of(Yigd.MOD_ID, "request_grave_compass"));
    public static final PacketCodec<RegistryByteBuf, RequestCompassC2SPacket> CODEC = PacketCodec.of(RequestCompassC2SPacket::write, RequestCompassC2SPacket::new);

    @Override
    public Id<RequestCompassC2SPacket> getId() {
        return ID;
    }

    public RequestCompassC2SPacket(RegistryByteBuf buf) {
        this(buf.readUuid());
    }
    public void write(RegistryByteBuf buf) {
        buf.writeUuid(this.graveId);
    }
}
