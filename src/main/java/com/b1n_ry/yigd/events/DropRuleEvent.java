package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.util.GraveOverrideAreas;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public interface DropRuleEvent {
    Event<DropRuleEvent> EVENT = EventFactory.createArrayBacked(DropRuleEvent.class, destroyItems -> (item, slot, context, modify) -> {
        for (DropRuleEvent event : destroyItems) {
            DropRule dropRule = event.getDropRule(item, slot, context, modify);
            if (dropRule != GraveOverrideAreas.INSTANCE.defaultDropRule) {
                return dropRule;
            }
        }
        return GraveOverrideAreas.INSTANCE.defaultDropRule;
    });

    DropRule getDropRule(ItemStack item, int slot, @Nullable DeathContext deathContext, boolean modify);
}
