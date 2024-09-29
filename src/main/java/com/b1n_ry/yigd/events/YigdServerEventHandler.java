package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.config.YigdConfig.ExtraFeatures.GraveKeyConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.data.ListMode;
import com.b1n_ry.yigd.util.GraveCompassHelper;
import com.b1n_ry.yigd.util.GraveOverrideAreas;
import com.b1n_ry.yigd.util.YigdTags;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class YigdServerEventHandler {
    public static void registerEventCallbacks() {
        registerPermissionEvents();

        DropRuleEvent.EVENT.register((item, slot, context, modify) -> {
            YigdConfig config = YigdConfig.getConfig();

            StatusEffect statusEffect = Registries.STATUS_EFFECT.get(Identifier.of("amethyst_imbuement", "soulbinding"));
            RegistryEntry<StatusEffect> statusEffectEntry = Registries.STATUS_EFFECT.getEntry(statusEffect);

            if (config.inventoryConfig.soulboundSlots.contains(slot)) return DropRule.KEEP;
            if (config.inventoryConfig.vanishingSlots.contains(slot)) return DropRule.DESTROY;
            if (config.inventoryConfig.dropOnGroundSlots.contains(slot)) return DropRule.DROP;

            if (item.isIn(YigdTags.NATURAL_SOULBOUND)) return DropRule.KEEP;
            if (item.isIn(YigdTags.NATURAL_VANISHING)) return DropRule.DESTROY;
            if (item.isIn(YigdTags.GRAVE_INCOMPATIBLE)) return DropRule.DROP;

            if (statusEffect != null && context != null && context.player().hasStatusEffect(statusEffectEntry))
                return DropRule.KEEP;

            if (!item.isEmpty() && item.contains(DataComponentTypes.CUSTOM_DATA)) {
                NbtComponent nbt = item.get(DataComponentTypes.CUSTOM_DATA);
                assert nbt != null;  // This should never be null, but it's sorta required for intelliJ to not complain
                NbtCompound itemNbt = nbt.copyNbt();
                if (itemNbt.contains("Botania_keepIvy") && itemNbt.getBoolean("Botania_keepIvy")) {
                    if (modify) {
                        NbtComponent replaced = nbt.apply(nbtCompound -> nbtCompound.remove("Botania_keepIvy"));
                        item.set(DataComponentTypes.CUSTOM_DATA, replaced);
                    }

                    return DropRule.KEEP;
                }
            }

            DropRule dropRule;
            if (context != null)
                dropRule = GraveOverrideAreas.INSTANCE.getDropRuleFromArea(BlockPos.ofFloored(context.deathPos()), context.world());
            else
                dropRule = GraveOverrideAreas.INSTANCE.defaultDropRule;


            // Get drop rule from enchantment
            if (EnchantmentHelper.hasAnyEnchantmentsIn(item, YigdTags.VANISHING))
                return DropRule.DESTROY;

            if (EnchantmentHelper.hasAnyEnchantmentsIn(item, YigdTags.SOULBOUND)) {
                if (config.inventoryConfig.loseSoulboundLevelOnDeath) {
                    for (RegistryEntry<Enchantment> enchantmentRegistryEntry : EnchantmentHelper.getEnchantments(item).getEnchantments()) {
                        if (!enchantmentRegistryEntry.isIn(YigdTags.SOULBOUND))
                            continue;

                        int level = EnchantmentHelper.getLevel(enchantmentRegistryEntry, item);
                        if (level > 1) EnchantmentHelper.apply(item, builder -> builder.set(enchantmentRegistryEntry, level - 1));
                        else EnchantmentHelper.apply(item, builder -> builder.remove(enchant -> enchant.equals(enchantmentRegistryEntry)));
                    }
                }
                return DropRule.KEEP;
            }

            return dropRule;
        });

        GraveClaimEvent.EVENT.register((player, world, pos, grave, tool) -> {
            if (player.isDead()) return false;

            YigdConfig config = YigdConfig.getConfig();

            if (config.extraFeatures.graveCompass.consumeOnUse || config.extraFeatures.graveCompass.pointToClosest != YigdConfig.ExtraFeatures.GraveCompassConfig.CompassGraveTarget.DISABLED) {
                PlayerInventory inventory = player.getInventory();
                for (int i = 0; i < inventory.size(); i++) {
                    ItemStack stack = inventory.getStack(i);
                    if (!stack.isOf(Items.COMPASS)) continue;

                    if (config.extraFeatures.graveCompass.consumeOnUse) {
                        NbtComponent stackNbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
                        NbtCompound stackNbt = stackNbtComponent != null ? stackNbtComponent.copyNbt() : null;
                        if (stack.isOf(Items.COMPASS) && stackNbt != null && stackNbt.contains("linked_grave")) {
                            UUID graveId = stackNbt.getUuid("linked_grave");
                            if (graveId.equals(grave.getGraveId())) {
                                stack.setCount(0);
                                break;
                            }
                        }
                    } else {  // Redirect closest grave pointer
                        GraveCompassHelper.updateClosestNbt(world.getRegistryKey(), player.getBlockPos(), player.getUuid(), stack);
                    }
                }
            }

            if (config.extraFeatures.graveKeys.enabled) {
                if (tool.isOf(Yigd.GRAVE_KEY_ITEM)) {
                    NbtComponent nbtComponent = tool.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                    NbtCompound nbt = nbtComponent.copyNbt();
                    NbtCompound userNbt = nbt.getCompound("user");
                    NbtElement uuidNbt = nbt.get("grave");
                    GraveKeyConfig.KeyTargeting targeting = config.extraFeatures.graveKeys.targeting;
                    switch (targeting) {
                        case ANY_GRAVE -> {
                            tool.decrement(1);
                            return true;
                        }
                        case PLAYER_GRAVE -> {
                            if (Objects.equals(ProfileComponent.CODEC.parse(NbtOps.INSTANCE, userNbt).result().orElse(null), grave.getOwner())) {
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
                if (config.extraFeatures.graveKeys.required) {
                    player.sendMessage(Text.translatable("text.yigd.message.missing_key"), true);
                    return false;  // The grave key didn't work
                }
            }

            if (config.graveConfig.requireShovelToLoot && !tool.isIn(ItemTags.SHOVELS)) {
                player.sendMessage(Text.translatable("text.yigd.message.no_shovel"), true);
                return false;
            }

            if (player.getUuid().equals(grave.getOwner().id().orElse(null))) return true;
            if (!grave.isLocked()) return true;

            YigdConfig.GraveConfig.GraveRobbing robConfig = config.graveConfig.graveRobbing;
            if (!robConfig.enabled) return false;

            final int tps = 20;  // ticks per second
            if (!grave.hasExistedTicks(robConfig.timeUnit.toSeconds(robConfig.afterTime) * tps)) {
                player.sendMessage(Text.translatable("text.yigd.message.rob.too_early", grave.getTimeUntilRobbable()), true);
                return false;
            }

            if (robConfig.onlyMurderer && !player.getUuid().equals(grave.getKillerId())) {
                player.sendMessage(Text.translatable("text.yigd.message.rob_not_killer",
                        grave.getOwner().name().orElse("PLAYER_NOT_FOUND")), true);
                return false;
            }

            return true;
        });

        AllowGraveGenerationEvent.EVENT.register((context, grave) -> {
            YigdConfig.GraveConfig graveConfig = YigdConfig.getConfig().graveConfig;
            if (!graveConfig.enabled) return false;

            if ((DeathInfoManager.INSTANCE.getGraveListMode() == ListMode.WHITELIST
                    && !DeathInfoManager.INSTANCE.isInList(new ProfileComponent(context.player().getGameProfile())))
                    || (DeathInfoManager.INSTANCE.getGraveListMode() == ListMode.BLACKLIST
                    && DeathInfoManager.INSTANCE.isInList(new ProfileComponent(context.player().getGameProfile())))) {
                Yigd.LOGGER.info("{} found on whitelist/blacklist, disallowing grave generation", context.player().getGameProfile().getName());
            }

            if (!graveConfig.generateEmptyGraves && grave.isGraveEmpty()) return false;

            if (graveConfig.dimensionBlacklist.contains(grave.getWorldRegistryKey().getValue().toString())) return false;

            if (!graveConfig.generateGraveInVoid && grave.getPos().getY() < context.world().getBottomY()) return false;

            if (graveConfig.requireItem) {
                Item item = Registries.ITEM.get(Identifier.of(graveConfig.requiredItem));
                if (!grave.getInventoryComponent().removeItem(stack -> stack.isOf(item), 1)) {
                    return false;
                }
            }

            return !graveConfig.ignoredDeathTypes.contains(context.deathSource().getName());
        });
        AllowBlockUnderGraveGenerationEvent.EVENT.register(
                (grave, currentUnder) -> YigdConfig.getConfig().graveConfig.blockUnderGrave.enabled && currentUnder.isIn(YigdTags.REPLACE_SOFT_WHITELIST));

        GraveGenerationEvent.EVENT.register((world, pos, nthTry) -> {
            BlockState state = world.getBlockState(pos);
            YigdConfig.GraveConfig config = YigdConfig.getConfig().graveConfig;
            if (world.getBlockEntity(pos) != null)  // Block entities should NOT be replaced by graves
                return false;
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
        DropItemEvent.EVENT.register((stack, x, y, z, world) -> !stack.isEmpty());
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
