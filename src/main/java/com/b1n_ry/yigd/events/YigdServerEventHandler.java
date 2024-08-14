package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.ListMode;
import com.b1n_ry.yigd.events.YigdEvents.*;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.util.GraveCompassHelper;
import com.b1n_ry.yigd.util.GraveOverrideAreas;
import com.b1n_ry.yigd.util.YigdTags;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Objects;
import java.util.UUID;

public class YigdServerEventHandler {
    @SubscribeEvent
    public void dropRuleEvent(DropRuleEvent event) {
        ItemStack item = event.getStack();
        int slot = event.getSlot();
        DeathContext context = event.getDeathContext();
        boolean modify = event.isModify();


        YigdConfig config = YigdConfig.getConfig();

        MobEffect statusEffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.fromNamespaceAndPath("amethyst_imbuement", "soulbinding"));

        if (statusEffect != null) {
            Holder<MobEffect> statusEffectEntry = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(statusEffect);
            if (context != null && context.player().hasEffect(statusEffectEntry)) {
                event.setDropRule(DropRule.KEEP);
                event.setCanceled(true);
                return;
            }
        }

        if (config.inventoryConfig.soulboundSlots.contains(slot)) {
            event.setDropRule(DropRule.KEEP);
            event.setCanceled(true);
            return;
        }
        if (config.inventoryConfig.vanishingSlots.contains(slot)) {
            event.setDropRule(DropRule.DESTROY);
            event.setCanceled(true);
            return;
        }
        if (config.inventoryConfig.dropOnGroundSlots.contains(slot)) {
            event.setDropRule(DropRule.DROP);
            event.setCanceled(true);
            return;
        }

        if (item.is(YigdTags.NATURAL_SOULBOUND)) {
            event.setDropRule(DropRule.KEEP);
            event.setCanceled(true);
            return;
        }
        if (item.is(YigdTags.NATURAL_VANISHING)) {
            event.setDropRule(DropRule.DESTROY);
            event.setCanceled(true);
            return;
        }
        if (item.is(YigdTags.GRAVE_INCOMPATIBLE)) {
            event.setDropRule(DropRule.DROP);
            event.setCanceled(true);
            return;
        }

        if (!item.isEmpty() && item.has(DataComponents.CUSTOM_DATA)) {
            CustomData nbt = item.get(DataComponents.CUSTOM_DATA);
            assert nbt != null;  // This should never be null, but it's sorta required for intelliJ to not complain
            CompoundTag itemNbt = nbt.copyTag();
            if (itemNbt.contains("Botania_keepIvy") && itemNbt.getBoolean("Botania_keepIvy")) {
                if (modify) {
                    CustomData replaced = nbt.update(nbtCompound -> nbtCompound.remove("Botania_keepIvy"));
                    item.set(DataComponents.CUSTOM_DATA, replaced);
                }

                event.setDropRule(DropRule.KEEP);
                event.setCanceled(true);
                return;
            }
        }

        DropRule dropRule;
        if (context != null)
            dropRule = GraveOverrideAreas.INSTANCE.getDropRuleFromArea(BlockPos.containing(context.deathPos()), context.world());
        else
            dropRule = GraveOverrideAreas.INSTANCE.defaultDropRule;


        // Get drop rule from enchantment
        if (EnchantmentHelper.hasTag(item, YigdTags.VANISHING)) {
            event.setDropRule(DropRule.DESTROY);
            event.setCanceled(true);
            return;
        }

