package com.b1n_ry.yigd.util;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.packets.LightGraveData;
import com.b1n_ry.yigd.packets.LightPlayerData;
import com.b1n_ry.yigd.packets.ServerPacketHandler;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.context.CommandContext;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.literal;

public class Commands {
    public static void register() {
        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.CommandConfig commandConfig = config.commandConfig;

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal(commandConfig.mainCommand)
                        .requires(Permissions.require("yigd.command.base_permission", true))
                        .executes(Commands::baseCommand)
                        .then(literal("latest")
                                .requires(Permissions.require("yigd.command.view_latest", 0))
                                .executes(Commands::viewLatest))
                        .then(literal("grave")
                                .requires(Permissions.require("yigd.command.view_self", 0))
                                .executes(Commands::viewSelf)
                                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                        .requires(Permissions.require("yigd.command.view_user", 2))
                                        .executes(context -> viewUser(context, GameProfileArgumentType.getProfileArgument(context, "player")))))
                        .then(literal("moderate")
                                .requires(Permissions.require("yigd.command.view_all", 2))
                                .executes(Commands::viewAll))
                        .then(literal("restore")
                                .requires(Permissions.require("yigd.command.restore", 2))
                                .executes(Commands::restore))  // Change to require parameters when implementing the restore command
        ));
    }

    private static int baseCommand(CommandContext<ServerCommandSource> context) {
        return 0;
    }
    private static int viewLatest(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) {
            return -1;
        }
        List<GraveComponent> components = DeathInfoManager.INSTANCE.getBackupData(player.getGameProfile());
        List<GraveComponent> unClaimedGraves = new ArrayList<>(components);
        unClaimedGraves.removeIf(graveComponent -> graveComponent.getStatus() != GraveStatus.UNCLAIMED);
        if (unClaimedGraves.size() == 0) {
            player.sendMessage(Text.translatable("yigd.command_callback.latest.fail"));
            return -1;
        }

        ServerPacketHandler.sendGraveOverviewPacket(player, unClaimedGraves.get(0));
        return 1;
    }
    private static int viewSelf(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return -1;

        return viewUser(context, List.of(player.getGameProfile()));
    }
    private static int viewUser(CommandContext<ServerCommandSource> context, Collection<GameProfile> profiles) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return -1;

        if (profiles.size() == 0) {
            return -1;
        }
        GameProfile profile = (GameProfile) profiles.toArray()[0];

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
            LightPlayerData lightData = new LightPlayerData(components.size(), unclaimed, destroyed, profile);
            lightPlayerData.add(lightData);
        });

        ServerPacketHandler.sendPlayerSelectionPacket(player, lightPlayerData);

        return 1;
    }
    private static int restore(CommandContext<ServerCommandSource> context) {
        return 0;
    }
}
