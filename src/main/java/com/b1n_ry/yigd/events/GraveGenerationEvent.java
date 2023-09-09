package com.b1n_ry.yigd.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface GraveGenerationEvent {
    Event<GraveGenerationEvent> EVENT = EventFactory.createArrayBacked(GraveGenerationEvent.class, graveGenerationEvents -> (world, pos, nthTry) -> {
        for (GraveGenerationEvent event : graveGenerationEvents) {
            if (!event.canGenerateAt(world, pos, nthTry))
                return false;
        }
        return true;
    });

    /**
     * Returns if a grave can generate at the given position
     * @param world The world the grave generates in
     * @param pos Which position the grave generates at
     * @param nthTry Which attempt on this position the grave is trying to check. Should be used to return true when equal
     *               to some specific number, or the grave will attempt all valid locations 50 times, which is not good
     * @return Weather or not the grave can generate at the given position under the given attempt number/iteration
     */
    boolean canGenerateAt(ServerWorld world, BlockPos pos, int nthTry);
}
