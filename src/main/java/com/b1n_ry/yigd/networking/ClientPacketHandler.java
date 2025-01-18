package com.b1n_ry.yigd.networking;

import com.b1n_ry.yigd.client.gui.GraveOverviewGui;
import com.b1n_ry.yigd.client.gui.GraveSelectionGui;
import com.b1n_ry.yigd.client.gui.PlayerSelectionGui;
import com.b1n_ry.yigd.client.gui.screens.GraveOverviewScreen;
import com.b1n_ry.yigd.client.gui.screens.GraveSelectionScreen;
import com.b1n_ry.yigd.client.gui.screens.PlayerSelectionScreen;
import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.networking.packets.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.List;
import java.util.UUID;

public class ClientPacketHandler {
    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(GraveOverviewS2CPacket.TYPE, (payload, context) -> {
            GraveComponent component = payload.component();
            boolean canRestore = payload.canRestore();
            boolean canRob = payload.canRob();
            boolean canDelete = payload.canDelete();
            boolean canUnlock = payload.canUnlock();
            boolean obtainableKeys = payload.obtainableKeys();
            boolean obtainableCompass = payload.obtainableCompass();

            Minecraft client = context.client();
            // Set screen on client thread
            client.execute(() -> client.setScreen(new GraveOverviewScreen(new GraveOverviewGui(component,
                    client.screen, canRestore, canRob, canDelete, canUnlock,
                    obtainableKeys, obtainableCompass))));
        });
        ClientPlayNetworking.registerGlobalReceiver(GraveSelectionS2CPacket.TYPE, (payload, context) -> {
            List<LightGraveData> data = payload.data();
            ResolvableProfile profile = payload.owner();

            Minecraft client = context.client();
            client.execute(() -> client.setScreen(new GraveSelectionScreen(new GraveSelectionGui(data, profile, client.screen))));
        });
        ClientPlayNetworking.registerGlobalReceiver(PlayerSelectionS2CPacket.TYPE, (payload, context) -> {
            List<LightPlayerData> data = payload.data();

            Minecraft client = context.client();
            client.execute(() -> client.setScreen(new PlayerSelectionScreen(new PlayerSelectionGui(data, client.screen))));
        });
        ClientPlayNetworking.registerGlobalReceiver(SyncConfigS2CPacket.TYPE, (payload, context) -> {
            YigdConfig.getConfig().graveConfig.retrieveMethods.onBreak = payload.gravesBreakable();
            GraveBlockEntityRenderer.syncedGlowing = payload.glowingGraves();
            GraveBlockEntityRenderer.syncedGlowingMaxDistance = payload.maxGraveGlowingDistance();
            GraveBlockEntityRenderer.syncedDeathSightDistance = payload.deathSightDistance();
        });
    }

    public static void sendRestoreGraveRequestPacket(UUID graveId, boolean itemsInGrave, boolean itemsDeleted, boolean itemsKept, boolean itemsDropped) {
        ClientPlayNetworking.send(new RestoreGraveC2SPacket(graveId, itemsInGrave, itemsDeleted, itemsKept, itemsDropped));
    }
    public static void sendRobGraveRequestPacket(UUID graveId, boolean itemsInGrave, boolean itemsDeleted, boolean itemsKept, boolean itemsDropped) {
        ClientPlayNetworking.send(new RobGraveC2SPacket(graveId, itemsInGrave, itemsDeleted, itemsKept, itemsDropped));
    }
    public static void sendDeleteGraveRequestPacket(UUID graveId) {
        ClientPlayNetworking.send(new DeleteGraveC2SPacket(graveId));
    }
    public static void sendGraveLockRequestPacket(UUID graveId, boolean locked) {
        ClientPlayNetworking.send(new LockGraveC2SPacket(graveId, locked));
    }
    public static void sendObtainKeysRequestPacket(UUID graveId) {
        ClientPlayNetworking.send(new RequestKeyC2SPacket(graveId));
    }
    public static void sendObtainCompassRequestPacket(UUID graveId) {
        ClientPlayNetworking.send(new RequestCompassC2SPacket(graveId));
    }
    public static void sendGraveOverviewRequest(UUID graveId) {
        ClientPlayNetworking.send(new GraveOverviewRequestC2SPacket(graveId));
    }
    public static void sendGraveSelectionRequest(ResolvableProfile profile) {
        ClientPlayNetworking.send(new GraveSelectionRequestC2SPacket(profile));
    }
    public static void sendConfigUpdate(YigdConfig config) {
        ClientPlayNetworking.send(new UpdateConfigC2SPacket(config.graveConfig.claimPriority, config.graveConfig.graveRobbing.robPriority));
    }
}
