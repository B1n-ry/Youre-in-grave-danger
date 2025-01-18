package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record GraveOverviewRequestC2SPacket(UUID graveId) implements CustomPacketPayload {
    public static final Type<GraveOverviewRequestC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "grave_overview_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GraveOverviewRequestC2SPacket> STREAM_CODEC = StreamCodec.ofMember(GraveOverviewRequestC2SPacket::write, GraveOverviewRequestC2SPacket::new);

    @Override
    public @NotNull Type<GraveOverviewRequestC2SPacket> type() {
        return TYPE;
    }

    public GraveOverviewRequestC2SPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readUUID());
    }
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.graveId);
    }
}
