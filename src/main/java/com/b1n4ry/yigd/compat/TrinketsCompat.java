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
    public Object getInventory(PlayerEntity player) {
        Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(player);
        if (optional.isPresent()) return optional.get().getInventory(player);
        
        return null;
    }

    @Override
    public void setInventory(Object inventory, PlayerEntity player) {
        Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(player);
        if (!optional.isPresent()) return;
        Map<String, Map<String, TrinketInventory>> playerInventory = optional.get();

        if (inventory instanceof Map<String, Map<String, TrinketInventory>> trinketInv) {
            trinketInv.forEach((index, value) -> {
                Map<String, TrinketInventory> trinkSlotInv = trinketInv.get(index);
                Map<String, TrinketInventory> playerSlotInv = playerInventory.get(index);
                playerInventory.get(index).forEach((index2, value2) -> {
                    TrinketInventory inventory = trinketSlotInv.get(index2);
                });
            });
        }
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
