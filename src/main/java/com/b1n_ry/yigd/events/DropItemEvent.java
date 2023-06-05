package com.b1n_ry.yigd.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public interface DropItemEvent {
    Event<DropItemEvent> EVENT = EventFactory.createArrayBacked(DropItemEvent.class, dropItemEvents -> (stack, x, y, z, world) -> {
        for (DropItemEvent event : dropItemEvents) {
            if (!event.shouldDropItem(stack, x, y, z, world)) {
                return false;
            }
        }
        return true;
    });

    boolean shouldDropItem(ItemStack stack, double x, double y, double z, World world);
}
