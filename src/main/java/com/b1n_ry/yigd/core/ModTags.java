package com.b1n_ry.yigd.core;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;

public class ModTags {
    public static final TagKey<Block> REPLACE_BLACKLIST = TagKey.of(RegistryKeys.BLOCK, new Identifier("yigd", "replace_blacklist"));
    public static final TagKey<Block> SOFT_WHITELIST = TagKey.of(RegistryKeys.BLOCK, new Identifier("yigd", "soft_whitelist"));
    public static final TagKey<Block> SUPPORT_REPLACE_WHITELIST = TagKey.of(RegistryKeys.BLOCK, new Identifier("yigd", "support_replace_whitelist"));

    public static final TagKey<Item> FORCE_ITEM_SLOT = TagKey.of(RegistryKeys.ITEM, new Identifier("yigd", "force_item_slot"));
    public static final TagKey<Item> SOULBOUND_ITEM = TagKey.of(RegistryKeys.ITEM, new Identifier("yigd", "soulbound_item"));
    public static final TagKey<Item> RANDOM_DELETE_BLACKLIST = TagKey.of(RegistryKeys.ITEM, new Identifier("yigd", "random_delete_blacklist"));
    public static final TagKey<Item> SOULBOUND_BLACKLIST = TagKey.of(RegistryKeys.ITEM, new Identifier("yigd", "soulbound_blacklist"));

    public static final TagKey<Structure> GRAVEYARD_STRUCTURES = TagKey.of(RegistryKeys.STRUCTURE, new Identifier("yigd", "graveyard_structures"));
}
