package com.b1n_ry.yigd.packets;

import com.b1n_ry.yigd.client.gui.GraveOverviewGui;
import com.b1n_ry.yigd.client.gui.GraveSelectionGui;
import com.b1n_ry.yigd.client.gui.PlayerSelectionGui;
import com.b1n_ry.yigd.client.gui.screens.GraveOverviewScreen;
import com.b1n_ry.yigd.client.gui.screens.GraveSelectionScreen;
import com.b1n_ry.yigd.client.gui.screens.PlayerSelectionScreen;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClientPacketHandler {
    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.GRAVE_OVERVIEW_S2C, (client, handler, buf, responseSender) -> {
            NbtCompound componentNbt = buf.readNbt();
            if (componentNbt == null) return;

            boolean canRestore = buf.readBoolean();
            boolean canRob = buf.readBoolean();
            boolean canDelete = buf.readBoolean();
            boolean canUnlock = buf.readBoolean();
            boolean obtainableKeys = buf.readBoolean();
            boolean obtainableCompass = buf.readBoolean();

            GraveComponent component = GraveComponent.fromNbt(componentNbt, null);

            // Set screen on client thread
            client.execute(() -> client.setScreen(new GraveOverviewScreen(new GraveOverviewGui(component,
                    client.currentScreen, canRestore, canRob, canDelete, canUnlock, obtainableKeys, obtainableCompass))));
        });

        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.GRAVE_SELECTION_S2C, (client, handler, buf, responseSender) -> {
            int size = buf.readInt();
            List<LightGraveData> data = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                NbtCompound nbtData = buf.readNbt();
                if (nbtData == null) continue;

                data.add(LightGraveData.fromNbt(nbtData));
            }

            GameProfile profile = buf.readGameProfile();

            client.execute(() -> client.setScreen(new GraveSelectionScreen(new GraveSelectionGui(data, profile, client.currentScreen))));
        });

        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.PLAYER_SELECTION_S2C, (client, handler, buf, responseSender) -> {
            int size = buf.readInt();
            List<LightPlayerData> data = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                NbtCompound nbtData = buf.readNbt();
                if (nbtData == null) continue;

                data.add(LightPlayerData.fromNbt(nbtData));
            }

            client.execute(() -> client.setScreen(new PlayerSelectionScreen(new PlayerSelectionGui(data, client.currentScreen))));
        });
    }

    public static void sendRestoreGraveRequestPacket(UUID graveId, boolean itemsInGrave, boolean itemsDeleted, boolean itemsKept, boolean itemsDropped) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(graveId);
        buf.writeBoolean(itemsInGrave);
        buf.writeBoolean(itemsDeleted);
        buf.writeBoolean(itemsKept);
        buf.writeBoolean(itemsDropped);
        ClientPlayNetworking.send(PacketIdentifiers.GRAVE_RESTORE_C2S, buf);
    }
    public static void sendRobGraveRequestPacket(UUID graveId, boolean itemsInGrave, boolean itemsDeleted, boolean itemsKept, boolean itemsDropped) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(graveId);
        buf.writeBoolean(itemsInGrave);
        buf.writeBoolean(itemsDeleted);
        buf.writeBoolean(itemsKept);
        buf.writeBoolean(itemsDropped);
        ClientPlayNetworking.send(PacketIdentifiers.GRAVE_ROBBING_C2S, buf);
    }
    public static void sendDeleteGraveRequestPacket(UUID graveId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(graveId);
        ClientPlayNetworking.send(PacketIdentifiers.GRAVE_DELETE_C2S, buf);
    }
    public static void sendGraveLockRequestPacket(UUID graveId, boolean locked) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(graveId);
        buf.writeBoolean(locked);
        ClientPlayNetworking.send(PacketIdentifiers.GRAVE_LOCKING_C2S, buf);
    }
    public static void sendObtainKeysRequestPacket(UUID graveId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(graveId);
        ClientPlayNetworking.send(PacketIdentifiers.GRAVE_OBTAIN_KEYS_C2S, buf);
    }
    public static void sendObtainCompassRequestPacket(UUID graveId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(graveId);
        ClientPlayNetworking.send(PacketIdentifiers.GRAVE_OBTAIN_COMPASS_C2S, buf);
    }
    public static void sendGraveOverviewRequest(UUID graveId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(graveId);
        ClientPlayNetworking.send(PacketIdentifiers.GRAVE_OVERVIEW_REQUEST_C2S, buf);
    }
    public static void sendGraveSelectionRequest(GameProfile profile) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeGameProfile(profile);
        ClientPlayNetworking.send(PacketIdentifiers.GRAVE_SELECT_REQUEST_C2S, buf);
    }
    public static void sendConfigUpdate(YigdConfig config) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeEnumConstant(config.graveConfig.claimPriority);
        buf.writeEnumConstant(config.graveConfig.graveRobbing.robPriority);

        ClientPlayNetworking.send(PacketIdentifiers.CONFIG_UPDATE_C2S, buf);
    }
}
