package com.b1n_ry.yigd.util;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public interface Tags {
    TagKey<Block> REPLACE_SOFT_WHITELIST = TagKey.of(RegistryKeys.BLOCK, new Identifier(Yigd.MOD_ID, "replace_soft_whitelist"));
    TagKey<Block> KEEP_STRICT_BLACKLIST = TagKey.of(RegistryKeys.BLOCK, new Identifier(Yigd.MOD_ID, "keep_strict_blacklist"));
}
