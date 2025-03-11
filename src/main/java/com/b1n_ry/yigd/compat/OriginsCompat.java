package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.CompatConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.data.GraveItem;
import com.b1n_ry.yigd.events.AdjustDropRuleEvent;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.mixin.accessor.KeepInventoryPowerAccessor;
import com.b1n_ry.yigd.util.DropRule;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.Active;
import io.github.apace100.apoli.power.InventoryPower;
import io.github.apace100.apoli.power.KeepInventoryPower;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

import java.util.*;
import java.util.function.Predicate;

public class OriginsCompat implements InvModCompat<Map<String, DefaultedList<GraveItem>>> {
    public OriginsCompat() {
        AdjustDropRuleEvent.EVENT.register((inventoryComponent, context) -> {
            List<KeepInventoryPower> powers = PowerHolderComponent.getPowers(context.player(), KeepInventoryPower.class);
            Predicate<ItemStack> keepCondition = Predicate.not(s -> true);  // Base to add onto
            for (KeepInventoryPower power : powers) {
                keepCondition = keepCondition.or(((KeepInventoryPowerAccessor) power).getKeepItemCondition());
            }

            Predicate<ItemStack> finalKeepCondition = keepCondition;  // The compiler requires this copy to compile
            inventoryComponent.handleGraveItems(mod -> true, (stack, slot, graveItem) -> {
                if (finalKeepCondition.test(stack)) {
                    graveItem.dropRule = DropRule.KEEP;
                }
            });
        });
    }

    @Override
    public String getModName() {
        return "apoli";
    }

    @Override
    public void clear(ServerPlayerEntity player) {
        PowerHolderComponent.getPowers(player, InventoryPower.class).forEach(InventoryPower::clear);
    }

    @Override
    public CompatComponent<Map<String, DefaultedList<GraveItem>>> readNbt(NbtCompound nbt) {
        Map<String, DefaultedList<GraveItem>> inventory = new HashMap<>();

        for (String key : nbt.getKeys()) {
            NbtCompound inventoryNbt = nbt.getCompound(key);

            DefaultedList<GraveItem> items = InventoryComponent.listFromNbt(inventoryNbt, itemNbt -> {
                ItemStack stack = ItemStack.fromNbt(itemNbt);

                DropRule dropRule;
                if (itemNbt.contains("dropRule")) {
                    dropRule = DropRule.valueOf(itemNbt.getString("dropRule"));
                } else {
                    dropRule = YigdConfig.getConfig().compatConfig.defaultOriginsDropRule;
                }

                return new GraveItem(stack, dropRule);
            }, InventoryComponent.EMPTY_GRAVE_ITEM);

            inventory.put(key, items);
        }

        return new OriginsCompatComponent(inventory);
    }

    @Override
    public CompatComponent<Map<String, DefaultedList<GraveItem>>> getNewComponent(ServerPlayerEntity player) {
        return new OriginsCompatComponent(player);
    }

    private static class OriginsCompatComponent extends CompatComponent<Map<String, DefaultedList<GraveItem>>> {

        public OriginsCompatComponent(ServerPlayerEntity player) {
            super(player);
        }

        public OriginsCompatComponent(Map<String, DefaultedList<GraveItem>> inventory) {
            super(inventory);
        }

        @Override
        public Map<String, DefaultedList<GraveItem>> getInventory(ServerPlayerEntity player) {
            CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

            Map<String, DefaultedList<GraveItem>> inventory = new HashMap<>();

            List<InventoryPower> powers = PowerHolderComponent.getPowers(player, InventoryPower.class);
            for (InventoryPower inventoryPower : powers) {
                Active.Key key = inventoryPower.getKey();
                DefaultedList<GraveItem> stacks = DefaultedList.of();

                for (int i = 0; i < inventoryPower.size(); i++) {
                    ItemStack stack = inventoryPower.getStack(i);
                    DropRule dropRule = inventoryPower.shouldDropOnDeath(stack) ? compatConfig.defaultOriginsDropRule : DropRule.KEEP;
                    stacks.add(new GraveItem(stack, dropRule));
                }

                inventory.put(key.key, stacks);
            }

            return inventory;
        }

