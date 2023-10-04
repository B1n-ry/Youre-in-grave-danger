package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.data.DeathContext;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface AllowGraveGenerationEvent {
    Event<AllowGraveGenerationEvent> EVENT = EventFactory.createArrayBacked(AllowGraveGenerationEvent.class, allowGraveGenerationEvents -> (context, grave) -> {
        for (AllowGraveGenerationEvent event : allowGraveGenerationEvents) {
            if (!event.allowGeneration(context, grave))
                return false;
        }
        return true;
    });

    /**
     * Determines weather or not the grave should generate based on conditions when
     * player has just died. Filters are not yet applied here.
     * @param context Context to how the player died. Used to filter out some death causes if
     *                mod is configured to.
     * @param grave The grave component that would generate the grave
     * @return Weather or not the grave should generate. If true, grave contents are
     * instead dropped.
     */
    boolean allowGeneration(DeathContext context, GraveComponent grave);
}
