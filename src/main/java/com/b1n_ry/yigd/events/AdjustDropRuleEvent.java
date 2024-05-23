package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.data.DeathContext;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface AdjustDropRuleEvent {
    Event<AdjustDropRuleEvent> EVENT = EventFactory.createArrayBacked(AdjustDropRuleEvent.class, events -> (inventory, context) -> {
        for (AdjustDropRuleEvent event : events) {
            event.adjustDropRules(inventory, context);
        }
    });

    void adjustDropRules(InventoryComponent inventoryComponent, DeathContext context);
}