        if (EnchantmentHelper.hasTag(item, YigdTags.SOULBOUND)) {
            if (config.inventoryConfig.loseSoulboundLevelOnDeath) {
                for (Holder<Enchantment> enchantmentRegistryEntry : EnchantmentHelper.getEnchantmentsForCrafting(item).keySet()) {
                    if (!enchantmentRegistryEntry.is(YigdTags.SOULBOUND))
                        continue;

                    int level = EnchantmentHelper.getTagEnchantmentLevel(enchantmentRegistryEntry, item);
                    if (level > 1) EnchantmentHelper.updateEnchantments(item, builder -> builder.set(enchantmentRegistryEntry, level - 1));
                    else EnchantmentHelper.updateEnchantments(item, builder -> builder.removeIf(enchant -> enchant.equals(enchantmentRegistryEntry)));
                }
            }
            event.setDropRule(DropRule.KEEP);
            event.setCanceled(true);
            return;
        }
        event.setDropRule(dropRule);
    }

    @SubscribeEvent
    public void graveClaimEvent(GraveClaimEvent event) {
        ServerPlayer player = event.getPlayer();
        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();
        GraveComponent grave = event.getGrave();
        ItemStack tool = event.getTool();

        if (player.isDeadOrDying()) {
            event.setCanClaim(false);
            event.setCanceled(true);
            return;
        }

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
                    GraveCompassHelper.updateClosestNbt(level.dimension(), player.blockPosition(), player.getUUID(), stack);
                }
            }
        }

        if (config.extraFeatures.graveKeys.enabled) {
            if (tool.is(Yigd.GRAVE_KEY_ITEM)) {
                CustomData nbtComponent = tool.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                CompoundTag nbt = nbtComponent.copyTag();
                CompoundTag userNbt = nbt.getCompound("user");
                Tag uuidNbt = nbt.get("grave");
                YigdConfig.ExtraFeatures.GraveKeyConfig.KeyTargeting targeting = config.extraFeatures.graveKeys.targeting;
                switch (targeting) {
                    case ANY_GRAVE -> {
                        tool.shrink(1);
                        event.setCanClaim(true);
                        event.setCanceled(true);
                        return;
                    }
                    case PLAYER_GRAVE -> {
                        if (userNbt != null && Objects.equals(ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, userNbt).result().orElse(null), grave.getOwner())) {
                            tool.shrink(1);
                            event.setCanClaim(true);
                            event.setCanceled(true);
                            return;
                        }
                    }
                    case SPECIFIC_GRAVE -> {
                        if (uuidNbt != null && Objects.equals(NbtUtils.loadUUID(uuidNbt), grave.getGraveId())) {
                            tool.shrink(1);
                            event.setCanClaim(true);
                            event.setCanceled(true);
                            return;
                        }
                    }
                }
            }
            if (config.extraFeatures.graveKeys.required) {
                player.sendSystemMessage(Component.translatable("text.yigd.message.missing_key"), true);
                event.setCanClaim(false);
                return;  // The grave key didn't work
            }
        }

        if (config.graveConfig.requireShovelToLoot && !tool.is(ItemTags.SHOVELS)) {
            player.sendSystemMessage(Component.translatable("text.yigd.message.no_shovel"), true);
            event.setCanClaim(false);
            return;
        }

        if (player.getUUID().equals(grave.getOwner().id().orElse(null))) {
            event.setCanClaim(true);
            return;
        }
        if (!grave.isLocked()) {
            event.setCanClaim(true);
            return;
        }

        YigdConfig.GraveConfig.GraveRobbing robConfig = config.graveConfig.graveRobbing;
        if (!robConfig.enabled) {
            event.setCanClaim(false);
            return;
        }

        final int tps = 20;  // ticks per second
        if (!grave.hasExistedTicks(robConfig.timeUnit.toSeconds(robConfig.afterTime) * tps)) {
            player.sendSystemMessage(Component.translatable("text.yigd.message.rob.too_early", grave.getTimeUntilRobbable()), true);
            event.setCanClaim(false);
            return;
        }

        if (robConfig.onlyMurderer && !player.getUUID().equals(grave.getKillerId())) {
            player.sendSystemMessage(Component.translatable("text.yigd.message.rob_not_killer",
                    grave.getOwner().name().orElse("PLAYER_NOT_FOUND")), true);
            event.setCanClaim(false);
            return;
        }

        event.setCanClaim(true);
    }

    @SubscribeEvent
    public void allowGraveGenerationEvent(AllowGraveGenerationEvent event) {
        DeathContext context = event.getDeathContext();
        GraveComponent grave = event.getGrave();

        YigdConfig.GraveConfig graveConfig = YigdConfig.getConfig().graveConfig;
        if (!graveConfig.enabled) {
            event.setAllowGeneration(false);
            return;
        }

        if (DeathInfoManager.INSTANCE.getGraveListMode() == ListMode.WHITELIST
                && !DeathInfoManager.INSTANCE.isInList(new ResolvableProfile(context.player().getGameProfile()))) {
            event.setAllowGeneration(false);
            return;
        }
        if (DeathInfoManager.INSTANCE.getGraveListMode() == ListMode.BLACKLIST
                && DeathInfoManager.INSTANCE.isInList(new ResolvableProfile(context.player().getGameProfile()))) {
            event.setAllowGeneration(false);
            return;
        }

        if (!graveConfig.generateEmptyGraves && grave.isGraveEmpty()) {
            event.setAllowGeneration(false);
            return;
        }

        if (graveConfig.dimensionBlacklist.contains(grave.getWorldRegistryKey().location().toString())) {
            event.setAllowGeneration(false);
            return;
        }

        if (!graveConfig.generateGraveInVoid && grave.getPos().getY() < 0) {
            event.setAllowGeneration(false);
            return;
        }

        if (graveConfig.requireItem) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(graveConfig.requiredItem));
            if (!grave.getInventoryComponent().removeItem(stack -> stack.is(item), 1)) {
                event.setAllowGeneration(false);
                return;
            }
        }

        event.setAllowGeneration(!graveConfig.ignoredDeathTypes.contains(context.deathSource().getMsgId()));
    }

    @SubscribeEvent
    public void allowBlockUnderGraveGenerationEvent(AllowBlockUnderGraveGenerationEvent event) {
        event.setAllowPlacement(
                YigdConfig.getConfig().graveConfig.blockUnderGrave.enabled && event.getBlockUnder().is(YigdTags.REPLACE_SOFT_WHITELIST));
    }

    @SubscribeEvent
    public void graveGenerationEvent(GraveGenerationEvent event) {
        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();
        int nthTry = event.getNthTry();

        BlockState state = level.getBlockState(pos);
        YigdConfig.GraveConfig config = YigdConfig.getConfig().graveConfig;
        if (level.getBlockEntity(pos) != null) {  // Block entities should NOT be replaced by graves
            event.setCanGenerate(false);
            return;
        }
        switch (nthTry) {
            case 0 -> {
                if (!config.useSoftBlockWhitelist) {
                    event.setCanGenerate(false);
                    return;
                }
                if (!state.is(YigdTags.REPLACE_SOFT_WHITELIST)) {
                    event.setCanGenerate(false);
                    return;
                }
            }
            case 1 -> {
                if (!config.useStrictBlockBlacklist) {
                    event.setCanGenerate(false);
                    return;
                }
                if (state.is(YigdTags.KEEP_STRICT_BLACKLIST)) {
                    event.setCanGenerate(false);
                    return;
                }
            }
        }
        event.setCanGenerate(true);
    }

    @SubscribeEvent
    public void dropItemEvent(DropItemEvent event) {
        if (event.getStack().isEmpty()) {
            event.setShouldDrop(false);
            event.setCanceled(true);
        } else {
            event.setShouldDrop(true);
        }
    }
}
