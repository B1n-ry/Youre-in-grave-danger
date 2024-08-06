package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.networking.LightPlayerData;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

public record PlayerSelectionS2CPacket(List<LightPlayerData> data) implements CustomPayload {
    public static final Id<PlayerSelectionS2CPacket> ID = new Id<>(Identifier.of(Yigd.MOD_ID, "player_selection"));
    public static final PacketCodec<RegistryByteBuf, PlayerSelectionS2CPacket> CODEC = PacketCodec.of(PlayerSelectionS2CPacket::write, PlayerSelectionS2CPacket::new);

    @Override
    public Id<PlayerSelectionS2CPacket> getId() {
        return ID;
    }

    public PlayerSelectionS2CPacket(RegistryByteBuf buf) {
        this(buf.readList(buf1 -> LightPlayerData.fromNbt(buf1.readNbt(), buf.getRegistryManager())));
    }

    public void write(RegistryByteBuf buf) {
        buf.writeCollection(this.data, (buf1, value) -> buf1.writeNbt(value.toNbt(buf.getRegistryManager())));
    }
}
