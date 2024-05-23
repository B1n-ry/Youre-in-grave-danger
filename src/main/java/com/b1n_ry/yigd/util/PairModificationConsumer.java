package com.b1n_ry.yigd.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

@FunctionalInterface
public interface PairModificationConsumer {
    void accept(ItemStack stack, int slot, Pair<ItemStack, DropRule> pair);
}
