package com.b1n_ry.yigd.networking;

import com.b1n_ry.yigd.networking.packets.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class PacketInitializer {
    public static void init() {
        PayloadTypeRegistry.playC2S().register(DeleteGraveC2SPacket.TYPE, DeleteGraveC2SPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(GraveOverviewRequestC2SPacket.TYPE, GraveOverviewRequestC2SPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(GraveSelectionRequestC2SPacket.TYPE, GraveSelectionRequestC2SPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(LockGraveC2SPacket.TYPE, LockGraveC2SPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestCompassC2SPacket.TYPE, RequestCompassC2SPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RequestKeyC2SPacket.TYPE, RequestKeyC2SPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RestoreGraveC2SPacket.TYPE, RestoreGraveC2SPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(RobGraveC2SPacket.TYPE, RobGraveC2SPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateConfigC2SPacket.TYPE, UpdateConfigC2SPacket.STREAM_CODEC);

        PayloadTypeRegistry.playS2C().register(GraveOverviewS2CPacket.TYPE, GraveOverviewS2CPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(GraveSelectionS2CPacket.TYPE, GraveSelectionS2CPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(PlayerSelectionS2CPacket.TYPE, PlayerSelectionS2CPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncConfigS2CPacket.TYPE, SyncConfigS2CPacket.STREAM_CODEC);
    }
}
