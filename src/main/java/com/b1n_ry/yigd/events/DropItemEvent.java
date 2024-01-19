package com.b1n_ry.yigd.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

/**
 * Here custom logic for item drops can be applied. If any items from any mod has a special drop function,
 * this event can be used.
 * Vanilla minecraft doesn't use this, but it exists if any mod wants to use it.
 */
public interface DropItemEvent {
    Event<DropItemEvent> EVENT = EventFactory.createArrayBacked(DropItemEvent.class, dropItemEvents -> (stack, x, y, z, world) -> {
        boolean allow = true;
        for (DropItemEvent event : dropItemEvents) {
            allow = allow && event.shouldDropItem(stack, x, y, z, world);
        }
        return allow;
    });

    boolean shouldDropItem(ItemStack stack, double x, double y, double z, ServerWorld world);
}
