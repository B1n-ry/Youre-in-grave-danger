package com.b1n4ry.yigd.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.List;

public interface YigdApi {
    // Object refers to a custom variable type that will store all your items from the inventory. MUST BE SAME TYPE EVERYWHERE OBJECT IS USED
    // With the Object as a custom type you can define location of said item with e.g. a map variable

    Object getInventory(PlayerEntity player); // Get a custom return value containing inventory items

    void setInventory(Object inventory, PlayerEntity player); // A method that places all items in a custom object to the player inventory

    int getInventorySize(PlayerEntity player); // Get the size of inventory extension (slot size)

    void dropAll(PlayerEntity player); // A clear method for removing all items from the inventory extension

    List<ItemStack> toStackList(Object inventory); // Get the inventory as a list of itemstacks

//    NbtElement toNbtCompound(Object inventory); // Required for modded inventory to be added to grave NBT
    // No "readNbt" is required as the inventory is saved to the DeadPlayerData class
}
