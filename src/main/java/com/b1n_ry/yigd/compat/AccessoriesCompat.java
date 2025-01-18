package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.compat.AccessoriesCompat.AccessoriesInventoryGroup;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class AccessoriesCompat implements InvModCompat<Map<String, AccessoriesInventoryGroup>> {
    @Override
    public String getModName() {
        return "accessories";
    }

    @Override
    public void clear(ServerPlayer player) {
        AccessoriesCapability.getOptionally(player).ifPresent(inv -> inv.reset(false));
//        AccessoriesCapability.getOptionally(player).ifPresent(inv -> inv.getContainers().forEach((s, container) -> {
//            ExpandedSimpleContainer normal = container.getAccessories();
//            ExpandedSimpleContainer cosmetic = container.getCosmeticAccessories();
//            for (int i = 0; i < normal.getContainerSize(); i++) {
//                normal.setItem(i, ItemStack.EMPTY);
//            }
//            for (int i = 0; i < cosmetic.getContainerSize(); i++) {
//                cosmetic.setItem(i, ItemStack.EMPTY);
//            }
//        }));
    }

    @Override
    public CompatComponent<Map<String, AccessoriesInventoryGroup>> readNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        Map<String, AccessoriesInventoryGroup> inventory = new HashMap<>();

        for (String key : nbt.getAllKeys()) {
            CompoundTag slotNbt = nbt.getCompound(key);
            NonNullList<AccessoriesInventorySlot> normalSlot = InventoryComponent.listFromNbt(slotNbt.getCompound("normal"), itemNbt -> {
                ItemStack stack = ItemStack.parse(registryLookup, itemNbt).orElse(ItemStack.EMPTY);
                DropRule dropRule;
                if (itemNbt.contains("dropRule")) {
                    // We need to check in case the drop rule is a trinket drop rule (only has one difference and that is trinkets have DEFAULT)
                    String dropRuleString = itemNbt.getString("dropRule");
                    if (dropRuleString.equals("DEFAULT")) {
                        dropRule = YigdConfig.getConfig().compatConfig.defaultAccessoriesDropRule;
                    } else {
                        dropRule = DropRule.valueOf(dropRuleString);
                    }
                } else {
                    dropRule = YigdConfig.getConfig().compatConfig.defaultAccessoriesDropRule;
                }
                boolean visible = !itemNbt.contains("visible") || itemNbt.getBoolean("visible");

                return new AccessoriesInventorySlot(stack, dropRule, visible);
            }, AccessoriesInventorySlot.EMPTY);
            NonNullList<AccessoriesInventorySlot> cosmeticSlot = InventoryComponent.listFromNbt(slotNbt.getCompound("cosmetic"), itemNbt -> {
                ItemStack stack = ItemStack.parse(registryLookup, itemNbt).orElse(ItemStack.EMPTY);
                DropRule dropRule;
                if (itemNbt.contains("dropRule")) {
                    // We need to check in case the drop rule is a trinket drop rule (only has one difference and that is trinkets have DEFAULT)
                    String dropRuleString = itemNbt.getString("dropRule");
                    if (dropRuleString.equals("DEFAULT")) {
                        dropRule = YigdConfig.getConfig().compatConfig.defaultAccessoriesDropRule;
                    } else {
                        dropRule = DropRule.valueOf(dropRuleString);
                    }
                } else {
                    dropRule = YigdConfig.getConfig().compatConfig.defaultAccessoriesDropRule;
                }
                boolean visible = !itemNbt.contains("visible") || itemNbt.getBoolean("visible");

                return new AccessoriesInventorySlot(stack, dropRule, visible);
            }, AccessoriesInventorySlot.EMPTY);

            inventory.put(key, new AccessoriesInventoryGroup(normalSlot, cosmeticSlot));
        }
        return new AccessoriesCompatComponent(inventory);
    }

    @Override
    public CompatComponent<Map<String, AccessoriesInventoryGroup>> getNewComponent(ServerPlayer player) {
        return new AccessoriesCompatComponent(player);
    }

    public record AccessoriesInventoryGroup(NonNullList<AccessoriesInventorySlot> normal, NonNullList<AccessoriesInventorySlot> cosmetic) {
        private void addAllNonEmptyToList(Collection<ItemStack> list) {
            for (AccessoriesInventorySlot slot : this.normal) {
                if (!slot.stack.isEmpty())
                    list.add(slot.stack.copy());
            }
            for (AccessoriesInventorySlot slot : this.cosmetic) {
                if (!slot.stack.isEmpty())
                    list.add(slot.stack.copy());
            }
        }
    }
    public static class AccessoriesInventorySlot {
        public ItemStack stack;
        public DropRule dropRule;
        public boolean visible;
        public AccessoriesInventorySlot(ItemStack stack, DropRule dropRule, boolean visible) {
            this.stack = stack;
            this.dropRule = dropRule;
            this.visible = visible;
        }
        public static AccessoriesInventorySlot EMPTY = new AccessoriesInventorySlot(ItemStack.EMPTY, DropRule.PUT_IN_GRAVE, true);
    }

    private static class AccessoriesCompatComponent extends CompatComponent<Map<String, AccessoriesInventoryGroup>> {
        public AccessoriesCompatComponent(ServerPlayer player) {
            super(player);
        }
        public AccessoriesCompatComponent(Map<String, AccessoriesInventoryGroup> inventory) {
            super(inventory);
        }

        @Override
        public Map<String, AccessoriesInventoryGroup> getInventory(ServerPlayer player) {
            Map<String, AccessoriesInventoryGroup> inventory = new HashMap<>();
            AccessoriesCapability capability = AccessoriesCapability.get(player);
            if (capability == null) return inventory;

            Map<String, AccessoriesContainer> containers = capability.getContainers();
            for (Map.Entry<String, AccessoriesContainer> entry : containers.entrySet()) {
                NonNullList<AccessoriesInventorySlot> normalSlot = NonNullList.create();
                NonNullList<AccessoriesInventorySlot> cosmeticSlot = NonNullList.create();

                AccessoriesContainer container = entry.getValue();
                ExpandedSimpleContainer normal = container.getAccessories();
                ExpandedSimpleContainer cosmetic = container.getCosmeticAccessories();
                for (int i = 0; i < normal.getContainerSize(); i++) {
                    normalSlot.add(new AccessoriesInventorySlot(normal.getItem(i).copy(), DropRule.PUT_IN_GRAVE, true));
                }
                for (int i = 0; i < cosmetic.getContainerSize(); i++) {
                    boolean shouldRender = container.shouldRender(i);
                    cosmeticSlot.add(new AccessoriesInventorySlot(cosmetic.getItem(i).copy(), DropRule.PUT_IN_GRAVE, shouldRender));
                }

                inventory.put(entry.getKey(), new AccessoriesInventoryGroup(normalSlot, cosmeticSlot));
            }
            return inventory;
        }

        @Override
        public NonNullList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayer merger) {
            NonNullList<ItemStack> extraItems = NonNullList.create();
            @SuppressWarnings("unchecked")
            Map<String, AccessoriesInventoryGroup> mergingInventory = (Map<String, AccessoriesInventoryGroup>) mergingComponent.inventory;

            for (Map.Entry<String, AccessoriesInventoryGroup> mergeEntry : mergingInventory.entrySet()) {
                String key = mergeEntry.getKey();
                if (!this.inventory.containsKey(key)) {
                    mergeEntry.getValue().addAllNonEmptyToList(extraItems);
                    continue;
                }
                AccessoriesInventoryGroup mergingSlot = mergeEntry.getValue();
                AccessoriesInventoryGroup thisSlot = this.inventory.get(key);

                for (int i = 0; i < mergingSlot.normal.size(); i++) {
                    AccessoriesInventorySlot mergingPair = mergingSlot.normal.get(i);
                    ItemStack mergingStack = mergingPair.stack.copy();
                    if (mergingStack.isEmpty()) continue;

                    if (thisSlot.normal.size() <= i) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    AccessoriesInventorySlot currentPair = thisSlot.normal.get(i);
                    ItemStack thisStack = currentPair.stack;
                    if (YigdConfig.getConfig().graveConfig.treatBindingCurse && !AccessoriesAPI.canUnequip(mergingStack, SlotReference.of(merger, key, i))) {
                        extraItems.add(currentPair.stack);  // Add the current item to extraItems (as it's being replaced)
                        thisSlot.normal.set(i, new AccessoriesInventorySlot(mergingStack, mergingPair.dropRule, mergingPair.visible));  // Can't be unequipped, so it's prioritized
                        continue;  // Already set the item, so we can skip the rest
                    }

                    if (!thisStack.isEmpty()) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    thisSlot.normal.set(i, mergingPair);
                }
                for (int i = 0; i < mergingSlot.cosmetic.size(); i++) {
                    AccessoriesInventorySlot mergingPair = mergingSlot.cosmetic.get(i);
                    ItemStack mergingStack = mergingPair.stack.copy();
                    if (mergingStack.isEmpty()) continue;

                    if (thisSlot.cosmetic.size() <= i) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    AccessoriesInventorySlot currentPair = thisSlot.cosmetic.get(i);
                    ItemStack thisStack = currentPair.stack;
                    if (YigdConfig.getConfig().graveConfig.treatBindingCurse && !AccessoriesAPI.canUnequip(mergingStack, SlotReference.of(merger, key, i))) {
                        extraItems.add(currentPair.stack);  // Add the current item to extraItems (as it's being replaced)
                        thisSlot.cosmetic.set(i, new AccessoriesInventorySlot(mergingStack, mergingPair.dropRule, mergingPair.visible));  // Can't be unequipped, so it's prioritized
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

            for (Map.Entry<String, AccessoriesInventoryGroup> entry : this.inventory.entrySet()) {
                AccessoriesInventoryGroup inventorySlot = entry.getValue();
                for (int i = 0; i < inventorySlot.normal.size(); i++) {
                    AccessoriesInventorySlot pair = inventorySlot.normal.get(i);
                    ItemStack stack = pair.stack;
                    boolean isBound = !AccessoriesAPI.canUnequip(stack, SlotReference.of(playerRef, entry.getKey(), i));
                    if (isBound) {
                        noUnequipItems.add(stack);
                        pair.stack = ItemStack.EMPTY;
                    }
                }
                for (int i = 0; i < inventorySlot.cosmetic.size(); i++) {
                    AccessoriesInventorySlot pair = inventorySlot.cosmetic.get(i);
                    ItemStack stack = pair.stack;
                    boolean isBound = !AccessoriesAPI.canUnequip(stack, SlotReference.of(playerRef, entry.getKey(), i));
                    if (isBound) {
                        noUnequipItems.add(stack);
                        pair.stack = ItemStack.EMPTY;
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
            for (Map.Entry<String, AccessoriesInventoryGroup> entry : this.inventory.entrySet()) {
                String key = entry.getKey();
                if (!containers.containsKey(key)) {
                    entry.getValue().addAllNonEmptyToList(extraItems);
                    continue;
                }
                AccessoriesContainer container = containers.get(key);
                AccessoriesInventoryGroup inventorySlot = entry.getValue();

                ExpandedSimpleContainer normalAccessories = container.getAccessories();
                ExpandedSimpleContainer cosmeticAccessories = container.getCosmeticAccessories();
                for (int i = 0; i < inventorySlot.normal.size(); i++) {
                    AccessoriesInventorySlot slot = inventorySlot.normal.get(i);
                    if (slot.stack.isEmpty()) continue;
                    if (i >= normalAccessories.getContainerSize()) {
                        extraItems.add(slot.stack.copy());
                        continue;
                    }

                    normalAccessories.setItem(i, slot.stack.copy());
                }
                for (int i = 0; i < inventorySlot.cosmetic.size(); i++) {
                    AccessoriesInventorySlot slot = inventorySlot.cosmetic.get(i);
                    if (slot.stack.isEmpty()) continue;
                    if (i >= cosmeticAccessories.getContainerSize()) {
                        extraItems.add(slot.stack.copy());
                        continue;
                    }

                    cosmeticAccessories.setItem(i, slot.stack.copy());
                    container.renderOptions().set(i, slot.visible);
                    container.markChanged(false);
                }
            }

            return extraItems;
        }

        @Override
        public void handleDropRules(DeathContext context) {
            for (Map.Entry<String, AccessoriesInventoryGroup> entry : this.inventory.entrySet()) {
                String key = entry.getKey();
                AccessoriesInventoryGroup inventorySlot = entry.getValue();
                for (int i = 0; i < inventorySlot.normal.size(); i++) {
                    AccessoriesInventorySlot pair = inventorySlot.normal.get(i);
                    ItemStack stack = pair.stack;
                    pair.dropRule = switch(AccessoriesAPI.getOrDefaultAccessory(stack)
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
                }
                for (int i = 0; i < inventorySlot.cosmetic.size(); i++) {
                    AccessoriesInventorySlot pair = inventorySlot.cosmetic.get(i);
                    ItemStack stack = pair.stack;
                    pair.dropRule = switch(AccessoriesAPI.getOrDefaultAccessory(stack)
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
                }
            }
        }

        @Override
        public NonNullList<Tuple<ItemStack, DropRule>> getAsStackDropList() {
            NonNullList<Tuple<ItemStack, DropRule>> allItems = NonNullList.create();
            for (AccessoriesInventoryGroup slot : this.inventory.values()) {
                for (AccessoriesInventorySlot entry : slot.normal) {
                    allItems.add(new Tuple<>(entry.stack, entry.dropRule));
                }
                for (AccessoriesInventorySlot entry : slot.cosmetic) {
                    allItems.add(new Tuple<>(entry.stack, entry.dropRule));
                }
            }

            return allItems;
        }

        @Override
        public CompatComponent<Map<String, AccessoriesInventoryGroup>> filterInv(Predicate<DropRule> predicate) {
            Map<String, AccessoriesInventoryGroup> filtered = new HashMap<>();
            for (Map.Entry<String, AccessoriesInventoryGroup> entry : this.inventory.entrySet()) {
                AccessoriesInventoryGroup inventorySlot = entry.getValue();
                NonNullList<AccessoriesInventorySlot> normalSlot = NonNullList.create();
                NonNullList<AccessoriesInventorySlot> cosmeticSlot = NonNullList.create();
                for (AccessoriesInventorySlot pair : inventorySlot.normal) {
                    if (predicate.test(pair.dropRule)) {
                        normalSlot.add(pair);
                    } else {
                        normalSlot.add(AccessoriesInventorySlot.EMPTY);
                    }
                }
                for (AccessoriesInventorySlot pair : inventorySlot.cosmetic) {
                    if (predicate.test(pair.dropRule)) {
                        cosmeticSlot.add(pair);
                    } else {
                        cosmeticSlot.add(AccessoriesInventorySlot.EMPTY);
                    }
                }

                filtered.put(entry.getKey(), new AccessoriesInventoryGroup(normalSlot, cosmeticSlot));
            }
            return new AccessoriesCompatComponent(filtered);
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            for (AccessoriesInventoryGroup inventorySlot : this.inventory.values()) {
                for (AccessoriesInventorySlot pair : inventorySlot.normal) {
                    ItemStack stack = pair.stack;
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
            for (AccessoriesInventoryGroup inventorySlot : this.inventory.values()) {
                Collections.fill(inventorySlot.normal, AccessoriesInventorySlot.EMPTY);
                Collections.fill(inventorySlot.cosmetic, AccessoriesInventorySlot.EMPTY);
            }
        }

        @Override
        public CompoundTag writeNbt(HolderLookup.Provider registryLookup) {
            CompoundTag nbt = new CompoundTag();
            for (Map.Entry<String, AccessoriesInventoryGroup> entry : this.inventory.entrySet()) {
                AccessoriesInventoryGroup inventorySlot = entry.getValue();
                CompoundTag slotNbt = new CompoundTag();
                CompoundTag normalNbt = InventoryComponent.listToNbt(inventorySlot.normal, pair -> {
                    CompoundTag itemNbt = (CompoundTag) pair.stack.save(registryLookup);
                    itemNbt.putString("dropRule", pair.dropRule.name());
                    itemNbt.putBoolean("visible", pair.visible);

                    return itemNbt;
                }, pair -> pair.stack.isEmpty());
                CompoundTag cosmeticNbt = InventoryComponent.listToNbt(inventorySlot.cosmetic, pair -> {
                    CompoundTag itemNbt = (CompoundTag) pair.stack.save(registryLookup);
                    itemNbt.putString("dropRule", pair.dropRule.name());
                    itemNbt.putBoolean("visible", pair.visible);

                    return itemNbt;
                }, pair -> pair.stack.isEmpty());

                slotNbt.put("normal", normalNbt);
                slotNbt.put("cosmetic", cosmeticNbt);

                nbt.put(entry.getKey(), slotNbt);
            }

            return nbt;
        }
    }
}