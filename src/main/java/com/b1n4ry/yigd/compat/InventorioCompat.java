package com.b1n4ry.yigd.compat;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.config.YigdConfig;
import com.b1n4ry.yigd.core.GraveHelper;
import me.lizardofoz.inventorio.api.InventorioAPI;
import me.lizardofoz.inventorio.player.PlayerInventoryAddon;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.List;


public class InventorioCompat implements YigdApi {
    @Override
    public Object getInventory(PlayerEntity player, boolean... handleAsDeath) {
        DefaultedList<ItemStack> inventories = DefaultedList.of();

        PlayerInventoryAddon inventoryAddon = InventorioAPI.getInventoryAddon(player);

        List<String> soulboundEnchantments = YigdConfig.getConfig().graveSettings.soulboundEnchantments;
        List<String> deleteEnchantments = YigdConfig.getConfig().graveSettings.deleteEnchantments;

        if (inventoryAddon == null) return inventories;
        for (int i = 0; i < inventoryAddon.size(); i++) {
            ItemStack stack = inventoryAddon.getStack(i);

            inventories.add(stack);
        }
        if (handleAsDeath.length > 0 && handleAsDeath[0]) {
            DefaultedList<ItemStack> soulboundItems = GraveHelper.getEnchantedItems(inventories, soulboundEnchantments);
            inventories = GraveHelper.removeFromList(inventories, soulboundItems);

            DefaultedList<ItemStack> deletedItems = GraveHelper.getEnchantedItems(inventories, deleteEnchantments);
            inventories = GraveHelper.removeFromList(inventories, deletedItems);

            Yigd.deadPlayerData.addModdedSoulbound(player.getUuid(), soulboundItems.stream().toList());
        }

        return inventories.stream().toList();
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
