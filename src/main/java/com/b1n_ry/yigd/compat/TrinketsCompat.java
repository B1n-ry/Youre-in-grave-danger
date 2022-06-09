package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.DeathEffectConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.b1n_ry.yigd.core.GraveHelper;
import com.b1n_ry.yigd.core.ModTags;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("unchecked")
public class TrinketsCompat implements YigdApi {
    @Override
    public String getModName() {
        return "trinkets";
    }

    @Override
    public Object getInventory(PlayerEntity player, boolean onDeath, @Nullable DeathEffectConfig onDeathHandling) {
        Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(player);
        if (optional.isEmpty()) return new HashMap<String, Map<String, TrinketInventory>>();

        TrinketComponent component = optional.get();
        Map<String, Map<String, TrinketInventory>> inventory = component.getInventory();
        Map<String, Map<String, DefaultedList<ItemStack>>> playerInv = new HashMap<>();

        List<String> soulboundEnchantments = YigdConfig.getConfig().graveSettings.soulboundEnchantments;
        List<String> deleteEnchantments = YigdConfig.getConfig().graveSettings.deleteEnchantments;

        Map<String, Map<String, DefaultedList<ItemStack>>> soulbound = new HashMap<>();
        inventory.forEach((group, slots) -> {
            Map<String, DefaultedList<ItemStack>> soulGroupRef = soulbound.computeIfAbsent(group, s -> new HashMap<>());
            Map<String, DefaultedList<ItemStack>> invGroupRef = playerInv.computeIfAbsent(group, s -> new HashMap<>());

            slots.forEach((slot, trinkets) -> {
                DefaultedList<ItemStack> soulInv = DefaultedList.of();
                DefaultedList<ItemStack> stacks = DefaultedList.of();
                for (int i = 0; i < trinkets.size(); i++) {
                    ItemStack stack = trinkets.getStack(i);
                    if (stack.isEmpty()) continue;

                    boolean removed = false;

                    if (onDeath) {
                        if (GraveHelper.hasEnchantments(deleteEnchantments, stack) || GraveHelper.hasBotaniaKeepIvy(stack, true)) {
                            trinkets.setStack(i, ItemStack.EMPTY);
                            removed = true;
                        } else if (GraveHelper.hasEnchantments(soulboundEnchantments, stack) || stack.isIn(ModTags.SOULBOUND_ITEM) || onDeathHandling == DeathEffectConfig.KEEP_ITEMS) {
                            trinkets.setStack(i, ItemStack.EMPTY);
                            soulInv.add(stack);
                            removed = true;
                        }
                    }
                    if (!removed) stacks.add(stack);
                }
                soulGroupRef.computeIfAbsent(slot, s -> soulInv);
                invGroupRef.computeIfAbsent(slot, s -> stacks);
            });
        });
        if (onDeath) DeadPlayerData.Soulbound.addModdedSoulbound(player.getUuid(), soulbound);
        return playerInv;
    }

    @Override
    public DefaultedList<ItemStack> setInventory(Object inventory, PlayerEntity player) {
        DefaultedList<ItemStack> extraItems = DefaultedList.of();

        Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(player);
        if (optional.isEmpty()) return extraItems;
        TrinketComponent playerComponent = optional.get();

        if (!(inventory instanceof Map fullInv)) return extraItems;
        fullInv.forEach((g, map) -> {
            if (!(map instanceof Map groupInv && g instanceof String group)) return;
            groupInv.forEach((s, trinket) -> {
                if (!(trinket instanceof DefaultedList stacks && s instanceof String slot)) return;
                if (playerComponent.getInventory().get(group) == null) {
                    extraItems.addAll(stacks);
                    return;
                }
                TrinketInventory equippedInv = playerComponent.getInventory().get(group).get(slot);
                if (equippedInv == null) {
                    extraItems.addAll(stacks);
                    return;
                }

                for (int i = 0; i < Math.min(equippedInv.size(), stacks.size()); i++) {
                    if (!(stacks.get(i) instanceof ItemStack stack)) continue;

                    ItemStack equipped = equippedInv.getStack(i);
                    if (!equipped.isEmpty()) {
                        extraItems.add(stack);
                        continue;
                    }
                    equippedInv.setStack(i, stack);
                }
            });
        });

        return extraItems;
    }

    @Override
    public int getInventorySize(Object inventory) {
        if (!(inventory instanceof Map)) return 0;
        Map<String, Map<String, DefaultedList<ItemStack>>> modInv = (Map<String, Map<String, DefaultedList<ItemStack>>>) inventory;

        List<ItemStack> items = new ArrayList<>();
        modInv.forEach(((g, group) -> group.forEach((s, slot) -> slot.forEach(stack -> {
            if (stack == null || stack.isEmpty()) return;
            items.add(stack);
        }))));

        return items.size();
    }

    @Override
    public void dropAll(PlayerEntity player) {
        Optional<TrinketComponent> optional = TrinketsApi.getTrinketComponent(player);
        if (optional.isEmpty()) return;
        TrinketComponent component = optional.get();

        component.forEach((ref, stack) -> {
            if (stack.isEmpty()) return;
            ref.inventory().clear();
        });
    }

    @Override
    public List<ItemStack> toStackList(Object inventory) {
        List<ItemStack> stacks = new ArrayList<>();
        if (!(inventory instanceof Map inv)) return stacks;
        inv.forEach((group, slots) -> {
            if (!(slots instanceof Map slotMap)) return;
            slotMap.forEach((slot, items) -> {
                if (!(items instanceof DefaultedList trinketInv)) return;
                for (ItemStack item : (DefaultedList<ItemStack>) trinketInv) {
                    if (item.isEmpty()) continue;
                    stacks.add(item);
                }
            });
        });

        return stacks;
    }

    @Override
    public NbtCompound writeNbt(Object o) {
        NbtCompound nbt = new NbtCompound();
        if (!(o instanceof Map inv)) return nbt;
        inv.forEach((group, slots) -> {
            if (!(group instanceof String groupName && slots instanceof Map slotMap)) return;
            NbtCompound groupNbt = new NbtCompound();

            slotMap.forEach((slot, items) -> {
                if (!(slot instanceof String slotName && items instanceof DefaultedList stacks)) return;
                groupNbt.put(slotName, Inventories.writeNbt(new NbtCompound(), (DefaultedList<ItemStack>) stacks));
            });

            nbt.put(groupName, groupNbt);
        });

        return nbt;
    }

    @Override
    public Object readNbt(NbtCompound nbt) {
        Map<String, Map<String, DefaultedList<ItemStack>>> inventory = new HashMap<>();
        Set<String> groupKeys = nbt.getKeys();
        for (String groupKey : groupKeys) {
            NbtCompound groupNbt = (NbtCompound) nbt.get(groupKey);
            Map<String, DefaultedList<ItemStack>> groupMap = inventory.computeIfAbsent(groupKey, s -> new HashMap<>());
            if (groupNbt == null) continue;

            Set<String> slotKeys = groupNbt.getKeys();
            for (String slotKey : slotKeys) {
                NbtCompound itemsNbt = (NbtCompound) groupNbt.get(slotKey);
                if (itemsNbt == null) continue;
                NbtList nbtList = itemsNbt.getList("Items", 10);

                DefaultedList<ItemStack> items = DefaultedList.ofSize(nbtList.size(), ItemStack.EMPTY);
                Inventories.readNbt(itemsNbt, items);

                groupMap.putIfAbsent(slotKey, items);
            }
        }

        return inventory;
    }
}