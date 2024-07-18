package com.b1n_ry.yigd.networking;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.ClaimPriority;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.networking.packets.*;
import com.b1n_ry.yigd.util.GraveCompassHelper;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ServerPacketHandler {
    public static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(RestoreGraveC2SPacket.ID, (payload, context) -> {
            YigdConfig config = YigdConfig.getConfig();
            ServerPlayerEntity player = context.player();
            if (!Permissions.check(player, "yigd.command.restore", config.commandConfig.restorePermissionLevel)) {
                player.sendMessage(Text.translatable("text.yigd.command.permission_fail"));
                return;
            }

            UUID graveId = payload.graveId();
            boolean itemsInGrave = payload.itemsInGrave();
            boolean itemsDeleted = payload.itemsDeleted();
            boolean itemsKept = payload.itemsKept();
            boolean itemsDropped = payload.itemsDropped();

            MinecraftServer server = player.server;
            server.execute(() -> {
                Optional<GraveComponent> maybeComponent = DeathInfoManager.INSTANCE.getGrave(graveId);
                maybeComponent.ifPresentOrElse(component -> {
                    ProfileComponent owner = component.getOwner();
                    UUID uuid = owner.id().orElse(null);
                    String playerName = owner.name().orElse(null);
                    ServerPlayerEntity restoringPlayer = uuid != null ?
                            server.getPlayerManager().getPlayer(uuid) : playerName != null ?
                            server.getPlayerManager().getPlayer(playerName) : null;

                    if (restoringPlayer == null) {
                        player.sendMessage(Text.translatable("text.yigd.command.restore.fail.offline_player"));
                        return;
                    }

                    component.applyToPlayer(restoringPlayer, restoringPlayer.getServerWorld(), restoringPlayer.getPos(), true, dropRule -> switch (dropRule) {
                        case KEEP -> itemsKept;
                        case DESTROY -> itemsDeleted;
                        case DROP -> itemsDropped;
                        case PUT_IN_GRAVE -> itemsInGrave;
                    });
                    if (itemsInGrave) {
                        component.setStatus(GraveStatus.CLAIMED);

                        component.removeGraveBlock();
                    }

                    player.sendMessage(Text.translatable("text.yigd.command.restore.success"));
                }, () -> player.sendMessage(Text.translatable("text.yigd.command.restore.fail")));
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(RobGraveC2SPacket.ID, (payload, context) -> {
            YigdConfig config = YigdConfig.getConfig();
            ServerPlayerEntity player = context.player();
            if (!Permissions.check(player, "yigd.command.rob", config.commandConfig.robPermissionLevel)) {
                player.sendMessage(Text.translatable("text.yigd.command.permission_fail"));
                return;
            }

            UUID graveId = payload.graveId();
            boolean itemsInGrave = payload.itemsInGrave();
            boolean itemsDeleted = payload.itemsDeleted();
            boolean itemsKept = payload.itemsKept();
            boolean itemsDropped = payload.itemsDropped();

            MinecraftServer server = player.server;

            server.execute(() -> {
                Optional<GraveComponent> maybeComponent = DeathInfoManager.INSTANCE.getGrave(graveId);
                maybeComponent.ifPresentOrElse(component -> {
                    component.applyToPlayer(player, player.getServerWorld(), player.getPos(), false, dropRule -> switch (dropRule) {
                        case KEEP -> itemsKept;
                        case DESTROY -> itemsDeleted;
                        case DROP -> itemsDropped;
                        case PUT_IN_GRAVE -> itemsInGrave;
                    });

                    if (itemsInGrave) {
                        component.setStatus(GraveStatus.CLAIMED);

                        component.removeGraveBlock();
                    }

                    player.sendMessage(Text.translatable("text.yigd.command.rob.success"));
                }, () -> player.sendMessage(Text.translatable("text.yigd.command.rob.fail")));
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(DeleteGraveC2SPacket.ID, (payload, context) -> {
            YigdConfig config = YigdConfig.getConfig();
            ServerPlayerEntity player = context.player();
            if (!Permissions.check(player, "yigd.command.delete", config.commandConfig.deletePermissionLevel)) {
                player.sendMessage(Text.translatable("text.yigd.command.permission_fail"));
                return;
            }

            UUID graveId = payload.graveId();
            MinecraftServer server = player.server;

            server.execute(() -> {
                ActionResult deleted = DeathInfoManager.INSTANCE.delete(graveId);
                DeathInfoManager.INSTANCE.markDirty();

                String translatable = switch (deleted) {
                    case SUCCESS -> "text.yigd.command.delete.success";
                    case PASS -> "text.yigd.command.delete.pass";
                    case FAIL -> "text.yigd.command.delete.fail";
                    default -> "If you see this, congratulations. You've broken YIGD";
                };
                player.sendMessage(Text.translatable(translatable));
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(LockGraveC2SPacket.ID, (payload, context) -> {
            YigdConfig config = YigdConfig.getConfig();
            ServerPlayerEntity player = context.player();
            if (!Permissions.check(player, "yigd.command.locking", config.commandConfig.unlockPermissionLevel)) {
                player.sendMessage(Text.translatable("text.yigd.command.permission_fail"));
                return;
            }

            UUID graveId = payload.graveId();
            boolean lockState = payload.locked();
            MinecraftServer server = player.server;
            server.execute(() -> {
                Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(graveId);
                component.ifPresentOrElse(grave -> grave.setLocked(lockState),
                        () -> player.sendMessage(Text.translatable("text.yigd.command.lock.fail")));
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(RequestKeyC2SPacket.ID, (payload, context) -> {
            YigdConfig config = YigdConfig.getConfig();
            ServerPlayerEntity player = context.player();
            if (!config.extraFeatures.graveKeys.enabled || !config.extraFeatures.graveKeys.obtainableFromGui) {
                player.sendMessage(Text.translatable("text.yigd.command.permission_fail"));
                return;
            }

            UUID graveId = payload.graveId();
            MinecraftServer server = player.server;
            server.execute(() -> {
                Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(graveId);
                component.ifPresentOrElse(grave -> {
                    ItemStack key = new ItemStack(Yigd.GRAVE_KEY_ITEM);
                    Yigd.GRAVE_KEY_ITEM.bindStackToGrave(graveId, grave.getOwner(), key);
                    player.giveItemStack(key);
                }, () -> player.sendMessage(Text.translatable("text.yigd.command.obtain_key.fail")));
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(RequestCompassC2SPacket.ID, (payload, context) -> {
            YigdConfig config = YigdConfig.getConfig();
            ServerPlayerEntity player = context.player();
            if (!config.extraFeatures.graveCompass.cloneRecoveryCompassWithGUI) {
                player.sendMessage(Text.translatable("text.yigd.command.permission_fail"));
                return;
            }

            UUID graveId = payload.graveId();
            MinecraftServer server = player.server;
            server.execute(() -> {
                Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(graveId);
                component.ifPresentOrElse(grave -> GraveCompassHelper.giveCompass(player, graveId, grave.getPos(), grave.getWorldRegistryKey()),
                        () -> player.sendMessage(Text.translatable("text.yigd.command.obtain_compass.fail")));
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(GraveOverviewRequestC2SPacket.ID, (payload, context) -> {
            YigdConfig config = YigdConfig.getConfig();
            ServerPlayerEntity player = context.player();
            if (!Permissions.check(player, "yigd.command.view_self", config.commandConfig.viewSelfPermissionLevel)) {
                player.sendMessage(Text.translatable("text.yigd.command.permission_fail"));
                return;
            }

            UUID graveId = payload.graveId();
            Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(graveId);
            component.ifPresentOrElse(grave -> sendGraveOverviewPacket(player, grave),
                    () -> player.sendMessage(Text.translatable("text.yigd.command.view_self.fail")));
        });
        ServerPlayNetworking.registerGlobalReceiver(GraveSelectionRequestC2SPacket.ID, (payload, context) -> {
            YigdConfig config = YigdConfig.getConfig();
            ServerPlayerEntity player = context.player();
            if (!Permissions.check(player, "yigd.command.view_user", config.commandConfig.viewUserPermissionLevel)) {
                player.sendMessage(Text.translatable("text.yigd.command.permission_fail"));
                return;
            }

            ProfileComponent profile = payload.profile();
            List<GraveComponent> components = DeathInfoManager.INSTANCE.getBackupData(profile);

            List<LightGraveData> lightGraveData = new ArrayList<>();
            for (GraveComponent component : components) {
                lightGraveData.add(component.toLightData());
            }

            sendGraveSelectionPacket(player, profile, lightGraveData);
        });
        ServerPlayNetworking.registerGlobalReceiver(UpdateConfigC2SPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();

            ClaimPriority claimPriority = payload.claiming();
            ClaimPriority robPriority = payload.robbing();

            UUID playerId = player.getUuid();
            Yigd.CLAIM_PRIORITIES.put(playerId, claimPriority);
            Yigd.ROB_PRIORITIES.put(playerId, robPriority);

            Yigd.LOGGER.info("Priority overwritten for player %s. Claiming: %s / Robbing: %s".formatted(player.getGameProfile().getName(), claimPriority.name(), robPriority.name()));
        });
    }

    public static void sendGraveOverviewPacket(ServerPlayerEntity player, GraveComponent component) {
        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.CommandConfig commandConfig = YigdConfig.getConfig().commandConfig;
        boolean canRestore = Permissions.check(player, "yigd.command.restore", commandConfig.restorePermissionLevel);
        boolean canRob = Permissions.check(player, "yigd.command.rob", commandConfig.robPermissionLevel);
        boolean canDelete = Permissions.check(player, "yigd.command.delete", commandConfig.deletePermissionLevel);
        boolean canUnlock = Permissions.check(player, "yigd.command.locking", commandConfig.unlockPermissionLevel);

        boolean obtainableKeys = config.extraFeatures.graveKeys.enabled && config.extraFeatures.graveKeys.obtainableFromGui;
        boolean obtainableCompass = config.extraFeatures.graveCompass.cloneRecoveryCompassWithGUI && player.getInventory().count(Items.RECOVERY_COMPASS) > 0;
        ServerPlayNetworking.send(player, new GraveOverviewS2CPacket(component, canRestore, canRob, canDelete, canUnlock, obtainableKeys, obtainableCompass));
    }

    public static void sendGraveSelectionPacket(ServerPlayerEntity player, ProfileComponent ofUser, List<LightGraveData> data) {
        ServerPlayNetworking.send(player, new GraveSelectionS2CPacket(data, ofUser));
    }

    public static void sendPlayerSelectionPacket(ServerPlayerEntity player, List<LightPlayerData> data) {
        ServerPlayNetworking.send(player, new PlayerSelectionS2CPacket(data));
    }
}
