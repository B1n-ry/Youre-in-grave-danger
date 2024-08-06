package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.networking.LightGraveData;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record GraveSelectionS2CPacket(List<LightGraveData> data, ProfileComponent owner) implements CustomPayload {
    public static final Id<GraveSelectionS2CPacket> ID = new Id<>(Identifier.of(Yigd.MOD_ID, "grave_selection"));
    public static final PacketCodec<RegistryByteBuf, GraveSelectionS2CPacket> CODEC = PacketCodec.of(GraveSelectionS2CPacket::write, GraveSelectionS2CPacket::new);

    @Override
    public Id<GraveSelectionS2CPacket> getId() {
        return ID;
    }

    public GraveSelectionS2CPacket(RegistryByteBuf buf) {
        this(buf.readList(buf1 -> LightGraveData.fromNbt(buf1.readNbt(), buf.getRegistryManager())), ProfileComponent.PACKET_CODEC.decode(buf));
    }

    public void write(RegistryByteBuf buf) {
        buf.writeCollection(this.data, (buf1, value) -> buf1.writeNbt(value.toNbt(buf.getRegistryManager())));
        ProfileComponent.PACKET_CODEC.encode(buf, this.owner);
    }
}
