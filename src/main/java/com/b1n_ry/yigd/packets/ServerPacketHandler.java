package com.b1n_ry.yigd.packets;

import com.b1n_ry.yigd.components.GraveComponent;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerPacketHandler {
    public static void registerReceivers() {

    }

    public static void sendGraveOverviewPacket(ServerPlayerEntity player, GraveComponent component) {
        PacketByteBuf buf = PacketByteBufs.create()
                .writeNbt(component.toNbt());

        ServerPlayNetworking.send(player, PacketIdentifiers.GRAVE_OVERVIEW_S2C, buf);
    }
}
