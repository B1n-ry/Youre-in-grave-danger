package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.util.DropRule;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface DropRuleEvent {
    Event<DropRuleEvent> EVENT = EventFactory.createArrayBacked(DropRuleEvent.class, destroyItems -> (item, slot, context) -> {
        for (DropRuleEvent event : destroyItems) {
            DropRule dropRule = event.getDropRule(item, slot, context);
            if (dropRule != DropRule.PUT_IN_GRAVE) {
                return dropRule;
            }
        }
        return DropRule.PUT_IN_GRAVE;
    });

    DropRule getDropRule(ItemStack item, int slot, @Nullable DeathContext deathContext);
}
