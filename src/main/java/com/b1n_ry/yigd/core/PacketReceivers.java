package com.b1n_ry.yigd.core;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.client.gui.GraveSelectScreen;
import com.b1n_ry.yigd.client.gui.GraveViewScreen;
import com.b1n_ry.yigd.client.gui.PlayerSelectScreen;
import com.b1n_ry.yigd.config.PriorityInventoryConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.util.*;

public class PacketReceivers {
    public static final Identifier CONFIG_UPDATE = new Identifier("yigd", "config_update");
    public static final Identifier RESTORE_INVENTORY = new Identifier("yigd", "restore_inventory");
    public static final Identifier DELETE_GRAVE = new Identifier("yigd", "delete_grave");

    public static final Identifier SINGLE_GRAVE_GUI = new Identifier("yigd", "single_grave");
    public static final Identifier PLAYER_GRAVES_GUI = new Identifier("yigd", "single_dead_guy");
    public static final Identifier ALL_PLAYER_GRAVES = new Identifier("yigd", "all_dead_people");

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(CONFIG_UPDATE, (server, player, handler, buf, responseSender) -> {
            if (player == null) return;
            PriorityInventoryConfig normalPriority = buf.readEnumConstant(PriorityInventoryConfig.class);
            PriorityInventoryConfig robbingPriority = buf.readEnumConstant(PriorityInventoryConfig.class);

            UUID playerId = player.getUuid();
            Yigd.clientPriorities.put(playerId, normalPriority);
            Yigd.clientRobPriorities.put(playerId, robbingPriority);

            Yigd.LOGGER.info("Priority overwritten for player " + player.getDisplayName().asString() + ". Normal: " + normalPriority.name() + " / Robbing: " + robbingPriority.name());
        });
        ServerPlayNetworking.registerGlobalReceiver(RESTORE_INVENTORY, (server, player, handler, buf, responseSender) -> {
            if (player == null) return;
            UUID graveOwnerId = buf.readUuid();
            UUID graveId = buf.readUuid();

            PlayerEntity graveOwner = server.getPlayerManager().getPlayer(graveOwnerId);
            if (graveOwner != null) {
                YigdCommand.restoreGrave(graveOwner, player, graveId);
            } else {
                player.sendMessage(new TranslatableText("text.yigd.message.backup.restore.fail").styled(style -> style.withColor(0xFF0000)), false);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(DELETE_GRAVE, (server, player, handler, buf, responseSender) -> {
            if (player == null) return;
            UUID graveOwnerId = buf.readUuid();
            UUID graveId = buf.readUuid();

            List<DeadPlayerData> deadPlayerData = DeathInfoManager.INSTANCE.data.get(graveOwnerId);
            for (DeadPlayerData data : deadPlayerData) {
                if (!data.id.equals(graveId)) continue;
                deadPlayerData.remove(data);

                player.sendMessage(new TranslatableText("text.yigd.message.backup.delete_one"), false);
                break;
            }
            DeathInfoManager.INSTANCE.markDirty();
        });
    }

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(SINGLE_GRAVE_GUI, (client, handler, buf, responseSender) -> {
            if (client == null) return;
            NbtCompound nbtData = buf.readNbt();
            DeadPlayerData data = DeadPlayerData.fromNbt(nbtData);

            client.execute(() -> {
                GraveViewScreen screen = new GraveViewScreen(data, null);
                MinecraftClient.getInstance().setScreen(screen);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(PLAYER_GRAVES_GUI, (client, handler, buf, responseSender) -> {
            if (client == null) return;

            int listSize = buf.readInt();
            List<DeadPlayerData> deadUserData = new ArrayList<>();
            for (int i = 0; i < listSize; i++) {
                NbtCompound nbtData = buf.readNbt();
                deadUserData.add(DeadPlayerData.fromNbt(nbtData));
            }

            client.execute(() -> {
                GraveSelectScreen screen = new GraveSelectScreen(deadUserData, 1, null);
                MinecraftClient.getInstance().setScreen(screen);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(ALL_PLAYER_GRAVES, (client, handler, buf, responseSender) -> {
            if (client == null) return;

            int mapSize = buf.readInt();
            Map<UUID, List<DeadPlayerData>> data = new HashMap<>();
            for (int i = 0; i < mapSize; i++) {
                UUID uuid = buf.readUuid();
                int listSize = buf.readInt();
                List<DeadPlayerData> userData = new ArrayList<>();
                for (int n = 0; n < listSize; n++) {
                    NbtCompound nbt = buf.readNbt();
                    userData.add(DeadPlayerData.fromNbt(nbt));
                }
                data.put(uuid, userData);
            }

            client.execute(() -> {
                PlayerSelectScreen screen = new PlayerSelectScreen(data, 1);
                client.setScreen(screen);
            });
        });
    }
}
