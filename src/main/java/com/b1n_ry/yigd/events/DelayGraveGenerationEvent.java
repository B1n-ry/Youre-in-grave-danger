package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.data.DeathContext;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.math.Direction;

public interface DelayGraveGenerationEvent {
    Event<DelayGraveGenerationEvent> EVENT = EventFactory.createArrayBacked(DelayGraveGenerationEvent.class,
            events -> (grave, direction, context, respawnComponent, caller) -> {
        for (DelayGraveGenerationEvent event : events) {
            // We want to stop at first match, so not multiple callers will get triggered,
            // if multiple independent ones are set up to only trigger when this event fires
            if (event.skipGenerationCall(grave, direction, context, respawnComponent, caller))
                return true;
        }
        return false;
    });

    /**
     * Weather or not grave generation calls are skipped or not. Can be used to delay grave generation to later (E.g. at respawn)
     * @param grave Grave-component that would generate the grave
     * @param direction Player facing direction
     * @param context Death context for when player dies
     * @param respawnComponent Respawn component for when player respawns
     * @param caller Unique string identifier to identify the caller. Mod ID recommended to use here
     * @return Weather or not this iteration should be skipped
     */
    boolean skipGenerationCall(GraveComponent grave, Direction direction, DeathContext context, RespawnComponent respawnComponent, String caller);
}
