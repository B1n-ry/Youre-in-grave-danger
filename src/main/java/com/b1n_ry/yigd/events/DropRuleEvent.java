package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.util.DropRule;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.item.ItemStack;

public interface DropRuleEvent {
    Event<DropRuleEvent> DESTROY_ITEM_EVENT = EventFactory.createArrayBacked(DropRuleEvent.class, destroyItems -> (item, slot, context) -> {
        for (DropRuleEvent event : destroyItems) {
            DropRule dropRule = event.getDropRule(item, slot, context);
            if (dropRule != DropRule.DROP) {
                return dropRule;
            }
        }
        return DropRule.DROP;
    });

    DropRule getDropRule(ItemStack item, int slot, DeathContext deathContext);
}
