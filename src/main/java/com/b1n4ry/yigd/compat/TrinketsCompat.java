package com.b1n4ry.yigd.compat;

import com.b1n4ry.yigd.api.YigdApi;
import dev.emi.trinkets.api.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

import java.util.*;

public class TrinketsCompat implements YigdApi {
    @Override
    public Object getInventory(PlayerEntity player) {
        Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(player);
        if (!optional.isPresent()) return null;

        TrinketComponent trinkets = optional.get();

        return trinkets.getAllEquipped();
    }

    @Override
    public void setInventory(Object inventory, PlayerEntity player) {
        if (!(inventory instanceof List)) return;
        List<Pair<SlotReference, ItemStack>> toEquip = (List<Pair<SlotReference, ItemStack>>) inventory;

        TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> {
            for (Pair<SlotReference, ItemStack> pair : toEquip) {
                SlotReference ref = pair.getLeft();
                SlotType slotType = ref.inventory().getSlotType();
                ref.inventory().setStack(ref.index(), pair.getRight());

                trinkets.forEach(((slotReference, itemStack) -> {
                    SlotType type = slotReference.inventory().getSlotType();

                    if (type.equals(slotType)) {
                        slotReference.inventory().setStack(ref.index(), pair.getRight());
                        return;
                    }
                }));
            }
        });

    }

    @Override
    public int getInventorySize(PlayerEntity player) {
        Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(player);
        if (!optional.isPresent()) return 0;

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
