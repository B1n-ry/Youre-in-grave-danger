package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import dev.emi.trinkets.api.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class TrinketsCompat implements InvModCompat<Map<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>>> {

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
    public CompatComponent<Map<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>>> readNbt(NbtCompound nbt) {
        Map<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> inventory = new HashMap<>();

        for (String groupName : nbt.getKeys()) {
            NbtCompound groupNbt = nbt.getCompound(groupName);
            Map<String, DefaultedList<Pair<ItemStack, DropRule>>> groupMap = new HashMap<>();

            for (String slotName : groupNbt.getKeys()) {
                NbtCompound slotNbt = groupNbt.getCompound(slotName);
                DefaultedList<Pair<ItemStack, DropRule>> items = InventoryComponent.listFromNbt(slotNbt, itemNbt -> {
                    ItemStack stack = ItemStack.fromNbt(itemNbt);
                    DropRule dropRule;
                    if (itemNbt.contains("dropRule")) {
                        // We need to check in case the drop rule is a trinket drop rule (only has one difference and that is trinkets have DEFAULT)
                        String dropRuleString = itemNbt.getString("dropRule");
                        if (dropRuleString.equals("DEFAULT")) {
                            dropRule = YigdConfig.getConfig().compatConfig.defaultTrinketsDropRule;
                        } else {
                            dropRule = DropRule.valueOf(dropRuleString);
                        }
                    } else {
                        dropRule = YigdConfig.getConfig().compatConfig.defaultTrinketsDropRule;
                    }

                    return new Pair<>(stack, dropRule);
                }, InventoryComponent.EMPTY_ITEM_PAIR, "inventory", "size");

                groupMap.put(slotName, items);
            }

            inventory.put(groupName, groupMap);
        }

        return new TrinketsCompatComponent(inventory);
    }

    @Override
    public CompatComponent<Map<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>>> getNewComponent(ServerPlayerEntity player) {
        return new TrinketsCompatComponent(player);
    }


    private static class TrinketsCompatComponent extends CompatComponent<Map<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>>> {

        public TrinketsCompatComponent(ServerPlayerEntity player) {
            super(player);
        }
        public TrinketsCompatComponent(Map<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> inventory) {
            super(inventory);
        }

        private DropRule convertDropRule(TrinketEnums.DropRule dropRule) {
            return switch (dropRule) {
                case KEEP -> DropRule.KEEP;
                case DESTROY -> DropRule.DESTROY;
                default -> DropRule.PUT_IN_GRAVE;
            };
        }

        @Override
        public Map<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> getInventory(ServerPlayerEntity player) {
            Map<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> items = new HashMap<>();

            TrinketsApi.getTrinketComponent(player).ifPresent(component -> {
                for (Map.Entry<String, Map<String, TrinketInventory>> group : component.getInventory().entrySet()) {
                    String groupString = group.getKey();
                    Map<String, DefaultedList<Pair<ItemStack, DropRule>>> slotMap = new HashMap<>();
                    for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
                        String slotString = slot.getKey();
                        TrinketInventory trinketInventory = slot.getValue();

                        DefaultedList<Pair<ItemStack, DropRule>> itemsInInventory = DefaultedList.of();
                        for (int i = 0; i < trinketInventory.size(); i++) {
                            ItemStack stack = trinketInventory.getStack(i);
                            SlotReference ref = new SlotReference(trinketInventory, i);
                            TrinketEnums.DropRule dropRule = TrinketsApi.getTrinket(stack.getItem()).getDropRule(stack, ref, player);

                            itemsInInventory.add(new Pair<>(trinketInventory.getStack(i), this.convertDropRule(dropRule)));
                        }

                        slotMap.put(slotString, itemsInInventory);
                    }
                    items.put(groupString, slotMap);
                }
            });

            return items;
        }

        @Override
        public DefaultedList<ItemStack> pullBindingCurseItems(ServerPlayerEntity playerRef) {
            DefaultedList<ItemStack> noUnequipItems = DefaultedList.of();

            if (!YigdConfig.getConfig().graveConfig.treatBindingCurse) return noUnequipItems;

            Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(playerRef);
            if (trinketComponent.isPresent()) {
                Map<String, Map<String, TrinketInventory>> trinketInventory = trinketComponent.get().getInventory();

                for (Map.Entry<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> group : this.inventory.entrySet()) {
                    Map<String, TrinketInventory> componentSlots = trinketInventory.get(group.getKey());
                    if (componentSlots == null) continue;

                    for (Map.Entry<String, DefaultedList<Pair<ItemStack, DropRule>>> slot : group.getValue().entrySet()) {
                        TrinketInventory trinketSlot = componentSlots.get(slot.getKey());
                        if (trinketSlot == null) continue;

                        DefaultedList<Pair<ItemStack, DropRule>> slotItems = slot.getValue();
                        for (int i = 0; i < slotItems.size(); i++) {
                            Pair<ItemStack, DropRule> pair = slotItems.get(i);
                            ItemStack item = pair.getLeft();
                            if (item.isEmpty()) {
                                continue;
                            }
                            SlotReference ref = new SlotReference(trinketSlot, i);
                            if (!TrinketsApi.getTrinket(item.getItem()).canUnequip(item, ref, playerRef)) {
                                noUnequipItems.add(item.copy());
                                slotItems.set(i, InventoryComponent.EMPTY_ITEM_PAIR);
                            }
                        }
                    }
                }
            }

            return noUnequipItems;
        }

        @Override
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayerEntity merger) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(merger);

            @SuppressWarnings("unchecked")
            Map<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> mergingInventory = (Map<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>>) mergingComponent.inventory;
            for (Map.Entry<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> groupEntry : mergingInventory.entrySet()) {  // From merging
                String groupName = groupEntry.getKey();
                Map<String, DefaultedList<Pair<ItemStack, DropRule>>> slotMap = this.inventory.get(groupName);  // From this
                if (slotMap == null) {
                    for (DefaultedList<Pair<ItemStack, DropRule>> items : groupEntry.getValue().values()) {
                        for (Pair<ItemStack, DropRule> stack : items) {
                            extraItems.add(stack.getLeft().copy());  // Solves the issue where the itemstacks are the same instance
                        }
                    }
                    continue;
                }
                for (Map.Entry<String, DefaultedList<Pair<ItemStack, DropRule>>> slotEntry : groupEntry.getValue().entrySet()) {  // From merging
                    String slotName = slotEntry.getKey();
                    DefaultedList<Pair<ItemStack, DropRule>> stacks = slotMap.get(slotName);  // From this
                    DefaultedList<Pair<ItemStack, DropRule>> mergingItems = slotEntry.getValue();  // From merging
                    if (stacks == null) {
                        for (Pair<ItemStack, DropRule> stack : mergingItems) {
                            extraItems.add(stack.getLeft().copy());  // Solves the issue where the itemstacks are the same instance
                        }
                        continue;
                    }

                    for (int i = 0; i < mergingItems.size(); i++) {
                        Pair<ItemStack, DropRule> pair = mergingItems.get(i);
                        ItemStack mergingStack = pair.getLeft().copy();  // Solves the issue where the itemstacks are the same instance

                        Pair<ItemStack, DropRule> currentPair = stacks.get(i);
                        if (YigdConfig.getConfig().graveConfig.treatBindingCurse && !this.canUnequip(trinketComponent.orElse(null), slotName, groupName, i, mergingStack, merger)) {
                            extraItems.add(currentPair.getLeft());  // Add the current item to extraItems (as it's being replaced)
                            stacks.set(i, new Pair<>(mergingStack, pair.getRight()));  // Can't be unequipped, so it's prioritized
                            continue;  // Already set the item, so we can skip the rest
                        }

                        if (stacks.size() <= i || !currentPair.getLeft().isEmpty()) {
                            extraItems.add(mergingStack);
                            continue;
                        }

                        stacks.set(i, new Pair<>(mergingStack, pair.getRight()));
                    }
                }
            }

            extraItems.removeIf(ItemStack::isEmpty);
            return extraItems;
        }
        private boolean canUnequip(@Nullable TrinketComponent component, String slot, String group, int index, ItemStack item, ServerPlayerEntity player) {
            if (component == null) return true;
            Map<String, TrinketInventory> trinketGroup = component.getInventory().get(group);
            if (trinketGroup == null) return true;
            TrinketInventory trinketInventory = trinketGroup.get(slot);
            if (trinketInventory == null || trinketInventory.size() <= index) return true;

            if (item.isEmpty()) return true;
            SlotReference ref = new SlotReference(trinketInventory, index);
            return TrinketsApi.getTrinket(item.getItem()).canUnequip(item, ref, player);
        }

        @Override
        public DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            TrinketsApi.getTrinketComponent(player).ifPresent(trinketComponent -> {
                // Traverse through groups
                for (Map.Entry<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> group : this.inventory.entrySet()) {
                    Map<String, TrinketInventory> componentSlots = trinketComponent.getInventory().get(group.getKey());
                    if (componentSlots == null) {  // The trinket group is missing, and all those items need to be added to extraItems
                        for (DefaultedList<Pair<ItemStack, DropRule>> itemList : group.getValue().values()) {
                            for (Pair<ItemStack, DropRule> stack : itemList) {
                                extraItems.add(stack.getLeft());
                            }
                        }
                        continue;
                    }

                    // Traverse through slots
                    for (Map.Entry<String, DefaultedList<Pair<ItemStack, DropRule>>> slot : group.getValue().entrySet()) {
                        TrinketInventory trinketInventory = componentSlots.get(slot.getKey());

                        DefaultedList<Pair<ItemStack, DropRule>> slotItems = slot.getValue();

                        if (trinketInventory == null) {  // The trinket slot is missing, and all those items need to be added to extraItems
                            for (Pair<ItemStack, DropRule> stack : slotItems) {
                                extraItems.add(stack.getLeft());
                            }
                            continue;
                        }

                        // Traverse through item stacks
                        for (int i = 0; i < slotItems.size(); i++) {
                            Pair<ItemStack, DropRule> pair = slotItems.get(i);
                            ItemStack item = pair.getLeft();
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
        public void handleDropRules(DeathContext context) {
            // Traverse through groups
            for (Map<String, DefaultedList<Pair<ItemStack, DropRule>>> group : this.inventory.values()) {

                // Traverse through slots
                for (DefaultedList<Pair<ItemStack, DropRule>> slotItems : group.values()) {

                    // Traverse through item stacks
                    for (Pair<ItemStack, DropRule> pair : slotItems) {
                        ItemStack item = pair.getLeft();

                        if (item.isEmpty()) continue;

                        DropRule dropRule = pair.getRight();
                        if (dropRule == DropRule.PUT_IN_GRAVE)
                            dropRule = DropRuleEvent.EVENT.invoker().getDropRule(item, -1, context, true);

                        pair.setRight(dropRule);
                    }
                }
            }
        }

        @Override
        public DefaultedList<Pair<ItemStack, DropRule>> getAsStackDropList() {
            DefaultedList<Pair<ItemStack, DropRule>> allItems = DefaultedList.of();
            for (Map<String, DefaultedList<Pair<ItemStack, DropRule>>> slotMap : this.inventory.values()) {
                for (DefaultedList<Pair<ItemStack, DropRule>> itemStacks : slotMap.values()) {
                    allItems.addAll(itemStacks);
                }
            }

            return allItems;
        }

        @Override
        public CompatComponent<Map<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>>> filterInv(Predicate<DropRule> predicate) {
            Map<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> filtered = new HashMap<>();

            for (Map.Entry<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> group : this.inventory.entrySet()) {
                Map<String, DefaultedList<Pair<ItemStack, DropRule>>> filteredGroup = new HashMap<>();

                for (Map.Entry<String, DefaultedList<Pair<ItemStack, DropRule>>> slot : group.getValue().entrySet()) {
                    DefaultedList<Pair<ItemStack, DropRule>> filteredSlot = DefaultedList.of();

                    DefaultedList<Pair<ItemStack, DropRule>> slotItems = slot.getValue();
                    for (Pair<ItemStack, DropRule> pair : slotItems) {
                        if (predicate.test(pair.getRight())) {
                            filteredSlot.add(pair);
                        } else {
                            filteredSlot.add(InventoryComponent.EMPTY_ITEM_PAIR);
                        }
                    }
                    filteredGroup.put(slot.getKey(), filteredSlot);
                }
                filtered.put(group.getKey(), filteredGroup);
            }
            return new TrinketsCompatComponent(filtered);
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            for (Map<String, DefaultedList<Pair<ItemStack, DropRule>>> group : this.inventory.values()) {
                for (DefaultedList<Pair<ItemStack, DropRule>> slot : group.values()) {
                    for (Pair<ItemStack, DropRule> pair : slot) {
                        ItemStack stack = pair.getLeft();
                        if (predicate.test(stack)) {
                            stack.decrement(itemCount);

                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public void clear() {
            for (Map<String, DefaultedList<Pair<ItemStack, DropRule>>> slotMap : this.inventory.values()) {
                for (DefaultedList<Pair<ItemStack, DropRule>> items : slotMap.values()) {
                    Collections.fill(items, InventoryComponent.EMPTY_ITEM_PAIR);
                }
            }
        }

        @Override
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();

            // Traverse through groups
            for (Map.Entry<String, Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> group : this.inventory.entrySet()) {
                NbtCompound groupNbt = new NbtCompound();

                // Traverse through slots
                for (Map.Entry<String, DefaultedList<Pair<ItemStack, DropRule>>> slot : group.getValue().entrySet()) {
                    DefaultedList<Pair<ItemStack, DropRule>> slotItems = slot.getValue();

                    NbtCompound slotNbt = InventoryComponent.listToNbt(slotItems, pair -> {
                        NbtCompound itemNbt = new NbtCompound();
                        pair.getLeft().writeNbt(itemNbt);
                        itemNbt.putString("dropRule", pair.getRight().name());

                        return itemNbt;
                    }, pair -> pair.getLeft().isEmpty(), "inventory", "size");

                    groupNbt.put(slot.getKey(), slotNbt);
                }
                nbt.put(group.getKey(), groupNbt);
            }

            return nbt;
        }
    }
}
