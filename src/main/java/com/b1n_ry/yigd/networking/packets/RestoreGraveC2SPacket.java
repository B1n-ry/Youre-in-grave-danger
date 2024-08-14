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

public record RestoreGraveC2SPacket(UUID graveId, boolean itemsInGrave, boolean itemsDeleted, boolean itemsKept,
                                    boolean itemsDropped) implements CustomPacketPayload {
    public static final Type<RestoreGraveC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "restore_grave_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RestoreGraveC2SPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RestoreGraveC2SPacket::graveId,
            ByteBufCodecs.BOOL, RestoreGraveC2SPacket::itemsInGrave,
            ByteBufCodecs.BOOL, RestoreGraveC2SPacket::itemsDeleted,
            ByteBufCodecs.BOOL, RestoreGraveC2SPacket::itemsKept,
            ByteBufCodecs.BOOL, RestoreGraveC2SPacket::itemsDropped,
            RestoreGraveC2SPacket::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
