package com.b1n4ry.yigd.core;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class DeadPlayerData {
    private final Map<UUID, DefaultedList<ItemStack>> soulboundInventories = new HashMap<>();
    private final Map<UUID, List<Object>> moddedSoulbound = new HashMap<>();
    private final Map<UUID, DefaultedList<ItemStack>> deathPlayerInventories = new HashMap<>();
    private final Map<UUID, List<Object>> moddedInventories = new HashMap<>();
    private final Map<UUID, BlockPos> deathPoses = new HashMap<>();
    private final Map<UUID, Integer> deathXp = new HashMap<>();

    public DefaultedList<ItemStack> getSoulboundInventory(UUID userId) {
        return soulboundInventories.get(userId);
    }
    public DefaultedList<ItemStack> getDeathPlayerInventory(UUID userId) {
        return deathPlayerInventories.get(userId);
    }
    public List<Object> getModdedInventories(UUID userId) {
        return moddedInventories.get(userId);
    }
    public BlockPos getDeathPos(UUID userId) {
        return deathPoses.get(userId);
    }
    public int getDeathXp(UUID userId) {
        return deathXp.get(userId);
    }
    public List<Object> getModdedSoulbound(UUID userId) {
        return moddedSoulbound.get(userId);
    }
    public void setSoulboundInventories(UUID userId, DefaultedList<ItemStack> soulboundItems) {
        dropSoulbound(userId);
        soulboundInventories.put(userId, soulboundItems);
    }
    public void setDeathPlayerInventories(UUID userId, DefaultedList<ItemStack> inventoryItems) {
        deathPlayerInventories.put(userId, inventoryItems);
    }
    public void setModdedInventories(UUID userId, List<Object> moddedInventory) {
        moddedInventories.put(userId, moddedInventory);
    }
    public void setDeathPos(UUID userId, BlockPos deathPos) {
        deathPoses.put(userId, deathPos);
    }
    public void setDeathXp(UUID userId, int xp) {
        deathXp.put(userId, xp);
    }
    public void addModdedSoulbound(UUID userId, Object modInventory) {
        if (!moddedSoulbound.containsKey(userId)) {
            moddedSoulbound.put(userId, new ArrayList<>());
        }
        moddedSoulbound.get(userId).add(modInventory);
    }

    public void dropSoulbound(UUID userId) {
        soulboundInventories.remove(userId);
    }
    public void dropDeathInventory(UUID userId) {
        deathPlayerInventories.remove(userId);
    }
    public void dropModdedInventory(UUID userId) {
        moddedInventories.remove(userId);
    }
    public void dropDeathPos(UUID userId) {
        deathPoses.remove(userId);
    }
    public void dropDeathXp(UUID userId) {
        deathXp.remove(userId);
    }
    public void dropModdedSoulbound(UUID userId) {
        moddedSoulbound.remove(userId);
    }

    public boolean hasStoredInventory(UUID userId) {
        return deathPlayerInventories.containsKey(userId);
    }
}
