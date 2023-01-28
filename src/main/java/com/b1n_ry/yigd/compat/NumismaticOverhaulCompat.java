package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.DeathEffectConfig;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.glisco.numismaticoverhaul.currency.CurrencyConverter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class NumismaticOverhaulCompat implements YigdApi {
    @Override
    public String getModName() {
        return "numismatic-overhaul";
    }

    @Override
    public Object getInventory(PlayerEntity player, boolean onDeath, @Nullable DeathEffectConfig onDeathHandling) {
        CurrencyComponent component = ModComponents.CURRENCY.get(player);

        long totalCoins = component.getValue();
        if (onDeath) {
            float dropPercentage = player.world.getGameRules().get(NumismaticOverhaul.MONEY_DROP_PERCENTAGE).get() * 0.01f;
            if (onDeathHandling == DeathEffectConfig.KEEP_ITEMS) {
                dropPercentage = 0;
            }
            long lost = (long) (totalCoins * dropPercentage);
            long kept = totalCoins - lost;
            DeadPlayerData.Soulbound.addModdedSoulbound(player.getUuid(), kept);

            totalCoins = lost;
        }
        return totalCoins;
    }

    @Override
    public DefaultedList<ItemStack> setInventory(Object inventory, PlayerEntity player) {
        DefaultedList<ItemStack> extraItems = DefaultedList.of();
        if (!(inventory instanceof Long coins)) return extraItems;

        CurrencyComponent component = ModComponents.CURRENCY.get(player);
        component.modify(coins);

        return extraItems;
    }

    @Override
    public int getInventorySize(Object inventory) {
        return 0;
    }

    @Override
    public void dropAll(PlayerEntity player) {
        CurrencyComponent component = ModComponents.CURRENCY.get(player);
        long value = component.getValue();
        component.modify(-value);
    }

    @Override
    public List<ItemStack> toStackList(Object inventory) {
        List<ItemStack> stacks = new ArrayList<>();
        if (!(inventory instanceof Long coins)) return stacks;

        return CurrencyConverter.getAsValidStacks(coins);
    }

    @Override
    public NbtCompound writeNbt(Object o) {
        NbtCompound nbt = new NbtCompound();
        if (!(o instanceof Long coins)) return nbt;

        nbt.putLong("coins", coins);
        return nbt;
    }

    @Override
    public Object readNbt(NbtCompound nbt) {
        return nbt.getLong("coins");
    }
}
