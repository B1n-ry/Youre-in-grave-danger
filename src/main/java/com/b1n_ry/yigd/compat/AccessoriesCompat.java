package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.compat.AccessoriesCompat.AccessoriesInventorySlot;
import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.YigdEvents;
import com.b1n_ry.yigd.util.DropRule;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class AccessoriesCompat implements InvModCompat<Map<String, AccessoriesInventorySlot>> {
    @Override
    public String getModName() {
        return "accessories";
    }

    @Override
    public void clear(ServerPlayer player) {
        AccessoriesCapability.getOptionally(player).ifPresent(inv -> inv.getContainers().forEach((s, accessoriesContainer) -> {
            accessoriesContainer.getAccessories().removeAllItems();
            accessoriesContainer.getCosmeticAccessories().removeAllItems();
        }));
    }

    @Override
    public CompatComponent<Map<String, AccessoriesInventorySlot>> readNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        Map<String, AccessoriesInventorySlot> inventory = new HashMap<>();

        for (String key : nbt.getAllKeys()) {
            CompoundTag slotNbt = nbt.getCompound(key);
            NonNullList<Tuple<ItemStack, DropRule>> normalSlot = InventoryComponent.listFromNbt(slotNbt.getCompound("normal"), itemNbt -> {
                ItemStack stack = ItemStack.parse(registryLookup, itemNbt).orElse(ItemStack.EMPTY);
                DropRule dropRule;
                if (itemNbt.contains("dropRule")) {
                    // We need to check in case the drop rule is a trinket drop rule (only has one difference and that is trinkets have DEFAULT)
                    String dropRuleString = itemNbt.getString("dropRule");
                    if (dropRuleString.equals("DEFAULT")) {
                        dropRule = YigdConfig.getConfig().compatConfig.defaultCuriosDropRule;
                    } else {
                        dropRule = DropRule.valueOf(dropRuleString);
                    }
                } else {
                    dropRule = YigdConfig.getConfig().compatConfig.defaultCuriosDropRule;
                }

                return new Tuple<>(stack, dropRule);
            }, InventoryComponent.EMPTY_ITEM_PAIR);
            NonNullList<Tuple<ItemStack, DropRule>> cosmeticSlot = InventoryComponent.listFromNbt(slotNbt.getCompound("cosmetic"), itemNbt -> {
                ItemStack stack = ItemStack.parse(registryLookup, itemNbt).orElse(ItemStack.EMPTY);
                DropRule dropRule;
                if (itemNbt.contains("dropRule")) {
                    // We need to check in case the drop rule is a trinket drop rule (only has one difference and that is trinkets have DEFAULT)
                    String dropRuleString = itemNbt.getString("dropRule");
                    if (dropRuleString.equals("DEFAULT")) {
                        dropRule = YigdConfig.getConfig().compatConfig.defaultCuriosDropRule;
                    } else {
                        dropRule = DropRule.valueOf(dropRuleString);
                    }
                } else {
                    dropRule = YigdConfig.getConfig().compatConfig.defaultCuriosDropRule;
                }

                return new Tuple<>(stack, dropRule);
            }, InventoryComponent.EMPTY_ITEM_PAIR);

            inventory.put(key, new AccessoriesInventorySlot(normalSlot, cosmeticSlot));
        }
        return new AccessoriesCompatComponent(inventory);
    }

    @Override
    public CompatComponent<Map<String, AccessoriesInventorySlot>> getNewComponent(ServerPlayer player) {
        return new AccessoriesCompatComponent(player);
    }

    public record AccessoriesInventorySlot(NonNullList<Tuple<ItemStack, DropRule>> normal, NonNullList<Tuple<ItemStack, DropRule>> cosmetic) {
        private void addAllNonEmptyToList(Collection<ItemStack> list) {
            for (Tuple<ItemStack, DropRule> pair : this.normal) {
                list.add(pair.getA().copy());
            }
            for (Tuple<ItemStack, DropRule> pair : this.cosmetic) {
                list.add(pair.getA().copy());
            }
        }
    }

    private static class AccessoriesCompatComponent extends CompatComponent<Map<String, AccessoriesInventorySlot>> {
        public AccessoriesCompatComponent(ServerPlayer player) {
            super(player);
        }
        public AccessoriesCompatComponent(Map<String, AccessoriesInventorySlot> inventory) {
            super(inventory);
        }

        @Override
        public Map<String, AccessoriesInventorySlot> getInventory(ServerPlayer player) {
            Map<String, AccessoriesInventorySlot> inventory = new HashMap<>();
            AccessoriesCapability capability = AccessoriesCapability.get(player);
            if (capability == null) return inventory;

            Map<String, AccessoriesContainer> containers = capability.getContainers();
            for (Map.Entry<String, AccessoriesContainer> entry : containers.entrySet()) {
                NonNullList<Tuple<ItemStack, DropRule>> normalSlot = NonNullList.create();
                NonNullList<Tuple<ItemStack, DropRule>> cosmeticSlot = NonNullList.create();

                ExpandedSimpleContainer normal = entry.getValue().getAccessories();
                ExpandedSimpleContainer cosmetic = entry.getValue().getCosmeticAccessories();
                for (int i = 0; i < normal.getContainerSize(); i++) {
                    normalSlot.add(new Tuple<>(normal.getItem(i).copy(), DropRule.PUT_IN_GRAVE));
                }
                for (int i = 0; i < cosmetic.getContainerSize(); i++) {
                    cosmeticSlot.add(new Tuple<>(cosmetic.getItem(i).copy(), DropRule.PUT_IN_GRAVE));
                }

                inventory.put(entry.getKey(), new AccessoriesInventorySlot(normalSlot, cosmeticSlot));
            }
            return inventory;
        }

        @Override
        public NonNullList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayer merger) {
            NonNullList<ItemStack> extraItems = NonNullList.create();
            @SuppressWarnings("unchecked")
            Map<String, AccessoriesInventorySlot> mergingInventory = (Map<String, AccessoriesInventorySlot>) mergingComponent.inventory;

            for (Map.Entry<String, AccessoriesInventorySlot> mergeEntry : mergingInventory.entrySet()) {
                String key = mergeEntry.getKey();
                if (!this.inventory.containsKey(key)) {
                    mergeEntry.getValue().addAllNonEmptyToList(extraItems);
                    continue;
                }
                AccessoriesInventorySlot mergingSlot = mergeEntry.getValue();
                AccessoriesInventorySlot thisSlot = this.inventory.get(key);

                for (int i = 0; i < mergingSlot.normal.size(); i++) {
                    Tuple<ItemStack, DropRule> mergingPair = mergingSlot.normal.get(i);
                    ItemStack mergingStack = mergingPair.getA().copy();
                    if (mergingStack.isEmpty()) continue;

                    if (thisSlot.normal.size() <= i) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    Tuple<ItemStack, DropRule> currentPair = thisSlot.normal.get(i);
                    ItemStack thisStack = currentPair.getA();
                    if (YigdConfig.getConfig().graveConfig.treatBindingCurse && !AccessoriesAPI.getOrDefaultAccessory(mergingStack).canUnequip(mergingStack, SlotReference.of(merger, key, i))) {
                        extraItems.add(currentPair.getA());  // Add the current item to extraItems (as it's being replaced)
                        thisSlot.normal.set(i, new Tuple<>(mergingStack, mergingPair.getB()));  // Can't be unequipped, so it's prioritized
                        continue;  // Already set the item, so we can skip the rest
                    }
                    if (!thisStack.isEmpty()) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    thisSlot.normal.set(i, mergingPair);
                }
                for (int i = 0; i < mergingSlot.cosmetic.size(); i++) {
                    Tuple<ItemStack, DropRule> mergingPair = mergingSlot.cosmetic.get(i);
                    ItemStack mergingStack = mergingPair.getA().copy();
                    if (mergingStack.isEmpty()) continue;

                    if (thisSlot.cosmetic.size() <= i) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    Tuple<ItemStack, DropRule> currentPair = thisSlot.cosmetic.get(i);
                    ItemStack thisStack = currentPair.getA();
                    if (YigdConfig.getConfig().graveConfig.treatBindingCurse && !AccessoriesAPI.getOrDefaultAccessory(mergingStack).canUnequip(mergingStack, SlotReference.of(merger, key, i))) {
                        extraItems.add(currentPair.getA());  // Add the current item to extraItems (as it's being replaced)
                        thisSlot.cosmetic.set(i, new Tuple<>(mergingStack, mergingPair.getB()));  // Can't be unequipped, so it's prioritized
                        continue;  // Already set the item, so we can skip the rest
                    }
                    if (!thisStack.isEmpty()) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    thisSlot.cosmetic.set(i, mergingPair);
                }
            }
            return extraItems;
        }

        @Override
        public NonNullList<ItemStack> pullBindingCurseItems(ServerPlayer playerRef) {
            NonNullList<ItemStack> noUnequipItems = NonNullList.create();

            if (!YigdConfig.getConfig().graveConfig.treatBindingCurse) return noUnequipItems;

            for (Map.Entry<String, AccessoriesInventorySlot> entry : this.inventory.entrySet()) {
                AccessoriesInventorySlot inventorySlot = entry.getValue();
                for (int i = 0; i < inventorySlot.normal.size(); i++) {
                    Tuple<ItemStack, DropRule> pair = inventorySlot.normal.get(i);
                    ItemStack stack = pair.getA();
                    Accessory accessory = AccessoriesAPI.getOrDefaultAccessory(stack);
                    boolean isBound = !accessory.canUnequip(stack, SlotReference.of(playerRef, entry.getKey(), i));
                    if (isBound) {
                        noUnequipItems.add(stack);
                        pair.setA(ItemStack.EMPTY);
                    }
                }
                for (int i = 0; i < inventorySlot.cosmetic.size(); i++) {
                    Tuple<ItemStack, DropRule> pair = inventorySlot.cosmetic.get(i);
                    ItemStack stack = pair.getA();
                    Accessory accessory = AccessoriesAPI.getOrDefaultAccessory(stack);
                    boolean isBound = !accessory.canUnequip(stack, SlotReference.of(playerRef, entry.getKey(), i));
                    if (isBound) {
                        noUnequipItems.add(stack);
                        pair.setA(ItemStack.EMPTY);
                    }
                }
            }

            return noUnequipItems;
        }

        @Override
        public NonNullList<ItemStack> storeToPlayer(ServerPlayer player) {
            NonNullList<ItemStack> extraItems = NonNullList.create();
            AccessoriesCapability capability = AccessoriesCapability.get(player);
            if (capability == null) return extraItems;

            Map<String, AccessoriesContainer> containers = capability.getContainers();
            for (Map.Entry<String, AccessoriesInventorySlot> entry : this.inventory.entrySet()) {
                String key = entry.getKey();
                if (!containers.containsKey(key)) {
                    entry.getValue().addAllNonEmptyToList(extraItems);
                    continue;
                }
                AccessoriesContainer container = containers.get(key);
                AccessoriesInventorySlot inventorySlot = entry.getValue();

                ExpandedSimpleContainer normalAccessories = container.getAccessories();
                ExpandedSimpleContainer cosmeticAccessories = container.getCosmeticAccessories();
                for (int i = 0; i < inventorySlot.normal.size(); i++) {
                    Tuple<ItemStack, DropRule> pair = inventorySlot.normal.get(i);
                    if (i >= normalAccessories.getContainerSize()) {
                        extraItems.add(pair.getA());
                        continue;
                    }

                    normalAccessories.setItem(i, pair.getA());
                }
                for (int i = 0; i < inventorySlot.cosmetic.size(); i++) {
                    Tuple<ItemStack, DropRule> pair = inventorySlot.cosmetic.get(i);
                    if (i >= cosmeticAccessories.getContainerSize()) {
                        extraItems.add(pair.getA());
                        continue;
                    }

                    cosmeticAccessories.setItem(i, pair.getA());
                }
            }

            return extraItems;
        }

        @Override
        public void handleDropRules(DeathContext context) {
            for (Map.Entry<String, AccessoriesInventorySlot> entry : this.inventory.entrySet()) {
                String key = entry.getKey();
                AccessoriesInventorySlot inventorySlot = entry.getValue();
                for (int i = 0; i < inventorySlot.normal.size(); i++) {
                    Tuple<ItemStack, DropRule> pair = inventorySlot.normal.get(i);
                    ItemStack stack = pair.getA();
                    DropRule dropRule = switch(AccessoriesAPI.getOrDefaultAccessory(stack)
                            .getDropRule(stack, SlotReference.of(context.player(), key, i), context.deathSource())) {
                        case DESTROY -> DropRule.DESTROY;
                        case KEEP -> DropRule.KEEP;
                        default -> {
                            DropRule defaultDropRule = YigdConfig.getConfig().compatConfig.defaultAccessoriesDropRule;
                            if (defaultDropRule == DropRule.PUT_IN_GRAVE)
                                yield NeoForge.EVENT_BUS.post(new YigdEvents.DropRuleEvent(stack, -1, context, true)).getDropRule();
                            else
                                yield defaultDropRule;
                        }
                    };

                    pair.setB(dropRule);
                }
                for (int i = 0; i < inventorySlot.cosmetic.size(); i++) {
                    Tuple<ItemStack, DropRule> pair = inventorySlot.cosmetic.get(i);
                    ItemStack stack = pair.getA();
                    DropRule dropRule = switch(AccessoriesAPI.getOrDefaultAccessory(stack)
                            .getDropRule(stack, SlotReference.of(context.player(), key, i), context.deathSource())) {
                        case DESTROY -> DropRule.DESTROY;
                        case KEEP -> DropRule.KEEP;
                        default -> {
                            DropRule defaultDropRule = YigdConfig.getConfig().compatConfig.defaultAccessoriesDropRule;
                            if (defaultDropRule == DropRule.PUT_IN_GRAVE)
                                yield NeoForge.EVENT_BUS.post(new YigdEvents.DropRuleEvent(stack, -1, context, true)).getDropRule();
                            else
                                yield defaultDropRule;
                        }
                    };

                    pair.setB(dropRule);
                }
            }
        }

        @Override
        public NonNullList<Tuple<ItemStack, DropRule>> getAsStackDropList() {
            NonNullList<Tuple<ItemStack, DropRule>> allItems = NonNullList.create();
            for (AccessoriesInventorySlot slot : this.inventory.values()) {
                allItems.addAll(slot.normal);
                allItems.addAll(slot.cosmetic);
            }

            return allItems;
        }

        @Override
        public CompatComponent<Map<String, AccessoriesInventorySlot>> filterInv(Predicate<DropRule> predicate) {
            Map<String, AccessoriesInventorySlot> filtered = new HashMap<>();
            for (Map.Entry<String, AccessoriesInventorySlot> entry : this.inventory.entrySet()) {
                AccessoriesInventorySlot inventorySlot = entry.getValue();
                NonNullList<Tuple<ItemStack, DropRule>> normalSlot = NonNullList.create();
                NonNullList<Tuple<ItemStack, DropRule>> cosmeticSlot = NonNullList.create();
                for (Tuple<ItemStack, DropRule> pair : inventorySlot.normal) {
                    if (predicate.test(pair.getB())) {
                        normalSlot.add(pair);
                    } else {
                        normalSlot.add(InventoryComponent.EMPTY_ITEM_PAIR);
                    }
                }
                for (Tuple<ItemStack, DropRule> pair : inventorySlot.cosmetic) {
                    if (predicate.test(pair.getB())) {
                        cosmeticSlot.add(pair);
                    } else {
                        cosmeticSlot.add(InventoryComponent.EMPTY_ITEM_PAIR);
                    }
                }

                filtered.put(entry.getKey(), new AccessoriesInventorySlot(normalSlot, cosmeticSlot));
            }
            return new AccessoriesCompatComponent(filtered);
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            for (AccessoriesInventorySlot inventorySlot : this.inventory.values()) {
                for (Tuple<ItemStack, DropRule> pair : inventorySlot.normal) {
                    ItemStack stack = pair.getA();
                    if (predicate.test(stack)) {
                        stack.shrink(itemCount);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void clear() {
            for (AccessoriesInventorySlot inventorySlot : this.inventory.values()) {
                Collections.fill(inventorySlot.normal, InventoryComponent.EMPTY_ITEM_PAIR);
                Collections.fill(inventorySlot.cosmetic, InventoryComponent.EMPTY_ITEM_PAIR);
            }
        }

        @Override
        public CompoundTag writeNbt(HolderLookup.Provider registryLookup) {
            CompoundTag nbt = new CompoundTag();
            for (Map.Entry<String, AccessoriesInventorySlot> entry : this.inventory.entrySet()) {
                AccessoriesInventorySlot inventorySlot = entry.getValue();
                CompoundTag slotNbt = new CompoundTag();
                CompoundTag normalNbt = InventoryComponent.listToNbt(inventorySlot.normal, pair -> {
                    CompoundTag itemNbt = (CompoundTag) pair.getA().save(registryLookup);
                    itemNbt.putString("dropRule", pair.getB().name());

                    return itemNbt;
                }, pair -> pair.getA().isEmpty());
                CompoundTag cosmeticNbt = InventoryComponent.listToNbt(inventorySlot.cosmetic, pair -> {
                    CompoundTag itemNbt = (CompoundTag) pair.getA().save(registryLookup);
                    itemNbt.putString("dropRule", pair.getB().name());

                    return itemNbt;
                }, pair -> pair.getA().isEmpty());

                slotNbt.put("normal", normalNbt);
                slotNbt.put("cosmetic", cosmeticNbt);

                nbt.put(entry.getKey(), slotNbt);
            }

            return nbt;
        }
    }
}