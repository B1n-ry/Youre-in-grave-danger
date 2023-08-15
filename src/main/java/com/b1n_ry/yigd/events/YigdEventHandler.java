package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.config.YigdConfig;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

public class YigdEventHandler {
    public static void registerEventCallbacks() {
        GraveClaimEvent.EVENT.register((player, world, pos, grave, tool) -> {
            YigdConfig config = YigdConfig.getConfig();
            if (config.graveConfig.requireShovelToLoot && !tool.isIn(ItemTags.SHOVELS)) return false;

            return player.getUuid().equals(grave.getOwner().getId());
        });

        AllowGraveGenerationEvent.EVENT.register((config, context, grave) -> {
            YigdConfig.GraveConfig graveConfig = config.graveConfig;
            if (!graveConfig.enabled) return false;

            if (!graveConfig.generateEmptyGraves && grave.isEmpty()) return false;

            if (graveConfig.dimensionBlacklist.contains(grave.getWorldRegistryKey().getValue().toString())) return false;

            if (!graveConfig.generateGraveInVoid && grave.getPos().getY() < 0) return false;

            if (graveConfig.requireItem) {
                Item item = Registries.ITEM.get(new Identifier(graveConfig.requiredItem));
                if (!grave.getInventoryComponent().removeItem(stack -> stack.isOf(item), 1)) {
                    return false;
                }
            }

            return !graveConfig.ignoredDeathTypes.contains(context.getDeathSource().getName());
        });
    }
}
