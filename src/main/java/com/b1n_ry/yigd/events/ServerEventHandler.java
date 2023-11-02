package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.DeathHandler;
import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ServerEventHandler {
    public static void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Clear and load from new stored data
            DeathInfoManager.INSTANCE.clear();

            ServerWorld overworld = server.getOverworld();
            DeathInfoManager.INSTANCE = (DeathInfoManager) overworld.getPersistentStateManager().getOrCreate(nbt -> DeathInfoManager.fromNbt(nbt, server), DeathInfoManager::new, "yigd_data");
            DeathInfoManager.INSTANCE.markDirty();
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) return;

            GameProfile newProfile = newPlayer.getGameProfile();
            Optional<RespawnComponent> respawnComponent = DeathInfoManager.INSTANCE.getRespawnComponent(newProfile);
            respawnComponent.ifPresent(component -> component.apply(newPlayer));

            if (YigdConfig.getConfig().graveConfig.informGraveLocation) {
                List<GraveComponent> graves = new ArrayList<>(DeathInfoManager.INSTANCE.getBackupData(newProfile));
                graves.removeIf(grave -> grave.getStatus() != GraveStatus.UNCLAIMED);
                if (graves.size() > 0) {
                    GraveComponent latest = graves.get(graves.size() - 1);
                    BlockPos gravePos = latest.getPos();
                    newPlayer.sendMessage(Text.translatable("yigd.message.grave_location", gravePos.getX(), gravePos.getY(), gravePos.getZ(), latest.getWorldRegistryKey().getValue()));
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

            GameProfile loggedOffProfile = handler.player.getGameProfile();
            List<GraveComponent> loggedOffGraves = DeathInfoManager.INSTANCE.getBackupData(loggedOffProfile);
            List<GraveComponent> loggedOffUnclaimed = new ArrayList<>(loggedOffGraves);
            loggedOffGraves.removeIf(c -> c.getStatus() == GraveStatus.UNCLAIMED);
            if (loggedOffUnclaimed.size() > 0) {
                GraveComponent component = loggedOffUnclaimed.get(0);
                BlockPos lastGravePos = component.getPos();
                server.sendMessage(Text.translatable("yigd.message.sellout_player",
                        loggedOffProfile.getName(), lastGravePos.getX(), lastGravePos.getY(), lastGravePos.getZ(),
                        component.getWorldRegistryKey().getValue()));
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            YigdConfig.GraveConfig.GraveRobbing robConfig = YigdConfig.getConfig().graveConfig.graveRobbing;
            UUID joiningId = handler.player.getUuid();

            if (!Yigd.NOT_NOTIFIED_ROBBERIES.containsKey(joiningId)) return;

            // Check if notifying when robbed is not required, since it has to be set to true for players to be added to NOT_NOTIFIED_ROBBERIES
            if (robConfig.tellWhoRobbed) {
                List<String> robbedBy = Yigd.NOT_NOTIFIED_ROBBERIES.remove(joiningId);
                for (String robber : robbedBy) {
                    handler.player.sendMessage(Text.translatable("yigd.message.inform_robbery.with_details", robber));
                }
            } else {
                Yigd.NOT_NOTIFIED_ROBBERIES.remove(joiningId);
                handler.player.sendMessage(Text.translatable("yigd.message.inform_robbery"));
            }
        });
    }
}
