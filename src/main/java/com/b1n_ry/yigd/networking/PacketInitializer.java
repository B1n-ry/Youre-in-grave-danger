package com.b1n_ry.yigd.networking;

import com.b1n_ry.yigd.networking.packets.*;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PacketInitializer {
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(DeleteGraveC2SPacket.TYPE, DeleteGraveC2SPacket.STREAM_CODEC, ServerPacketHandler::deleteGraveRequest);
        registrar.playToServer(GraveOverviewRequestC2SPacket.TYPE, GraveOverviewRequestC2SPacket.STREAM_CODEC, ServerPacketHandler::graveOverviewRequest);
        registrar.playToServer(GraveSelectionRequestC2SPacket.TYPE, GraveSelectionRequestC2SPacket.STREAM_CODEC, ServerPacketHandler::graveSelectionRequest);
        registrar.playToServer(LockGraveC2SPacket.TYPE, LockGraveC2SPacket.STREAM_CODEC, ServerPacketHandler::lockGrave);
        registrar.playToServer(RequestCompassC2SPacket.TYPE, RequestCompassC2SPacket.STREAM_CODEC, ServerPacketHandler::requestCompass);
        registrar.playToServer(RequestKeyC2SPacket.TYPE, RequestKeyC2SPacket.STREAM_CODEC, ServerPacketHandler::requestKey);
        registrar.playToServer(RestoreGraveC2SPacket.TYPE, RestoreGraveC2SPacket.STREAM_CODEC, ServerPacketHandler::restoreGrave);
        registrar.playToServer(RobGraveC2SPacket.TYPE, RobGraveC2SPacket.STREAM_CODEC, ServerPacketHandler::robGrave);
        registrar.playToServer(UpdateConfigC2SPacket.TYPE, UpdateConfigC2SPacket.STREAM_CODEC, ServerPacketHandler::updateConfig);

        registrar.playToClient(GraveOverviewS2CPacket.TYPE, GraveOverviewS2CPacket.STREAM_CODEC, ClientPacketHandler::graveOverview);
        registrar.playToClient(GraveSelectionS2CPacket.TYPE, GraveSelectionS2CPacket.STREAM_CODEC, ClientPacketHandler::graveSelection);
        registrar.playToClient(PlayerSelectionS2CPacket.TYPE, PlayerSelectionS2CPacket.STREAM_CODEC, ClientPacketHandler::playerSelection);
    }
}
