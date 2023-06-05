package com.b1n_ry.yigd.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface GraveGenerationEvent {
    Event<GraveGenerationEvent> EVENT = EventFactory.createArrayBacked(GraveGenerationEvent.class, graveGenerationEvents -> (world, pos) -> {
        for (GraveGenerationEvent event : graveGenerationEvents) {
            if (!event.canGenerateAt(world, pos))
                return false;
        }
        return true;
    });

    boolean canGenerateAt(ServerWorld world, BlockPos pos);
}
