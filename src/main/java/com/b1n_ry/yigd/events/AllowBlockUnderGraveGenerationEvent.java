package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.components.GraveComponent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface AllowBlockUnderGraveGenerationEvent {
    Event<AllowBlockUnderGraveGenerationEvent> EVENT = EventFactory.createArrayBacked(AllowBlockUnderGraveGenerationEvent.class, events -> grave -> {
        for (AllowBlockUnderGraveGenerationEvent event : events) {
            if (!event.allowBlockGeneration(grave))
                return false;
        }

        return true;
    });

    boolean allowBlockGeneration(GraveComponent grave);
}
