package com.b1n_ry.yigd.core;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.compat.PermissionsCompat;
import com.b1n_ry.yigd.config.YigdConfig;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.MessageType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class YigdCommand {
    public static void registerCommands() {
        YigdConfig.CommandToggles config = YigdConfig.getConfig().commandToggles;

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> dispatcher.register(literal(config.coreCommandName)
                .executes(ctx -> viewGrave(ctx.getSource().getPlayer(), ctx.getSource().getPlayer()))
                .then(literal("restore")
                        .requires(source -> hasPermission(source, "yigd.command.restore") && config.retrieveGrave)
                        .then(argument("player", EntityArgumentType.player())
                                .executes(ctx -> restoreGrave(EntityArgumentType.getPlayer(ctx, "player"), ctx.getSource().getPlayer(), null))
                        )
                        .executes(ctx -> restoreGrave(ctx.getSource().getPlayer(), ctx.getSource().getPlayer(), null))
                )
                .then(literal("rob")
                        .requires(source -> hasPermission(source, "yigd.command.rob") && config.robGrave)
                        .then(argument("victim", EntityArgumentType.player())
                                .executes(ctx -> robGrave(EntityArgumentType.getPlayer(ctx, "victim").getGameProfile(), ctx.getSource().getPlayer(), null))
                        )
                )
                .then(literal("grave")
                        .requires(source -> config.selfView || hasPermission(source, "yigd.command.view"))
                        .executes(ctx -> viewGrave(ctx.getSource().getPlayer(), ctx.getSource().getPlayer()))
                        .then(argument("player", EntityArgumentType.player())
                                .requires(source -> hasPermission(source, "yigd.command.view") && config.adminView)
                                .executes(ctx -> viewGrave(EntityArgumentType.getPlayer(ctx, "player"), ctx.getSource().getPlayer()))
                        )
                )
                .then(literal("moderate")
                        .requires(source -> hasPermission(source, "yigd.command.moderate") && config.moderateGraves)
                        .executes(ctx -> moderateGraves(ctx.getSource().getPlayer()))
                )
                .then(literal("clear")
                        .requires(source -> hasPermission(source, "yigd.command.clear") && config.clearGraveBackups)
                        .then(argument("victim", EntityArgumentType.players())
                                .executes(ctx -> clearBackup(EntityArgumentType.getPlayers(ctx, "victim"), ctx.getSource().getPlayer()))
                        )
                )
                .then(literal("whitelist")
                        .requires(source -> hasPermission(source, "yigd.command.whitelist") && config.whitelist)
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

    private static int addWhitelist(ServerPlayerEntity commandUser, ServerPlayerEntity addedPlayer) {
        if (!hasPermission(commandUser, "yigd.command.whitelist") || !YigdConfig.getConfig().commandToggles.whitelistAdd) {
            commandUser.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.missing_permission")).styled(style -> style.withColor(0xFF0000)), MessageType.SYSTEM);
            return -1;
        }

        DeathInfoManager.INSTANCE.addToWhiteList(addedPlayer.getUuid());
        commandUser.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.whitelist.added_player", addedPlayer.getDisplayName().getString())), MessageType.SYSTEM);
        return 1;
    }
    private static int removeWhitelist(ServerPlayerEntity commandUser, ServerPlayerEntity removedPlayer) {
        if (!hasPermission(commandUser, "yigd.command.whitelist") || !YigdConfig.getConfig().commandToggles.whitelistRemove) {
            commandUser.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.missing_permission")).styled(style -> style.withColor(0xFF0000)), MessageType.SYSTEM);
            return -1;
        }

        DeathInfoManager.INSTANCE.removeFromWhiteList(removedPlayer.getUuid());
        commandUser.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.whitelist.removed_player", removedPlayer.getDisplayName().getString())), MessageType.SYSTEM);
        return 1;
    }
    private static int toggleWhitelist(ServerPlayerEntity commandUser) {
        if (!hasPermission(commandUser, "yigd.command.whitelist") || !YigdConfig.getConfig().commandToggles.whitelistToggle) {
            commandUser.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.missing_permission")).styled(style -> style.withColor(0xFF0000)), MessageType.SYSTEM);
            return -1;
        }

        boolean toggledTo = DeathInfoManager.INSTANCE.toggleListMode();
        commandUser.sendMessage(MutableText.of(new TranslatableTextContent(toggledTo ? "text.yigd.message.whitelist.to_whitelist" : "text.yigd.message.whitelist.to_blacklist")), MessageType.SYSTEM);
        return 1;
    }

    private static int moderateGraves(ServerPlayerEntity player) {
        if (!hasPermission(player, "yigd.command.moderate") || !YigdConfig.getConfig().commandToggles.moderateGraves) {
            player.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.missing_permission")).styled(style -> style.withColor(0xFF0000)), MessageType.SYSTEM);
            return -1;
        }
        boolean existsGraves = false;
        for (List<DeadPlayerData> data : DeathInfoManager.INSTANCE.data.values()) {
            if (data.size() > 0) {
                existsGraves = true;
                break;
            }
        }
        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.GraveKeySettings keySettings = config.utilitySettings.graveKeySettings;
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
            buf.writeBoolean(config.graveSettings.unlockableGraves);

            int unlockedGravesAmount = DeathInfoManager.INSTANCE.unlockedGraves.size();
            buf.writeInt(unlockedGravesAmount);
            for (int i = 0; i < unlockedGravesAmount; i++) {
                buf.writeUuid(DeathInfoManager.INSTANCE.unlockedGraves.get(i));
            }

            ServerPlayNetworking.send(player, PacketIdentifiers.ALL_PLAYER_GRAVES, buf);
        } else {
            player.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.grave_not_found")), MessageType.SYSTEM);
            return 0;
        }
        return 1;
    }

    private static int viewGrave(@Nullable ServerPlayerEntity player, ServerPlayerEntity commandUser) {
        if (player == null) return -1;
        UUID userId = player.getUuid();
        YigdConfig config = YigdConfig.getConfig();
        if (!((hasPermission(commandUser, "yigd.command.view") && config.commandToggles.adminView) || (config.commandToggles.selfView && userId.equals(commandUser.getUuid())))) {
            commandUser.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.missing_permission")).styled(style -> style.withColor(0xFF0000)), MessageType.SYSTEM);
            return -1;
        }
        YigdConfig.GraveKeySettings keySettings = config.utilitySettings.graveKeySettings;
        if (DeathInfoManager.INSTANCE.data.containsKey(userId) && DeathInfoManager.INSTANCE.data.get(userId).size() > 0) {
            List<DeadPlayerData> deadPlayerData = DeathInfoManager.INSTANCE.data.get(userId);
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(deadPlayerData.size());
            for (DeadPlayerData data : deadPlayerData) {
                buf.writeNbt(data.toNbt());
            }
            buf.writeBoolean(keySettings.enableKeys && keySettings.getFromGui);
            buf.writeBoolean(config.graveSettings.unlockableGraves);

            int unlockedGravesAmount = DeathInfoManager.INSTANCE.unlockedGraves.size();
            buf.writeInt(unlockedGravesAmount);
            for (int i = 0; i < unlockedGravesAmount; i++) {
                buf.writeUuid(DeathInfoManager.INSTANCE.unlockedGraves.get(i));
            }

            ServerPlayNetworking.send(commandUser, PacketIdentifiers.PLAYER_GRAVES_GUI, buf);
            Yigd.LOGGER.info("Sending packet to " + commandUser.getDisplayName().getString() + " with grave info");
        } else {
            commandUser.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.view_command.fail", player.getDisplayName().getString())).styled(style -> style.withColor(0xFF0000)), MessageType.SYSTEM);
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

    public static int robGrave(GameProfile victim, ServerPlayerEntity stealer, @Nullable UUID graveId) {
        if (!hasPermission(stealer, "yigd.command.rob") || !YigdConfig.getConfig().commandToggles.robGrave) {
            stealer.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.missing_permission")).styled(style -> style.withColor(0xFF0000)), MessageType.SYSTEM);
            return -1;
        }
        UUID victimId = victim.getId();

        if (!DeathInfoManager.INSTANCE.data.containsKey(victimId)) {
            stealer.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.rob_command.fail")), MessageType.SYSTEM);
            return 0;
        }
        List<DeadPlayerData> deadPlayerData = DeathInfoManager.INSTANCE.data.get(victimId);

        if (deadPlayerData.size() <= 0) {
            stealer.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.unclaimed_grave_missing", victim.getName())).styled(style -> style.withColor(0xFF0000)), true);
            return 0;
        }
        DeadPlayerData foundDeath = deadPlayerData.get(deadPlayerData.size() - 1);
        if (graveId != null) {
            for (DeadPlayerData data : deadPlayerData) {
                if (data.id != graveId) continue;
                foundDeath = data;
                break;
            }
        }
        DeathInfoManager.INSTANCE.markDirty();

        Map<String, Object> modInv = new HashMap<>();
        for (int i = 0; i < Yigd.apiMods.size(); i++) {
            YigdApi yigdApi = Yigd.apiMods.get(i);
            modInv.put(yigdApi.getModName(), foundDeath.modInventories.get(i));
        }

        ServerWorld world = worldFromId(stealer.getServer(), foundDeath.worldId);

        if (world != null && foundDeath.gravePos != null && world.getBlockState(foundDeath.gravePos).isOf(Yigd.GRAVE_BLOCK)) {
            world.removeBlock(foundDeath.gravePos, false);
            if (YigdConfig.getConfig().graveSettings.dropGraveBlock) {
                ItemScatterer.spawn(world, foundDeath.gravePos.getX(), foundDeath.gravePos.getY(), foundDeath.gravePos.getZ(), Yigd.GRAVE_BLOCK.asItem().getDefaultStack());
            }
        }

        GraveHelper.RetrieveItems(stealer, foundDeath.inventory, modInv, foundDeath.xp, true);

        stealer.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.rob_command.success")), true);

        ServerPlayerEntity victimPlayer = stealer.server.getPlayerManager().getPlayer(victimId);
        if (victimPlayer != null) {
            victimPlayer.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.rob_command.victim")), MessageType.SYSTEM);
        } else {
            Yigd.notNotifiedRobberies.add(victimId);
        }
        return 1;
    }

    public static int restoreGrave(ServerPlayerEntity player, ServerPlayerEntity commandUser, @Nullable UUID graveId) {
        if (!hasPermission(commandUser, "yigd.command.restore") || !YigdConfig.getConfig().commandToggles.retrieveGrave) {
            commandUser.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.missing_permission")).styled(style -> style.withColor(0xFF0000)), MessageType.SYSTEM);
            return -1;
        }
        UUID userId = player.getUuid();

        if (!DeathInfoManager.INSTANCE.data.containsKey(userId)) {
            commandUser.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.restore_command.fail")), true);
            return -1;
        }
        List<DeadPlayerData> deadPlayerData = DeathInfoManager.INSTANCE.data.get(userId);

        if (deadPlayerData.size() <= 0) {
            commandUser.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.unclaimed_grave_missing", player.getDisplayName().getString())).styled(style -> style.withColor(0xFF0000)), MessageType.SYSTEM);
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

        if (world != null && foundDeath.gravePos != null && world.getBlockState(foundDeath.gravePos).isOf(Yigd.GRAVE_BLOCK)) {
            world.removeBlock(foundDeath.gravePos, false);

            if (YigdConfig.getConfig().graveSettings.dropGraveBlock) {
                ItemScatterer.spawn(world, foundDeath.gravePos.getX(), foundDeath.gravePos.getY(), foundDeath.gravePos.getZ(), Yigd.GRAVE_BLOCK.asItem().getDefaultStack());
            }
        }
        foundDeath.availability = 0;
        DeathInfoManager.INSTANCE.markDirty();

        GraveHelper.RetrieveItems(player, foundDeath.inventory, modInv, foundDeath.xp, false);

        commandUser.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.restore_command.success")), true);
        return 1;
    }

    private static int clearBackup(Collection<ServerPlayerEntity> victims, ServerPlayerEntity commandUser) {
        if (!hasPermission(commandUser, "yigd.command.clear") || !YigdConfig.getConfig().commandToggles.clearGraveBackups) {
            commandUser.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.missing_permission")).styled(style -> style.withColor(0xFF0000)), MessageType.SYSTEM);
            return -1;
        }
        int i = 0;
        for (ServerPlayerEntity victim : victims) {
            UUID victimId = victim.getUuid();

            if (!DeathInfoManager.INSTANCE.data.containsKey(victimId)) continue;
            i++;
            DeathInfoManager.INSTANCE.data.get(victimId).clear();
        }
        DeathInfoManager.INSTANCE.markDirty();
        commandUser.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.backup.delete_player", i)), MessageType.SYSTEM);
        return 1;
    }

    public static boolean hasPermission(PlayerEntity player, String permission) {
        if (Yigd.miscCompatMods.contains("permissions")) {
            return switch (permission) {
                case "yigd.command.moderate" -> PermissionsCompat.moderatePermission(player);
                case "yigd.command.rob" -> PermissionsCompat.robPermission(player);
                case "yigd.command.restore" -> PermissionsCompat.restorePermission(player);
                case "yigd.command.view" -> PermissionsCompat.viewPermission(player);
                case "yigd.command.clear" -> PermissionsCompat.clearPermission(player);
                case "yigd.command.whitelist" -> PermissionsCompat.whitelistPermission(player);
                case "yigd.command.delete" -> PermissionsCompat.deletePermission(player);
                default -> false;
            };
        }
        return player.hasPermissionLevel(4);
    }
    private static boolean hasPermission(ServerCommandSource source, String permission) {
        if (Yigd.miscCompatMods.contains("permissions")) {
            return switch (permission) {
                case "yigd.command.moderate" -> PermissionsCompat.moderatePermission(source);
                case "yigd.command.rob" -> PermissionsCompat.robPermission(source);
                case "yigd.command.restore" -> PermissionsCompat.restorePermission(source);
                case "yigd.command.view" -> PermissionsCompat.viewPermission(source);
                case "yigd.command.clear" -> PermissionsCompat.clearPermission(source);
                case "yigd.command.whitelist" -> PermissionsCompat.whitelistPermission(source);
                default -> false;
            };
        }
        return source.hasPermissionLevel(4);
    }
}
