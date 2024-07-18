package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GraveSelectionRequestC2SPacket(ProfileComponent profile) implements CustomPayload {
    public static final Id<GraveSelectionRequestC2SPacket> ID = new Id<>(new Identifier(Yigd.MOD_ID, "grave_selection_request"));
    public static final PacketCodec<RegistryByteBuf, GraveSelectionRequestC2SPacket> CODEC = PacketCodec.of(GraveSelectionRequestC2SPacket::write, GraveSelectionRequestC2SPacket::new);

    @Override
    public Id<GraveSelectionRequestC2SPacket> getId() {
        return ID;
    }

    public GraveSelectionRequestC2SPacket(RegistryByteBuf buf) {
        this(ProfileComponent.PACKET_CODEC.decode(buf));
    }
    public void write(RegistryByteBuf buf) {
        ProfileComponent.PACKET_CODEC.encode(buf, this.profile);
    }
}
