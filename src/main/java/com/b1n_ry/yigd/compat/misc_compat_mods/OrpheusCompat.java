package com.b1n_ry.yigd.compat.misc_compat_mods;

import com.b1n_ry.yigd.events.AdjustDropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import com.ecarrascon.orpheus.Orpheus;
import com.ecarrascon.orpheus.registry.ItemsRegistry;

public class OrpheusCompat {
    public static void init() {
        AdjustDropRuleEvent.EVENT.register((inventory, context) -> {
            String orpheusLyrePower = Orpheus.CONFIG_VALUES.getOrpheusLyrePower();

            if (!orpheusLyrePower.equals("keep") && !orpheusLyrePower.equals("both")) return;

            if (inventory.containsAny(stack -> stack.isOf(ItemsRegistry.ORPHEUS_LYRE.get()), s -> true, i -> true)) {
                inventory.handleItemPairs(mod -> true, (item, slot, pair) -> pair.setRight(DropRule.KEEP));
            }
        });
    }
}
