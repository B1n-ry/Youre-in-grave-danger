package com.b1n_ry.yigd.util;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public interface YigdTags {
    TagKey<Block> REPLACE_SOFT_WHITELIST = TagKey.of(RegistryKeys.BLOCK, new Identifier(Yigd.MOD_ID, "replace_soft_whitelist"));
    TagKey<Block> KEEP_STRICT_BLACKLIST = TagKey.of(RegistryKeys.BLOCK, new Identifier(Yigd.MOD_ID, "keep_strict_blacklist"));


    TagKey<Item> NATURAL_SOULBOUND = TagKey.of(RegistryKeys.ITEM, new Identifier(Yigd.MOD_ID, "natural_soulbound"));
    TagKey<Item> NATURAL_VANISHING = TagKey.of(RegistryKeys.ITEM, new Identifier(Yigd.MOD_ID, "natural_vanishing"));
    TagKey<Item> LOSS_IMMUNE = TagKey.of(RegistryKeys.ITEM, new Identifier(Yigd.MOD_ID, "loss_immune"));
}
