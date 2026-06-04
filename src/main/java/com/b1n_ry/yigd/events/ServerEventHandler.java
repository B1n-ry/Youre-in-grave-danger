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
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3d;

import java.util.*;

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
        if (player.onGround()) {
            SubLevelAccess subLevel = SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(player);
            if (subLevel != null) {
                player.setData(Yigd.LAST_GROUND_POS, subLevel.logicalPose().transformPositionInverse(player.position()));
            } else {
                player.setData(Yigd.LAST_GROUND_POS, player.position());
            }
        }
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
    public void onPlayerDeathDropItems(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID playerId = player.getUUID();

        DeathHandler unfinished = Yigd.UNFINISHED_DEATHS.remove(playerId);
        if (unfinished != null) {
            Collection<ItemEntity> drops = event.getDrops();
            for (ItemEntity itemEntity : drops) {
                unfinished.addItem(itemEntity.getItem());
            }
            drops.clear();
            unfinished.finalizeDeath();
        } else {
            Yigd.LOGGER.error("Did not find cached death handler for {}. Can't generate player loot", player.getGameProfile().getName());
        }
    }

    /**
     * This event is used to handle deaths.
     * It stores the death information in a map for later processing. Note that THIS WILL CLEAR THE INVENTORY so that
     * items are not dropped twice.
     * Event priority is set to LOWEST to ensure other mods can manipulate certain items first.
     * Mods which would handle inventory drops in this method (in my opinion) are doing it wrong,
     * and should use the {@link YigdEvents.DropItemEvent} instead.
     * I'll argue that I'm in the right though, since I have to make sure that I keep track of all slots
     * (can't be done in DropItemEvent), and since I run this with lowest priority, it should be fine.
     * Additionally, this event has to run after some events that can stop the death from happening all together.
     * <br>
     * If you have any questions or concerns about this, feel free to open an issue on the GitHub repository.
     *
     * @param event Death event
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();

        if (!(entity instanceof ServerPlayer player)) return;

        if (Yigd.UNFINISHED_DEATHS.containsKey(player.getUUID())) {
            Yigd.LOGGER.warn("Player '{}' already has a registered, unhandled death. Ignoring this one", player.getGameProfile().getName());
            return;
        }
        if (!player.isDeadOrDying()) {
            Yigd.LOGGER.error("Player '{}' is not dead, but LivingDeathEvent was called. This should not happen. Ignoring this event", player.getGameProfile().getName());
            return;
        }
        if (player.isSpectator()) return;

        ServerLevel level = player.serverLevel();
        DamageSource damageSource = event.getSource();

        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;

        Yigd.UNFINISHED_DEATHS.put(player.getUUID(), new DeathHandler(player, level, player.position(), damageSource));
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

                if (SableCompanion.INSTANCE.isInPlotGrid(latest.getWorld(), gravePos)) {
                    Vector3d temp = SableCompanion.INSTANCE.projectOutOfSubLevel(latest.getWorld(), new Vector3d(gravePos.getX(), gravePos.getY(), gravePos.getZ()));
                    String formattedX = String.format("%.1f", temp.x());
                    String formattedY = String.format("%.1f", temp.y());
                    String formattedZ = String.format("%.1f", temp.z());
                    newPlayer.sendSystemMessage(Component.translatable("text.yigd.message.grave_location_sublevel",
                            formattedX, formattedY, formattedZ,
                            latest.getWorldRegistryKey().location().toString()));
                } else {
                    newPlayer.sendSystemMessage(Component.translatable("text.yigd.message.grave_location",
                            gravePos.getX(), gravePos.getY(), gravePos.getZ(),
                            latest.getWorldRegistryKey().location().toString()));
                }
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

            if (SableCompanion.INSTANCE.isInPlotGrid(component.getWorld(), lastGravePos)) {
                Vector3d temp = SableCompanion.INSTANCE.projectOutOfSubLevel(component.getWorld(), new Vector3d(lastGravePos.getX(), lastGravePos.getY(), lastGravePos.getZ()));
                String formattedX = String.format("%.1f", temp.x());
                String formattedY = String.format("%.1f", temp.y());
                String formattedZ = String.format("%.1f", temp.z());
                player.server.sendSystemMessage(Component.translatable("text.yigd.message.sellout_player_sublevel",
                        loggedOffProfile.name().orElse("PLAYER_NOT_FOUND"), formattedX, formattedY, formattedZ,
                        component.getWorldRegistryKey().location().toString()));
            } else {
                player.server.sendSystemMessage(Component.translatable("text.yigd.message.sellout_player",
                        loggedOffProfile.name().orElse("PLAYER_NOT_FOUND"), lastGravePos.getX(), lastGravePos.getY(), lastGravePos.getZ(),
                        component.getWorldRegistryKey().location().toString()));
            }
        }
    }
}
