package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ServerEventHandler {
    public static void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Clear and load from new stored data
            DeathInfoManager.INSTANCE.clear();

            ServerLevel overworld = server.overworld();
            DeathInfoManager.INSTANCE = overworld.getDataStorage().computeIfAbsent(DeathInfoManager.getPersistentStateType(server), "yigd_data");
            DeathInfoManager.INSTANCE.setDirty();
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) return;

            BeforeSoulboundEvent.EVENT.invoker().beforeSoulbound(oldPlayer, newPlayer);

            ResolvableProfile newProfile = new ResolvableProfile(newPlayer.getGameProfile());
            Optional<RespawnComponent> respawnComponent = DeathInfoManager.INSTANCE.getRespawnComponent(newProfile);
            respawnComponent.ifPresent(component -> component.apply(newPlayer));

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
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            List<Runnable> tickFunctions = new ArrayList<>(Yigd.END_OF_TICK);
            Yigd.END_OF_TICK.clear();
            for (Runnable function : tickFunctions) {
                function.run();
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            YigdConfig config = YigdConfig.getConfig();
            if (!config.graveConfig.sellOutOfflinePeople) return;

            ResolvableProfile loggedOffProfile = new ResolvableProfile(handler.player.getGameProfile());
            List<GraveComponent> loggedOffGraves = DeathInfoManager.INSTANCE.getBackupData(loggedOffProfile);
            List<GraveComponent> loggedOffUnclaimed = new ArrayList<>(loggedOffGraves);
            loggedOffGraves.removeIf(c -> c.getStatus() == GraveStatus.UNCLAIMED);
            if (!loggedOffUnclaimed.isEmpty()) {
                GraveComponent component = loggedOffUnclaimed.getFirst();
                BlockPos lastGravePos = component.getPos();
                server.sendSystemMessage(Component.translatable("text.yigd.message.sellout_player",
                        loggedOffProfile.name().orElse("PLAYER_NOT_FOUND"), lastGravePos.getX(), lastGravePos.getY(), lastGravePos.getZ(),
                        component.getWorldRegistryKey().location().toString()));
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            YigdConfig.GraveConfig.GraveRobbing robConfig = YigdConfig.getConfig().graveConfig.graveRobbing;
            UUID joiningId = handler.player.getUUID();

            if (!Yigd.NOT_NOTIFIED_ROBBERIES.containsKey(joiningId)) return;

            // Check if notifying when robbed is not required, since it has to be set to true for players to be added to NOT_NOTIFIED_ROBBERIES
            if (robConfig.tellWhoRobbed) {
                List<String> robbedBy = Yigd.NOT_NOTIFIED_ROBBERIES.remove(joiningId);
                for (String robber : robbedBy) {
                    handler.player.sendSystemMessage(Component.translatable("text.yigd.message.inform_robbery.with_details", robber));
                }
            } else {
                Yigd.NOT_NOTIFIED_ROBBERIES.remove(joiningId);
                handler.player.sendSystemMessage(Component.translatable("text.yigd.message.inform_robbery"));
            }
        });
    }
}
