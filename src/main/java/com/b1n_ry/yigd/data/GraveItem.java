package com.b1n_ry.yigd.data;

import com.b1n_ry.yigd.util.DropRule;
import net.minecraft.world.item.ItemStack;

public class GraveItem {
    public ItemStack stack;
    public DropRule dropRule;

    public GraveItem(ItemStack stack, DropRule dropRule) {
        this.stack = stack;
        this.dropRule = dropRule;
    }

    public GraveItem copy() {
        return new GraveItem(this.stack.copy(), this.dropRule);
    }
}
