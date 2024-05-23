package com.b1n_ry.yigd.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

public interface BeforeSoulboundEvent {
    Event<BeforeSoulboundEvent> EVENT = EventFactory.createArrayBacked(BeforeSoulboundEvent.class, events -> (oldPlayer, newPlayer) -> {
        for (BeforeSoulboundEvent event : events) {
            event.beforeSoulbound(oldPlayer, newPlayer);
        }
    });

    void beforeSoulbound(ServerPlayerEntity oldPlayer, ServerPlayerEntity newPlayer);
}
