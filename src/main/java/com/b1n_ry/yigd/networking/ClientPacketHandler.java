package com.b1n_ry.yigd.networking;

import com.b1n_ry.yigd.client.gui.GraveOverviewScreen;
import com.b1n_ry.yigd.client.gui.GraveSelectionScreen;
import com.b1n_ry.yigd.client.gui.PlayerSelectionScreen;
import com.b1n_ry.yigd.networking.packets.GraveOverviewS2CPacket;
import com.b1n_ry.yigd.networking.packets.GraveSelectionS2CPacket;
import com.b1n_ry.yigd.networking.packets.PlayerSelectionS2CPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPacketHandler {
    public static void graveOverview(GraveOverviewS2CPacket payload, IPayloadContext context) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.setScreen(new GraveOverviewScreen(payload.component(), client.screen,
                payload.canRestore(), payload.canRob(), payload.canDelete(), payload.canUnlock(),
                payload.obtainableKeys(), payload.obtainableCompass())));
    }
    public static void graveSelection(GraveSelectionS2CPacket payload, IPayloadContext context) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.setScreen(new GraveSelectionScreen(payload.data(), payload.profile(), client.screen)));
    }
    public static void playerSelection(PlayerSelectionS2CPacket payload, IPayloadContext context) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.setScreen(new PlayerSelectionScreen(payload.data(), client.screen)));
    }
}
