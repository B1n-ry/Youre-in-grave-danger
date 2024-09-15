package com.b1n_ry.yigd.networking;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.ClaimPriority;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.networking.packets.*;
import com.b1n_ry.yigd.util.GraveCompassHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ServerPacketHandler {
    public static void deleteGraveRequest(DeleteGraveC2SPacket payload, IPayloadContext context) {
        YigdConfig config = YigdConfig.getConfig();
        Player player = context.player();
        if (!player.hasPermissions(config.commandConfig.deletePermissionLevel)) {
            player.sendSystemMessage(Component.translatable("text.yigd.command.permission_fail"));
            return;
        }

        UUID graveId = payload.graveId();
        MinecraftServer server = player.getServer();

        if (server != null) server.execute(() -> {
            InteractionResult deleted = DeathInfoManager.INSTANCE.delete(graveId);
            DeathInfoManager.INSTANCE.setDirty();

            String translatable = switch (deleted) {
                case SUCCESS -> "text.yigd.command.delete.success";
                case PASS -> "text.yigd.command.delete.pass";
                case FAIL -> "text.yigd.command.delete.fail";
                default -> "If you see this, congratulations. You've broken YIGD";
            };
            player.sendSystemMessage(Component.translatable(translatable));
        });
    }
    public static void graveOverviewRequest(GraveOverviewRequestC2SPacket payload, IPayloadContext context) {
        YigdConfig config = YigdConfig.getConfig();
        Player player = context.player();
        if (!player.hasPermissions(config.commandConfig.viewSelfPermissionLevel)) {
            player.sendSystemMessage(Component.translatable("text.yigd.command.permission_fail"));
            return;
        }

        YigdConfig.CommandConfig commandConfig = config.commandConfig;

        UUID graveId = payload.graveId();
        Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(graveId);
        component.ifPresentOrElse(grave -> PacketDistributor.sendToPlayer((ServerPlayer) player, new GraveOverviewS2CPacket(grave,
                    player.hasPermissions(commandConfig.restorePermissionLevel),
                    player.hasPermissions(commandConfig.robPermissionLevel),
                    player.hasPermissions(commandConfig.deletePermissionLevel),
                    player.hasPermissions(commandConfig.unlockPermissionLevel) && config.graveConfig.unlockable,
                    config.extraFeatures.graveKeys.enabled && config.extraFeatures.graveKeys.obtainableFromGui,
                    config.extraFeatures.graveCompass.cloneRecoveryCompassWithGUI && player.getInventory().countItem(Items.RECOVERY_COMPASS) > 0)),
                () -> player.sendSystemMessage(Component.translatable("text.yigd.command.view_self.fail")));
    }
    public static void graveSelectionRequest(GraveSelectionRequestC2SPacket payload, IPayloadContext context) {
        YigdConfig config = YigdConfig.getConfig();
        Player player = context.player();
        if (!player.hasPermissions(config.commandConfig.viewUserPermissionLevel)) {
            player.sendSystemMessage(Component.translatable("text.yigd.command.permission_fail"));
            return;
        }

        ResolvableProfile profile = payload.profile();
        List<GraveComponent> components = DeathInfoManager.INSTANCE.getBackupData(profile);

        List<LightGraveData> lightGraveData = new ArrayList<>();
        for (GraveComponent component : components) {
            lightGraveData.add(component.toLightData());
        }

        PacketDistributor.sendToPlayer((ServerPlayer) player, new GraveSelectionS2CPacket(lightGraveData, profile));
    }
    public static void lockGrave(LockGraveC2SPacket payload, IPayloadContext context) {
        YigdConfig config = YigdConfig.getConfig();
        Player player = context.player();
        if (!player.hasPermissions(config.commandConfig.unlockPermissionLevel)) {
            player.sendSystemMessage(Component.translatable("text.yigd.command.permission_fail"));
            return;
        }

        UUID graveId = payload.graveId();
        boolean lockState = payload.locked();
        MinecraftServer server = player.getServer();
        if (server != null) server.execute(() -> {
            Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(graveId);
            component.ifPresentOrElse(grave -> grave.setLocked(lockState),
                    () -> player.sendSystemMessage(Component.translatable("text.yigd.command.lock.fail")));
        });
    }
    public static void requestCompass(RequestCompassC2SPacket payload, IPayloadContext context) {
        YigdConfig config = YigdConfig.getConfig();
        Player player = context.player();
        if (!config.extraFeatures.graveCompass.cloneRecoveryCompassWithGUI) {
            player.sendSystemMessage(Component.translatable("text.yigd.command.permission_fail"));
            return;
        }

        UUID graveId = payload.graveId();
        MinecraftServer server = player.getServer();
        if (server != null) server.execute(() -> {
            Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(graveId);
            component.ifPresentOrElse(grave -> GraveCompassHelper.giveCompass((ServerPlayer) player, graveId, grave.getPos(), grave.getWorldRegistryKey()),
                    () -> player.sendSystemMessage(Component.translatable("text.yigd.command.obtain_compass.fail")));
        });
    }
    public static void requestKey(RequestKeyC2SPacket payload, IPayloadContext context) {
        YigdConfig config = YigdConfig.getConfig();
        Player player = context.player();
        if (!config.extraFeatures.graveKeys.enabled || !config.extraFeatures.graveKeys.obtainableFromGui) {
            player.sendSystemMessage(Component.translatable("text.yigd.command.permission_fail"));
            return;
        }

        UUID graveId = payload.graveId();
        MinecraftServer server = player.getServer();
        if (server != null) server.execute(() -> {
            Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(graveId);
            component.ifPresentOrElse(grave -> {
                ItemStack key = new ItemStack(Yigd.GRAVE_KEY_ITEM.get());
                Yigd.GRAVE_KEY_ITEM.get().bindStackToGrave(graveId, grave.getOwner(), key);
                player.addItem(key);
            }, () -> player.sendSystemMessage(Component.translatable("text.yigd.command.obtain_key.fail")));
        });
    }
    public static void restoreGrave(RestoreGraveC2SPacket payload, IPayloadContext context) {
        YigdConfig config = YigdConfig.getConfig();
        Player player = context.player();
        if (!player.hasPermissions(config.commandConfig.restorePermissionLevel)) {
            player.sendSystemMessage(Component.translatable("text.yigd.command.permission_fail"));
            return;
        }

        UUID graveId = payload.graveId();
        boolean itemsInGrave = payload.itemsInGrave();
        boolean itemsDeleted = payload.itemsDeleted();
        boolean itemsKept = payload.itemsKept();
        boolean itemsDropped = payload.itemsDropped();

        MinecraftServer server = player.getServer();
        if (server != null) server.execute(() -> {
            Optional<GraveComponent> maybeComponent = DeathInfoManager.INSTANCE.getGrave(graveId);
            maybeComponent.ifPresentOrElse(component -> {
                ResolvableProfile owner = component.getOwner();
                UUID uuid = owner.id().orElse(null);
                String playerName = owner.name().orElse(null);
                ServerPlayer restoringPlayer = uuid != null ?
                        server.getPlayerList().getPlayer(uuid) : playerName != null ?
                        server.getPlayerList().getPlayerByName(playerName) : null;

                if (restoringPlayer == null) {
                    player.sendSystemMessage(Component.translatable("text.yigd.command.restore.fail.offline_player"));
                    return;
                }

                component.applyToPlayer(restoringPlayer, restoringPlayer.serverLevel(), restoringPlayer.position(), true, dropRule -> switch (dropRule) {
                    case KEEP -> itemsKept;
                    case DESTROY -> itemsDeleted;
                    case DROP -> itemsDropped;
                    case PUT_IN_GRAVE -> itemsInGrave;
                });
                if (itemsInGrave) {
                    component.setStatus(GraveStatus.CLAIMED);

                    component.removeGraveBlock();
                }

                player.sendSystemMessage(Component.translatable("text.yigd.command.restore.success"));
            }, () -> player.sendSystemMessage(Component.translatable("text.yigd.command.restore.fail")));
        });
    }
    public static void robGrave(RobGraveC2SPacket payload, IPayloadContext context) {
        YigdConfig config = YigdConfig.getConfig();
        Player player = context.player();
        if (!player.hasPermissions(config.commandConfig.robPermissionLevel)) {
            player.sendSystemMessage(Component.translatable("text.yigd.command.permission_fail"));
            return;
        }

        UUID graveId = payload.graveId();
        boolean itemsInGrave = payload.itemsInGrave();
        boolean itemsDeleted = payload.itemsDeleted();
        boolean itemsKept = payload.itemsKept();
        boolean itemsDropped = payload.itemsDropped();

        MinecraftServer server = player.getServer();

        if (server != null) server.execute(() -> {
            Optional<GraveComponent> maybeComponent = DeathInfoManager.INSTANCE.getGrave(graveId);
            maybeComponent.ifPresentOrElse(component -> {
                component.applyToPlayer((ServerPlayer) player, (ServerLevel) player.level(), player.position(), false, dropRule -> switch (dropRule) {
                    case KEEP -> itemsKept;
                    case DESTROY -> itemsDeleted;
                    case DROP -> itemsDropped;
                    case PUT_IN_GRAVE -> itemsInGrave;
                });

                if (itemsInGrave) {
                    component.setStatus(GraveStatus.CLAIMED);

                    component.removeGraveBlock();
                }

                player.sendSystemMessage(Component.translatable("text.yigd.command.rob.success"));
            }, () -> player.sendSystemMessage(Component.translatable("text.yigd.command.rob.fail")));
        });
    }
    public static void updateConfig(UpdateConfigC2SPacket payload, IPayloadContext context) {
        Player player = context.player();

        ClaimPriority claimPriority = payload.claiming();
        ClaimPriority robPriority = payload.robbing();

        UUID playerId = player.getUUID();
        Yigd.CLAIM_PRIORITIES.put(playerId, claimPriority);
        Yigd.ROB_PRIORITIES.put(playerId, robPriority);

        Yigd.LOGGER.info("Priority overwritten for player {}. Claiming: {} / Robbing: {}", player.getGameProfile().getName(), claimPriority.name(), robPriority.name());
    }
}
