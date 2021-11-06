package com.b1n4ry.yigd.compat;

import com.b1n4ry.yigd.api.YigdApi;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TrinketsCompat implements YigdApi {
    @Override
    public List<ItemStack> getInventory(PlayerEntity player) {
        List<ItemStack> itemStacks = new ArrayList<>();

        Inventory inventory = TrinketsApi.getTrinketsInventory(player);

        for (int i = 0; i < inventory.size(); i++) {
            itemStacks.add(inventory.getStack(i));
        }
        return itemStacks;
    }

    @Override
    public void setInventory(List<ItemStack> inventory, PlayerEntity player) {
        for (ItemStack itemStack : inventory) {
            TrinketsApi.getTrinketComponent(player).equip(itemStack);
        }
    }

    @Override
    public int getInventorySize(PlayerEntity player) {
        return TrinketsApi.getTrinketsInventory(player).size();
    }

    @Override
    public void dropAll(PlayerEntity player) {
        TrinketsApi.TRINKETS.get(player).getInventory().clear();
    }
}
