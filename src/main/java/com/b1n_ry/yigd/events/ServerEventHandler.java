package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.DeathHandler;
import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.data.DeathInfoManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServerEventHandler {
    public static void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Clear and load from new stored data
            DeathInfoManager.INSTANCE.clear();

            ServerWorld overworld = server.getOverworld();
            DeathInfoManager.INSTANCE = (DeathInfoManager) overworld.getPersistentStateManager().getOrCreate(nbt -> DeathInfoManager.fromNbt(nbt, server), () -> new DeathInfoManager(server), "yigd_data");
            DeathInfoManager.INSTANCE.markDirty();
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, ignoredAmount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                DeathHandler deathHandler = new DeathHandler();
                deathHandler.onPlayerDeath(player, player.getServerWorld(), player.getPos(), damageSource);
            }
            return true;
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) return;

            Optional<RespawnComponent> respawnComponent = DeathInfoManager.INSTANCE.getRespawnComponent(newPlayer.getGameProfile());
            respawnComponent.ifPresent(component -> component.apply(newPlayer));
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            List<Runnable> tickFunctions = new ArrayList<>(Yigd.END_OF_TICK);
            Yigd.END_OF_TICK.clear();
            for (Runnable function : tickFunctions) {
                function.run();
            }
        });
    }
}
