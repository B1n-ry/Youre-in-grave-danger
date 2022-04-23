package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.DeathEffectConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.b1n_ry.yigd.core.GraveHelper;
import com.b1n_ry.yigd.core.ModTags;
import me.lizardofoz.inventorio.api.InventorioAPI;
import me.lizardofoz.inventorio.player.PlayerInventoryAddon;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public class InventorioCompat implements YigdApi {
    @Override
    public String getModName() {
        return "inventorio";
    }

    @Override
    public Object getInventory(PlayerEntity player, boolean onDeath, @Nullable DeathEffectConfig onDeathHandling) {
        DefaultedList<ItemStack> inventories = DefaultedList.of();

        PlayerInventoryAddon inventoryAddon = InventorioAPI.getInventoryAddon(player);

        List<String> soulboundEnchantments = YigdConfig.getConfig().graveSettings.soulboundEnchantments;
        List<String> deleteEnchantments = YigdConfig.getConfig().graveSettings.deleteEnchantments;

        if (inventoryAddon == null) return inventories;
        for (int i = 0; i < inventoryAddon.size(); i++) {
            ItemStack stack = inventoryAddon.getStack(i);

            inventories.add(stack);
        }
        if (onDeath) {
            DefaultedList<ItemStack> soulboundItems = GraveHelper.getEnchantedItems(inventories, soulboundEnchantments);
            GraveHelper.removeFromList(inventories, soulboundItems);

            // Add defaulted soulbound items
            for (int i = 0; i < inventories.size(); i++) {
                ItemStack stack = inventories.get(i);

                if (stack.isIn(ModTags.SOULBOUND_ITEM) || onDeathHandling == DeathEffectConfig.KEEP_ITEMS) soulboundItems.set(i, stack);
            }

            DefaultedList<ItemStack> deletedItems = GraveHelper.getEnchantedItems(inventories, deleteEnchantments);
            GraveHelper.removeFromList(inventories, deletedItems);

            DeadPlayerData.Soulbound.addModdedSoulbound(player.getUuid(), soulboundItems.stream().toList());
        }

        return inventories.stream().toList();
    }

    @Override
    public DefaultedList<ItemStack> setInventory(Object inventory, PlayerEntity player) {
        DefaultedList<ItemStack> extraItems = DefaultedList.of();
        if (!(inventory instanceof List)) return extraItems;

        List<ItemStack> inventories = (List<ItemStack>) inventory;
        PlayerInventoryAddon inventoryAddon = InventorioAPI.getInventoryAddon(player);

        if (inventoryAddon == null) return extraItems;

        inventoryAddon.setSelectedUtility(4);
        for (int i = 0; i < inventories.size(); i++) {
            ItemStack stack = inventories.get(i);
            if (inventoryAddon.getStack(i).isEmpty()) {
                inventoryAddon.setStack(i, stack);
            } else {
                extraItems.add(stack);
            }
        }

        return extraItems;
    }

    @Override
    public int getInventorySize(Object inventory) {
        if (!(inventory instanceof List)) return 0;
        List<ItemStack> modInv = (List<ItemStack>) inventory;

        List<ItemStack> items = new ArrayList<>();
        for (ItemStack stack : modInv) {
            if (stack == null || stack.isEmpty()) continue;
            items.add(stack);
        }

        return items.size();
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
        List<ItemStack> stacks = (List<ItemStack>) inventory;
        List<ItemStack> newList = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) newList.add(stack);
        }
        return newList;
    }

    @Override
    public NbtCompound writeNbt(Object o) {
        if (!(o instanceof List)) return new NbtCompound();
        List<ItemStack> items = (List<ItemStack>) o;

        DefaultedList<ItemStack> stacks = DefaultedList.of();
        stacks.addAll(items);

        NbtCompound nbt = Inventories.writeNbt(new NbtCompound(), stacks);
        nbt.putInt("size", stacks.size());
        return nbt;
    }

    @Override
    public Object readNbt(NbtCompound nbt) {
        int size = nbt.getInt("size");
        DefaultedList<ItemStack> items = DefaultedList.ofSize(size, ItemStack.EMPTY);
        Inventories.readNbt(nbt, items);

        return items.stream().toList();
    }
}