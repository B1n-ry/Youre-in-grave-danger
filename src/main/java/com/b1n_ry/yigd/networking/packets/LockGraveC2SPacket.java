package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record LockGraveC2SPacket(UUID graveId, boolean locked) implements CustomPacketPayload {
    public static final Type<LockGraveC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "lock_grave_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LockGraveC2SPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, LockGraveC2SPacket::graveId, ByteBufCodecs.BOOL, LockGraveC2SPacket::locked, LockGraveC2SPacket::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