        @Override
        public DefaultedList<GraveItem> merge(CompatComponent<?> mergingComponent, ServerPlayerEntity merger) {
            DefaultedList<GraveItem> extraItems = DefaultedList.of();

            @SuppressWarnings("unchecked")
            Map<String, DefaultedList<GraveItem>> mergingInventory = (Map<String, DefaultedList<GraveItem>>) mergingComponent.inventory;
            for (Map.Entry<String, DefaultedList<GraveItem>> entry : mergingInventory.entrySet()) {
                DefaultedList<GraveItem> currentItems = this.inventory.getOrDefault(entry.getKey(), DefaultedList.of());
                DefaultedList<GraveItem> mergingItems = entry.getValue();

                for (int i = 0; i < mergingItems.size(); i++) {
                    GraveItem mergingItem = mergingItems.get(i).copy();  // Solves the case where the itemstacks are the same instance

                    if (i >= currentItems.size()) {
                        extraItems.add(mergingItem);
                        continue;
                    }

                    GraveItem currentGraveItem = currentItems.get(i);
                    if (!currentGraveItem.stack.isEmpty()) {
                        extraItems.add(mergingItem);
                    } else {
                        currentItems.set(i, mergingItem);
                    }
                }
            }

            return extraItems;
        }

        @Override
        public DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            List<InventoryPower> powers = PowerHolderComponent.getPowers(player, InventoryPower.class);
            List<String> unhandledPowers = new ArrayList<>(this.inventory.keySet());

            for (InventoryPower power : powers) {
                String key = power.getKey().key;
                unhandledPowers.remove(key);

                DefaultedList<GraveItem> inventoryItems = this.inventory.get(key);
                if (inventoryItems == null)
                    continue;

                for (int i = 0; i < inventoryItems.size(); i++) {
                    ItemStack currentStack = inventoryItems.get(i).stack.copy();

                    if (i >= power.size()) {
                        extraItems.add(currentStack);
                    } else {
                        power.setStack(i, currentStack);
                    }
                }
            }

            for (String key : unhandledPowers) {
                for (GraveItem graveItem : this.inventory.get(key)) {
                    extraItems.add(graveItem.stack.copy());
                }
            }


            return extraItems;
        }

        @Override
        public void handleDropRules(DeathContext context) {
            for (Map.Entry<String, DefaultedList<GraveItem>> entry : this.inventory.entrySet()) {
                DefaultedList<GraveItem> items = entry.getValue();

                for (GraveItem graveItem : items) {
                    ItemStack item = graveItem.stack;
                    if (item.isEmpty()) continue;

                    DropRule dropRule = graveItem.dropRule;

                    if (dropRule == DropRule.PUT_IN_GRAVE)
                        dropRule = DropRuleEvent.EVENT.invoker().getDropRule(item, -1, context, true);

                    graveItem.dropRule = dropRule;
                }
            }
        }

        @Override
        public DefaultedList<GraveItem> getAsGraveItemList() {
            DefaultedList<GraveItem> allItems = DefaultedList.of();
            for (DefaultedList<GraveItem> stacks : this.inventory.values())
                allItems.addAll(stacks);

            return allItems;
        }

        @Override
        public CompatComponent<Map<String, DefaultedList<GraveItem>>> filterInv(Predicate<DropRule> predicate) {
            Map<String, DefaultedList<GraveItem>> inventory = new HashMap<>();
            for (Map.Entry<String, DefaultedList<GraveItem>> entry : this.inventory.entrySet()) {
                DefaultedList<GraveItem> items = entry.getValue();

                DefaultedList<GraveItem> filteredItems = DefaultedList.of();
                for (GraveItem graveItem : items) {
                    if (predicate.test(graveItem.dropRule)) {
                        filteredItems.add(graveItem);
                    } else {
                        filteredItems.add(InventoryComponent.EMPTY_GRAVE_ITEM);
                    }
                }

                inventory.put(entry.getKey(), filteredItems);
            }

            return new OriginsCompatComponent(inventory);
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            for (DefaultedList<GraveItem> stacks : this.inventory.values()) {
                for (GraveItem graveItem : stacks) {
                    ItemStack stack = graveItem.stack;
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
            for (DefaultedList<GraveItem> stacks : this.inventory.values()) {
                Collections.fill(stacks, InventoryComponent.EMPTY_GRAVE_ITEM);
            }
        }

        @Override
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();
            for (Map.Entry<String, DefaultedList<GraveItem>> entry : this.inventory.entrySet()) {
                DefaultedList<GraveItem> items = entry.getValue();

                NbtCompound itemsNbt = InventoryComponent.listToNbt(items, graveItem -> {
                    NbtCompound itemNbt = new NbtCompound();
                    graveItem.stack.writeNbt(itemNbt);
                    itemNbt.putString("dropRule", graveItem.dropRule.toString());
                    return itemNbt;
                }, graveItem -> graveItem.stack.isEmpty());

                nbt.put(entry.getKey(), itemsNbt);
            }
            return nbt;
        }
    }
}
