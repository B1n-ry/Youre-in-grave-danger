package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

import java.util.HashMap;
import java.util.Map;

public class TrinketsCompat implements InvModCompat<Map<String, Map<String, DefaultedList<ItemStack>>>> {
    @Override
    public String getModName() {
        return "trinkets";
    }

    @Override
    public Map<String, Map<String, DefaultedList<ItemStack>>> getInventory(ServerPlayerEntity player) {
        Map<String, Map<String, DefaultedList<ItemStack>>> items = new HashMap<>();

        TrinketsApi.getTrinketComponent(player).ifPresent(component -> {
            for (Map.Entry<String, Map<String, TrinketInventory>> group : component.getInventory().entrySet()) {
                String groupString = group.getKey();
                Map<String, DefaultedList<ItemStack>> slotMap = new HashMap<>();
                for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
                    String slotString = slot.getKey();
                    TrinketInventory trinketInventory = slot.getValue();

                    DefaultedList<ItemStack> itemsInInventory = DefaultedList.of();
                    for (int i = 0; i < trinketInventory.size(); i++) {
                        itemsInInventory.add(trinketInventory.getStack(i));
                    }

                    slotMap.put(slotString, itemsInInventory);
                }
                items.put(groupString, slotMap);
            }
        });

        return items;
    }

    @Override
    public DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player, Map<String, Map<String, DefaultedList<ItemStack>>> inventory) {
        DefaultedList<ItemStack> extraItems = DefaultedList.of();

        TrinketsApi.getTrinketComponent(player).ifPresent(trinketComponent -> {
            // Traverse through groups
            for (Map.Entry<String, Map<String, DefaultedList<ItemStack>>> group : inventory.entrySet()) {
                Map<String, TrinketInventory> componentSlots = trinketComponent.getInventory().get(group.getKey());
                if (componentSlots == null) {  // The trinket group is missing, and all those items need to be added to extraItems
                    for (DefaultedList<ItemStack> itemList : group.getValue().values()) {
                        extraItems.addAll(itemList);
                    }
                    continue;
                }

                // Traverse through slots
                for (Map.Entry<String, DefaultedList<ItemStack>> slot : group.getValue().entrySet()) {
                    TrinketInventory trinketInventory = componentSlots.get(slot.getKey());

                    if (trinketInventory == null) {  // The trinket slot is missing, and all those items need to be added to extraItems
                        extraItems.addAll(slot.getValue());
                        continue;
                    }

                    DefaultedList<ItemStack> slotItems = slot.getValue();

                    // Traverse through item stacks
                    for (int i = 0; i < slotItems.size(); i++) {
                        ItemStack item = slotItems.get(i);
                        if (i >= trinketInventory.size() || !trinketInventory.getStack(i).isEmpty()) {
                            extraItems.add(item);
                            continue;
                        }
                        trinketInventory.setStack(i, item);
                    }
                }
            }
        });

        return extraItems;
    }

    @Override
    public Map<String, Map<String, DefaultedList<ItemStack>>> handleDropRules(Map<String, Map<String, DefaultedList<ItemStack>>> inventory, DeathContext context) {
        Map<String, Map<String, DefaultedList<ItemStack>>> soulboundInventory = new HashMap<>();

        // Traverse through groups
        for (Map.Entry<String, Map<String, DefaultedList<ItemStack>>> group : inventory.entrySet()) {
            Map<String, DefaultedList<ItemStack>> soulboundGroup = new HashMap<>();

            // Traverse through slots
            for (Map.Entry<String, DefaultedList<ItemStack>> slot : group.getValue().entrySet()) {
                DefaultedList<ItemStack> slotItems = slot.getValue();

                DefaultedList<ItemStack> soulboundItems = DefaultedList.ofSize(slotItems.size(), ItemStack.EMPTY);

                // Traverse through item stacks
                for (int i = 0; i < slotItems.size(); i++) {
                    ItemStack item = slotItems.get(i);

                    DropRule dropRule = DropRuleEvent.DESTROY_ITEM_EVENT.invoker().getDropRule(item, -1, context);
                    switch (dropRule) {
                        case DESTROY -> slotItems.set(i, ItemStack.EMPTY);
                        case KEEP -> {
                            slotItems.set(i, ItemStack.EMPTY);
                            soulboundItems.set(i, item);
                        }
                    }
                }

                soulboundGroup.put(slot.getKey(), soulboundItems);
            }
            soulboundInventory.put(group.getKey(), soulboundGroup);
        }

        return soulboundInventory;
    }

    @Override
    public DefaultedList<ItemStack> getAsStackList(Map<String, Map<String, DefaultedList<ItemStack>>> inventory) {
        DefaultedList<ItemStack> allItems = DefaultedList.of();
        for (Map<String, DefaultedList<ItemStack>> slotMap : inventory.values()) {
            for (DefaultedList<ItemStack> itemStacks : slotMap.values()) {
                allItems.addAll(itemStacks);
            }
        }

        return allItems;
    }

    @Override
    public void clear(ServerPlayerEntity player) {
        TrinketsApi.getTrinketComponent(player).ifPresent(trinketComponent -> {
            for (Map.Entry<String, Map<String, TrinketInventory>> groupEntry : trinketComponent.getInventory().entrySet()) {
                for (Map.Entry<String, TrinketInventory> slotEntry : groupEntry.getValue().entrySet()) {
                    slotEntry.getValue().clear();
                }
            }
        });
    }

    @Override
    public NbtCompound writeNbt(Map<String, Map<String, DefaultedList<ItemStack>>> inventory) {
        NbtCompound nbt = new NbtCompound();

        // Traverse through groups
        for (Map.Entry<String, Map<String, DefaultedList<ItemStack>>> group : inventory.entrySet()) {
            NbtCompound groupNbt = new NbtCompound();

            // Traverse through slots
            for (Map.Entry<String, DefaultedList<ItemStack>> slot : group.getValue().entrySet()) {
                DefaultedList<ItemStack> slotItems = slot.getValue();
                NbtCompound slotNbt = Inventories.writeNbt(new NbtCompound(), slotItems);
                slotNbt.putInt("size", slotItems.size());

                groupNbt.put(slot.getKey(), slotNbt);
            }
            nbt.put(group.getKey(), groupNbt);
        }

        return nbt;
    }

    @Override
    public Map<String, Map<String, DefaultedList<ItemStack>>> readNbt(NbtCompound nbt) {
        Map<String, Map<String, DefaultedList<ItemStack>>> inventory = new HashMap<>();

        for (String groupName : nbt.getKeys()) {
            NbtCompound groupNbt = nbt.getCompound(groupName);
            Map<String, DefaultedList<ItemStack>> groupMap = new HashMap<>();

            for (String slotName : groupNbt.getKeys()) {
                NbtCompound slotNbt = groupNbt.getCompound(slotName);
                int listSize = slotNbt.getInt("size");
                DefaultedList<ItemStack> items = DefaultedList.ofSize(listSize, ItemStack.EMPTY);

                Inventories.readNbt(slotNbt, items);

                groupMap.put(slotName, items);
            }

            inventory.put(groupName, groupMap);
        }

        return inventory;
    }
}
