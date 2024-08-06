package com.b1n_ry.yigd.networking.packets;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.ClaimPriority;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UpdateConfigC2SPacket(ClaimPriority claiming, ClaimPriority robbing) implements CustomPayload {
    public static final Id<UpdateConfigC2SPacket> ID = new Id<>(Identifier.of(Yigd.MOD_ID, "update_config"));
    public static final PacketCodec<RegistryByteBuf, UpdateConfigC2SPacket> CODEC = PacketCodec.of(UpdateConfigC2SPacket::write, UpdateConfigC2SPacket::new);

    @Override
    public Id<UpdateConfigC2SPacket> getId() {
        return ID;
    }

    public UpdateConfigC2SPacket(RegistryByteBuf buf) {
        this(buf.readEnumConstant(ClaimPriority.class), buf.readEnumConstant(ClaimPriority.class));
    }
    public void write(RegistryByteBuf buf) {
        buf.writeEnumConstant(this.claiming);
        buf.writeEnumConstant(this.robbing);
    }
}
