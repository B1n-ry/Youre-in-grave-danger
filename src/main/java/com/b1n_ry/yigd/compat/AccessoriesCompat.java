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

public class AccessoriesCompat implements InvModCompat<Map<String, AccessoriesInventoryGroup>> {
    @Override
    public String getModName() {
        return "accessories";
    }

    @Override
    public void clear(ServerPlayerEntity player) {
        AccessoriesCapability.getOptionally(player).ifPresent(inv -> inv.reset(false));
    }

    @Override
    public CompatComponent<Map<String, AccessoriesInventoryGroup>> readNbt(NbtCompound nbt) {
        Map<String, AccessoriesInventoryGroup> inventory = new HashMap<>();

        for (String key : nbt.getKeys()) {
            NbtCompound slotNbt = nbt.getCompound(key);
            DefaultedList<AccessoriesInventorySlot> normalSlot = InventoryComponent.listFromNbt(slotNbt.getCompound("normal"), itemNbt -> {
                ItemStack stack = ItemStack.fromNbt(itemNbt);
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
            DefaultedList<AccessoriesInventorySlot> cosmeticSlot = InventoryComponent.listFromNbt(slotNbt.getCompound("cosmetic"), itemNbt -> {
                ItemStack stack = ItemStack.fromNbt(itemNbt);
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
    public CompatComponent<Map<String, AccessoriesInventoryGroup>> getNewComponent(ServerPlayerEntity player) {
        return new AccessoriesCompatComponent(player);
    }

    public record AccessoriesInventoryGroup(DefaultedList<AccessoriesInventorySlot> normal, DefaultedList<AccessoriesInventorySlot> cosmetic) {
        private void addAllNonEmptyToList(Collection<ItemStack> list) {
            for (AccessoriesInventorySlot slot : this.normal) {
                list.add(slot.stack.copy());
            }
            for (AccessoriesInventorySlot slot : this.cosmetic) {
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
        public AccessoriesCompatComponent(ServerPlayerEntity player) {
            super(player);
        }
        public AccessoriesCompatComponent(Map<String, AccessoriesInventoryGroup> inventory) {
            super(inventory);
        }

        @Override
        public Map<String, AccessoriesInventoryGroup> getInventory(ServerPlayerEntity player) {
            Map<String, AccessoriesInventoryGroup> inventory = new HashMap<>();
            AccessoriesCapability capability = AccessoriesCapability.get(player);
            if (capability == null) return inventory;

            Map<String, AccessoriesContainer> containers = capability.getContainers();
            for (Map.Entry<String, AccessoriesContainer> entry : containers.entrySet()) {
                DefaultedList<AccessoriesInventorySlot> normalSlot = DefaultedList.of();
                DefaultedList<AccessoriesInventorySlot> cosmeticSlot = DefaultedList.of();

                AccessoriesContainer container = entry.getValue();
                ExpandedSimpleContainer normal = container.getAccessories();
                ExpandedSimpleContainer cosmetic = container.getCosmeticAccessories();
                for (int i = 0; i < normal.size(); i++) {
                    normalSlot.add(new AccessoriesInventorySlot(normal.getStack(i).copy(), DropRule.PUT_IN_GRAVE, true));
                }
                for (int i = 0; i < cosmetic.size(); i++) {
                    boolean shouldRender = container.shouldRender(i);

                    cosmeticSlot.add(new AccessoriesInventorySlot(cosmetic.getStack(i).copy(), DropRule.PUT_IN_GRAVE, shouldRender));
                }

                inventory.put(entry.getKey(), new AccessoriesInventoryGroup(normalSlot, cosmeticSlot));
            }
            return inventory;
        }

        @Override
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayerEntity merger) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();
            @SuppressWarnings("unchecked")
            Map<String, AccessoriesInventoryGroup> mergingInventory = (Map<String, AccessoriesInventoryGroup>) mergingComponent.inventory;

            for (Map.Entry<String, AccessoriesInventoryGroup> mergeEntry : mergingInventory.entrySet()) {
                String key = mergeEntry.getKey();
                if (!this.inventory.containsKey(key)) {
                    mergeEntry.getValue().addAllNonEmptyToList(extraItems);
                    continue;
                }
                AccessoriesInventoryGroup mergingGroup = mergeEntry.getValue();
                AccessoriesInventoryGroup thisGroup = this.inventory.get(key);

                for (int i = 0; i < mergingGroup.normal.size(); i++) {
                    AccessoriesInventorySlot mergingSlot = mergingGroup.normal.get(i);
                    ItemStack mergingStack = mergingSlot.stack.copy();
                    if (mergingStack.isEmpty()) continue;

                    if (thisGroup.normal.size() <= i) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    AccessoriesInventorySlot currentSlot = thisGroup.normal.get(i);
                    ItemStack thisStack = currentSlot.stack;
                    if (YigdConfig.getConfig().graveConfig.treatBindingCurse && !AccessoriesAPI.canUnequip(mergingStack, SlotReference.of(merger, key, i))) {
                        extraItems.add(currentSlot.stack);  // Add the current item to extraItems (as it's being replaced)
                        thisGroup.normal.set(i, new AccessoriesInventorySlot(mergingStack, mergingSlot.dropRule, mergingSlot.visible));  // Can't be unequipped, so it's prioritized
                        continue;  // Already set the item, so we can skip the rest
                    }
                    if (!thisStack.isEmpty()) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    thisGroup.normal.set(i, mergingSlot);
                }
                for (int i = 0; i < mergingGroup.cosmetic.size(); i++) {
                    AccessoriesInventorySlot mergingSlot = mergingGroup.cosmetic.get(i);
                    ItemStack mergingStack = mergingSlot.stack.copy();
                    if (mergingStack.isEmpty()) continue;

                    if (thisGroup.cosmetic.size() <= i) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    AccessoriesInventorySlot currentSlot = thisGroup.cosmetic.get(i);
                    ItemStack thisStack = currentSlot.stack;
                    if (YigdConfig.getConfig().graveConfig.treatBindingCurse && !AccessoriesAPI.canUnequip(mergingStack, SlotReference.of(merger, key, i))) {
                        extraItems.add(currentSlot.stack);  // Add the current item to extraItems (as it's being replaced)
                        thisGroup.cosmetic.set(i, new AccessoriesInventorySlot(mergingStack, mergingSlot.dropRule, mergingSlot.visible));  // Can't be unequipped, so it's prioritized
                        continue;  // Already set the item, so we can skip the rest
                    }
                    if (!thisStack.isEmpty()) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    thisGroup.cosmetic.set(i, mergingSlot);
                }
            }
            return extraItems;
        }

        @Override
        public DefaultedList<ItemStack> pullBindingCurseItems(ServerPlayerEntity playerRef) {
            DefaultedList<ItemStack> noUnequipItems = DefaultedList.of();

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
        public DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();
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
                    if (i >= normalAccessories.size()) {
                        extraItems.add(slot.stack.copy());
                        continue;
                    }

                    normalAccessories.setStack(i, slot.stack.copy());
                }
                for (int i = 0; i < inventorySlot.cosmetic.size(); i++) {
                    AccessoriesInventorySlot slot = inventorySlot.cosmetic.get(i);
                    if (i >= cosmeticAccessories.size()) {
                        extraItems.add(slot.stack.copy());
                        continue;
                    }

                    cosmeticAccessories.setStack(i, slot.stack.copy());
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
                AccessoriesInventoryGroup inventoryGroup = entry.getValue();
                for (int i = 0; i < inventoryGroup.normal.size(); i++) {
                    AccessoriesInventorySlot slot = inventoryGroup.normal.get(i);
                    ItemStack stack = slot.stack;
                    slot.dropRule = switch(AccessoriesAPI.getOrDefaultAccessory(stack)
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
                for (int i = 0; i < inventoryGroup.cosmetic.size(); i++) {
                    AccessoriesInventorySlot slot = inventoryGroup.cosmetic.get(i);
                    ItemStack stack = slot.stack;
                    slot.dropRule = switch(AccessoriesAPI.getOrDefaultAccessory(stack)
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
        public DefaultedList<Pair<ItemStack, DropRule>> getAsStackDropList() {
            DefaultedList<Pair<ItemStack, DropRule>> allItems = DefaultedList.of();
            for (AccessoriesInventoryGroup group : this.inventory.values()) {
                for (AccessoriesInventorySlot slot : group.normal) {
                    allItems.add(new Pair<>(slot.stack, slot.dropRule));
                }
                for (AccessoriesInventorySlot slot : group.cosmetic) {
                    allItems.add(new Pair<>(slot.stack, slot.dropRule));
                }
            }

            return allItems;
        }

        @Override
        public CompatComponent<Map<String, AccessoriesInventoryGroup>> filterInv(Predicate<DropRule> predicate) {
            Map<String, AccessoriesInventoryGroup> filtered = new HashMap<>();
            for (Map.Entry<String, AccessoriesInventoryGroup> entry : this.inventory.entrySet()) {
                AccessoriesInventoryGroup inventorySlot = entry.getValue();
                DefaultedList<AccessoriesInventorySlot> normalSlot = DefaultedList.of();
                DefaultedList<AccessoriesInventorySlot> cosmeticSlot = DefaultedList.of();
                for (AccessoriesInventorySlot slot : inventorySlot.normal) {
                    if (predicate.test(slot.dropRule)) {
                        normalSlot.add(slot);
                    } else {
                        normalSlot.add(AccessoriesInventorySlot.EMPTY);
                    }
                }
                for (AccessoriesInventorySlot slot : inventorySlot.cosmetic) {
                    if (predicate.test(slot.dropRule)) {
                        cosmeticSlot.add(slot);
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
                for (AccessoriesInventorySlot slot : inventorySlot.normal) {
                    ItemStack stack = slot.stack;
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
            for (AccessoriesInventoryGroup inventorySlot : this.inventory.values()) {
                Collections.fill(inventorySlot.normal, AccessoriesInventorySlot.EMPTY);
                Collections.fill(inventorySlot.cosmetic, AccessoriesInventorySlot.EMPTY);
            }
        }

        @Override
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();
            for (Map.Entry<String, AccessoriesInventoryGroup> entry : this.inventory.entrySet()) {
                AccessoriesInventoryGroup inventorySlot = entry.getValue();
                NbtCompound slotNbt = new NbtCompound();
                NbtCompound normalNbt = InventoryComponent.listToNbt(inventorySlot.normal, slot -> {
                    NbtCompound itemNbt = new NbtCompound();
                    slot.stack.writeNbt(itemNbt);
                    itemNbt.putString("dropRule", slot.dropRule.name());
                    itemNbt.putBoolean("visible", slot.visible);

                    return itemNbt;
                }, slot -> slot.stack.isEmpty());
                NbtCompound cosmeticNbt = InventoryComponent.listToNbt(inventorySlot.cosmetic, slot -> {
                    NbtCompound itemNbt = new NbtCompound();
                    slot.stack.writeNbt(itemNbt);
                    itemNbt.putString("dropRule", slot.dropRule.name());
                    itemNbt.putBoolean("visible", slot.visible);

                    return itemNbt;
                }, slot -> slot.stack.isEmpty());

                slotNbt.put("normal", normalNbt);
                slotNbt.put("cosmetic", cosmeticNbt);

                nbt.put(entry.getKey(), slotNbt);
            }

            return nbt;
        }
    }
}
