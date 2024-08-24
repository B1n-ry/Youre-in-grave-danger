package com.b1n_ry.yigd.networking;

import com.b1n_ry.yigd.client.gui.GraveOverviewScreen;
import com.b1n_ry.yigd.client.gui.GraveSelectionScreen;
import com.b1n_ry.yigd.client.gui.PlayerSelectionScreen;
import com.b1n_ry.yigd.networking.packets.GraveOverviewS2CPacket;
import com.b1n_ry.yigd.networking.packets.GraveSelectionS2CPacket;
import com.b1n_ry.yigd.networking.packets.PlayerSelectionS2CPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPacketHandler {
    public static void graveOverview(GraveOverviewS2CPacket payload, IPayloadContext context) {
        GraveOverviewScreen.openScreen(payload);
    }
    public static void graveSelection(GraveSelectionS2CPacket payload, IPayloadContext context) {
        GraveSelectionScreen.openScreen(payload);
    }
    public static void playerSelection(PlayerSelectionS2CPacket payload, IPayloadContext context) {
        PlayerSelectionScreen.openScreen(payload);
    }
}
