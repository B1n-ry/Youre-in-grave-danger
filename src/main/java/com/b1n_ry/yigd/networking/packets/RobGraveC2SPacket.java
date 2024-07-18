package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record RobGraveC2SPacket(UUID graveId, boolean itemsInGrave, boolean itemsDeleted, boolean itemsKept,
                                boolean itemsDropped) implements CustomPayload {
    public static final Id<RobGraveC2SPacket> ID = new Id<>(new Identifier(Yigd.MOD_ID, "rob_grave_request"));
    public static final PacketCodec<RegistryByteBuf, RobGraveC2SPacket> CODEC = PacketCodec.of(RobGraveC2SPacket::write, RobGraveC2SPacket::new);

    @Override
    public Id<RobGraveC2SPacket> getId() {
        return ID;
    }

    public RobGraveC2SPacket(RegistryByteBuf buf) {
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
