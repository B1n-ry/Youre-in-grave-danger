package com.b1n_ry.yigd.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;

public interface BeforeSoulboundEvent {
    Event<BeforeSoulboundEvent> EVENT = EventFactory.createArrayBacked(BeforeSoulboundEvent.class, events -> (oldPlayer, newPlayer) -> {
        for (BeforeSoulboundEvent event : events) {
            event.beforeSoulbound(oldPlayer, newPlayer);
        }
    });

    void beforeSoulbound(ServerPlayer oldPlayer, ServerPlayer newPlayer);
}
