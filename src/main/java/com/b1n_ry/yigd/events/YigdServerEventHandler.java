package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.config.YigdConfig.ExtraFeatures.GraveKeyConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.data.ListMode;
import com.b1n_ry.yigd.util.YigdTags;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class YigdServerEventHandler {
    public static void registerEventCallbacks() {
        registerPermissionEvents();

        DropRuleEvent.EVENT.register((item, slot, context) -> {
            YigdConfig config = YigdConfig.getConfig();

            if (config.inventoryConfig.soulboundSlots.contains(slot)) return DropRule.KEEP;
            if (config.inventoryConfig.vanishingSlots.contains(slot)) return DropRule.DESTROY;

            if (item.isIn(YigdTags.NATURAL_SOULBOUND)) return DropRule.KEEP;
            if (item.isIn(YigdTags.NATURAL_VANISHING)) return DropRule.DESTROY;

            DropRule dropRule = DropRule.DROP;

            // Get drop rule from enchantment. This is set up so that the first drop rule related enchantment will take effect, no matter what more enchantments there are
            NbtList enchantmentsNbt = item.getEnchantments();
            Set<NbtCompound> removeEnchantment = new HashSet<>();
            for (NbtElement enchantmentElement : enchantmentsNbt) {
                if (!(enchantmentElement instanceof NbtCompound enchantNbt)) continue;

                String id = enchantNbt.getString("id");
                if (config.inventoryConfig.vanishingEnchantments.contains(id)) {
                    return DropRule.DESTROY;
                }
                if (!config.inventoryConfig.soulboundEnchantments.contains(id)) continue;

                int level = enchantNbt.getInt("lvl");
                if (config.inventoryConfig.loseSoulboundLevelOnDeath) {
                    if (level == 1) {
                        removeEnchantment.add(enchantNbt);
                    }
                    else {
                        enchantNbt.putInt("lvl", level - 1);
                    }
                }
                dropRule = DropRule.KEEP;  // Do not return value, since enchantment might have to be deleted if it was level 1 and should be deleted
                break; // Break the loop. This way if 2 soulbound enchantments are on the item, only one is "consumed"
            }

            enchantmentsNbt.removeAll(removeEnchantment);

            return dropRule;
        });

        GraveClaimEvent.EVENT.register((player, world, pos, grave, tool) -> {
            YigdConfig config = YigdConfig.getConfig();

            if (config.extraFeatures.graveKeys.enabled) {
                if (tool.isOf(Yigd.GRAVE_KEY_ITEM)) {
                    NbtCompound nbt = tool.getOrCreateNbt();
                    NbtCompound userNbt = nbt.getCompound("user");
                    NbtElement uuidNbt = nbt.get("grave");
                    GraveKeyConfig.KeyTargeting targeting = config.extraFeatures.graveKeys.targeting;
                    switch (targeting) {
                        case ANY_GRAVE -> {
                            tool.decrement(1);
                            return true;
                        }
                        case PLAYER_GRAVE -> {
                            if (userNbt != null && Objects.equals(NbtHelper.toGameProfile(userNbt), grave.getOwner())) {
                                tool.decrement(1);
                                return true;
                            }
                        }
                        case SPECIFIC_GRAVE -> {
                            if (uuidNbt != null && Objects.equals(NbtHelper.toUuid(uuidNbt), grave.getGraveId())) {
                                tool.decrement(1);
                                return true;
                            }
                        }
                    }
                }
                if (config.extraFeatures.graveKeys.required) return false;  // The grave key didn't work
            }

            if (config.graveConfig.requireShovelToLoot && !tool.isIn(ItemTags.SHOVELS)) return false;

            if (player.getUuid().equals(grave.getOwner().getId())) return true;
            if (!grave.isLocked()) return true;

            YigdConfig.GraveConfig.GraveRobbing robConfig = config.graveConfig.graveRobbing;
            if (!robConfig.enabled) return false;

            final int tps = 20;  // ticks per second
            if (!grave.hasExistedMs(robConfig.timeUnit.toSeconds(robConfig.afterTime) * tps)) return false;

            return robConfig.onlyMurderer && player.getUuid().equals(grave.getKillerId());
        });

        AllowGraveGenerationEvent.EVENT.register((context, grave) -> {
            YigdConfig.GraveConfig graveConfig = YigdConfig.getConfig().graveConfig;
            if (!graveConfig.enabled) return false;

            if (DeathInfoManager.INSTANCE.getGraveListMode() == ListMode.WHITELIST && !DeathInfoManager.INSTANCE.isInList(context.getPlayer().getGameProfile())) return false;
            if (DeathInfoManager.INSTANCE.getGraveListMode() == ListMode.BLACKLIST && DeathInfoManager.INSTANCE.isInList(context.getPlayer().getGameProfile())) return false;

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
        AllowBlockUnderGraveGenerationEvent.EVENT.register(
                (grave, currentUnder) -> YigdConfig.getConfig().graveConfig.blockUnderGrave.enabled && currentUnder.isIn(YigdTags.REPLACE_SOFT_WHITELIST));

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
