package com.b1n_ry.yigd.util;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.data.ListMode;
import com.b1n_ry.yigd.networking.LightGraveData;
import com.b1n_ry.yigd.networking.LightPlayerData;
import com.b1n_ry.yigd.networking.ServerPacketHandler;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class YigdCommands {
    public static void register() {
        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.CommandConfig commandConfig = config.commandConfig;

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal(commandConfig.mainCommand)
                        .requires(Permissions.require("yigd.command.base_permission", commandConfig.basePermissionLevel))
                        .executes(YigdCommands::baseCommand)
                        .then(literal("latest")
                                .requires(Permissions.require("yigd.command.view_latest", commandConfig.viewLatestPermissionLevel))
                                .executes(YigdCommands::viewLatest))
                        .then(literal("grave")
                                .requires(Permissions.require("yigd.command.view_self", commandConfig.viewSelfPermissionLevel))
                                .executes(YigdCommands::viewSelf)
                                .then(argument("player", EntityArgumentType.player())
                                        .requires(Permissions.require("yigd.command.view_user", commandConfig.viewUserPermissionLevel))
                                        .executes(context -> viewUser(context, EntityArgumentType.getPlayer(context, "player")))))
                        .then(literal("moderate")
                                .requires(Permissions.require("yigd.command.view_all", commandConfig.viewAllPermissionLevel))
                                .executes(YigdCommands::viewAll))
                        .then(literal("restore")
                                .requires(Permissions.require("yigd.command.restore", commandConfig.restorePermissionLevel))
                                .executes(YigdCommands::restore)
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(context -> restore(context, EntityArgumentType.getPlayer(context, "player")))
                                        .then(argument("pos", BlockPosArgumentType.blockPos())
                                                .executes(context -> restore(
                                                        context,
                                                        EntityArgumentType.getPlayer(context, "player"),
                                                        BlockPosArgumentType.getBlockPos(context, "pos"))))))
                        .then(literal("rob")
                                .requires(Permissions.require("yigd.command.rob", commandConfig.robPermissionLevel))
                                .then(argument("victim", EntityArgumentType.player())
                                        .executes(context -> rob(context, EntityArgumentType.getPlayer(context, "victim"))))
                                .then(argument("grave_id", UuidArgumentType.uuid())
                                        .executes(context -> rob(context, UuidArgumentType.getUuid(context, "grave_id")))))
                        .then(literal("whitelist")
                                .requires(Permissions.require("yigd.command.whitelist", commandConfig.whitelistPermissionLevel))
                                .executes(YigdCommands::showListType)
                                .then(literal("add")
                                        .then(argument("target", EntityArgumentType.players())
                                                .executes(context -> addToList(context, EntityArgumentType.getPlayers(context, "target")))))
                                .then(literal("remove")
                                        .then(argument("target", EntityArgumentType.players())
                                                .executes(context -> removeFromList(context, EntityArgumentType.getPlayers(context, "target")))))
                                .then(literal("toggle")
                                        .executes(YigdCommands::toggleListType))
                                .then(literal("list")
                                        .executes(YigdCommands::showList)))
        ));
    }

    private static int baseCommand(CommandContext<ServerCommandSource> context) {
        return viewSelf(context);
    }
    private static int viewLatest(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            return -1;
        }
        List<GraveComponent> components = DeathInfoManager.INSTANCE.getBackupData(new ProfileComponent(player.getGameProfile()));
        List<GraveComponent> unClaimedGraves = new ArrayList<>(components);
        unClaimedGraves.removeIf(graveComponent -> graveComponent.getStatus() != GraveStatus.UNCLAIMED);
        if (unClaimedGraves.isEmpty()) {
            player.sendMessage(Text.translatable("text.yigd.command.latest.fail"));
            return -1;
        }

        ServerPacketHandler.sendGraveOverviewPacket(player, unClaimedGraves.getFirst());
        return 1;
    }
    private static int viewSelf(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return -1;

        return viewUser(context, player);
    }
    private static int viewUser(CommandContext<ServerCommandSource> context, ServerPlayerEntity user) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (user == null) return -1;

        ProfileComponent profile = new ProfileComponent(user.getGameProfile());

        List<GraveComponent> components = DeathInfoManager.INSTANCE.getBackupData(profile);

        List<LightGraveData> lightGraveData = new ArrayList<>();
        for (GraveComponent component : components) {
            lightGraveData.add(component.toLightData());
        }

        ServerPacketHandler.sendGraveSelectionPacket(player, profile, lightGraveData);
        return 1;
    }
    private static int viewAll(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();

        Map<GameProfile, List<GraveComponent>> players = DeathInfoManager.INSTANCE.getPlayerGraves();

        List<LightPlayerData> lightPlayerData = new ArrayList<>();
        players.forEach((profile, components) -> {
            int unclaimed = 0;
            int destroyed = 0;
            for (GraveComponent component : components) {
                switch (component.getStatus()) {
                    case UNCLAIMED -> unclaimed++;
                    case DESTROYED -> destroyed++;
                }
            }
            LightPlayerData lightData = new LightPlayerData(components.size(), unclaimed, destroyed, new ProfileComponent(profile));
            lightPlayerData.add(lightData);
        });

        ServerPacketHandler.sendPlayerSelectionPacket(player, lightPlayerData);

        return 1;
    }
    private static int restore(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return -1;

        return restore(context, player);
    }
    private static int restore(CommandContext<ServerCommandSource> context, ServerPlayerEntity target) {
        ProfileComponent profile = new ProfileComponent(target.getGameProfile());

        List<GraveComponent> graves = new ArrayList<>(DeathInfoManager.INSTANCE.getBackupData(profile));
        graves.removeIf(graveComponent -> graveComponent.getStatus() == GraveStatus.CLAIMED);
        int size = graves.size();
        if (size < 1) return -1;

        return restore(context, target, graves.get(size - 1));
    }
    private static int restore(CommandContext<ServerCommandSource> context, ServerPlayerEntity target, BlockPos pos) {
        ProfileComponent profile = new ProfileComponent(target.getGameProfile());

        List<GraveComponent> graves = new ArrayList<>(DeathInfoManager.INSTANCE.getBackupData(profile));
        graves.removeIf(graveComponent -> graveComponent.getStatus() == GraveStatus.CLAIMED);

        for (GraveComponent grave : graves) {
            if (grave.getPos().equals(pos))
                return restore(context, target, grave);
        }
        return -1;
    }
    private static int restore(CommandContext<ServerCommandSource> context, ServerPlayerEntity target, GraveComponent component) {
        component.applyToPlayer(target, target.getServerWorld(), target.getPos(), true);
        component.setStatus(GraveStatus.CLAIMED);

        component.removeGraveBlock();

        context.getSource().sendMessage(Text.translatable("text.yigd.command.restore.success"));
        return 1;
    }

    private static int rob(CommandContext<ServerCommandSource> context, ServerPlayerEntity victim) {
        ProfileComponent profile = new ProfileComponent(victim.getGameProfile());
        List<GraveComponent> graves = new ArrayList<>(DeathInfoManager.INSTANCE.getBackupData(profile));
        graves.removeIf(graveComponent -> graveComponent.getStatus() != GraveStatus.UNCLAIMED);

        int size = graves.size();
        if (size < 1) return -1;
        GraveComponent component = graves.get(size - 1);

        return rob(context, component);
    }
    private static int rob(CommandContext<ServerCommandSource> context, UUID graveId) {
        Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(graveId);
        return component.map(graveComponent -> rob(context, graveComponent)).orElse(-1);
    }
    private static int rob(CommandContext<ServerCommandSource> context, GraveComponent component) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return -1;

        component.applyToPlayer(player, context.getSource().getWorld(), player.getPos(), false);
        component.setStatus(GraveStatus.CLAIMED);

        component.removeGraveBlock();

        player.sendMessage(Text.translatable("text.yigd.command.rob.success"));
        return 1;
    }

    private static int showListType(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.translatable("text.yigd.command.whitelist.show_current", DeathInfoManager.INSTANCE.getGraveListMode().name()));
        return 1;
    }
    private static int addToList(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players) {
        int i = 0;
        for (ServerPlayerEntity player : players) {
            DeathInfoManager.INSTANCE.addToList(new ProfileComponent(player.getGameProfile()));
            ++i;
        }
        DeathInfoManager.INSTANCE.markDirty();

        context.getSource().sendMessage(Text.translatable("text.yigd.command.whitelist.added_players", i, DeathInfoManager.INSTANCE.getGraveListMode().name()));
        return i > 0 ? 1 : 0;
    }
    private static int removeFromList(CommandContext<ServerCommandSource> context, Collection<ServerPlayerEntity> players) {
        int i = 0;
        for (ServerPlayerEntity player : players) {
            if (DeathInfoManager.INSTANCE.removeFromList(new ProfileComponent(player.getGameProfile())))
                ++i;
        }

        context.getSource().sendMessage(Text.translatable("text.yigd.command.whitelist.removed_players", i, DeathInfoManager.INSTANCE.getGraveListMode().name()));
        return i > 0 ? 1 : 0;
    }
    private static int toggleListType(CommandContext<ServerCommandSource> context) {
        ListMode listMode = DeathInfoManager.INSTANCE.getGraveListMode();
        ListMode newMode = listMode == ListMode.WHITELIST ? ListMode.BLACKLIST : ListMode.WHITELIST;
        DeathInfoManager.INSTANCE.setGraveListMode(newMode);
        DeathInfoManager.INSTANCE.markDirty();
        context.getSource().sendMessage(Text.translatable("text.yigd.command.whitelist.toggle", newMode.name()));

        return 1;
    }
    private static int showList(CommandContext<ServerCommandSource> context) {
        ListMode listMode = DeathInfoManager.INSTANCE.getGraveListMode();
        Set<GameProfile> affectedPlayers = DeathInfoManager.INSTANCE.getAffectedPlayers();

        StringJoiner joiner = new StringJoiner(", ");
        for (GameProfile profile : affectedPlayers) {
            joiner.add(Optional.ofNullable(profile.getName()).orElse("PLAYER_NOT_FOUND"));
        }
        context.getSource().sendMessage(Text.literal(listMode.name() + ": %s" + joiner));

        return 1;
    }
}
