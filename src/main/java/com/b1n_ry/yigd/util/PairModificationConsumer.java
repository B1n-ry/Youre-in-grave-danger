package com.b1n_ry.yigd.util;

import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface PairModificationConsumer {
    void accept(ItemStack stack, int slot, Tuple<ItemStack, DropRule> pair);
}
