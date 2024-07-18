package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record RestoreGraveC2SPacket(UUID graveId, boolean itemsInGrave, boolean itemsDeleted, boolean itemsKept,
                                    boolean itemsDropped) implements CustomPayload {
    public static final Id<RestoreGraveC2SPacket> ID = new Id<>(new Identifier(Yigd.MOD_ID, "restore_grave_request"));
    public static final PacketCodec<RegistryByteBuf, RestoreGraveC2SPacket> CODEC = PacketCodec.of(RestoreGraveC2SPacket::write, RestoreGraveC2SPacket::new);

    @Override
    public Id<RestoreGraveC2SPacket> getId() {
        return ID;
    }

    public RestoreGraveC2SPacket(RegistryByteBuf buf) {
        this(buf.readUuid(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public void write(RegistryByteBuf buf) {
        buf.writeUuid(this.graveId);
        buf.writeBoolean(this.itemsInGrave);
        buf.writeBoolean(this.itemsDeleted);
        buf.writeBoolean(this.itemsKept);
        buf.writeBoolean(this.itemsDropped);
    }
}
