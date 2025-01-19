package com.b1n_ry.yigd.util;

import com.b1n_ry.yigd.data.GraveItem;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface GraveItemModificationConsumer {
    void accept(ItemStack stack, int slot, GraveItem graveItem);
}
