package com.b1n_ry.yigd.core;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.PriorityInventoryConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.item.KeyItem;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
                player.sendMessage(Text.translatable("text.yigd.message.backup.restore.fail").styled(style -> style.withColor(0xFF0000)), false);
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

                player.sendMessage(Text.translatable("text.yigd.message.backup.delete_one"), false);
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
            DeathInfoManager.INSTANCE.markDirty();
        });

        // Request listeners
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.PLAYER_GRAVES_GUI, (server, player, handler, serverBuf, responseSender) -> {
            GameProfile profile = serverBuf.readGameProfile();

            List<DeadPlayerData> graveData = DeathInfoManager.INSTANCE.data.get(profile.getId());
            if (graveData == null) return;

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeGameProfile(profile);
            buf.writeInt(graveData.size());
            for (DeadPlayerData grave : graveData) {
                int itemCount = 0;
                for (ItemStack stack : grave.inventory) {
                    if (!stack.isEmpty()) itemCount++;
                }
                for (YigdApi yigdApi : Yigd.apiMods) {
                    String modName = yigdApi.getModName();
                    if (!grave.modInventories.containsKey(modName)) continue;

                    itemCount += yigdApi.getInventorySize(grave.modInventories.get(modName));
                }

                int points = grave.xp;
                int i;
                for (i = 0; points >= 0; i++) {
                    if (i < 16) points -= (2 * i) + 7;
                    else if (i < 31) points -= (5 * i) - 38;
                    else points -= (9 * i) - 158;
                }

                buf.writeUuid(grave.id);
                buf.writeBlockPos(grave.gravePos);
                buf.writeString(grave.dimensionName);
                buf.writeInt(itemCount);
                buf.writeInt(i - 1);
                buf.writeByte(grave.availability);
            }

            responseSender.sendPacket(PacketIdentifiers.PLAYER_GRAVES_GUI, buf);
        });
        ServerPlayNetworking.registerGlobalReceiver(PacketIdentifiers.SINGLE_GRAVE_GUI, (server, player, handler, serverBuf, responseSender) -> {
            GameProfile profile = serverBuf.readGameProfile();
            UUID graveId = serverBuf.readUuid();

            YigdConfig config = YigdConfig.getConfig();
            YigdConfig.GraveKeySettings keySettings = config.utilitySettings.graveKeySettings;

            List<DeadPlayerData> data = DeathInfoManager.INSTANCE.data.get(profile.getId());
            if (data == null) return;

            DeadPlayerData graveData = null;
            for (DeadPlayerData grave : data) {
                if (!grave.id.equals(graveId)) continue;

                graveData = grave;
                break;
            }

            if (graveData == null) return;
            NbtCompound graveNbt = graveData.toNbt();

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeNbt(graveNbt);
            buf.writeBoolean(keySettings.enableKeys && keySettings.getFromGui);
            buf.writeBoolean(config.graveSettings.unlockableGraves);
            buf.writeBoolean(config.commandToggles.retrieveGrave && YigdCommand.hasPermission(player, "yigd.command.restore"));
            buf.writeBoolean(YigdCommand.hasPermission(player, "yigd.command.delete"));
            buf.writeBoolean(config.commandToggles.robGrave && YigdCommand.hasPermission(player, "yigd.command.rob"));

            buf.writeBoolean(DeathInfoManager.INSTANCE.unlockedGraves.contains(graveId));
            buf.writeBoolean(config.graveSettings.graveRobbing.tellRobber);

            responseSender.sendPacket(PacketIdentifiers.SINGLE_GRAVE_GUI, buf);
        });
    }
}
