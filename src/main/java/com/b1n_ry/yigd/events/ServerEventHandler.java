package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.DeathHandler;
import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.config.GraveConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.networking.packets.SyncConfigS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.stream.Collectors;

public class ServerEventHandler {
    @SubscribeEvent
    public void onEndOfTick(ServerTickEvent.Post event) {
        List<Runnable> methodsToRun = new ArrayList<>(Yigd.END_OF_TICK);
        Yigd.END_OF_TICK.clear();

        for (Runnable runnable : methodsToRun) {
            runnable.run();
        }
    }

    @SubscribeEvent
    public void endPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.onGround())
            player.setData(Yigd.LAST_GROUND_POS, player.position());
    }

    @SubscribeEvent
    public void serverStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        DeathInfoManager.INSTANCE.clear();

        ServerLevel overworld = server.overworld();
        DeathInfoManager.INSTANCE = overworld.getDataStorage().computeIfAbsent(DeathInfoManager.getPersistentStateType(server), "yigd_data");
        DeathInfoManager.INSTANCE.setDirty();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerDeath(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID playerId = player.getUUID();

        DeathHandler unfinished = Yigd.UNFINISHED_DEATHS.remove(playerId);
        if (unfinished != null) {
            for (ItemEntity itemEntity : event.getDrops()) {
                unfinished.addItem(itemEntity.getItem());
            }
            unfinished.finalizeDeath();
        } else {
            Yigd.LOGGER.error("Did not find cached death handler for {}. Can't generate player loot", player.getGameProfile().getName());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void afterRespawn(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;

        Player newPlayer = event.getEntity();
        Player oldPlayer = event.getOriginal();
        if (newPlayer.level().isClientSide || oldPlayer.level().isClientSide) return;

        NeoForge.EVENT_BUS.post(new YigdEvents.BeforeSoulboundEvent((ServerPlayer) oldPlayer, (ServerPlayer) newPlayer));

        ResolvableProfile newProfile = new ResolvableProfile(newPlayer.getGameProfile());
        Optional<RespawnComponent> respawnComponent = DeathInfoManager.INSTANCE.getRespawnComponent(newProfile);
        respawnComponent.ifPresent(component -> component.apply((ServerPlayer) newPlayer));

        if (YigdConfig.getConfig().graveConfig.informGraveLocation && respawnComponent.isPresent() && respawnComponent.get().wasGraveGenerated()) {
            List<GraveComponent> graves = new ArrayList<>(DeathInfoManager.INSTANCE.getBackupData(newProfile));
            graves.removeIf(grave -> grave.getStatus() != GraveStatus.UNCLAIMED);
            if (!graves.isEmpty()) {
                GraveComponent latest = graves.getLast();
                BlockPos gravePos = latest.getPos();
                newPlayer.sendSystemMessage(Component.translatable("text.yigd.message.grave_location",
                        gravePos.getX(), gravePos.getY(), gravePos.getZ(),
                        latest.getWorldRegistryKey().location().toString()));
            }
        }
    }

    @SubscribeEvent
    public void playerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        YigdConfig config = YigdConfig.getConfig();
        PacketDistributor.sendToPlayer(player, new SyncConfigS2CPacket(
                config.graveConfig.retrieveMethods.onBreak,
                config.graveRendering.useGlowingEffect,
                config.graveRendering.glowingDistance,
                config.extraFeatures.deathSightEnchant.range));

        GraveConfig.GraveRobbing robConfig = config.graveConfig.graveRobbing;
        UUID joiningId = player.getUUID();

        if (!Yigd.NOT_NOTIFIED_ROBBERIES.containsKey(joiningId)) return;

        // Check if notifying when robbed is not required, since it has to be set to true for players to be added to NOT_NOTIFIED_ROBBERIES
        if (robConfig.tellWhoRobbed) {
            List<String> robbedBy = Yigd.NOT_NOTIFIED_ROBBERIES.remove(joiningId);
            for (String robber : robbedBy) {
                player.sendSystemMessage(Component.translatable("text.yigd.message.inform_robbery.with_details", robber));
            }
        } else {
            Yigd.NOT_NOTIFIED_ROBBERIES.remove(joiningId);
            player.sendSystemMessage(Component.translatable("text.yigd.message.inform_robbery"));
        }
    }

    @SubscribeEvent
    public void playerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        YigdConfig config = YigdConfig.getConfig();
        if (!config.graveConfig.sellOutOfflinePeople) return;

        ResolvableProfile loggedOffProfile = new ResolvableProfile(player.getGameProfile());
        List<GraveComponent> loggedOffGraves = DeathInfoManager.INSTANCE.getBackupData(loggedOffProfile);
        List<GraveComponent> loggedOffUnclaimed = new ArrayList<>(loggedOffGraves);
        loggedOffGraves.removeIf(c -> c.getStatus() == GraveStatus.UNCLAIMED);
        if (!loggedOffUnclaimed.isEmpty()) {
            GraveComponent component = loggedOffUnclaimed.getFirst();
            BlockPos lastGravePos = component.getPos();
            player.server.sendSystemMessage(Component.translatable("text.yigd.message.sellout_player",
                    loggedOffProfile.name().orElse("PLAYER_NOT_FOUND"), lastGravePos.getX(), lastGravePos.getY(), lastGravePos.getZ(),
                    component.getWorldRegistryKey().location().toString()));
        }
    }
}
