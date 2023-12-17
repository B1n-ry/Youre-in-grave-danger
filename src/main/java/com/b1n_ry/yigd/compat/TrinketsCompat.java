package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import dev.emi.trinkets.api.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class TrinketsCompat implements InvModCompat<Map<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>>> {
    private static final Pair<TrinketEnums.DropRule, ItemStack> EMPTY_PAIR = new Pair<>(TrinketEnums.DropRule.DEFAULT, ItemStack.EMPTY);

    @Override
    public String getModName() {
        return "trinkets";
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
    public CompatComponent<Map<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>>> readNbt(NbtCompound nbt) {
        Map<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>> inventory = new HashMap<>();

        for (String groupName : nbt.getKeys()) {
            NbtCompound groupNbt = nbt.getCompound(groupName);
            Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>> groupMap = new HashMap<>();

            for (String slotName : groupNbt.getKeys()) {
                NbtCompound slotNbt = groupNbt.getCompound(slotName);
                int listSize = slotNbt.getInt("size");
                DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>> items = DefaultedList.ofSize(listSize, EMPTY_PAIR);

                NbtList nbtInventory = slotNbt.getList("inventory", NbtElement.COMPOUND_TYPE);
                for (NbtElement elem : nbtInventory) {
                    NbtCompound comp = (NbtCompound) elem;
                    ItemStack stack = ItemStack.fromNbt(comp);
                    TrinketEnums.DropRule dropRule = TrinketEnums.DropRule.valueOf(comp.getString("dropRule"));
                    int slot = comp.getInt("slot");

                    items.set(slot, new Pair<>(dropRule, stack));
                }

                groupMap.put(slotName, items);
            }

            inventory.put(groupName, groupMap);
        }

        return new TrinketsCompatComponent(inventory);
    }

    @Override
    public CompatComponent<Map<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>>> getNewComponent(ServerPlayerEntity player) {
        return new TrinketsCompatComponent(player);
    }


    private static class TrinketsCompatComponent extends CompatComponent<Map<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>>> {

        public TrinketsCompatComponent(ServerPlayerEntity player) {
            super(player);
        }
        public TrinketsCompatComponent(Map<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>> inventory) {
            super(inventory);
        }

        @Override
        public Map<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>> getInventory(ServerPlayerEntity player) {
            Map<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>> items = new HashMap<>();

            TrinketsApi.getTrinketComponent(player).ifPresent(component -> {
                for (Map.Entry<String, Map<String, TrinketInventory>> group : component.getInventory().entrySet()) {
                    String groupString = group.getKey();
                    Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>> slotMap = new HashMap<>();
                    for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
                        String slotString = slot.getKey();
                        TrinketInventory trinketInventory = slot.getValue();

                        DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>> itemsInInventory = DefaultedList.of();
                        for (int i = 0; i < trinketInventory.size(); i++) {
                            ItemStack stack = trinketInventory.getStack(i);
                            SlotReference ref = new SlotReference(trinketInventory, i);
                            TrinketEnums.DropRule dropRule = TrinketsApi.getTrinket(stack.getItem()).getDropRule(stack, ref, player);
                            itemsInInventory.add(new Pair<>(dropRule, trinketInventory.getStack(i)));
                        }

                        slotMap.put(slotString, itemsInInventory);
                    }
                    items.put(groupString, slotMap);
                }
            });

            return items;
        }

        @Override
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            @SuppressWarnings("unchecked")
            Map<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>> mergingInventory = (Map<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>>) mergingComponent.inventory;
            for (Map.Entry<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>> groupEntry : mergingInventory.entrySet()) {  // From merging
                String groupName = groupEntry.getKey();
                Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>> slotMap = this.inventory.get(groupName);  // From this
                if (slotMap == null) {
                    for (DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>> items : groupEntry.getValue().values()) {
                        for (Pair<TrinketEnums.DropRule, ItemStack> stack : items) {
                            extraItems.add(stack.getRight().copy());  // Solves the issue where the itemstacks are the same instance
                        }
                    }
                    continue;
                }
                for (Map.Entry<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>> slotEntry : groupEntry.getValue().entrySet()) {  // From merging
                    String slotName = slotEntry.getKey();
                    DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>> stacks = slotMap.get(slotName);  // From this
                    DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>> mergingItems = slotEntry.getValue();  // From merging
                    if (stacks == null) {
                        for (Pair<TrinketEnums.DropRule, ItemStack> stack : mergingItems) {
                            extraItems.add(stack.getRight().copy());  // Solves the issue where the itemstacks are the same instance
                        }
                        continue;
                    }

                    for (int i = 0; i < mergingItems.size(); i++) {
                        Pair<TrinketEnums.DropRule, ItemStack> pair = mergingItems.get(i);
                        ItemStack mergingStack = pair.getRight().copy();  // Solves the issue where the itemstacks are the same instance
                        if (stacks.size() <= i || !stacks.get(i).getRight().isEmpty()) {
                            extraItems.add(mergingStack);
                            continue;
                        }

                        stacks.set(i, pair);
                    }
                }
            }

            extraItems.removeIf(ItemStack::isEmpty);
            return extraItems;
        }

        @Override
        public DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            TrinketsApi.getTrinketComponent(player).ifPresent(trinketComponent -> {
                // Traverse through groups
                for (Map.Entry<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>> group : this.inventory.entrySet()) {
                    Map<String, TrinketInventory> componentSlots = trinketComponent.getInventory().get(group.getKey());
                    if (componentSlots == null) {  // The trinket group is missing, and all those items need to be added to extraItems
                        for (DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>> itemList : group.getValue().values()) {
                            for (Pair<TrinketEnums.DropRule, ItemStack> stack : itemList) {
                                extraItems.add(stack.getRight());
                            }
                        }
                        continue;
                    }

                    // Traverse through slots
                    for (Map.Entry<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>> slot : group.getValue().entrySet()) {
                        TrinketInventory trinketInventory = componentSlots.get(slot.getKey());

                        DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>> slotItems = slot.getValue();

                        if (trinketInventory == null) {  // The trinket slot is missing, and all those items need to be added to extraItems
                            for (Pair<TrinketEnums.DropRule, ItemStack> stack : slotItems) {
                                extraItems.add(stack.getRight());
                            }
                            continue;
                        }

                        // Traverse through item stacks
                        for (int i = 0; i < slotItems.size(); i++) {
                            Pair<TrinketEnums.DropRule, ItemStack> pair = slotItems.get(i);
                            ItemStack item = pair.getRight();
                            if (i >= trinketInventory.size()) {
                                extraItems.add(item);
                                continue;
                            }
                            trinketInventory.setStack(i, item);
                        }
                    }
                }
            });

            extraItems.removeIf(ItemStack::isEmpty);
            return extraItems;
        }

        @Override
        public CompatComponent<Map<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>>> handleDropRules(DeathContext context) {
            YigdConfig.CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;
            Map<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>> soulboundInventory = new HashMap<>();

            Vec3d deathPos = context.getDeathPos();
            // Traverse through groups
            for (Map.Entry<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>> group : this.inventory.entrySet()) {
                Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>> soulboundGroup = new HashMap<>();

                // Traverse through slots
                for (Map.Entry<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>> slot : group.getValue().entrySet()) {
                    DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>> slotItems = slot.getValue();

                    DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>> soulboundItems = DefaultedList.ofSize(slotItems.size(), EMPTY_PAIR);

                    // Traverse through item stacks
                    for (int i = 0; i < slotItems.size(); i++) {
                        Pair<TrinketEnums.DropRule, ItemStack> pair = slotItems.get(i);
                        ItemStack item = pair.getRight();

                        DropRule dropRule = compatConfig.defaultTrinketsDropRule;
                        if (dropRule == DropRule.PUT_IN_GRAVE)
                            dropRule = DropRuleEvent.EVENT.invoker().getDropRule(item, -1, context, true);

                        if (dropRule == DropRule.PUT_IN_GRAVE) {
                            switch (pair.getLeft()) {  // Translate trinket drop rules
                                case DESTROY -> dropRule = DropRule.DESTROY;
                                case KEEP -> dropRule = DropRule.KEEP;
                            }
                        }

                        switch (dropRule) {
                            case KEEP -> soulboundItems.set(i, new Pair<>(TrinketEnums.DropRule.DEFAULT, item));
                            case DROP -> InventoryComponent.dropItemIfToBeDropped(item, deathPos.x, deathPos.y, deathPos.z, context.getWorld());
                        }

                        if (dropRule != DropRule.PUT_IN_GRAVE)
                            slotItems.set(i, EMPTY_PAIR);
                    }

                    soulboundGroup.put(slot.getKey(), soulboundItems);
                }
                soulboundInventory.put(group.getKey(), soulboundGroup);
            }

            return new TrinketsCompatComponent(soulboundInventory);
        }

        @Override
        public DefaultedList<ItemStack> getAsStackList() {
            DefaultedList<ItemStack> allItems = DefaultedList.of();
            for (Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>> slotMap : this.inventory.values()) {
                for (DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>> itemStacks : slotMap.values()) {
                    for (Pair<TrinketEnums.DropRule, ItemStack> pair : itemStacks) {
                        allItems.add(pair.getRight());
                    }
                }
            }

            return allItems;
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            for (Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>> group : this.inventory.values()) {
                for (DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>> slot : group.values()) {
                    for (Pair<TrinketEnums.DropRule, ItemStack> stack : slot) {
                        ItemStack item = stack.getRight();
                        if (predicate.test(item)) {
                            item.decrement(itemCount);

                            if (item.getCount() == 0) {
                                stack.setRight(ItemStack.EMPTY);
                            }
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public void clear() {
            for (Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>> slotMap : this.inventory.values()) {
                for (DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>> items : slotMap.values()) {
                    Collections.fill(items, EMPTY_PAIR);
                }
            }
        }

        @Override
        public boolean isEmpty() {
            DefaultedList<ItemStack> items = DefaultedList.of();
            items.addAll(this.getAsStackList());

            items.removeIf(ItemStack::isEmpty);
            return items.isEmpty();
        }

        @Override
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();

            // Traverse through groups
            for (Map.Entry<String, Map<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>>> group : this.inventory.entrySet()) {
                NbtCompound groupNbt = new NbtCompound();

                // Traverse through slots
                for (Map.Entry<String, DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>>> slot : group.getValue().entrySet()) {
                    DefaultedList<Pair<TrinketEnums.DropRule, ItemStack>> slotItems = slot.getValue();

                    NbtCompound slotNbt = new NbtCompound();
                    NbtList itemNbtList = new NbtList();
                    for (int i = 0; i < slotItems.size(); i++) {
                        Pair<TrinketEnums.DropRule, ItemStack> item = slotItems.get(i);
                        ItemStack stack = item.getRight();
                        if (stack.isEmpty()) continue;

                        NbtCompound itemNbt = new NbtCompound();
                        itemNbt.putString("dropRule", item.getLeft().toString());
                        itemNbt.putInt("slot", i);
                        stack.writeNbt(itemNbt);

                        itemNbtList.add(itemNbt);
                    }
                    slotNbt.put("inventory", itemNbtList);
                    slotNbt.putInt("size", slotItems.size());

                    groupNbt.put(slot.getKey(), slotNbt);
                }
                nbt.put(group.getKey(), groupNbt);
            }

            return nbt;
        }
    }
}
