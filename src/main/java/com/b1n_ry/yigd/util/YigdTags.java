package com.b1n_ry.yigd.util;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

public interface YigdTags {
    TagKey<Block> REPLACE_SOFT_WHITELIST = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "replace_soft_whitelist"));
    TagKey<Block> KEEP_STRICT_BLACKLIST = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "keep_strict_blacklist"));
    TagKey<Block> REPLACE_GRAVE_BLACKLIST = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "replace_grave_blacklist"));

    TagKey<Item> NATURAL_SOULBOUND = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "natural_soulbound"));
    TagKey<Item> NATURAL_VANISHING = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "natural_vanishing"));
    TagKey<Item> LOSS_IMMUNE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "loss_immune"));
    TagKey<Item> GRAVE_INCOMPATIBLE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "grave_incompatible"));  // For items that should be dropped instead of put into graves

    TagKey<Enchantment> SOULBOUND = TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "soulbound"));
    TagKey<Enchantment> VANISHING = TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "vanishing"));
    TagKey<Enchantment> DEATH_SIGHT = TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "death_sight"));
}
