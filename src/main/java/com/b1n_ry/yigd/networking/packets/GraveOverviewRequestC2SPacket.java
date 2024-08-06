package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record GraveOverviewRequestC2SPacket(UUID graveId) implements CustomPayload {
    public static final Id<GraveOverviewRequestC2SPacket> ID = new Id<>(Identifier.of(Yigd.MOD_ID, "grave_overview_request"));
    public static final PacketCodec<RegistryByteBuf, GraveOverviewRequestC2SPacket> CODEC = PacketCodec.of(GraveOverviewRequestC2SPacket::write, GraveOverviewRequestC2SPacket::new);

    @Override
    public Id<GraveOverviewRequestC2SPacket> getId() {
        return ID;
    }

    public GraveOverviewRequestC2SPacket(RegistryByteBuf buf) {
        this(buf.readUuid());
    }
    public void write(RegistryByteBuf buf) {
        buf.writeUuid(this.graveId);
    }
}
