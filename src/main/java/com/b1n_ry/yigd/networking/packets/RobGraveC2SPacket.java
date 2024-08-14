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

public record RobGraveC2SPacket(UUID graveId, boolean itemsInGrave, boolean itemsDeleted, boolean itemsKept,
                                boolean itemsDropped) implements CustomPacketPayload {
    public static final Type<RobGraveC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "rob_grave_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RobGraveC2SPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RobGraveC2SPacket::graveId,
            ByteBufCodecs.BOOL, RobGraveC2SPacket::itemsInGrave,
            ByteBufCodecs.BOOL, RobGraveC2SPacket::itemsDeleted,
            ByteBufCodecs.BOOL, RobGraveC2SPacket::itemsKept,
            ByteBufCodecs.BOOL, RobGraveC2SPacket::itemsDropped,
            RobGraveC2SPacket::new);

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
