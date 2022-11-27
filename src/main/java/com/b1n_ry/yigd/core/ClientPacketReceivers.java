package com.b1n_ry.yigd.core;

import com.b1n_ry.yigd.client.gui.GraveSelectScreen;
import com.b1n_ry.yigd.client.gui.GraveViewScreen;
import com.b1n_ry.yigd.client.gui.PlayerSelectScreen;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;

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

            boolean unlocked = buf.readBoolean();
            boolean showGraveRobber = buf.readBoolean();

            DeadPlayerData data = DeadPlayerData.fromNbt(nbtData);

            client.execute(() -> {
                GraveViewScreen screen = new GraveViewScreen(data, unlocked, showGraveRobber, client.currentScreen);
                MinecraftClient.getInstance().setScreen(screen);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.PLAYER_GRAVES_GUI, (client, handler, buf, responseSender) -> {
            if (client == null) return;

            NbtCompound profileNbt = buf.readNbt();
            if (profileNbt == null) {
                return;
            }
            GameProfile profile = NbtHelper.toGameProfile(profileNbt);
            int graveCount = buf.readInt();
            Map<UUID, GraveSelectScreen.GraveGuiInfo> graves = new HashMap<>();
            for (int i = 0; i < graveCount; i++) {
                UUID graveId = buf.readUuid();
                BlockPos pos = buf.readBlockPos();
                String dimensionName = buf.readString();
                int itemCount = buf.readInt();
                int levelCount = buf.readInt();
                byte availability = buf.readByte();

                GraveSelectScreen.GraveGuiInfo info = new GraveSelectScreen.GraveGuiInfo(pos, dimensionName, itemCount, levelCount, availability);

                graves.put(graveId, info);
            }

            client.execute(() -> {
                GraveSelectScreen screen = new GraveSelectScreen(profile, graves, 1, client.currentScreen);
                client.setScreen(screen);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(PacketIdentifiers.ALL_PLAYER_GRAVES, (client, handler, buf, responseSender) -> {
            if (client == null) return;

            List<GameProfile> gameProfiles = new ArrayList<>();
            Map<UUID, List<Byte>> graveAvailabilities = new HashMap<>();

            int playerCount = buf.readInt();
            for (int i = 0; i < playerCount; i++) {
                NbtCompound profileNbt = buf.readNbt();
                if (profileNbt != null) {
                    GameProfile profile = NbtHelper.toGameProfile(profileNbt);

                    if (profile != null) {
                        List<Byte> bytes = new ArrayList<>();
                        int graveCount = buf.readInt();
                        for (int n = 0; n < graveCount; n++) {
                            byte availability = buf.readByte();
                            bytes.add(availability);
                        }

                        gameProfiles.add(profile);
                        graveAvailabilities.put(profile.getId(), bytes);
                    }
                }
            }

            client.execute(() -> {
                PlayerSelectScreen screen = new PlayerSelectScreen(gameProfiles, graveAvailabilities, 1);
                client.setScreen(screen);
            });
        });
    }
}
