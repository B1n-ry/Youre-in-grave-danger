package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record RestoreGraveC2SPacket(UUID graveId, boolean itemsInGrave, boolean itemsDeleted, boolean itemsKept,
                                    boolean itemsDropped) implements CustomPacketPayload {
    public static final Type<RestoreGraveC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "restore_grave_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RestoreGraveC2SPacket> STREAM_CODEC = StreamCodec.ofMember(RestoreGraveC2SPacket::write, RestoreGraveC2SPacket::new);

    @Override
    public @NotNull Type<RestoreGraveC2SPacket> type() {
        return TYPE;
    }

    public RestoreGraveC2SPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.graveId);
        buf.writeBoolean(this.itemsInGrave);
        buf.writeBoolean(this.itemsDeleted);
        buf.writeBoolean(this.itemsKept);
        buf.writeBoolean(this.itemsDropped);
    }
}
