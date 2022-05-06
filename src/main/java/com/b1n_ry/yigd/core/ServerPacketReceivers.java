package com.b1n_ry.yigd.core;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.PriorityInventoryConfig;
import com.b1n_ry.yigd.item.KeyItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.UUID;

public class ServerPacketReceivers {
    public static final Identifier CONFIG_UPDATE = new Identifier("yigd", "config_update");
    public static final Identifier RESTORE_INVENTORY = new Identifier("yigd", "restore_inventory");
    public static final Identifier DELETE_GRAVE = new Identifier("yigd", "delete_grave");
    public static final Identifier GIVE_KEY_ITEM = new Identifier("yigd", "give_key_item");
    public static final Identifier SET_GRAVE_LOCK = new Identifier("yigd", "set_grave_lock");

    public static void register() {
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
        ServerPlayNetworking.registerGlobalReceiver(GIVE_KEY_ITEM, (server, player, handler, buf, responseSender) -> {
            if (player == null) return;
            UUID userId = buf.readUuid();
            UUID graveId = buf.readUuid();

            KeyItem.giveStackToPlayer(player, userId, graveId);
        });
        ServerPlayNetworking.registerGlobalReceiver(SET_GRAVE_LOCK, (server, player, handler, buf, responseSender) -> {
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