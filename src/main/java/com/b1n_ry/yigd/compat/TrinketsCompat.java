package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.data.GraveItem;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import dev.emi.trinkets.api.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class TrinketsCompat implements InvModCompat<Map<String, Map<String, DefaultedList<GraveItem>>>> {

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
    public CompatComponent<Map<String, Map<String, DefaultedList<GraveItem>>>> readNbt(NbtCompound nbt) {
        Map<String, Map<String, DefaultedList<GraveItem>>> inventory = new HashMap<>();

        for (String groupName : nbt.getKeys()) {
            NbtCompound groupNbt = nbt.getCompound(groupName);
            Map<String, DefaultedList<GraveItem>> groupMap = new HashMap<>();

            for (String slotName : groupNbt.getKeys()) {
                NbtCompound slotNbt = groupNbt.getCompound(slotName);
                DefaultedList<GraveItem> items = InventoryComponent.listFromNbt(slotNbt, itemNbt -> {
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

                    return new GraveItem(stack, dropRule);
                }, InventoryComponent.EMPTY_GRAVE_ITEM, "inventory", "size");

                groupMap.put(slotName, items);
            }

            inventory.put(groupName, groupMap);
        }

        return new TrinketsCompatComponent(inventory);
    }

    @Override
    public CompatComponent<Map<String, Map<String, DefaultedList<GraveItem>>>> getNewComponent(ServerPlayerEntity player) {
        return new TrinketsCompatComponent(player);
    }


    private static class TrinketsCompatComponent extends CompatComponent<Map<String, Map<String, DefaultedList<GraveItem>>>> {

        public TrinketsCompatComponent(ServerPlayerEntity player) {
            super(player);
        }
        public TrinketsCompatComponent(Map<String, Map<String, DefaultedList<GraveItem>>> inventory) {
            super(inventory);
        }

        private DropRule convertDropRule(TrinketEnums.DropRule dropRule) {
            return switch (dropRule) {
                case KEEP -> DropRule.KEEP;
                case DESTROY -> DropRule.DESTROY;
                default -> YigdConfig.getConfig().compatConfig.defaultTrinketsDropRule;
            };
        }

        @Override
        public Map<String, Map<String, DefaultedList<GraveItem>>> getInventory(ServerPlayerEntity player) {
            Map<String, Map<String, DefaultedList<GraveItem>>> items = new HashMap<>();

            TrinketsApi.getTrinketComponent(player).ifPresent(component -> {
                for (Map.Entry<String, Map<String, TrinketInventory>> group : component.getInventory().entrySet()) {
                    String groupString = group.getKey();
                    Map<String, DefaultedList<GraveItem>> slotMap = new HashMap<>();
                    for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
                        String slotString = slot.getKey();
                        TrinketInventory trinketInventory = slot.getValue();

                        DefaultedList<GraveItem> itemsInInventory = DefaultedList.of();
                        for (int i = 0; i < trinketInventory.size(); i++) {
                            ItemStack stack = trinketInventory.getStack(i);
                            SlotReference ref = new SlotReference(trinketInventory, i);
                            TrinketEnums.DropRule dropRule = TrinketsApi.getTrinket(stack.getItem()).getDropRule(stack, ref, player);

                            itemsInInventory.add(new GraveItem(trinketInventory.getStack(i), this.convertDropRule(dropRule)));
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

                for (Map.Entry<String, Map<String, DefaultedList<GraveItem>>> group : this.inventory.entrySet()) {
                    Map<String, TrinketInventory> componentSlots = trinketInventory.get(group.getKey());
                    if (componentSlots == null) continue;

                    for (Map.Entry<String, DefaultedList<GraveItem>> slot : group.getValue().entrySet()) {
                        TrinketInventory trinketSlot = componentSlots.get(slot.getKey());
                        if (trinketSlot == null) continue;

                        DefaultedList<GraveItem> slotItems = slot.getValue();
                        for (int i = 0; i < slotItems.size(); i++) {
                            GraveItem graveItem = slotItems.get(i);
                            ItemStack item = graveItem.stack;
                            if (item.isEmpty()) {
                                continue;
                            }
                            SlotReference ref = new SlotReference(trinketSlot, i);
                            if (!TrinketsApi.getTrinket(item.getItem()).canUnequip(item, ref, playerRef)) {
                                noUnequipItems.add(item.copy());
                                slotItems.set(i, InventoryComponent.EMPTY_GRAVE_ITEM);
                            }
                        }
                    }
                }
            }

            return noUnequipItems;
        }

        @Override
        public DefaultedList<GraveItem> merge(CompatComponent<?> mergingComponent, ServerPlayerEntity merger) {
            DefaultedList<GraveItem> extraItems = DefaultedList.of();

            Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(merger);

            @SuppressWarnings("unchecked")
            Map<String, Map<String, DefaultedList<GraveItem>>> mergingInventory = (Map<String, Map<String, DefaultedList<GraveItem>>>) mergingComponent.inventory;
            for (Map.Entry<String, Map<String, DefaultedList<GraveItem>>> groupEntry : mergingInventory.entrySet()) {  // From merging
                String groupName = groupEntry.getKey();
                Map<String, DefaultedList<GraveItem>> slotMap = this.inventory.get(groupName);  // From this
                if (slotMap == null) {
                    for (DefaultedList<GraveItem> items : groupEntry.getValue().values()) {
                        for (GraveItem graveItem : items) {
                            extraItems.add(graveItem.copy());  // Solves the issue where the itemstacks are the same instance
                        }
                    }
                    continue;
                }
                for (Map.Entry<String, DefaultedList<GraveItem>> slotEntry : groupEntry.getValue().entrySet()) {  // From merging
                    String slotName = slotEntry.getKey();
                    DefaultedList<GraveItem> stacks = slotMap.get(slotName);  // From this
                    DefaultedList<GraveItem> mergingItems = slotEntry.getValue();  // From merging
                    if (stacks == null) {
                        for (GraveItem graveItem : mergingItems) {
                            extraItems.add(graveItem.copy());  // Solves the issue where the itemstacks are the same instance
                        }
                        continue;
                    }

                    for (int i = 0; i < mergingItems.size(); i++) {
                        GraveItem mergingGraveItem = mergingItems.get(i).copy();  // Solves the issue where the itemstacks are the same instance
                        ItemStack mergingStack = mergingGraveItem.stack;
                        if (mergingStack.isEmpty()) continue;

                        if (stacks.size() <= i) {
                            extraItems.add(mergingGraveItem);
                            continue;
                        }

                        GraveItem currentGraveItem = stacks.get(i);
                        ItemStack currentStack = currentGraveItem.stack;
                        if (YigdConfig.getConfig().graveConfig.treatBindingCurse && !this.canUnequip(trinketComponent.orElse(null), slotName, groupName, i, mergingStack, merger)) {
                            extraItems.add(currentGraveItem);  // Add the current item to extraItems (as it's being replaced)
                            stacks.set(i, mergingGraveItem);  // Can't be unequipped, so it's prioritized
                            continue;  // Already set the item, so we can skip the rest
                        }

                        if (!currentStack.isEmpty()) {
                            extraItems.add(mergingGraveItem);
                            continue;
                        }

                        stacks.set(i, mergingGraveItem);
                    }
                }
            }

            extraItems.removeIf(graveItem -> graveItem.stack.isEmpty());
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
                for (Map.Entry<String, Map<String, DefaultedList<GraveItem>>> group : this.inventory.entrySet()) {
                    Map<String, TrinketInventory> componentSlots = trinketComponent.getInventory().get(group.getKey());
                    if (componentSlots == null) {  // The trinket group is missing, and all those items need to be added to extraItems
                        for (DefaultedList<GraveItem> itemList : group.getValue().values()) {
                            for (GraveItem graveItem : itemList) {
                                extraItems.add(graveItem.stack.copy());
                            }
                        }
                        continue;
                    }

                    // Traverse through slots
                    for (Map.Entry<String, DefaultedList<GraveItem>> slot : group.getValue().entrySet()) {
                        TrinketInventory trinketInventory = componentSlots.get(slot.getKey());

                        DefaultedList<GraveItem> slotItems = slot.getValue();

                        if (trinketInventory == null) {  // The trinket slot is missing, and all those items need to be added to extraItems
                            for (GraveItem graveItem : slotItems) {
                                extraItems.add(graveItem.stack.copy());
                            }
                            continue;
                        }

                        // Traverse through item stacks
                        for (int i = 0; i < slotItems.size(); i++) {
                            GraveItem graveItem = slotItems.get(i);
                            ItemStack item = graveItem.stack.copy();
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
            for (Map<String, DefaultedList<GraveItem>> group : this.inventory.values()) {

                // Traverse through slots
                for (DefaultedList<GraveItem> slotItems : group.values()) {

                    // Traverse through item stacks
                    for (GraveItem graveItem : slotItems) {
                        ItemStack item = graveItem.stack;

                        if (item.isEmpty()) continue;

                        DropRule dropRule = graveItem.dropRule;
                        if (dropRule == DropRule.PUT_IN_GRAVE)
                            dropRule = DropRuleEvent.EVENT.invoker().getDropRule(item, -1, context, true);

                        graveItem.dropRule = dropRule;
                    }
                }
            }
        }

        @Override
        public DefaultedList<GraveItem> getAsGraveItemList() {
            DefaultedList<GraveItem> allItems = DefaultedList.of();
            for (Map<String, DefaultedList<GraveItem>> slotMap : this.inventory.values()) {
                for (DefaultedList<GraveItem> itemStacks : slotMap.values()) {
                    allItems.addAll(itemStacks);
                }
            }

            return allItems;
        }

        @Override
        public CompatComponent<Map<String, Map<String, DefaultedList<GraveItem>>>> filterInv(Predicate<DropRule> predicate) {
            Map<String, Map<String, DefaultedList<GraveItem>>> filtered = new HashMap<>();

            for (Map.Entry<String, Map<String, DefaultedList<GraveItem>>> group : this.inventory.entrySet()) {
                Map<String, DefaultedList<GraveItem>> filteredGroup = new HashMap<>();

                for (Map.Entry<String, DefaultedList<GraveItem>> slot : group.getValue().entrySet()) {
                    DefaultedList<GraveItem> filteredSlot = DefaultedList.of();

                    DefaultedList<GraveItem> slotItems = slot.getValue();
                    for (GraveItem graveItem : slotItems) {
                        if (predicate.test(graveItem.dropRule)) {
                            filteredSlot.add(graveItem);
                        } else {
                            filteredSlot.add(InventoryComponent.EMPTY_GRAVE_ITEM);
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
            for (Map<String, DefaultedList<GraveItem>> group : this.inventory.values()) {
                for (DefaultedList<GraveItem> slot : group.values()) {
                    for (GraveItem graveItem : slot) {
                        ItemStack stack = graveItem.stack;
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
            for (Map<String, DefaultedList<GraveItem>> slotMap : this.inventory.values()) {
                for (DefaultedList<GraveItem> items : slotMap.values()) {
                    Collections.fill(items, InventoryComponent.EMPTY_GRAVE_ITEM);
                }
            }
        }

        @Override
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();

            // Traverse through groups
            for (Map.Entry<String, Map<String, DefaultedList<GraveItem>>> group : this.inventory.entrySet()) {
                NbtCompound groupNbt = new NbtCompound();

                // Traverse through slots
                for (Map.Entry<String, DefaultedList<GraveItem>> slot : group.getValue().entrySet()) {
                    DefaultedList<GraveItem> slotItems = slot.getValue();

                    NbtCompound slotNbt = InventoryComponent.listToNbt(slotItems, graveItem -> {
                        NbtCompound itemNbt = new NbtCompound();
                        graveItem.stack.writeNbt(itemNbt);
                        itemNbt.putString("dropRule", graveItem.dropRule.name());

                        return itemNbt;
                    }, graveItem -> graveItem.stack.isEmpty(), "inventory", "size");

                    groupNbt.put(slot.getKey(), slotNbt);
                }
                nbt.put(group.getKey(), groupNbt);
            }

            return nbt;
        }
    }
}
