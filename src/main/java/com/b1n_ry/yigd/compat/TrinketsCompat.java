package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import dev.emi.trinkets.api.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public class TrinketsCompat implements InvModCompat<Map<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>>> {

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
    public CompatComponent<Map<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>>> readNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        Map<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>> inventory = new HashMap<>();

        for (String groupName : nbt.getAllKeys()) {
            CompoundTag groupNbt = nbt.getCompound(groupName);
            Map<String, NonNullList<Tuple<ItemStack, DropRule>>> groupMap = new HashMap<>();

            for (String slotName : groupNbt.getAllKeys()) {
                CompoundTag slotNbt = groupNbt.getCompound(slotName);
                NonNullList<Tuple<ItemStack, DropRule>> items = InventoryComponent.listFromNbt(slotNbt, itemNbt -> {
                    Optional<ItemStack> oStack = ItemStack.parse(registryLookup, itemNbt);
                    if (oStack.isEmpty()) return InventoryComponent.EMPTY_ITEM_PAIR;

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

                    return new Tuple<>(stack, dropRule);
                }, InventoryComponent.EMPTY_ITEM_PAIR, "inventory", "size");

                groupMap.put(slotName, items);
            }

            inventory.put(groupName, groupMap);
        }

        return new TrinketsCompatComponent(inventory);
    }

    @Override
    public CompatComponent<Map<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>>> getNewComponent(ServerPlayer player) {
        return new TrinketsCompatComponent(player);
    }


    private static class TrinketsCompatComponent extends CompatComponent<Map<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>>> {

        public TrinketsCompatComponent(ServerPlayer player) {
            super(player);
        }
        public TrinketsCompatComponent(Map<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>> inventory) {
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
        public Map<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>> getInventory(ServerPlayer player) {
            Map<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>> items = new HashMap<>();

            TrinketsApi.getTrinketComponent(player).ifPresent(component -> {
                for (Map.Entry<String, Map<String, TrinketInventory>> group : component.getInventory().entrySet()) {
                    String groupString = group.getKey();
                    Map<String, NonNullList<Tuple<ItemStack, DropRule>>> slotMap = new HashMap<>();
                    for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
                        String slotString = slot.getKey();
                        TrinketInventory trinketInventory = slot.getValue();

                        NonNullList<Tuple<ItemStack, DropRule>> itemsInInventory = NonNullList.create();
                        for (int i = 0; i < trinketInventory.getContainerSize(); i++) {
                            ItemStack stack = trinketInventory.getItem(i);
                            SlotReference ref = new SlotReference(trinketInventory, i);
                            TrinketEnums.DropRule dropRule = TrinketsApi.getTrinket(stack.getItem()).getDropRule(stack, ref, player);

                            itemsInInventory.add(new Tuple<>(trinketInventory.getItem(i), this.convertDropRule(dropRule)));
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

                for (Map.Entry<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>> group : this.inventory.entrySet()) {
                    Map<String, TrinketInventory> componentSlots = trinketInventory.get(group.getKey());
                    if (componentSlots == null) continue;

                    for (Map.Entry<String, NonNullList<Tuple<ItemStack, DropRule>>> slot : group.getValue().entrySet()) {
                        TrinketInventory trinketSlot = componentSlots.get(slot.getKey());
                        if (trinketSlot == null) continue;

                        NonNullList<Tuple<ItemStack, DropRule>> slotItems = slot.getValue();
                        for (int i = 0; i < slotItems.size(); i++) {
                            Tuple<ItemStack, DropRule> pair = slotItems.get(i);
                            ItemStack item = pair.getA();
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
        public NonNullList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayer merger) {
            NonNullList<ItemStack> extraItems = NonNullList.create();

            Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(merger);

            @SuppressWarnings("unchecked")
            Map<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>> mergingInventory = (Map<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>>) mergingComponent.inventory;
            for (Map.Entry<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>> groupEntry : mergingInventory.entrySet()) {  // From merging
                String groupName = groupEntry.getKey();
                Map<String, NonNullList<Tuple<ItemStack, DropRule>>> slotMap = this.inventory.get(groupName);  // From this
                if (slotMap == null) {
                    for (NonNullList<Tuple<ItemStack, DropRule>> items : groupEntry.getValue().values()) {
                        for (Tuple<ItemStack, DropRule> stack : items) {
                            extraItems.add(stack.getA().copy());  // Solves the issue where the itemstacks are the same instance
                        }
                    }
                    continue;
                }
                for (Map.Entry<String, NonNullList<Tuple<ItemStack, DropRule>>> slotEntry : groupEntry.getValue().entrySet()) {  // From merging
                    String slotName = slotEntry.getKey();
                    NonNullList<Tuple<ItemStack, DropRule>> stacks = slotMap.get(slotName);  // From this
                    NonNullList<Tuple<ItemStack, DropRule>> mergingItems = slotEntry.getValue();  // From merging
                    if (stacks == null) {
                        for (Tuple<ItemStack, DropRule> stack : mergingItems) {
                            extraItems.add(stack.getA().copy());  // Solves the issue where the itemstacks are the same instance
                        }
                        continue;
                    }

                    for (int i = 0; i < mergingItems.size(); i++) {
                        Tuple<ItemStack, DropRule> pair = mergingItems.get(i);
                        ItemStack mergingStack = pair.getA().copy();  // Solves the issue where the itemstacks are the same instance

                        if (stacks.size() <= i) {
                            extraItems.add(mergingStack);
                            continue;
                        }

                        Tuple<ItemStack, DropRule> currentPair = stacks.get(i);
                        if (YigdConfig.getConfig().graveConfig.treatBindingCurse && !this.canUnequip(trinketComponent.orElse(null), slotName, groupName, i, mergingStack, merger)) {
                            extraItems.add(currentPair.getA());  // Add the current item to extraItems (as it's being replaced)
                            stacks.set(i, new Tuple<>(mergingStack, pair.getB()));  // Can't be unequipped, so it's prioritized
                            continue;  // Already set the item, so we can skip the rest
                        }

                        if (!currentPair.getA().isEmpty()) {
                            extraItems.add(mergingStack);
                            continue;
                        }

                        stacks.set(i, new Tuple<>(mergingStack, pair.getB()));
                    }
                }
            }

            extraItems.removeIf(ItemStack::isEmpty);
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
                for (Map.Entry<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>> group : this.inventory.entrySet()) {
                    Map<String, TrinketInventory> componentSlots = trinketComponent.getInventory().get(group.getKey());
                    if (componentSlots == null) {  // The trinket group is missing, and all those items need to be added to extraItems
                        for (NonNullList<Tuple<ItemStack, DropRule>> itemList : group.getValue().values()) {
                            for (Tuple<ItemStack, DropRule> stack : itemList) {
                                extraItems.add(stack.getA().copy());
                            }
                        }
                        continue;
                    }

                    // Traverse through slots
                    for (Map.Entry<String, NonNullList<Tuple<ItemStack, DropRule>>> slot : group.getValue().entrySet()) {
                        TrinketInventory trinketInventory = componentSlots.get(slot.getKey());

                        NonNullList<Tuple<ItemStack, DropRule>> slotItems = slot.getValue();

                        if (trinketInventory == null) {  // The trinket slot is missing, and all those items need to be added to extraItems
                            for (Tuple<ItemStack, DropRule> stack : slotItems) {
                                extraItems.add(stack.getA().copy());
                            }
                            continue;
                        }

                        // Traverse through item stacks
                        for (int i = 0; i < slotItems.size(); i++) {
                            Tuple<ItemStack, DropRule> pair = slotItems.get(i);
                            ItemStack item = pair.getA().copy();
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
            for (Map<String, NonNullList<Tuple<ItemStack, DropRule>>> group : this.inventory.values()) {

                // Traverse through slots
                for (NonNullList<Tuple<ItemStack, DropRule>> slotItems : group.values()) {

                    // Traverse through item stacks
                    for (Tuple<ItemStack, DropRule> pair : slotItems) {
                        ItemStack item = pair.getA();

                        if (item.isEmpty()) continue;

                        DropRule dropRule = pair.getB();
                        if (dropRule == DropRule.PUT_IN_GRAVE)
                            dropRule = DropRuleEvent.EVENT.invoker().getDropRule(item, -1, context, true);

                        pair.setB(dropRule);
                    }
                }
            }
        }

        @Override
        public NonNullList<Tuple<ItemStack, DropRule>> getAsStackDropList() {
            NonNullList<Tuple<ItemStack, DropRule>> allItems = NonNullList.create();
            for (Map<String, NonNullList<Tuple<ItemStack, DropRule>>> slotMap : this.inventory.values()) {
                for (NonNullList<Tuple<ItemStack, DropRule>> itemStacks : slotMap.values()) {
                    allItems.addAll(itemStacks);
                }
            }

            return allItems;
        }

        @Override
        public CompatComponent<Map<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>>> filterInv(Predicate<DropRule> predicate) {
            Map<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>> filtered = new HashMap<>();

            for (Map.Entry<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>> group : this.inventory.entrySet()) {
                Map<String, NonNullList<Tuple<ItemStack, DropRule>>> filteredGroup = new HashMap<>();

                for (Map.Entry<String, NonNullList<Tuple<ItemStack, DropRule>>> slot : group.getValue().entrySet()) {
                    NonNullList<Tuple<ItemStack, DropRule>> filteredSlot = NonNullList.create();

                    NonNullList<Tuple<ItemStack, DropRule>> slotItems = slot.getValue();
                    for (Tuple<ItemStack, DropRule> pair : slotItems) {
                        if (predicate.test(pair.getB())) {
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
            for (Map<String, NonNullList<Tuple<ItemStack, DropRule>>> group : this.inventory.values()) {
                for (NonNullList<Tuple<ItemStack, DropRule>> slot : group.values()) {
                    for (Tuple<ItemStack, DropRule> pair : slot) {
                        ItemStack stack = pair.getA();
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
            for (Map<String, NonNullList<Tuple<ItemStack, DropRule>>> slotMap : this.inventory.values()) {
                for (NonNullList<Tuple<ItemStack, DropRule>> items : slotMap.values()) {
                    Collections.fill(items, InventoryComponent.EMPTY_ITEM_PAIR);
                }
            }
        }

        @Override
        public CompoundTag writeNbt(HolderLookup.Provider registryLookup) {
            CompoundTag nbt = new CompoundTag();

            // Traverse through groups
            for (Map.Entry<String, Map<String, NonNullList<Tuple<ItemStack, DropRule>>>> group : this.inventory.entrySet()) {
                CompoundTag groupNbt = new CompoundTag();

                // Traverse through slots
                for (Map.Entry<String, NonNullList<Tuple<ItemStack, DropRule>>> slot : group.getValue().entrySet()) {
                    NonNullList<Tuple<ItemStack, DropRule>> slotItems = slot.getValue();

                    CompoundTag slotNbt = InventoryComponent.listToNbt(slotItems, pair -> {
                        CompoundTag itemNbt = (CompoundTag) pair.getA().save(registryLookup);
                        itemNbt.putString("dropRule", pair.getB().name());

                        return itemNbt;
                    }, pair -> pair.getA().isEmpty(), "inventory", "size");

                    groupNbt.put(slot.getKey(), slotNbt);
                }
                nbt.put(group.getKey(), groupNbt);
            }

            return nbt;
        }
    }
}
