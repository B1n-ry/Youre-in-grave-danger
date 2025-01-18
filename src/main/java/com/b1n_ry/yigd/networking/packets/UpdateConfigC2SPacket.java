package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.ClaimPriority;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record UpdateConfigC2SPacket(ClaimPriority claiming, ClaimPriority robbing) implements CustomPacketPayload {
    public static final Type<UpdateConfigC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "update_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateConfigC2SPacket> STREAM_CODEC = StreamCodec.ofMember(UpdateConfigC2SPacket::write, UpdateConfigC2SPacket::new);

    @Override
    public @NotNull Type<UpdateConfigC2SPacket> type() {
        return TYPE;
    }

    public UpdateConfigC2SPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readEnum(ClaimPriority.class), buf.readEnum(ClaimPriority.class));
    }
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeEnum(this.claiming);
        buf.writeEnum(this.robbing);
    }
}
