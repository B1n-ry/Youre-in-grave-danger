package com.b1n4ry.yigd.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.List;

public interface YigdApi {
    List<ItemStack> getInventory(PlayerEntity player); // Get a List element with all ItemsStack in inventory extension

    void setInventory(List<ItemStack> inventory, PlayerEntity player); // A method that places all items in the player inventory

    int getInventorySize(PlayerEntity player); // Get the size of inventory extension (slot size)

    void dropAll(PlayerEntity player); // A clear method for removing all items from the inventory extension
}
