package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.components.GraveComponent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;

public interface AllowBlockUnderGraveGenerationEvent {
    Event<AllowBlockUnderGraveGenerationEvent> EVENT = EventFactory.createArrayBacked(AllowBlockUnderGraveGenerationEvent.class, events -> (grave, currentUnder) -> {
        for (AllowBlockUnderGraveGenerationEvent event : events) {
            if (!event.allowBlockGeneration(grave, currentUnder))
                return false;
        }

        return true;
    });

    boolean allowBlockGeneration(GraveComponent grave, BlockState currentUnder);
}
