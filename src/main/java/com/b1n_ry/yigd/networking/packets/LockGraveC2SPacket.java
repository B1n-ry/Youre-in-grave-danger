package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record LockGraveC2SPacket(UUID graveId, boolean locked) implements CustomPacketPayload {
    public static final Type<LockGraveC2SPacket> ID = new Type<>(ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "lock_grave_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LockGraveC2SPacket> CODEC = StreamCodec.ofMember(LockGraveC2SPacket::write, LockGraveC2SPacket::new);

    @Override
    public @NotNull Type<LockGraveC2SPacket> type() {
        return ID;
    }

    public LockGraveC2SPacket(RegistryFriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readBoolean());
    }
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.graveId);
        buf.writeBoolean(this.locked);
    }
}
