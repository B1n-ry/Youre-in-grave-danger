package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record DeleteGraveC2SPacket(UUID graveId) implements CustomPayload {
    public static final Id<DeleteGraveC2SPacket> ID = new Id<>(Identifier.of(Yigd.MOD_ID, "delete_grave_request"));
    public static final PacketCodec<RegistryByteBuf, DeleteGraveC2SPacket> CODEC = PacketCodec.of(DeleteGraveC2SPacket::write, DeleteGraveC2SPacket::new);

    @Override
    public Id<DeleteGraveC2SPacket> getId() {
        return ID;
    }

    public DeleteGraveC2SPacket(RegistryByteBuf buf) {
        this(buf.readUuid());
    }
    public void write(RegistryByteBuf buf) {
        buf.writeUuid(this.graveId);
    }
}
