package com.b1n_ry.yigd.networking;

import com.b1n_ry.yigd.networking.packets.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class PacketInitializer {
    public static void init() {
        PayloadTypeRegistry.playC2S().register(DeleteGraveC2SPacket.ID, DeleteGraveC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(GraveOverviewRequestC2SPacket.ID, GraveOverviewRequestC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(GraveSelectionRequestC2SPacket.ID, GraveSelectionRequestC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(LockGraveC2SPacket.ID, LockGraveC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestCompassC2SPacket.ID, RequestCompassC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestKeyC2SPacket.ID, RequestKeyC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RestoreGraveC2SPacket.ID, RestoreGraveC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(RobGraveC2SPacket.ID, RobGraveC2SPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateConfigC2SPacket.ID, UpdateConfigC2SPacket.CODEC);

        PayloadTypeRegistry.playS2C().register(GraveOverviewS2CPacket.ID, GraveOverviewS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(GraveSelectionS2CPacket.ID, GraveSelectionS2CPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerSelectionS2CPacket.ID, PlayerSelectionS2CPacket.CODEC);
    }
}
