package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.compat.AccessoriesCompat.AccessoriesInventorySlot;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;

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
    public void clear(ServerPlayerEntity player) {
        AccessoriesCapability.getOptionally(player).ifPresent(inv -> inv.getContainers().forEach((s, accessoriesContainer) -> {
            accessoriesContainer.getAccessories().clear();
            accessoriesContainer.getCosmeticAccessories().clear();
        }));
    }

    @Override
    public CompatComponent<Map<String, AccessoriesInventorySlot>> readNbt(NbtCompound nbt) {
        Map<String, AccessoriesInventorySlot> inventory = new HashMap<>();

        for (String key : nbt.getKeys()) {
            NbtCompound slotNbt = nbt.getCompound(key);
            DefaultedList<Pair<ItemStack, DropRule>> normalSlot = InventoryComponent.listFromNbt(slotNbt.getCompound("normal"), itemNbt -> {
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
            }, InventoryComponent.EMPTY_ITEM_PAIR);
            DefaultedList<Pair<ItemStack, DropRule>> cosmeticSlot = InventoryComponent.listFromNbt(slotNbt.getCompound("cosmetic"), itemNbt -> {
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
            }, InventoryComponent.EMPTY_ITEM_PAIR);

            inventory.put(key, new AccessoriesInventorySlot(normalSlot, cosmeticSlot));
        }
        return new AccessoriesCompatComponent(inventory);
    }

    @Override
    public CompatComponent<Map<String, AccessoriesInventorySlot>> getNewComponent(ServerPlayerEntity player) {
        return new AccessoriesCompatComponent(player);
    }

    public record AccessoriesInventorySlot(DefaultedList<Pair<ItemStack, DropRule>> normal, DefaultedList<Pair<ItemStack, DropRule>> cosmetic) {
        private void addAllNonEmptyToList(Collection<ItemStack> list) {
            for (Pair<ItemStack, DropRule> pair : this.normal) {
                list.add(pair.getLeft().copy());
            }
            for (Pair<ItemStack, DropRule> pair : this.cosmetic) {
                list.add(pair.getLeft().copy());
            }
        }
    }

    private static class AccessoriesCompatComponent extends CompatComponent<Map<String, AccessoriesInventorySlot>> {
        public AccessoriesCompatComponent(ServerPlayerEntity player) {
            super(player);
        }
        public AccessoriesCompatComponent(Map<String, AccessoriesInventorySlot> inventory) {
            super(inventory);
        }

        @Override
        public Map<String, AccessoriesInventorySlot> getInventory(ServerPlayerEntity player) {
            Map<String, AccessoriesInventorySlot> inventory = new HashMap<>();
            AccessoriesCapability capability = AccessoriesCapability.get(player);
            if (capability == null) return inventory;

            Map<String, AccessoriesContainer> containers = capability.getContainers();
            for (Map.Entry<String, AccessoriesContainer> entry : containers.entrySet()) {
                DefaultedList<Pair<ItemStack, DropRule>> normalSlot = DefaultedList.of();
                DefaultedList<Pair<ItemStack, DropRule>> cosmeticSlot = DefaultedList.of();

                ExpandedSimpleContainer normal = entry.getValue().getAccessories();
                ExpandedSimpleContainer cosmetic = entry.getValue().getCosmeticAccessories();
                for (int i = 0; i < normal.size(); i++) {
                    normalSlot.add(new Pair<>(normal.getStack(i).copy(), DropRule.PUT_IN_GRAVE));
                }
                for (int i = 0; i < cosmetic.size(); i++) {
                    cosmeticSlot.add(new Pair<>(cosmetic.getStack(i).copy(), DropRule.PUT_IN_GRAVE));
                }

                inventory.put(entry.getKey(), new AccessoriesInventorySlot(normalSlot, cosmeticSlot));
            }
            return inventory;
        }

        @Override
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayerEntity merger) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();
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
                    Pair<ItemStack, DropRule> mergingPair = mergingSlot.normal.get(i);
                    ItemStack mergingStack = mergingPair.getLeft();
                    if (mergingStack.isEmpty()) continue;

                    Pair<ItemStack, DropRule> currentPair = thisSlot.normal.get(i);
                    ItemStack thisStack = currentPair.getLeft();
                    if (YigdConfig.getConfig().graveConfig.treatBindingCurse && !AccessoriesAPI.getOrDefaultAccessory(mergingStack).canUnequip(mergingStack, SlotReference.of(merger, key, i))) {
                        extraItems.add(currentPair.getLeft());  // Add the current item to extraItems (as it's being replaced)
                        thisSlot.normal.set(i, new Pair<>(mergingStack, mergingPair.getRight()));  // Can't be unequipped, so it's prioritized
                        continue;  // Already set the item, so we can skip the rest
                    }
                    if (!thisStack.isEmpty()) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    thisSlot.normal.set(i, mergingPair);
                }
                for (int i = 0; i < mergingSlot.cosmetic.size(); i++) {
                    Pair<ItemStack, DropRule> mergingPair = mergingSlot.cosmetic.get(i);
                    ItemStack mergingStack = mergingPair.getLeft();
                    if (mergingStack.isEmpty()) continue;

                    Pair<ItemStack, DropRule> currentPair = thisSlot.cosmetic.get(i);
                    ItemStack thisStack = currentPair.getLeft();
                    if (YigdConfig.getConfig().graveConfig.treatBindingCurse && !AccessoriesAPI.getOrDefaultAccessory(mergingStack).canUnequip(mergingStack, SlotReference.of(merger, key, i))) {
                        extraItems.add(currentPair.getLeft());  // Add the current item to extraItems (as it's being replaced)
                        thisSlot.cosmetic.set(i, new Pair<>(mergingStack, mergingPair.getRight()));  // Can't be unequipped, so it's prioritized
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
        public DefaultedList<ItemStack> pullBindingCurseItems(ServerPlayerEntity playerRef) {
            DefaultedList<ItemStack> noUnequipItems = DefaultedList.of();

            if (!YigdConfig.getConfig().graveConfig.treatBindingCurse) return noUnequipItems;

            for (Map.Entry<String, AccessoriesInventorySlot> entry : this.inventory.entrySet()) {
                AccessoriesInventorySlot inventorySlot = entry.getValue();
                for (int i = 0; i < inventorySlot.normal.size(); i++) {
                    Pair<ItemStack, DropRule> pair = inventorySlot.normal.get(i);
                    ItemStack stack = pair.getLeft();
                    Accessory accessory = AccessoriesAPI.getOrDefaultAccessory(stack);
                    boolean isBound = accessory.canUnequip(stack, SlotReference.of(playerRef, entry.getKey(), i));
                    if (isBound) {
                        noUnequipItems.add(stack);
                        pair.setLeft(ItemStack.EMPTY);
                    }
                }
                for (int i = 0; i < inventorySlot.cosmetic.size(); i++) {
                    Pair<ItemStack, DropRule> pair = inventorySlot.cosmetic.get(i);
                    ItemStack stack = pair.getLeft();
                    Accessory accessory = AccessoriesAPI.getOrDefaultAccessory(stack);
                    boolean isBound = !accessory.canUnequip(stack, SlotReference.of(playerRef, entry.getKey(), i));
                    if (isBound) {
                        noUnequipItems.add(stack);
                        pair.setLeft(ItemStack.EMPTY);
                    }
                }
            }

            return noUnequipItems;
        }

        @Override
        public DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();
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
                    Pair<ItemStack, DropRule> pair = inventorySlot.normal.get(i);
                    if (i > normalAccessories.size()) {
                        extraItems.add(pair.getLeft());
                    }

                    normalAccessories.setStack(i, pair.getLeft());
                }
                for (int i = 0; i < inventorySlot.cosmetic.size(); i++) {
                    Pair<ItemStack, DropRule> pair = inventorySlot.cosmetic.get(i);
                    if (i > cosmeticAccessories.size()) {
                        extraItems.add(pair.getLeft());
                    }

                    cosmeticAccessories.setStack(i, pair.getLeft());
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
                    Pair<ItemStack, DropRule> pair = inventorySlot.normal.get(i);
                    ItemStack stack = pair.getLeft();
                    DropRule dropRule = switch(AccessoriesAPI.getOrDefaultAccessory(stack)
                            .getDropRule(stack, SlotReference.of(context.player(), key, i), context.deathSource())) {
                        case DESTROY -> DropRule.DESTROY;
                        case KEEP -> DropRule.KEEP;
                        default -> {
                            DropRule defaultDropRule = YigdConfig.getConfig().compatConfig.defaultAccessoriesDropRule;
                            if (defaultDropRule == DropRule.PUT_IN_GRAVE)
                                yield DropRuleEvent.EVENT.invoker().getDropRule(stack, -1, context, true);
                            else
                                yield defaultDropRule;
                        }
                    };

                    pair.setRight(dropRule);
                }
                for (int i = 0; i < inventorySlot.cosmetic.size(); i++) {
                    Pair<ItemStack, DropRule> pair = inventorySlot.cosmetic.get(i);
                    ItemStack stack = pair.getLeft();
                    DropRule dropRule = switch(AccessoriesAPI.getOrDefaultAccessory(stack)
                            .getDropRule(stack, SlotReference.of(context.player(), key, i), context.deathSource())) {
                        case DESTROY -> DropRule.DESTROY;
                        case KEEP -> DropRule.KEEP;
                        default -> {
                            DropRule defaultDropRule = YigdConfig.getConfig().compatConfig.defaultAccessoriesDropRule;
                            if (defaultDropRule == DropRule.PUT_IN_GRAVE)
                                yield DropRuleEvent.EVENT.invoker().getDropRule(stack, -1, context, true);
                            else
                                yield defaultDropRule;
                        }
                    };

                    pair.setRight(dropRule);
                }
            }
        }

        @Override
        public DefaultedList<Pair<ItemStack, DropRule>> getAsStackDropList() {
            DefaultedList<Pair<ItemStack, DropRule>> allItems = DefaultedList.of();
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
                DefaultedList<Pair<ItemStack, DropRule>> normalSlot = DefaultedList.of();
                DefaultedList<Pair<ItemStack, DropRule>> cosmeticSlot = DefaultedList.of();
                for (Pair<ItemStack, DropRule> pair : inventorySlot.normal) {
                    if (predicate.test(pair.getRight())) {
                        normalSlot.add(pair);
                    } else {
                        normalSlot.add(InventoryComponent.EMPTY_ITEM_PAIR);
                    }
                }
                for (Pair<ItemStack, DropRule> pair : inventorySlot.cosmetic) {
                    if (predicate.test(pair.getRight())) {
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
                for (Pair<ItemStack, DropRule> pair : inventorySlot.normal) {
                    ItemStack stack = pair.getLeft();
                    if (predicate.test(stack)) {
                        stack.decrement(itemCount);
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
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();
            for (Map.Entry<String, AccessoriesInventorySlot> entry : this.inventory.entrySet()) {
                AccessoriesInventorySlot inventorySlot = entry.getValue();
                NbtCompound slotNbt = new NbtCompound();
                NbtCompound normalNbt = InventoryComponent.listToNbt(inventorySlot.normal, pair -> {
                    NbtCompound itemNbt = new NbtCompound();
                    pair.getLeft().writeNbt(itemNbt);
                    itemNbt.putString("dropRule", pair.getRight().name());

                    return itemNbt;
                }, pair -> pair.getLeft().isEmpty());
                NbtCompound cosmeticNbt = InventoryComponent.listToNbt(inventorySlot.cosmetic, pair -> {
                    NbtCompound itemNbt = new NbtCompound();
                    pair.getLeft().writeNbt(itemNbt);
                    itemNbt.putString("dropRule", pair.getRight().name());

                    return itemNbt;
                }, pair -> pair.getLeft().isEmpty());

                slotNbt.put("normal", normalNbt);
                slotNbt.put("cosmetic", cosmeticNbt);

                nbt.put(entry.getKey(), slotNbt);
            }

            return nbt;
        }
    }
}
