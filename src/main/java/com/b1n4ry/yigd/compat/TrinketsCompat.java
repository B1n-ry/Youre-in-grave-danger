package com.b1n4ry.yigd.compat;

import com.b1n4ry.yigd.api.YigdApi;
import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.*;

public class TrinketsCompat implements YigdApi {
    @Override
    public List<ItemStack> getInventory(PlayerEntity player) {
        List<ItemStack> itemStacks = new ArrayList<>();

        Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(player);
        if (optional.isPresent()) {
            Collection<Map<String, TrinketInventory>> collection = optional.get().getInventory().values();
            for (Map<String, TrinketInventory> map : collection) {
                for (int i = 0; i < map.size(); i++) {
                    System.out.println(map.keySet().toArray()[i] + ", " + map.values().toArray()[i].toString());
                }
            }
        }

        return itemStacks;
    }

    @Override
    public void setInventory(List<ItemStack> inventory, PlayerEntity player) {
//        for (ItemStack itemStack : inventory) {
//            TrinketsApi.getTrinketComponent(player).equip(itemStack);
//        }
    }

    @Override
    public int getInventorySize(PlayerEntity player) {
//        return TrinketsApi.getTrinketsInventory(player).size();
        return 0;
    }

    @Override
    public void dropAll(PlayerEntity player) {
//        TrinketsApi.TRINKETS.get(player).getInventory().clear();
    }
}
