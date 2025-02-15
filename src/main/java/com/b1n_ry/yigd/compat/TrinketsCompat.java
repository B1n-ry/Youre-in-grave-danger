package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.data.GraveItem;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import dev.emi.trinkets.api.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class TrinketsCompat implements InvModCompat<Map<String, Map<String, NonNullList<GraveItem>>>> {

    @Override
    public String getModName() {
        return "trinkets";
    }

    @Override
    public void clear(ServerPlayer player) {
        TrinketsApi.getTrinketComponent(player).ifPresent(trinketComponent -> {
            for (Map.Entry<String, Map<String, TrinketInventory>> groupEntry : trinketComponent.getInventory().entrySet()) {
                for (Map.Entry<String, TrinketInventory> slotEntry : groupEntry.getValue().entrySet()) {
                    slotEntry.getValue().clearContent();
                }
            }
        });
    }

    @Override
    public CompatComponent<Map<String, Map<String, NonNullList<GraveItem>>>> readNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        Map<String, Map<String, NonNullList<GraveItem>>> inventory = new HashMap<>();

        for (String groupName : nbt.getAllKeys()) {
            CompoundTag groupNbt = nbt.getCompound(groupName);
            Map<String, NonNullList<GraveItem>> groupMap = new HashMap<>();

            for (String slotName : groupNbt.getAllKeys()) {
                CompoundTag slotNbt = groupNbt.getCompound(slotName);
                NonNullList<GraveItem> items = InventoryComponent.listFromNbt(slotNbt, itemNbt -> {
                    Optional<ItemStack> oStack = ItemStack.parse(registryLookup, itemNbt);
                    if (oStack.isEmpty()) return InventoryComponent.EMPTY_GRAVE_ITEM;

                    ItemStack stack = oStack.get();
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
    public CompatComponent<Map<String, Map<String, NonNullList<GraveItem>>>> getNewComponent(ServerPlayer player) {
        return new TrinketsCompatComponent(player);
    }


    private static class TrinketsCompatComponent extends CompatComponent<Map<String, Map<String, NonNullList<GraveItem>>>> {

        public TrinketsCompatComponent(ServerPlayer player) {
            super(player);
        }
        public TrinketsCompatComponent(Map<String, Map<String, NonNullList<GraveItem>>> inventory) {
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
        public Map<String, Map<String, NonNullList<GraveItem>>> getInventory(ServerPlayer player) {
            Map<String, Map<String, NonNullList<GraveItem>>> items = new HashMap<>();

            TrinketsApi.getTrinketComponent(player).ifPresent(component -> {
                for (Map.Entry<String, Map<String, TrinketInventory>> group : component.getInventory().entrySet()) {
                    String groupString = group.getKey();
                    Map<String, NonNullList<GraveItem>> slotMap = new HashMap<>();
                    for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
                        String slotString = slot.getKey();
                        TrinketInventory trinketInventory = slot.getValue();

                        NonNullList<GraveItem> itemsInInventory = NonNullList.create();
                        for (int i = 0; i < trinketInventory.getContainerSize(); i++) {
                            ItemStack stack = trinketInventory.getItem(i);
                            SlotReference ref = new SlotReference(trinketInventory, i);
                            TrinketEnums.DropRule dropRule = TrinketsApi.getTrinket(stack.getItem()).getDropRule(stack, ref, player);

                            itemsInInventory.add(new GraveItem(trinketInventory.getItem(i), this.convertDropRule(dropRule)));
                        }

                        slotMap.put(slotString, itemsInInventory);
                    }
                    items.put(groupString, slotMap);
                }
            });

            return items;
        }

        @Override
        public NonNullList<ItemStack> pullBindingCurseItems(ServerPlayer playerRef) {
            NonNullList<ItemStack> noUnequipItems = NonNullList.create();

            if (!YigdConfig.getConfig().graveConfig.treatBindingCurse) return noUnequipItems;

            Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(playerRef);
            if (trinketComponent.isPresent()) {
                Map<String, Map<String, TrinketInventory>> trinketInventory = trinketComponent.get().getInventory();

                for (Map.Entry<String, Map<String, NonNullList<GraveItem>>> group : this.inventory.entrySet()) {
                    Map<String, TrinketInventory> componentSlots = trinketInventory.get(group.getKey());
                    if (componentSlots == null) continue;

                    for (Map.Entry<String, NonNullList<GraveItem>> slot : group.getValue().entrySet()) {
                        TrinketInventory trinketSlot = componentSlots.get(slot.getKey());
                        if (trinketSlot == null) continue;

                        NonNullList<GraveItem> slotItems = slot.getValue();
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
        public NonNullList<GraveItem> merge(CompatComponent<?> mergingComponent, ServerPlayer merger) {
            NonNullList<GraveItem> extraItems = NonNullList.create();

            Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(merger);

            @SuppressWarnings("unchecked")
            Map<String, Map<String, NonNullList<GraveItem>>> mergingInventory = (Map<String, Map<String, NonNullList<GraveItem>>>) mergingComponent.inventory;
            for (Map.Entry<String, Map<String, NonNullList<GraveItem>>> groupEntry : mergingInventory.entrySet()) {  // From merging
                String groupName = groupEntry.getKey();
                Map<String, NonNullList<GraveItem>> slotMap = this.inventory.get(groupName);  // From this
                if (slotMap == null) {
                    for (NonNullList<GraveItem> items : groupEntry.getValue().values()) {
                        for (GraveItem graveItem : items) {
                            extraItems.add(graveItem.copy());  // Solves the issue where the itemstacks are the same instance
                        }
                    }
                    continue;
                }
                for (Map.Entry<String, NonNullList<GraveItem>> slotEntry : groupEntry.getValue().entrySet()) {  // From merging
                    String slotName = slotEntry.getKey();
                    NonNullList<GraveItem> stacks = slotMap.get(slotName);  // From this
                    NonNullList<GraveItem> mergingItems = slotEntry.getValue();  // From merging
                    if (stacks == null) {
                        for (GraveItem graveItem : mergingItems) {
                            extraItems.add(graveItem.copy());  // Solves the issue where the itemstacks are the same instance
                        }
                        continue;
                    }

                    for (int i = 0; i < mergingItems.size(); i++) {
                        GraveItem graveItem = mergingItems.get(i);
                        GraveItem mergingGraveItem = graveItem.copy();  // Solves the issue where the itemstacks are the same instance

                        if (stacks.size() <= i) {
                            extraItems.add(mergingGraveItem);
                            continue;
                        }

                        GraveItem currentGraveItem = stacks.get(i);
                        if (YigdConfig.getConfig().graveConfig.treatBindingCurse && !this.canUnequip(trinketComponent.orElse(null), slotName, groupName, i, mergingGraveItem.stack, merger)) {
                            extraItems.add(currentGraveItem);  // Add the current item to extraItems (as it's being replaced)
                            stacks.set(i, new GraveItem(mergingGraveItem.stack, graveItem.dropRule));  // Can't be unequipped, so it's prioritized
                            continue;  // Already set the item, so we can skip the rest
                        }

                        if (!currentGraveItem.stack.isEmpty()) {
                            extraItems.add(mergingGraveItem);
                            continue;
                        }

                        stacks.set(i, new GraveItem(mergingGraveItem.stack, graveItem.dropRule));
                    }
                }
            }

            extraItems.removeIf(graveItem -> graveItem.stack.isEmpty());
            return extraItems;
        }
        private boolean canUnequip(@Nullable TrinketComponent component, String slot, String group, int index, ItemStack item, ServerPlayer player) {
            if (component == null) return true;
            Map<String, TrinketInventory> trinketGroup = component.getInventory().get(group);
            if (trinketGroup == null) return true;
            TrinketInventory trinketInventory = trinketGroup.get(slot);
            if (trinketInventory == null || trinketInventory.getContainerSize() <= index) return true;

            if (item.isEmpty()) return true;
            SlotReference ref = new SlotReference(trinketInventory, index);
            return TrinketsApi.getTrinket(item.getItem()).canUnequip(item, ref, player);
        }

        @Override
        public NonNullList<ItemStack> storeToPlayer(ServerPlayer player) {
            NonNullList<ItemStack> extraItems = NonNullList.create();

            TrinketsApi.getTrinketComponent(player).ifPresent(trinketComponent -> {
                // Traverse through groups
                for (Map.Entry<String, Map<String, NonNullList<GraveItem>>> group : this.inventory.entrySet()) {
                    Map<String, TrinketInventory> componentSlots = trinketComponent.getInventory().get(group.getKey());
                    if (componentSlots == null) {  // The trinket group is missing, and all those items need to be added to extraItems
                        for (NonNullList<GraveItem> itemList : group.getValue().values()) {
                            for (GraveItem graveItem : itemList) {
                                extraItems.add(graveItem.stack.copy());
                            }
                        }
                        continue;
                    }

                    // Traverse through slots
                    for (Map.Entry<String, NonNullList<GraveItem>> slot : group.getValue().entrySet()) {
                        TrinketInventory trinketInventory = componentSlots.get(slot.getKey());

                        NonNullList<GraveItem> slotItems = slot.getValue();

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
                            if (i >= trinketInventory.getContainerSize()) {
                                extraItems.add(item);
                                continue;
                            }
                            trinketInventory.setItem(i, item);
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
            for (Map<String, NonNullList<GraveItem>> group : this.inventory.values()) {

                // Traverse through slots
                for (NonNullList<GraveItem> slotItems : group.values()) {

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
        public NonNullList<GraveItem> getAsGraveItemList() {
            NonNullList<GraveItem> allItems = NonNullList.create();
            for (Map<String, NonNullList<GraveItem>> slotMap : this.inventory.values()) {
                for (NonNullList<GraveItem> itemStacks : slotMap.values()) {
                    allItems.addAll(itemStacks);
                }
            }

            return allItems;
        }

        @Override
        public CompatComponent<Map<String, Map<String, NonNullList<GraveItem>>>> filterInv(Predicate<DropRule> predicate) {
            Map<String, Map<String, NonNullList<GraveItem>>> filtered = new HashMap<>();

            for (Map.Entry<String, Map<String, NonNullList<GraveItem>>> group : this.inventory.entrySet()) {
                Map<String, NonNullList<GraveItem>> filteredGroup = new HashMap<>();

                for (Map.Entry<String, NonNullList<GraveItem>> slot : group.getValue().entrySet()) {
                    NonNullList<GraveItem> filteredSlot = NonNullList.create();

                    NonNullList<GraveItem> slotItems = slot.getValue();
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
            for (Map<String, NonNullList<GraveItem>> group : this.inventory.values()) {
                for (NonNullList<GraveItem> slot : group.values()) {
                    for (GraveItem graveItem : slot) {
                        ItemStack stack = graveItem.stack;
                        if (predicate.test(stack)) {
                            stack.shrink(itemCount);

                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public void clear() {
            for (Map<String, NonNullList<GraveItem>> slotMap : this.inventory.values()) {
                for (NonNullList<GraveItem> items : slotMap.values()) {
                    Collections.fill(items, InventoryComponent.EMPTY_GRAVE_ITEM);
                }
            }
        }

        @Override
        public CompoundTag writeNbt(HolderLookup.Provider registryLookup) {
            CompoundTag nbt = new CompoundTag();

            // Traverse through groups
            for (Map.Entry<String, Map<String, NonNullList<GraveItem>>> group : this.inventory.entrySet()) {
                CompoundTag groupNbt = new CompoundTag();

                // Traverse through slots
                for (Map.Entry<String, NonNullList<GraveItem>> slot : group.getValue().entrySet()) {
                    NonNullList<GraveItem> slotItems = slot.getValue();

                    CompoundTag slotNbt = InventoryComponent.listToNbt(slotItems, graveItem -> {
                        CompoundTag itemNbt = (CompoundTag) graveItem.stack.save(registryLookup);
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
