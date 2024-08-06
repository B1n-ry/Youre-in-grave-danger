package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record LockGraveC2SPacket(UUID graveId, boolean locked) implements CustomPayload {
    public static final Id<LockGraveC2SPacket> ID = new Id<>(Identifier.of(Yigd.MOD_ID, "lock_grave_request"));
    public static final PacketCodec<RegistryByteBuf, LockGraveC2SPacket> CODEC = PacketCodec.of(LockGraveC2SPacket::write, LockGraveC2SPacket::new);

    @Override
    public Id<LockGraveC2SPacket> getId() {
        return ID;
    }

    public LockGraveC2SPacket(RegistryByteBuf buf) {
        this(buf.readUuid(), buf.readBoolean());
    }
    public void write(RegistryByteBuf buf) {
        buf.writeUuid(this.graveId);
        buf.writeBoolean(this.locked);
    }
}
