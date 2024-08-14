package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.ClaimPriority;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record UpdateConfigC2SPacket(ClaimPriority claiming, ClaimPriority robbing) implements CustomPacketPayload {
    public static final Type<UpdateConfigC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "update_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateConfigC2SPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, o -> o.claiming.toString(),
            ByteBufCodecs.STRING_UTF8, o -> o.robbing.toString(),
            (s1, s2) -> new UpdateConfigC2SPacket(ClaimPriority.valueOf(s1), ClaimPriority.valueOf(s2)));

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
