package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.networking.LightPlayerData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record PlayerSelectionS2CPacket(List<LightPlayerData> data) implements CustomPacketPayload {
    public static final Type<PlayerSelectionS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "player_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSelectionS2CPacket> STREAM_CODEC = StreamCodec.ofMember(PlayerSelectionS2CPacket::write, PlayerSelectionS2CPacket::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public PlayerSelectionS2CPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readList(friendlyByteBuf -> LightPlayerData.fromNbt(friendlyByteBuf.readNbt(), buf.registryAccess())));
    }
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeCollection(this.data, (buf1, value) -> buf1.writeNbt(value.toNbt(buf.registryAccess())));
    }
}
