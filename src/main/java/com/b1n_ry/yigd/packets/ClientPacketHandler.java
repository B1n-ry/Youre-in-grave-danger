package com.b1n_ry.yigd.packets;

import com.b1n_ry.yigd.client.gui.GraveOverviewGui;
import com.b1n_ry.yigd.client.screens.GraveOverviewScreen;
import com.b1n_ry.yigd.components.GraveComponent;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;

public class ClientPacketHandler {
    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.GRAVE_OVERVIEW_S2C, (client, handler, buf, responseSender) -> {
            NbtCompound componentNbt = buf.readNbt();
            if (componentNbt == null) return;

            GraveComponent component = GraveComponent.fromNbt(componentNbt, null);

            // Set screen on client thread
            client.execute(() -> client.setScreen(new GraveOverviewScreen(new GraveOverviewGui(component, client.currentScreen))));
        });
    }
}
