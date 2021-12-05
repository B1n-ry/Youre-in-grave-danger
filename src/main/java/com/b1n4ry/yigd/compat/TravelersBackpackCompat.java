package com.b1n4ry.yigd.compat;

import com.b1n4ry.yigd.api.YigdApi;
import com.tiviacz.travelersbackpack.component.ComponentUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.List;

public class TravelersBackpackCompat implements YigdApi {
    @Override
    public Object getInventory(PlayerEntity player, boolean... handleAsDeath) {
        return ComponentUtils.getWearingBackpack(player);
    }

    @Override
    public DefaultedList<ItemStack> setInventory(Object inventory, PlayerEntity player) {
        DefaultedList<ItemStack> extraItems = DefaultedList.of();
        if (!(inventory instanceof ItemStack stack) || stack.isEmpty()) return extraItems;
        if (ComponentUtils.isWearingBackpack(player)) {
            extraItems.add(stack);
        } else {
            ComponentUtils.equipBackpack(player, stack);
        }
        return extraItems;
    }

    @Override
    public int getInventorySize(PlayerEntity player) {
        return 1;
    }

    @Override
    public void dropAll(PlayerEntity player) {
        ComponentUtils.getComponent(player).setWearable(ItemStack.EMPTY);

        ComponentUtils.sync(player);
        ComponentUtils.syncToTracking(player);
    }

    @Override
    public List<ItemStack> toStackList(Object inventory) {
        List<ItemStack> stacks = new ArrayList<>();
        if (!(inventory instanceof ItemStack stack)) return stacks;
        stacks.add(stack);
        return stacks;
    }
}
