package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.data.DeathInfoManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.world.ServerWorld;

public class ServerEventHandler {
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Clear and load from new stored data
            DeathInfoManager.INSTANCE.clear();

            ServerWorld overworld = server.getOverworld();
            DeathInfoManager.INSTANCE = (DeathInfoManager) overworld.getPersistentStateManager().getOrCreate(nbt -> DeathInfoManager.fromNbt(nbt, server), () -> new DeathInfoManager(server), "yigd_data");
        });
    }
}
