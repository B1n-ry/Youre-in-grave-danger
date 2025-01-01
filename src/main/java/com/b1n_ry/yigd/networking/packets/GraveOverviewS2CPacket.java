package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.components.GraveComponent;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record GraveOverviewS2CPacket(GraveComponent component, boolean canRestore, boolean canRob, boolean canDelete,
                                     boolean canUnlock, boolean obtainableKeys, boolean obtainableCompass) implements CustomPacketPayload {
    public static final Type<GraveOverviewS2CPacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "grave_overview"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GraveOverviewS2CPacket> CODEC = StreamCodec.ofMember(GraveOverviewS2CPacket::write, GraveOverviewS2CPacket::new);

    @Override
    public @NotNull Type<GraveOverviewS2CPacket> type() {
        return ID;
    }

    private GraveOverviewS2CPacket(RegistryFriendlyByteBuf buf) {
        this(GraveComponent.fromNbt(buf.readNbt(), buf.registryAccess(), null), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }
    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeNbt(this.component.toNbt(buf.registryAccess()));

        buf.writeBoolean(this.canRestore);
        buf.writeBoolean(this.canRob);
        buf.writeBoolean(this.canDelete);
        buf.writeBoolean(this.canUnlock);
        buf.writeBoolean(this.obtainableKeys);
        buf.writeBoolean(this.obtainableCompass);
    }
}
