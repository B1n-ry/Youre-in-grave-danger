package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.components.GraveComponent;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GraveOverviewS2CPacket(GraveComponent component, boolean canRestore, boolean canRob, boolean canDelete,
                                     boolean canUnlock, boolean obtainableKeys, boolean obtainableCompass) implements CustomPayload {
    public static final Id<GraveOverviewS2CPacket> ID = new Id<>(Identifier.of(Yigd.MOD_ID, "grave_overview"));
    public static final PacketCodec<RegistryByteBuf, GraveOverviewS2CPacket> CODEC = PacketCodec.of(GraveOverviewS2CPacket::write, GraveOverviewS2CPacket::new);

    @Override
    public Id<GraveOverviewS2CPacket> getId() {
        return ID;
    }

    private GraveOverviewS2CPacket(RegistryByteBuf buf) {
        this(GraveComponent.fromNbt(buf.readNbt(), buf.getRegistryManager(), null), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }
    private void write(RegistryByteBuf buf) {
        buf.writeNbt(this.component.toNbt(buf.getRegistryManager()));

        buf.writeBoolean(this.canRestore);
        buf.writeBoolean(this.canRob);
        buf.writeBoolean(this.canDelete);
        buf.writeBoolean(this.canUnlock);
        buf.writeBoolean(this.obtainableKeys);
        buf.writeBoolean(this.obtainableCompass);
    }
}
