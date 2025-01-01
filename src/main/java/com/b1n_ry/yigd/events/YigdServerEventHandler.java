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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class YigdServerEventHandler {
    public static void registerEventCallbacks() {
        registerPermissionEvents();

        DropRuleEvent.EVENT.register((item, slot, context, modify) -> {
            YigdConfig config = YigdConfig.getConfig();

            MobEffect statusEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("amethyst_imbuement", "soulbinding"));
            Holder<MobEffect> statusEffectEntry = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(statusEffect);

            if (config.inventoryConfig.soulboundSlots.contains(slot)) return DropRule.KEEP;
            if (config.inventoryConfig.vanishingSlots.contains(slot)) return DropRule.DESTROY;
            if (config.inventoryConfig.dropOnGroundSlots.contains(slot)) return DropRule.DROP;

            if (item.is(YigdTags.NATURAL_SOULBOUND)) return DropRule.KEEP;
            if (item.is(YigdTags.NATURAL_VANISHING)) return DropRule.DESTROY;
            if (item.is(YigdTags.GRAVE_INCOMPATIBLE)) return DropRule.DROP;

            if (statusEffect != null && context != null && context.player().hasEffect(statusEffectEntry))
                return DropRule.KEEP;

            if (!item.isEmpty() && item.has(DataComponents.CUSTOM_DATA)) {
                CustomData nbt = item.get(DataComponents.CUSTOM_DATA);
                assert nbt != null;  // This should never be null, but it's sorta required for intelliJ to not complain
                CompoundTag itemNbt = nbt.copyTag();
                if (itemNbt.contains("Botania_keepIvy") && itemNbt.getBoolean("Botania_keepIvy")) {
                    if (modify) {
                        CustomData replaced = nbt.update(nbtCompound -> nbtCompound.remove("Botania_keepIvy"));
                        item.set(DataComponents.CUSTOM_DATA, replaced);
                    }

                    return DropRule.KEEP;
                }
            }

            DropRule dropRule;
            if (context != null)
                dropRule = GraveOverrideAreas.INSTANCE.getDropRuleFromArea(BlockPos.containing(context.deathPos()), context.world());
            else
                dropRule = GraveOverrideAreas.INSTANCE.defaultDropRule;


            // Get drop rule from enchantment
            if (EnchantmentHelper.hasTag(item, YigdTags.VANISHING))
                return DropRule.DESTROY;

            if (EnchantmentHelper.hasTag(item, YigdTags.SOULBOUND)) {
                if (config.inventoryConfig.loseSoulboundLevelOnDeath) {
                    for (Holder<Enchantment> enchantmentRegistryEntry : EnchantmentHelper.getEnchantmentsForCrafting(item).keySet()) {
                        if (!enchantmentRegistryEntry.is(YigdTags.SOULBOUND))
                            continue;

                        int level = EnchantmentHelper.getItemEnchantmentLevel(enchantmentRegistryEntry, item);
                        if (level > 1) EnchantmentHelper.updateEnchantments(item, builder -> builder.set(enchantmentRegistryEntry, level - 1));
                        else EnchantmentHelper.updateEnchantments(item, builder -> builder.removeIf(enchant -> enchant.equals(enchantmentRegistryEntry)));
                    }
                }
                return DropRule.KEEP;
            }

            return dropRule;
        });

        GraveClaimEvent.EVENT.register((player, world, pos, grave, tool) -> {
            if (player.isDeadOrDying()) return false;

            YigdConfig config = YigdConfig.getConfig();

            if (config.extraFeatures.graveCompass.consumeOnUse || config.extraFeatures.graveCompass.pointToClosest != YigdConfig.ExtraFeatures.GraveCompassConfig.CompassGraveTarget.DISABLED) {
                Inventory inventory = player.getInventory();
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (!stack.is(Items.COMPASS)) continue;

                    if (config.extraFeatures.graveCompass.consumeOnUse) {
                        CustomData stackNbtComponent = stack.get(DataComponents.CUSTOM_DATA);
                        CompoundTag stackNbt = stackNbtComponent != null ? stackNbtComponent.copyTag() : null;
                        if (stack.is(Items.COMPASS) && stackNbt != null && stackNbt.contains("linked_grave")) {
                            UUID graveId = stackNbt.getUUID("linked_grave");
                            if (graveId.equals(grave.getGraveId())) {
                                stack.setCount(0);
                                break;
                            }
                        }
                    } else {  // Redirect closest grave pointer
                        GraveCompassHelper.updateClosestNbt(world.dimension(), player.blockPosition(), player.getUUID(), stack);
                    }
                }
            }

            if (config.extraFeatures.graveKeys.enabled) {
                if (tool.is(Yigd.GRAVE_KEY_ITEM)) {
                    CustomData nbtComponent = tool.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                    CompoundTag nbt = nbtComponent.copyTag();
                    CompoundTag userNbt = nbt.getCompound("user");
                    Tag uuidNbt = nbt.get("grave");
                    GraveKeyConfig.KeyTargeting targeting = config.extraFeatures.graveKeys.targeting;
                    switch (targeting) {
                        case ANY_GRAVE -> {
                            tool.shrink(1);
                            return true;
                        }
                        case PLAYER_GRAVE -> {
                            if (Objects.equals(ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, userNbt).result().orElse(null), grave.getOwner())) {
                                tool.shrink(1);
                                return true;
                            }
                        }
                        case SPECIFIC_GRAVE -> {
                            if (uuidNbt != null && Objects.equals(NbtUtils.loadUUID(uuidNbt), grave.getGraveId())) {
                                tool.shrink(1);
                                return true;
                            }
                        }
                    }
                }
                if (config.extraFeatures.graveKeys.required) {
                    player.sendSystemMessage(Component.translatable("text.yigd.message.missing_key"), true);
                    return false;  // The grave key didn't work
                }
            }

            if (config.graveConfig.requireShovelToLoot && !tool.is(ItemTags.SHOVELS)) {
                player.sendSystemMessage(Component.translatable("text.yigd.message.no_shovel"), true);
                return false;
            }

            if (player.getUUID().equals(grave.getOwner().id().orElse(null))) return true;
            if (!grave.isLocked()) return true;

            YigdConfig.GraveConfig.GraveRobbing robConfig = config.graveConfig.graveRobbing;
            if (!robConfig.enabled) return false;

            if (robConfig.killerSkipWaitTime && player.getUUID().equals(grave.getKillerId())) {
                return true;
            }

            final int tps = 20;  // ticks per second
            if (!grave.hasExistedTicks(robConfig.timeUnit.toSeconds(robConfig.afterTime) * tps)) {
                player.sendSystemMessage(Component.translatable("text.yigd.message.rob.too_early", grave.getTimeUntilRobbable()), true);
                return false;
            }

            return true;
        });

        AllowGraveGenerationEvent.EVENT.register((context, grave) -> {
            YigdConfig.GraveConfig graveConfig = YigdConfig.getConfig().graveConfig;
            if (!graveConfig.enabled) return false;

            if ((DeathInfoManager.INSTANCE.getGraveListMode() == ListMode.WHITELIST
                    && !DeathInfoManager.INSTANCE.isInList(new ResolvableProfile(context.player().getGameProfile())))
                    || (DeathInfoManager.INSTANCE.getGraveListMode() == ListMode.BLACKLIST
                    && DeathInfoManager.INSTANCE.isInList(new ResolvableProfile(context.player().getGameProfile())))) {
                Yigd.LOGGER.info("{} found on whitelist/blacklist, disallowing grave generation", context.player().getGameProfile().getName());
            }

            if (!graveConfig.generateEmptyGraves && grave.isGraveEmpty()) return false;

            if (graveConfig.dimensionBlacklist.contains(grave.getWorldRegistryKey().location().toString())) return false;

            if (!graveConfig.generateGraveInVoid && grave.getPos().getY() < context.world().getMinBuildHeight()) return false;

            if (graveConfig.requireItem) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(graveConfig.requiredItem));
                if (!grave.getInventoryComponent().removeItem(stack -> stack.is(item), graveConfig.requiredItemCount)) {
                    return false;
                }
            }

            return !graveConfig.ignoredDeathTypes.contains(context.deathSource().getMsgId());
        });
        AllowBlockUnderGraveGenerationEvent.EVENT.register(
                (grave, currentUnder) -> YigdConfig.getConfig().graveConfig.blockUnderGrave.enabled && currentUnder.is(YigdTags.REPLACE_SOFT_WHITELIST));

        GraveGenerationEvent.EVENT.register((world, pos, nthTry) -> {
            if (world.isOutsideBuildHeight(pos) || !world.getWorldBorder().isWithinBounds(pos)) {
                return false;
            }

            BlockState state = world.getBlockState(pos);
            YigdConfig.GraveConfig config = YigdConfig.getConfig().graveConfig;
            if (world.getBlockEntity(pos) != null)  // Block entities should NOT be replaced by graves
                return false;
            switch (nthTry) {
                case 0 -> {
                    if (!config.useSoftBlockWhitelist) return false;
                    if (!state.is(YigdTags.REPLACE_SOFT_WHITELIST)) return false;
                }
                case 1 -> {
                    if (!config.useStrictBlockBlacklist) return false;
                    if (state.is(YigdTags.KEEP_STRICT_BLACKLIST)) return false;
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
