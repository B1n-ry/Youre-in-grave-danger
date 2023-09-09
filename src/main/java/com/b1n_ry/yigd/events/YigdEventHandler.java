package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.util.YigdTags;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

public class YigdEventHandler {
    public static void registerEventCallbacks() {
        registerPermissionEvents();

        GraveClaimEvent.EVENT.register((player, world, pos, grave, tool) -> {
            YigdConfig config = YigdConfig.getConfig();
            if (config.graveConfig.requireShovelToLoot && !tool.isIn(ItemTags.SHOVELS)) return false;

            if (player.getUuid().equals(grave.getOwner().getId())) return true;
            if (!grave.isLocked()) return true;

            YigdConfig.GraveConfig.GraveRobbing robConfig = config.graveConfig.graveRobbing;
            if (!robConfig.enabled) return false;

            final int tps = 20;  // ticks per second
            if (!grave.hasExistedMs(robConfig.timeUnit.toSeconds(robConfig.afterTime) * tps)) return false;

            return robConfig.onlyMurderer && player.getUuid().equals(grave.getKillerId());
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

        GraveGenerationEvent.EVENT.register((world, pos, nthTry) -> {
            BlockState state = world.getBlockState(pos);
            YigdConfig.GraveConfig config = YigdConfig.getConfig().graveConfig;
            switch (nthTry) {
                case 0 -> {
                    if (!config.useSoftBlockWhitelist) return false;
                    if (!state.isIn(YigdTags.REPLACE_SOFT_WHITELIST)) return false;
                }
                case 1 -> {
                    if (!config.useStrictBlockBlacklist) return false;
                    if (state.isIn(YigdTags.KEEP_STRICT_BLACKLIST)) return false;
                }
            }
            return true;
        });
    }

    /**
     * Will register permission checks YiGD uses, appropriate to configs (and possibly other stuff)
     */
    private static void registerPermissionEvents() {
        PermissionCheckEvent.EVENT.register((source, permission) -> {
            if (permission.equals("yigd.command.locking") && !YigdConfig.getConfig().graveConfig.unlockable) {
                return TriState.FALSE;
            }
            return TriState.DEFAULT;
        });
    }
}
