package com.b1n_ry.yigd.core;

import com.b1n_ry.yigd.client.gui.GraveSelectScreen;
import com.b1n_ry.yigd.client.gui.GraveViewScreen;
import com.b1n_ry.yigd.client.gui.PlayerSelectScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;

import java.util.*;

public class ClientPacketReceivers {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.SINGLE_GRAVE_GUI, (client, handler, buf, responseSender) -> {
            if (client == null) return;
            NbtCompound nbtData = buf.readNbt();
            GraveViewScreen.Permissions.giveKey = buf.readBoolean();
            GraveViewScreen.Permissions.toggleLock = buf.readBoolean();
            GraveViewScreen.Permissions.restore = buf.readBoolean();
            GraveViewScreen.Permissions.delete = buf.readBoolean();
            GraveViewScreen.Permissions.rob = buf.readBoolean();

            DeadPlayerData data = DeadPlayerData.fromNbt(nbtData);

            GraveViewScreen.unlockedGraves.clear();
            int unlockedGraveSize = buf.readInt();
            for (int i = 0; i < unlockedGraveSize; i++) {
                UUID uuid = buf.readUuid();
                GraveViewScreen.unlockedGraves.add(uuid);
            }

            client.execute(() -> {
                GraveViewScreen screen = new GraveViewScreen(data, null);
                MinecraftClient.getInstance().setScreen(screen);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.PLAYER_GRAVES_GUI, (client, handler, buf, responseSender) -> {
            if (client == null) return;

            UUID userId = buf.readUuid();
            DeadPlayerData data = DeadPlayerData.fromNbt(buf.readNbt());
            boolean isUnlocked = buf.readBoolean();

            client.execute(() -> {
                if (isUnlocked) GraveViewScreen.unlockedGraves.add(data.id);

                if (client.currentScreen instanceof GraveSelectScreen openScreen) {
                    openScreen.addData(userId, data);
                } else {
                        GraveSelectScreen screen = new GraveSelectScreen(List.of(data), 1, client.currentScreen);
                        client.setScreen(screen);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.ALL_PLAYER_GRAVES, (client, handler, buf, responseSender) -> {
            if (client == null) return;

            UUID userId = buf.readUuid();
            DeadPlayerData data = DeadPlayerData.fromNbt(buf.readNbt());
            boolean isUnlocked = buf.readBoolean();

            client.execute(() -> {
                if (isUnlocked) GraveViewScreen.unlockedGraves.add(data.id);

                if (client.currentScreen instanceof PlayerSelectScreen openScreen) {
                    openScreen.addData(userId, data);
                } else {
                    Map<UUID, List<DeadPlayerData>> dataMap = new HashMap<>();
                    dataMap.put(userId, List.of(data));

                        PlayerSelectScreen screen = new PlayerSelectScreen(dataMap, 1);
                        client.setScreen(screen);
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.GUI_CONFIGS, (client, handler, buf, responseSender) -> {
            GraveViewScreen.Permissions.giveKey = buf.readBoolean();
            GraveViewScreen.Permissions.toggleLock = buf.readBoolean();
            GraveViewScreen.showGraveRobber = buf.readBoolean();
            GraveViewScreen.Permissions.restore = buf.readBoolean();
            GraveViewScreen.Permissions.delete = buf.readBoolean();
            GraveViewScreen.Permissions.rob = buf.readBoolean();

            GraveViewScreen.unlockedGraves.clear();
            int unlockedGraveSize = buf.readInt();
            for (int i = 0; i < unlockedGraveSize; i++) {
                UUID uuid = buf.readUuid();
                GraveViewScreen.unlockedGraves.add(uuid);
            }
        });
    }
}
