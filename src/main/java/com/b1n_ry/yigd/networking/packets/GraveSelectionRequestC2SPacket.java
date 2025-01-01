package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.NotNull;

public record GraveSelectionRequestC2SPacket(ResolvableProfile profile) implements CustomPacketPayload {
    public static final Type<GraveSelectionRequestC2SPacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "grave_selection_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GraveSelectionRequestC2SPacket> CODEC = StreamCodec.ofMember(GraveSelectionRequestC2SPacket::write, GraveSelectionRequestC2SPacket::new);

    @Override
    public @NotNull Type<GraveSelectionRequestC2SPacket> type() {
        return ID;
    }

    public GraveSelectionRequestC2SPacket(RegistryFriendlyByteBuf buf) {
        this(ResolvableProfile.STREAM_CODEC.decode(buf));
    }
    public void write(RegistryFriendlyByteBuf buf) {
        ResolvableProfile.STREAM_CODEC.encode(buf, this.profile);
    }
}
