package com.b1n_ry.yigd.core;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.YigdConfig;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class YigdCommand {
    public static void registerCommands() {
        YigdConfig.CommandToggles config = YigdConfig.getConfig().commandToggles;

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(literal(config.coreCommandName)
                .executes(ctx -> viewGrave(ctx.getSource().getPlayer(), ctx.getSource().getPlayer()))
                .then(literal("restore")
                        .requires(source -> source.hasPermissionLevel(4) && config.retrieveGrave)
                        .then(argument("player", EntityArgumentType.player())
                                .executes(ctx -> restoreGrave(EntityArgumentType.getPlayer(ctx, "player"), ctx.getSource().getPlayer(), null))
                        )
                        .executes(ctx -> restoreGrave(ctx.getSource().getPlayer(), ctx.getSource().getPlayer(), null))
                )
                .then(literal("rob")
                        .requires(source -> source.hasPermissionLevel(4) && config.robGrave)
                        .then(argument("victim", EntityArgumentType.player())
                                .executes(ctx -> robGrave(EntityArgumentType.getPlayer(ctx, "victim"), ctx.getSource().getPlayer()))
                        )
                )
                .then(literal("grave")
                        .requires(source -> config.selfView || source.hasPermissionLevel(4))
                        .executes(ctx -> viewGrave(ctx.getSource().getPlayer(), ctx.getSource().getPlayer()))
                        .then(argument("player", EntityArgumentType.player())
                                .requires(source -> source.hasPermissionLevel(4) && config.adminView)
                                .executes(ctx -> viewGrave(EntityArgumentType.getPlayer(ctx, "player"), ctx.getSource().getPlayer()))
                        )
                )
                .then(literal("moderate")
                        .requires(source -> source.hasPermissionLevel(4) && config.moderateGraves)
                        .executes(ctx -> moderateGraves(ctx.getSource().getPlayer()))
                )
                .then(literal("clear")
                        .requires(source -> source.hasPermissionLevel(4) && config.clearGraveBackups)
                        .then(argument("victim", EntityArgumentType.players())
                                .executes(ctx -> clearBackup(EntityArgumentType.getPlayers(ctx, "victim"), ctx.getSource().getPlayer()))
                        )
                )
                .then(literal("whitelist")
                        .requires(source -> source.hasPermissionLevel(4) && config.whitelist)
                        .then(literal("add")
                                .requires(source -> config.whitelistAdd)
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> addWhitelist(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player")))
                                )
                        )
                        .then(literal("remove")
                                .requires(source -> config.whitelistRemove)
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> removeWhitelist(ctx.getSource().getPlayer(), EntityArgumentType.getPlayer(ctx, "player")))
                                )
                        )
                        .then(literal("toggle")
                                .requires(source -> config.whitelistToggle)
                                .executes(ctx -> toggleWhitelist(ctx.getSource().getPlayer()))
                        )
                )
        ));
    }

    private static int addWhitelist(PlayerEntity commandUser, PlayerEntity addedPlayer) {
        if (!commandUser.hasPermissionLevel(4) || !YigdConfig.getConfig().commandToggles.whitelistAdd) {
            commandUser.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }

        DeathInfoManager.INSTANCE.addToWhiteList(addedPlayer.getUuid());
        commandUser.sendMessage(new TranslatableText("text.yigd.message.whitelist.added_player", addedPlayer.getDisplayName().asString()), false);
        return 1;
    }
    private static int removeWhitelist(PlayerEntity commandUser, PlayerEntity removedPlayer) {
        if (!commandUser.hasPermissionLevel(4) || !YigdConfig.getConfig().commandToggles.whitelistRemove) {
            commandUser.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }

        DeathInfoManager.INSTANCE.removeFromWhiteList(removedPlayer.getUuid());
        commandUser.sendMessage(new TranslatableText("text.yigd.message.whitelist.removed_player", removedPlayer.getDisplayName().asString()), false);
        return 1;
    }
    private static int toggleWhitelist(PlayerEntity commandUser) {
        if (!commandUser.hasPermissionLevel(4) || !YigdConfig.getConfig().commandToggles.whitelistToggle) {
            commandUser.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }

        boolean toggledTo = DeathInfoManager.INSTANCE.toggleListMode();
        commandUser.sendMessage(new TranslatableText(toggledTo ? "text.yigd.message.whitelist.to_whitelist" : "text.yigd.message.whitelist.to_blacklist"), false);
        return 1;
    }

    private static int moderateGraves(ServerPlayerEntity player) {
        if (!player.hasPermissionLevel(4) || !YigdConfig.getConfig().commandToggles.moderateGraves) {
            player.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }
        boolean existsGraves = false;
        for (List<DeadPlayerData> data : DeathInfoManager.INSTANCE.data.values()) {
            if (data.size() > 0) {
                existsGraves = true;
                break;
            }
        }
        YigdConfig.GraveKeySettings keySettings = YigdConfig.getConfig().utilitySettings.graveKeySettings;
        if (existsGraves) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(DeathInfoManager.INSTANCE.data.size());
            DeathInfoManager.INSTANCE.data.forEach((uuid, deadPlayerData) -> {
                buf.writeUuid(uuid);
                buf.writeInt(deadPlayerData.size());
                for (DeadPlayerData data : deadPlayerData) {
                    buf.writeNbt(data.toNbt());
                }
            });

            buf.writeBoolean(keySettings.enableKeys && keySettings.getFromGui);

            ServerPlayNetworking.send(player, PacketReceivers.ALL_PLAYER_GRAVES, buf);
        } else {
            player.sendMessage(new TranslatableText("text.yigd.message.grave_not_found"), false);
            return 0;
        }
        return 1;
    }

    private static int viewGrave(PlayerEntity player, PlayerEntity commandUser) {
        UUID userId = player.getUuid();
        if (!(commandUser.hasPermissionLevel(4) && YigdConfig.getConfig().commandToggles.adminView) || !(YigdConfig.getConfig().commandToggles.selfView && userId.equals(commandUser.getUuid()))) {
            commandUser.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }
        YigdConfig.GraveKeySettings keySettings = YigdConfig.getConfig().utilitySettings.graveKeySettings;
        if (commandUser instanceof ServerPlayerEntity spe && DeathInfoManager.INSTANCE.data.containsKey(userId) && DeathInfoManager.INSTANCE.data.get(userId).size() > 0) {
            List<DeadPlayerData> deadPlayerData = DeathInfoManager.INSTANCE.data.get(userId);
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(deadPlayerData.size());
            for (DeadPlayerData data : deadPlayerData) {
                buf.writeNbt(data.toNbt());
            }
            buf.writeBoolean(keySettings.enableKeys && keySettings.getFromGui);

            ServerPlayNetworking.send(spe, PacketReceivers.PLAYER_GRAVES_GUI, buf);
            Yigd.LOGGER.info("Sending packet to " + spe.getDisplayName().asString() + " with grave info");
        } else {
            commandUser.sendMessage(new TranslatableText("text.yigd.message.view_command.fail", player.getDisplayName().asString()).styled(style -> style.withColor(0xFF0000)), false);
            return 0;
        }
        return 1;
    }

    // Get world reference variable from registry key identifier
    private static ServerWorld worldFromId (@Nullable MinecraftServer server, Identifier worldId) {
        ServerWorld world = null;
        if (server != null) {
            for (ServerWorld serverWorld : server.getWorlds()) {
                world = serverWorld;
                if (world.getRegistryKey().getValue() == worldId) break;
            }
        }

        return world;
    }

    private static int robGrave(PlayerEntity victim, PlayerEntity stealer) {
        if (!stealer.hasPermissionLevel(4) || !YigdConfig.getConfig().commandToggles.robGrave) {
            stealer.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }
        UUID victimId = victim.getUuid();

        if (!DeathInfoManager.INSTANCE.data.containsKey(victimId)) {
            stealer.sendMessage(new TranslatableText("text.yigd.message.rob_command.fail"), true);
            return 0;
        }
        List<DeadPlayerData> deadPlayerData = DeathInfoManager.INSTANCE.data.get(victimId);

        if (deadPlayerData.size() <= 0) {
            stealer.sendMessage(new TranslatableText("text.yigd.message.unclaimed_grave_missing", victim.getDisplayName().asString()).styled(style -> style.withColor(0xFF0000)), true);
            return 0;
        }
        DeadPlayerData latestDeath = deadPlayerData.remove(deadPlayerData.size() - 1);
        DeathInfoManager.INSTANCE.markDirty();

        Map<String, Object> modInv = new HashMap<>();
        for (int i = 0; i < Yigd.apiMods.size(); i++) {
            YigdApi yigdApi = Yigd.apiMods.get(i);
            modInv.put(yigdApi.getModName(), latestDeath.modInventories.get(i));
        }

        ServerWorld world = worldFromId(stealer.getServer(), latestDeath.worldId);

        if (world != null && latestDeath.gravePos != null && !world.getBlockState(latestDeath.gravePos).getBlock().equals(Yigd.GRAVE_BLOCK)) {
            world.removeBlock(latestDeath.gravePos, false);
            if (YigdConfig.getConfig().graveSettings.dropGraveBlock) {
                ItemScatterer.spawn(world, latestDeath.gravePos.getX(), latestDeath.gravePos.getY(), latestDeath.gravePos.getZ(), Yigd.GRAVE_BLOCK.asItem().getDefaultStack());
            }
        }

        GraveHelper.RetrieveItems(stealer, latestDeath.inventory, modInv, latestDeath.xp, true);

        stealer.sendMessage(new TranslatableText("text.yigd.message.rob_command.success"), true);
        victim.sendMessage(new TranslatableText("text.yigd.message.rob_command.victim"), false);
        return 1;
    }

    public static int restoreGrave(PlayerEntity player, PlayerEntity commandUser, @Nullable UUID graveId) {
        if (!commandUser.hasPermissionLevel(4) || !YigdConfig.getConfig().commandToggles.retrieveGrave) {
            commandUser.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }
        UUID userId = player.getUuid();

        if (!DeathInfoManager.INSTANCE.data.containsKey(userId)) {
            commandUser.sendMessage(new TranslatableText("text.yigd.message.restore_command.fail"), true);
            return -1;
        }
        List<DeadPlayerData> deadPlayerData = DeathInfoManager.INSTANCE.data.get(userId);

        if (deadPlayerData.size() <= 0) {
            commandUser.sendMessage(new TranslatableText("text.yigd.message.unclaimed_grave_missing", player.getDisplayName().asString()).styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }
        DeadPlayerData foundDeath = null;
        if (graveId == null) {
            foundDeath = deadPlayerData.get(deadPlayerData.size() - 1);
        } else {
            for (DeadPlayerData data : deadPlayerData) {
                if (!data.id.equals(graveId)) continue;
                foundDeath = data;
                break;
            }
        }

        if (foundDeath == null) return -1;

        Map<String, Object> modInv = new HashMap<>();
        for (int i = 0; i < Yigd.apiMods.size(); i++) {
            YigdApi yigdApi = Yigd.apiMods.get(i);
            modInv.put(yigdApi.getModName(), foundDeath.modInventories.get(i));
        }

        ServerWorld world = worldFromId(player.getServer(), foundDeath.worldId);

        if (world != null && foundDeath.gravePos != null && world.getBlockState(foundDeath.gravePos).getBlock().equals(Yigd.GRAVE_BLOCK)) {
            world.removeBlock(foundDeath.gravePos, false);

            if (YigdConfig.getConfig().graveSettings.dropGraveBlock) {
                ItemScatterer.spawn(world, foundDeath.gravePos.getX(), foundDeath.gravePos.getY(), foundDeath.gravePos.getZ(), Yigd.GRAVE_BLOCK.asItem().getDefaultStack());
            }
        }
        foundDeath.availability = 0;
        DeathInfoManager.INSTANCE.markDirty();

        GraveHelper.RetrieveItems(player, foundDeath.inventory, modInv, foundDeath.xp, false);

        commandUser.sendMessage(new TranslatableText("text.yigd.message.restore_command.success"), true);
        return 1;
    }

    private static int clearBackup(Collection<ServerPlayerEntity> victims, PlayerEntity commandUser) {
        if (!commandUser.hasPermissionLevel(4) || !YigdConfig.getConfig().commandToggles.clearGraveBackups) {
            commandUser.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }
        int i = 0;
        for (PlayerEntity victim : victims) {
            UUID victimId = victim.getUuid();

            if (!DeathInfoManager.INSTANCE.data.containsKey(victimId)) continue;
            i++;
            DeathInfoManager.INSTANCE.data.get(victimId).clear();
        }
        DeathInfoManager.INSTANCE.markDirty();
        commandUser.sendMessage(new TranslatableText("text.yigd.message.backup.delete_player", i), false);
        return 1;
    }
}
