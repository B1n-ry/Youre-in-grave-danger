package com.b1n_ry.yigd.compat.misc_compat_mods;

import com.b1n_ry.yigd.events.AdjustDropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class TwilightCompat {
    public static void init() {
        AdjustDropRuleEvent.EVENT.register((inventoryComponent, context) -> {
            int selectedSlot = context.player().getInventory().selectedSlot;
            Item keepingCharm3 = Registries.ITEM.get(Identifier.of("twilightforest", "charm_of_keeping_3"));
            Item keepingCharm2 = Registries.ITEM.get(Identifier.of("twilightforest", "charm_of_keeping_2"));
            Item keepingCharm1 = Registries.ITEM.get(Identifier.of("twilightforest", "charm_of_keeping_1"));
            boolean tier3 = inventoryComponent.containsAny(
                    stack -> stack.isOf(keepingCharm3), mod -> true, slot -> true);
            boolean tier2 = tier3 || inventoryComponent.containsAny(
                    stack -> stack.isOf(keepingCharm2), mod -> true, slot -> true);
            boolean tier1 = tier2 || inventoryComponent.containsAny(
                    stack -> stack.isOf(keepingCharm1),
                    mod -> true, slot -> true);

            Item firstMatch = tier3 ? keepingCharm3 :
                    tier2 ? keepingCharm2 :
                    tier1 ? keepingCharm1 :
                    null;
            if (firstMatch == null) return;

            final int hotbarSize = 9;  // We don't know for certain, but we can be pretty confident
            int afterOffhandIndex = inventoryComponent.mainSize + inventoryComponent.armorSize + inventoryComponent.offHandSize;
            inventoryComponent.handleGraveItems(mod -> true, (stack, slot, graveItem) -> {
                if (slot >= inventoryComponent.mainSize && slot < afterOffhandIndex || slot == selectedSlot || slot < 0) {  // Tier 1: Will always be true, otherwise we exit sooner
                    graveItem.dropRule = DropRule.KEEP;
                } else if (tier2 && slot < hotbarSize) {
                    graveItem.dropRule = DropRule.KEEP;
                } else if (tier3) {
                    graveItem.dropRule = DropRule.KEEP;
                }

                if (!stack.isOf(firstMatch)) return;
                graveItem.dropRule = DropRule.DESTROY;
            });
        });
    }
}
