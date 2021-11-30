package com.b1n4ry.yigd.compat;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.config.YigdConfig;
import com.b1n4ry.yigd.core.GraveHelper;
import dev.emi.trinkets.api.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;

import java.util.*;

public class TrinketsCompat implements YigdApi {
    @Override
    public Object getInventory(PlayerEntity player, boolean... handleAsDeath) {
        Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(player);
        if (optional.isEmpty()) return null;

        TrinketComponent trinkets = optional.get();

        List<Pair<SlotReference, ItemStack>> inv = trinkets.getAllEquipped();

        if (handleAsDeath.length > 0 && handleAsDeath[0]) {
            List<String> soulboundEnchantments = YigdConfig.getConfig().graveSettings.soulboundEnchantments;
            List<String> deleteEnchantments = YigdConfig.getConfig().graveSettings.deleteEnchantments;

            List<Pair<SlotReference, ItemStack>> soulbound = new ArrayList<>();

            for (Pair<SlotReference, ItemStack> pair : inv) {
                SlotReference ref = pair.getLeft();
                ItemStack stack = pair.getRight();
                if (GraveHelper.hasEnchantments(soulboundEnchantments, stack)) {
                    soulbound.add(new Pair<>(ref, stack.copy()));
                    pair.setRight(ItemStack.EMPTY);
                }
                if (GraveHelper.hasEnchantments(deleteEnchantments, stack)) {
                    pair.setRight(ItemStack.EMPTY);
                }
            }

            Yigd.deadPlayerData.addModdedSoulbound(player.getUuid(), soulbound);
        }

        return inv;
    }

    @Override
    public DefaultedList<ItemStack> setInventory(Object inventory, PlayerEntity player) {
        DefaultedList<ItemStack> extraItems = DefaultedList.of();
        if (!(inventory instanceof List)) return extraItems;
        List<Pair<SlotReference, ItemStack>> toEquip = (List<Pair<SlotReference, ItemStack>>) inventory;

        TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> {
            for (Pair<SlotReference, ItemStack> pair : toEquip) {
                SlotReference ref = pair.getLeft();
                SlotType slotType = ref.inventory().getSlotType();
                ItemStack stack = pair.getRight();

                trinkets.forEach(((slotReference, itemStack) -> {
                    SlotType type = slotReference.inventory().getSlotType();

                    if (type.equals(slotType)) {
                        TrinketInventory trinketInventory = slotReference.inventory();
                        int index = ref.index();
                        if (trinketInventory.getStack(index).isEmpty()) {
                            trinketInventory.setStack(index, stack);
                        } else {
                            extraItems.add(stack);
                        }
                    }
                }));
            }
        });

        return extraItems;
    }

    @Override
    public int getInventorySize(PlayerEntity player) {
        Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(player);
        if (optional.isEmpty()) return 0;

        int size = 0;

        TrinketComponent component = optional.get();

        Set<TrinketInventory> inventorySet = component.getTrackingUpdates();
        for (TrinketInventory inventory : inventorySet) {
            size += inventory.size();
        }

        return size;
    }

    @Override
    public void dropAll(PlayerEntity player) {
        TrinketsApi.getTrinketComponent(player).ifPresent(trinketComponent -> {
            trinketComponent.forEach((slotReference, itemStack) -> slotReference.inventory().clear());
        });
    }

    @Override
    public List<ItemStack> toStackList(Object inventory) {
        if (!(inventory instanceof List)) return null;
        List<Pair<SlotReference, ItemStack>> nested = (List<Pair<SlotReference, ItemStack>>) inventory;

        List<ItemStack> items = new ArrayList<>();
        for (Pair<SlotReference, ItemStack> pair : nested) {
            ItemStack stack = pair.getRight();
            if (!stack.isEmpty()) items.add(stack);
        }

        return items;
    }
}
