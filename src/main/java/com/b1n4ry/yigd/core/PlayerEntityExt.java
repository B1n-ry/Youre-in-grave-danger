package com.b1n4ry.yigd.core;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

public interface PlayerEntityExt {
    DefaultedList<ItemStack> getSoulboundInventory();
}
