package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.compat.InvModCompat;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import java.util.List;

public interface LoadModCompatEvent {
    Event<LoadModCompatEvent> EVENT = EventFactory.createArrayBacked(LoadModCompatEvent.class, events -> mods -> {
        for (LoadModCompatEvent event : events) {
            event.loadModCompat(mods);
        }
    });

    void loadModCompat(List<InvModCompat<?>> invCompatMods);
}
