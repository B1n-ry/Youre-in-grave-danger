package com.b1n_ry.yigd.compat;


import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.b1n_ry.yigd.core.GraveHelper;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@SuppressWarnings("unchecked")
public class TrinketsCompat implements YigdApi {
    @Override
    public String getModName() {
        return "trinkets";
    }

    @Override
    public Object getInventory(PlayerEntity player) {
        return this.getInventory(player, false);
    }

    @Override
    public Object getInventory(PlayerEntity player, boolean onDeath) {
        List<String> soulboundEnchantments = YigdConfig.getConfig().graveSettings.soulboundEnchantments;
        List<String> deleteEnchantments = YigdConfig.getConfig().graveSettings.deleteEnchantments;

        DefaultedList<ItemStack> itemStacks = DefaultedList.of();
        Inventory trinketInv = TrinketsApi.getTrinketsInventory(player);
        for (int i = 0; i < trinketInv.size(); i++) {
            ItemStack stack = trinketInv.getStack(i);
            itemStacks.add(stack);
        }
        if (onDeath) {
            DefaultedList<ItemStack> soulboundItems = GraveHelper.getEnchantedItems(itemStacks, soulboundEnchantments);
            // Add defaulted soulbound items
            for (int i = 0; i < itemStacks.size(); i++) {
                ItemStack stack = itemStacks.get(i);

                Collection<Identifier> tags = player.world.getTagManager().getItems().getTagsFor(stack.getItem());
                if (tags.contains(new Identifier("yigd", "soulbound_item"))) soulboundItems.set(i, stack);
            }
            GraveHelper.removeFromList(itemStacks, soulboundItems);

            DefaultedList<ItemStack> deleteItems = GraveHelper.getEnchantedItems(itemStacks, deleteEnchantments);
            GraveHelper.removeFromList(itemStacks, deleteItems);

            DeadPlayerData.Soulbound.addModdedSoulbound(player.getUuid(), soulboundItems);
        }

        return itemStacks;
    }

    @Override
    public DefaultedList<ItemStack> setInventory(Object inventory, PlayerEntity player) {
        DefaultedList<ItemStack> extraItems = DefaultedList.of();
        if (!(inventory instanceof DefaultedList)) return extraItems;
        DefaultedList<ItemStack> modItems = (DefaultedList<ItemStack>) inventory;

        Inventory trinketInv = TrinketsApi.getTrinketsInventory(player);
        for (int i = 0; i < modItems.size(); i++) {
            ItemStack present = trinketInv.getStack(i);
            ItemStack toAdd = modItems.get(i);
            if (toAdd.isEmpty()) continue;
            if (present == null) {
                extraItems.add(toAdd);
                continue;
            }
            if (!present.isEmpty()) extraItems.add(present);

            trinketInv.setStack(i, toAdd);
        }
        return extraItems;
    }

    @Override
    public int getInventorySize(Object inventory) {
        if (!(inventory instanceof DefaultedList)) return 0;
        DefaultedList<ItemStack> stacks = (DefaultedList<ItemStack>) inventory;

        List<ItemStack> size = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) size.add(stack);
        }
        return size.size();
    }

    @Override
    public void dropAll(PlayerEntity player) {
        Inventory inventory = TrinketsApi.getTrinketsInventory(player);
        for (int i = 0; i < inventory.size(); i++) {
            inventory.setStack(i, ItemStack.EMPTY);
        }
    }

    @Override
    public List<ItemStack> toStackList(Object inventory) {
        if (!(inventory instanceof DefaultedList)) return new ArrayList<>();
        return new ArrayList<>((DefaultedList<ItemStack>) inventory);
    }

    @Override
    public NbtCompound writeNbt(Object o) {
        if (!(o instanceof DefaultedList)) return new NbtCompound();
        DefaultedList<ItemStack> modInv = (DefaultedList<ItemStack>) o;
        NbtCompound nbt = Inventories.writeNbt(new NbtCompound(), modInv);
        nbt.putInt("size", modInv.size());
        return nbt;
    }

    @Override
    public Object readNbt(NbtCompound nbt) {
        int invSize = nbt.getInt("size");
        DefaultedList<ItemStack> modItems = DefaultedList.ofSize(invSize, ItemStack.EMPTY);
        Inventories.readNbt(nbt, modItems);
        return modItems;
    }
}