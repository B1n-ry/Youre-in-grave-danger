package com.b1n4ry.yigd.core;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class DeadPlayerData {
    private static final Map<UUID, DefaultedList<ItemStack>> soulboundInventories = new HashMap<>();
    private static final Map<UUID, DefaultedList<ItemStack>> deathPlayerInventories = new HashMap<>();
    private static final Map<UUID, List<Object>> moddedInventories = new HashMap<>();
    private static final Map<UUID, BlockPos> deathPoses = new HashMap<>();
    private static final Map<UUID, Integer> deathXp = new HashMap<>();

    public static DefaultedList<ItemStack> getSoulboundInventory(UUID userId) {
        return soulboundInventories.get(userId);
    }
    public static DefaultedList<ItemStack> getDeathPlayerInventory(UUID userId) {
        return deathPlayerInventories.get(userId);
    }
    public static List<Object> getModdedInventories(UUID userId) {
        return moddedInventories.get(userId);
    }
    public static BlockPos getDeathPos(UUID userId) {
        return deathPoses.get(userId);
    }
    public static int getDeathXp(UUID userId) {
        return deathXp.get(userId);
    }
    public static void setSoulboundInventories(UUID userId, DefaultedList<ItemStack> soulboundItems) {
        dropSoulbound(userId);
        soulboundInventories.put(userId, soulboundItems);
    }
    public static void setDeathPlayerInventories(UUID userId, DefaultedList<ItemStack> inventoryItems) {
        deathPlayerInventories.put(userId, inventoryItems);
    }
    public static void setModdedInventories(UUID userId, List<Object> moddedInvetory) {
        moddedInventories.put(userId, moddedInvetory);
    }
    public static void setDeathPos(UUID userId, BlockPos deathPos) {
        deathPoses.put(userId, deathPos);
    }
    public static void setDeathXp(UUID userId, int xp) {
        deathXp.put(userId, xp);
    }

    public static void dropSoulbound(UUID userId) {
        soulboundInventories.remove(userId);
    }
    public static void dropDeathInventory(UUID userId) {
        deathPlayerInventories.remove(userId);
    }
    public static void dropModdedInventory(UUID userId) {
        moddedInventories.remove(userId);
    }
    public static void dropDeathPos(UUID userId) {
        deathPoses.remove(userId);
    }
    public static void dropDeathXp(UUID userId) {
        deathXp.remove(userId);
    }

    public static boolean hasStoredInventory(UUID userId) {
        return deathPlayerInventories.containsKey(userId);
    }
}
