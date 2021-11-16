package com.b1n4ry.yigd.compat;

import com.b1n4ry.yigd.api.YigdApi;
import me.lizardofoz.inventorio.api.InventorioAPI;
import me.lizardofoz.inventorio.player.PlayerInventoryAddon;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class InventorioCompat implements YigdApi {
    @Override
    public Object getInventory(PlayerEntity player) {
        List<ItemStack> inventories = new ArrayList<>();

        PlayerInventoryAddon inventoryAddon = InventorioAPI.getInventoryAddon(player);
        if (inventoryAddon == null) return inventories;
        for (int i = 0; i < inventoryAddon.size(); i++) {
            ItemStack stack = inventoryAddon.getStack(i);

            inventories.add(stack);
        }

        return inventories;
    }

    @Override
    public void setInventory(Object inventory, PlayerEntity player) {
        if (!(inventory instanceof List)) return;

        List<ItemStack> inventories = (List<ItemStack>) inventory;
        PlayerInventoryAddon inventoryAddon = InventorioAPI.getInventoryAddon(player);

        if (inventoryAddon == null) return;

        inventoryAddon.setSelectedUtility(4);
        for (int i = 0; i < inventories.size(); i++) {
            ItemStack stack = inventories.get(i);
            inventoryAddon.setStack(i, stack);
        }
    }

    @Override
    public int getInventorySize(PlayerEntity player) {
        PlayerInventoryAddon inventoryAddon = InventorioAPI.getInventoryAddon(player);
        if (inventoryAddon == null) return 0;
        return inventoryAddon.size();
    }

    @Override
    public void dropAll(PlayerEntity player) {
        PlayerInventoryAddon inventoryAddon = InventorioAPI.getInventoryAddon(player);
        if (inventoryAddon == null) return;

        inventoryAddon.clear();
    }

    @Override
    public List<ItemStack> toStackList(Object inventory) {
        if (!(inventory instanceof List)) return new ArrayList<>(0);
        return (List<ItemStack>) inventory;
    }
}
