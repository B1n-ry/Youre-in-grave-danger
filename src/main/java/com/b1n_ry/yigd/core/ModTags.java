package com.b1n_ry.yigd.core;

import net.fabricmc.fabric.api.tag.TagFactory;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;

public class ModTags {
    public static final Tag<Block> REPLACE_BLACKLIST = TagFactory.BLOCK.create(new Identifier("yigd", "replace_blacklist"));
    public static final Tag<Block> SOFT_WHITELIST = TagFactory.BLOCK.create(new Identifier("yigd", "soft_whitelist"));
    public static final Tag<Block> SUPPORT_REPLACE_WHITELIST = TagFactory.BLOCK.create(new Identifier("yigd", "support_replace_whitelist"));

    public static final Tag<Item> FORCE_ITEM_SLOT = TagFactory.ITEM.create(new Identifier("yigd", "force_item_slot"));
    public static final Tag<Item> SOULBOUND_ITEM = TagFactory.ITEM.create(new Identifier("yigd", "soulbound_item"));
    public static final Tag<Item> RANDOM_DELETE_BLACKLIST = TagFactory.ITEM.create(new Identifier("yigd", "random_delete_blacklist"));
}
