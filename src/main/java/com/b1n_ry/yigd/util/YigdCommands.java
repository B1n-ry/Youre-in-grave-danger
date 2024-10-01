package com.b1n_ry.yigd.util;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.data.ListMode;
import com.b1n_ry.yigd.networking.LightGraveData;
import com.b1n_ry.yigd.networking.LightPlayerData;
import com.b1n_ry.yigd.networking.packets.GraveOverviewS2CPacket;
import com.b1n_ry.yigd.networking.packets.GraveSelectionS2CPacket;
import com.b1n_ry.yigd.networking.packets.PlayerSelectionS2CPacket;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class YigdCommands {
    public static void registerCommands(RegisterCommandsEvent event) {
        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.CommandConfig commandConfig = config.commandConfig;

        event.getDispatcher().register(
                literal(commandConfig.mainCommand)
                        .requires(source -> source.hasPermission(commandConfig.basePermissionLevel))
                        .executes(YigdCommands::baseCommand)
                        .then(literal("latest")
                                .requires(source -> source.hasPermission(commandConfig.viewLatestPermissionLevel))
                                .executes(YigdCommands::viewLatest))
                        .then(literal("grave")
                                .requires(source -> source.hasPermission(commandConfig.viewSelfPermissionLevel))
                                .executes(YigdCommands::viewSelf)
                                .then(argument("player", EntityArgument.player())
                                        .requires(source -> source.hasPermission(commandConfig.viewUserPermissionLevel))
                                        .executes(context -> viewUser(context, EntityArgument.getPlayer(context, "player")))))
                        .then(literal("moderate")
                                .requires(source -> source.hasPermission(commandConfig.viewAllPermissionLevel))
                                .executes(YigdCommands::viewAll))
                        .then(literal("restore")
                                .requires(source -> source.hasPermission(commandConfig.restorePermissionLevel))
                                .executes(YigdCommands::restore)
                                .then(argument("player", EntityArgument.player())
                                        .executes(context -> restore(context, EntityArgument.getPlayer(context, "player")))
                                        .then(argument("pos", BlockPosArgument.blockPos())
                                                .executes(context -> restore(
                                                        context,
                                                        EntityArgument.getPlayer(context, "player"),
                                                        BlockPosArgument.getBlockPos(context, "pos"))))))
                        .then(literal("rob")
                                .requires(source -> source.hasPermission(commandConfig.robPermissionLevel))
                                .then(argument("victim", EntityArgument.player())
                                        .executes(context -> rob(context, EntityArgument.getPlayer(context, "victim"))))
                                .then(argument("grave_id", UuidArgument.uuid())
                                        .executes(context -> rob(context, UuidArgument.getUuid(context, "grave_id")))))
                        .then(literal("whitelist")
                                .requires(source -> source.hasPermission(commandConfig.whitelistPermissionLevel))
                                .executes(YigdCommands::showListType)
                                .then(literal("add")
                                        .then(argument("target", EntityArgument.players())
                                                .executes(context -> addToList(context, EntityArgument.getPlayers(context, "target")))))
                                .then(literal("remove")
                                        .then(argument("target", EntityArgument.players())
                                                .executes(context -> removeFromList(context, EntityArgument.getPlayers(context, "target")))))
                                .then(literal("toggle")
                                        .executes(YigdCommands::toggleListType))
                                .then(literal("list")
                                        .executes(YigdCommands::showList)))
        );
    }

    private static int baseCommand(CommandContext<CommandSourceStack> context) {
        return viewSelf(context);
    }
    private static int viewLatest(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return -1;
        }
        List<GraveComponent> components = DeathInfoManager.INSTANCE.getBackupData(new ResolvableProfile(player.getGameProfile()));
        List<GraveComponent> unClaimedGraves = new ArrayList<>(components);
        unClaimedGraves.removeIf(graveComponent -> graveComponent.getStatus() != GraveStatus.UNCLAIMED);
        if (unClaimedGraves.isEmpty()) {
            player.sendSystemMessage(Component.translatable("text.yigd.command.latest.fail"));
            return -1;
        }

        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.CommandConfig commandConfig = config.commandConfig;
        PacketDistributor.sendToPlayer(player, new GraveOverviewS2CPacket(
                unClaimedGraves.getFirst(),
                player.hasPermissions(commandConfig.restorePermissionLevel),
                player.hasPermissions(commandConfig.robPermissionLevel),
                player.hasPermissions(commandConfig.deletePermissionLevel),
                player.hasPermissions(commandConfig.unlockPermissionLevel) && config.graveConfig.unlockable,
                config.extraFeatures.graveKeys.enabled && config.extraFeatures.graveKeys.obtainableFromGui,
                config.extraFeatures.graveCompass.cloneRecoveryCompassWithGUI && player.getInventory().countItem(Items.RECOVERY_COMPASS) > 0));
        return 1;
    }
    private static int viewSelf(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return -1;

        return viewUser(context, player);
    }
    private static int viewUser(CommandContext<CommandSourceStack> context, ServerPlayer user) {
        ServerPlayer player = context.getSource().getPlayer();
        if (user == null || player == null) return -1;

        ResolvableProfile profile = new ResolvableProfile(user.getGameProfile());

        List<GraveComponent> components = DeathInfoManager.INSTANCE.getBackupData(profile);

        List<LightGraveData> lightGraveData = new ArrayList<>();
        for (GraveComponent component : components) {
            lightGraveData.add(component.toLightData());
        }

        PacketDistributor.sendToPlayer(player, new GraveSelectionS2CPacket(lightGraveData, profile));
        return 1;
    }
    private static int viewAll(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();

        if (player == null) return -1;

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
            LightPlayerData lightData = new LightPlayerData(components.size(), unclaimed, destroyed, new ResolvableProfile(profile));
            lightPlayerData.add(lightData);
        });

        PacketDistributor.sendToPlayer(player, new PlayerSelectionS2CPacket(lightPlayerData));

        return 1;
    }
    private static int restore(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return -1;

        return restore(context, player);
    }
    private static int restore(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        ResolvableProfile profile = new ResolvableProfile(target.getGameProfile());

        List<GraveComponent> graves = new ArrayList<>(DeathInfoManager.INSTANCE.getBackupData(profile));
        graves.removeIf(graveComponent -> graveComponent.getStatus() == GraveStatus.CLAIMED);
        int size = graves.size();
        if (size < 1) return -1;

        return restore(context, target, graves.get(size - 1));
    }
    private static int restore(CommandContext<CommandSourceStack> context, ServerPlayer target, BlockPos pos) {
        ResolvableProfile profile = new ResolvableProfile(target.getGameProfile());

        List<GraveComponent> graves = new ArrayList<>(DeathInfoManager.INSTANCE.getBackupData(profile));
        graves.removeIf(graveComponent -> graveComponent.getStatus() == GraveStatus.CLAIMED);

        for (GraveComponent grave : graves) {
            if (grave.getPos().equals(pos))
                return restore(context, target, grave);
        }
        return -1;
    }
    private static int restore(CommandContext<CommandSourceStack> context, ServerPlayer target, GraveComponent component) {
        component.applyToPlayer(target, target.serverLevel(), target.position(), true);
        component.setStatus(GraveStatus.CLAIMED);

        component.removeGraveBlock();

        context.getSource().sendSystemMessage(Component.translatable("text.yigd.command.restore.success"));
        return 1;
    }

    private static int rob(CommandContext<CommandSourceStack> context, ServerPlayer victim) {
        ResolvableProfile profile = new ResolvableProfile(victim.getGameProfile());
        List<GraveComponent> graves = new ArrayList<>(DeathInfoManager.INSTANCE.getBackupData(profile));
        graves.removeIf(graveComponent -> graveComponent.getStatus() != GraveStatus.UNCLAIMED);

        int size = graves.size();
        if (size < 1) return -1;
        GraveComponent component = graves.get(size - 1);

        return rob(context, component);
    }
    private static int rob(CommandContext<CommandSourceStack> context, UUID graveId) {
        Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(graveId);
        return component.map(graveComponent -> rob(context, graveComponent)).orElse(-1);
    }
    private static int rob(CommandContext<CommandSourceStack> context, GraveComponent component) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return -1;

        component.applyToPlayer(player, context.getSource().getLevel(), player.position(), false);
        component.setStatus(GraveStatus.CLAIMED);

        component.removeGraveBlock();

        player.sendSystemMessage(Component.translatable("text.yigd.command.rob.success"));
        return 1;
    }

    private static int showListType(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(Component.translatable("text.yigd.command.whitelist.show_current", DeathInfoManager.INSTANCE.getGraveListMode().name()));
        return 1;
    }
    private static int addToList(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
        int i = 0;
        for (ServerPlayer player : players) {
            DeathInfoManager.INSTANCE.addToList(new ResolvableProfile(player.getGameProfile()));
            ++i;
        }
        DeathInfoManager.INSTANCE.setDirty();

        context.getSource().sendSystemMessage(Component.translatable("text.yigd.command.whitelist.added_players", i, DeathInfoManager.INSTANCE.getGraveListMode().name()));
        return i > 0 ? 1 : 0;
    }
    private static int removeFromList(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
        int i = 0;
        for (ServerPlayer player : players) {
            if (DeathInfoManager.INSTANCE.removeFromList(new ResolvableProfile(player.getGameProfile())))
                ++i;
        }

        context.getSource().sendSystemMessage(Component.translatable("text.yigd.command.whitelist.removed_players", i, DeathInfoManager.INSTANCE.getGraveListMode().name()));
        return i > 0 ? 1 : 0;
    }
    private static int toggleListType(CommandContext<CommandSourceStack> context) {
        ListMode listMode = DeathInfoManager.INSTANCE.getGraveListMode();
        ListMode newMode = listMode == ListMode.WHITELIST ? ListMode.BLACKLIST : ListMode.WHITELIST;
        DeathInfoManager.INSTANCE.setGraveListMode(newMode);
        DeathInfoManager.INSTANCE.setDirty();
        context.getSource().sendSystemMessage(Component.translatable("text.yigd.command.whitelist.toggle", newMode.name()));

        return 1;
    }
    private static int showList(CommandContext<CommandSourceStack> context) {
        ListMode listMode = DeathInfoManager.INSTANCE.getGraveListMode();
        Set<GameProfile> affectedPlayers = DeathInfoManager.INSTANCE.getAffectedPlayers();

        StringJoiner joiner = new StringJoiner(", ");
        for (GameProfile profile : affectedPlayers) {
            joiner.add(Optional.ofNullable(profile.getName()).orElse("PLAYER_NOT_FOUND"));
        }
        context.getSource().sendSystemMessage(Component.literal(listMode.name() + ": %s" + joiner));

        return 1;
    }
}
