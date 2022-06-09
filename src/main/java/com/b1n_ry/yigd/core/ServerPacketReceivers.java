package com.b1n_ry.yigd.core;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.PriorityInventoryConfig;
import com.b1n_ry.yigd.item.KeyItem;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.message.MessageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;

import java.util.List;
import java.util.UUID;

public class ServerPacketReceivers {
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.CONFIG_UPDATE, (server, player, handler, buf, responseSender) -> {
            if (player == null) return;
            PriorityInventoryConfig normalPriority = buf.readEnumConstant(PriorityInventoryConfig.class);
            PriorityInventoryConfig robbingPriority = buf.readEnumConstant(PriorityInventoryConfig.class);

            UUID playerId = player.getUuid();
            Yigd.clientPriorities.put(playerId, normalPriority);
            Yigd.clientRobPriorities.put(playerId, robbingPriority);

            Yigd.LOGGER.info("Priority overwritten for player " + player.getDisplayName().getString() + ". Normal: " + normalPriority.name() + " / Robbing: " + robbingPriority.name());
        });
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.RESTORE_INVENTORY, (server, player, handler, buf, responseSender) -> {
            if (player == null) return;
            UUID graveOwnerId = buf.readUuid();
            UUID graveId = buf.readUuid();

            ServerPlayerEntity graveOwner = server.getPlayerManager().getPlayer(graveOwnerId);
            if (graveOwner != null) {
                YigdCommand.restoreGrave(graveOwner, player, graveId);
            } else {
                player.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.backup.restore.fail")).styled(style -> style.withColor(0xFF0000)), MessageType.SYSTEM);
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.DELETE_GRAVE, (server, player, handler, buf, responseSender) -> {
            if (player == null) return;
            UUID graveOwnerId = buf.readUuid();
            UUID graveId = buf.readUuid();

            List<DeadPlayerData> deadPlayerData = DeathInfoManager.INSTANCE.data.get(graveOwnerId);
            if (deadPlayerData == null) return;
            for (DeadPlayerData data : deadPlayerData) {
                if (!data.id.equals(graveId)) continue;
                deadPlayerData.remove(data);

                player.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.backup.delete_one")), MessageType.SYSTEM);
                break;
            }
            DeathInfoManager.INSTANCE.markDirty();
        });
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.ROB_GRAVE, (server, player, handler, buf, responseSender) -> {
            if (player == null) return;
            String ownerName = buf.readString();
            UUID ownerId = buf.readUuid();
            UUID graveId = buf.readUuid();

            GameProfile gameProfile = new GameProfile(ownerId, ownerName);
            YigdCommand.robGrave(gameProfile, player, graveId);
        });
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.GIVE_KEY_ITEM, (server, player, handler, buf, responseSender) -> {
            if (player == null) return;
            UUID userId = buf.readUuid();
            UUID graveId = buf.readUuid();

            KeyItem.giveStackToPlayer(player, userId, graveId);
        });
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.SET_GRAVE_LOCK, (server, player, handler, buf, responseSender) -> {
            UUID graveId = buf.readUuid();
            boolean graveLocked = buf.readBoolean();

            if (graveLocked) {
                DeathInfoManager.INSTANCE.unlockedGraves.remove(graveId);
            } else if (!DeathInfoManager.INSTANCE.unlockedGraves.contains(graveId)) {
                DeathInfoManager.INSTANCE.unlockedGraves.add(graveId);
            }
        });
    }
}
