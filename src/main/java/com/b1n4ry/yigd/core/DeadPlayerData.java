package com.b1n4ry.yigd.core;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeadPlayerData {
    private static Map<UUID, DefaultedList<ItemStack>> soulboundInventories = new HashMap<>();
    private static Map<UUID, DefaultedList<ItemStack>> deathPlayerInventories = new HashMap<>();

    public static DefaultedList<ItemStack> getSoulboundInventory(UUID userId) {
        return soulboundInventories.get(userId);
    }
    public static DefaultedList<ItemStack> getDeathPlayerInventory(UUID userId) {
        return deathPlayerInventories.get(userId);
    }
    public static void setSoulboundInventories(UUID userId, DefaultedList<ItemStack> soulboundItems) {
        dropSoulbound(userId);
        soulboundInventories.put(userId, soulboundItems);
    }
    public static void setDeathPlayerInventories(UUID userId, DefaultedList<ItemStack> inventoryItems) {
        deathPlayerInventories.put(userId, inventoryItems);
    }

    public static void dropSoulbound(UUID userId) {
        if (soulboundInventories.containsKey(userId)) soulboundInventories.remove(userId);
    }
    public static void dropDeathInventory(UUID userId) {
        if (deathPlayerInventories.containsKey(userId)) deathPlayerInventories.remove(userId);
    }
}
