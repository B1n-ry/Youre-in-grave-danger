package com.b1n_ry.yigd.core;

import com.b1n_ry.yigd.client.gui.GraveSelectScreen;
import com.b1n_ry.yigd.client.gui.GraveViewScreen;
import com.b1n_ry.yigd.client.gui.PlayerSelectScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.nbt.NbtCompound;

import java.util.*;

public class ClientPacketReceivers {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.SINGLE_GRAVE_GUI, (client, handler, buf, responseSender) -> {
            if (client == null) return;
            NbtCompound nbtData = buf.readNbt();
            GraveViewScreen.getKeysFromGui = buf.readBoolean();
            GraveViewScreen.unlockableGraves = buf.readBoolean();
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
//
//            int listSize = buf.readInt();
//            List<DeadPlayerData> deadUserData = new ArrayList<>();
//            for (int i = 0; i < listSize; i++) {
//                NbtCompound nbtData = buf.readNbt();
//                deadUserData.add(DeadPlayerData.fromNbt(nbtData));
//            }
//            GraveViewScreen.getKeysFromGui = buf.readBoolean();
//            GraveViewScreen.unlockableGraves = buf.readBoolean();
//
//            GraveViewScreen.unlockedGraves.clear();
//            int unlockedGraveSize = buf.readInt();
//            for (int i = 0; i < unlockedGraveSize; i++) {
//                UUID uuid = buf.readUuid();
//                GraveViewScreen.unlockedGraves.add(uuid);
//            }
//
//            client.execute(() -> {
//                GraveSelectScreen screen = new GraveSelectScreen(deadUserData, 1, null);
//                MinecraftClient.getInstance().setScreen(screen);
//            });

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

//            int mapSize = buf.readInt();
//            Map<UUID, List<DeadPlayerData>> data = new HashMap<>();
//            for (int i = 0; i < mapSize; i++) {
//                UUID uuid = buf.readUuid();
//                int listSize = buf.readInt();
//                List<DeadPlayerData> userData = new ArrayList<>();
//                for (int n = 0; n < listSize; n++) {
//                    NbtCompound nbt = buf.readNbt();
//                    userData.add(DeadPlayerData.fromNbt(nbt));
//                }
//                data.put(uuid, userData);
//            }
//            GraveViewScreen.getKeysFromGui = buf.readBoolean();
//            GraveViewScreen.unlockableGraves = buf.readBoolean();
//
//            GraveViewScreen.unlockedGraves.clear();
//            int unlockedGraveSize = buf.readInt();
//            for (int i = 0; i < unlockedGraveSize; i++) {
//                UUID uuid = buf.readUuid();
//                GraveViewScreen.unlockedGraves.add(uuid);
//            }
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
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.KEY_AND_UNLOCK_CONFIG, (client, handler, buf, responseSender) -> {
            GraveViewScreen.getKeysFromGui = buf.readBoolean();
            GraveViewScreen.unlockableGraves = buf.readBoolean();
        });
    }
}
