package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.YigdConfig;
import net.levelz.access.PlayerStatsManagerAccess;
import net.levelz.access.PlayerSyncAccess;
import net.levelz.stats.PlayerStatsManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.List;

public class LevelzCompat implements YigdApi {
    @Override
    public String getModName() {
        return "levelz";
    }

    @Override
    public Object getInventory(PlayerEntity player) {
        return this.getInventory(player, false);
    }

    @Override
    public Object getInventory(PlayerEntity player, boolean onDeath) {
        YigdConfig.GraveCompatConfig graveCompatConfig = YigdConfig.getConfig().graveSettings.graveCompatConfig;

        PlayerStatsManager playerStatsManager = ((PlayerStatsManagerAccess) player).getPlayerStatsManager(player);
        int exp = (int) (playerStatsManager.levelProgress * playerStatsManager.getNextLevelExperience());

        if (onDeath) {
            // Handle experience
            if (!graveCompatConfig.levelzXpInGraves) exp = 0; // If feature not enabled, set to 0 and let other functions handle everything
            exp *= (float) graveCompatConfig.levelzXpDropPercent / 100f;
        }
        return exp;
    }

    @Override
    public DefaultedList<ItemStack> setInventory(Object inventory, PlayerEntity player) {
        DefaultedList<ItemStack> empty = DefaultedList.of();
        if (!(inventory instanceof Integer xp)) return empty;

        ((PlayerSyncAccess) player).addLevelExperience(xp);
        return empty;
    }

    @Override
    public int getInventorySize(Object inventory) {
        return 0;
    }

    @Override
    public void dropAll(PlayerEntity player) {
        if (!YigdConfig.getConfig().graveSettings.graveCompatConfig.levelzXpInGraves) return; // If feature is disabled, let the levelz mod itself handle the xp
        PlayerStatsManager playerStatsManager = ((PlayerStatsManagerAccess) player).getPlayerStatsManager(player);
        playerStatsManager.levelProgress = 0;
    }

    @Override
    public List<ItemStack> toStackList(Object inventory) {
        return new ArrayList<>();
    }

    @Override
    public NbtCompound writeNbt(Object o) {
        NbtCompound nbt = new NbtCompound();
        if (!(o instanceof Integer xp)) return nbt;
        nbt.putInt("xp", xp);
        return nbt;
    }

    @Override
    public Object readNbt(NbtCompound nbt) {
        return nbt.getFloat("xp");
    }
}
