package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.Active;
import io.github.apace100.apoli.power.InventoryPower;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;

import java.util.*;
import java.util.function.Predicate;

public class OriginsCompat implements InvModCompat<Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> {
    @Override
    public String getModName() {
        return "apoli";
    }

    @Override
    public void clear(ServerPlayerEntity player) {
        PowerHolderComponent.getPowers(player, InventoryPower.class).forEach(InventoryPower::clear);
    }

    @Override
    public CompatComponent<Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> readNbt(NbtCompound nbt) {
        Map<String, DefaultedList<Pair<ItemStack, DropRule>>> inventory = new HashMap<>();

        for (String key : nbt.getKeys()) {
            NbtCompound inventoryNbt = nbt.getCompound(key);

            DefaultedList<Pair<ItemStack, DropRule>> items = InventoryComponent.listFromNbt(inventoryNbt, itemNbt -> {
                ItemStack stack = ItemStack.fromNbt(itemNbt);

                DropRule dropRule;
                if (itemNbt.contains("dropRule")) {
                    dropRule = DropRule.valueOf(itemNbt.getString("dropRule"));
                } else {
                    dropRule = YigdConfig.getConfig().compatConfig.defaultOriginsDropRule;
                }

                return new Pair<>(stack, dropRule);
            }, InventoryComponent.EMPTY_ITEM_PAIR);

            inventory.put(key, items);
        }

        return new OriginsCompatComponent(inventory);
    }

    @Override
    public CompatComponent<Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> getNewComponent(ServerPlayerEntity player) {
        return new OriginsCompatComponent(player);
    }

    private static class OriginsCompatComponent extends CompatComponent<Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> {

        public OriginsCompatComponent(ServerPlayerEntity player) {
            super(player);
        }

        public OriginsCompatComponent(Map<String, DefaultedList<Pair<ItemStack, DropRule>>> inventory) {
            super(inventory);
        }

        @Override
        public Map<String, DefaultedList<Pair<ItemStack, DropRule>>> getInventory(ServerPlayerEntity player) {
            YigdConfig.CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

            Map<String, DefaultedList<Pair<ItemStack, DropRule>>> inventory = new HashMap<>();

            List<InventoryPower> powers = PowerHolderComponent.getPowers(player, InventoryPower.class);
            for (InventoryPower inventoryPower : powers) {
                Active.Key key = inventoryPower.getKey();
                DefaultedList<Pair<ItemStack, DropRule>> stacks = DefaultedList.of();

                for (int i = 0; i < inventoryPower.size(); i++) {
                    ItemStack stack = inventoryPower.getStack(i);
                    DropRule dropRule = inventoryPower.shouldDropOnDeath(stack) ? compatConfig.defaultOriginsDropRule : DropRule.KEEP;
                    stacks.add(new Pair<>(stack, dropRule));
                }

                inventory.put(key.key, stacks);
            }

            return inventory;
        }

        @Override
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayerEntity merger) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            @SuppressWarnings("unchecked")
            Map<String, DefaultedList<Pair<ItemStack, DropRule>>> mergingInventory = (Map<String, DefaultedList<Pair<ItemStack, DropRule>>>) mergingComponent.inventory;
            for (Map.Entry<String, DefaultedList<Pair<ItemStack, DropRule>>> entry : mergingInventory.entrySet()) {
                DefaultedList<Pair<ItemStack, DropRule>> currentItems = this.inventory.getOrDefault(entry.getKey(), DefaultedList.of());
                DefaultedList<Pair<ItemStack, DropRule>> mergingItems = entry.getValue();

                for (int i = 0; i < mergingItems.size(); i++) {
                    ItemStack mergingStack = mergingItems.get(i).getLeft().copy();  // Solves the case where the itemstacks are the same instance

                    if (i >= currentItems.size()) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    Pair<ItemStack, DropRule> currentPair = currentItems.get(i);
                    if (!currentPair.getLeft().isEmpty()) {
                        extraItems.add(mergingStack);
                    } else {
                        currentItems.set(i, new Pair<>(mergingStack, currentPair.getRight()));
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

                DefaultedList<Pair<ItemStack, DropRule>> inventoryItems = this.inventory.get(key);
                if (inventoryItems == null)
                    continue;

                for (int i = 0; i < inventoryItems.size(); i++) {
                    ItemStack currentStack = inventoryItems.get(i).getLeft();

                    if (i >= power.size()) {
                        extraItems.add(currentStack);
                    } else {
                        power.setStack(i, currentStack);
                    }
                }
            }

            for (String key : unhandledPowers) {
                for (Pair<ItemStack, DropRule> pair : this.inventory.get(key)) {
                    extraItems.add(pair.getLeft());
                }
            }


            return extraItems;
        }

        @Override
        public void handleDropRules(DeathContext context) {
            for (Map.Entry<String, DefaultedList<Pair<ItemStack, DropRule>>> entry : this.inventory.entrySet()) {
                DefaultedList<Pair<ItemStack, DropRule>> items = entry.getValue();

                for (Pair<ItemStack, DropRule> pair : items) {
                    ItemStack item = pair.getLeft();
                    if (item.isEmpty()) continue;

                    DropRule dropRule = pair.getRight();

                    if (dropRule == DropRule.PUT_IN_GRAVE)
                        dropRule = DropRuleEvent.EVENT.invoker().getDropRule(item, -1, context, true);

                    pair.setRight(dropRule);
                }
            }
        }

        @Override
        public DefaultedList<Pair<ItemStack, DropRule>> getAsStackDropList() {
            DefaultedList<Pair<ItemStack, DropRule>> allItems = DefaultedList.of();
            for (DefaultedList<Pair<ItemStack, DropRule>> stacks : this.inventory.values())
                allItems.addAll(stacks);

            return allItems;
        }

        @Override
        public CompatComponent<Map<String, DefaultedList<Pair<ItemStack, DropRule>>>> filterInv(Predicate<DropRule> predicate) {
            Map<String, DefaultedList<Pair<ItemStack, DropRule>>> inventory = new HashMap<>();
            for (Map.Entry<String, DefaultedList<Pair<ItemStack, DropRule>>> entry : this.inventory.entrySet()) {
                DefaultedList<Pair<ItemStack, DropRule>> items = entry.getValue();

                DefaultedList<Pair<ItemStack, DropRule>> filteredItems = DefaultedList.of();
                for (Pair<ItemStack, DropRule> pair : items) {
                    if (predicate.test(pair.getRight())) {
                        filteredItems.add(pair);
                    } else {
                        filteredItems.add(InventoryComponent.EMPTY_ITEM_PAIR);
                    }
                }

                inventory.put(entry.getKey(), filteredItems);
            }

            return new OriginsCompatComponent(inventory);
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            for (DefaultedList<Pair<ItemStack, DropRule>> stacks : this.inventory.values()) {
                for (Pair<ItemStack, DropRule> pair : stacks) {
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
            for (DefaultedList<Pair<ItemStack, DropRule>> stacks : this.inventory.values()) {
                Collections.fill(stacks, InventoryComponent.EMPTY_ITEM_PAIR);
            }
        }

        @Override
        public boolean containsGraveItems() {
            for (Pair<ItemStack, DropRule> pair : this.getAsStackDropList()) {
                if (!pair.getLeft().isEmpty() && pair.getRight() == DropRule.PUT_IN_GRAVE) return true;
            }
            return false;
        }

        @Override
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();
            for (Map.Entry<String, DefaultedList<Pair<ItemStack, DropRule>>> entry : this.inventory.entrySet()) {
                DefaultedList<Pair<ItemStack, DropRule>> items = entry.getValue();

                NbtCompound itemsNbt = InventoryComponent.listToNbt(items, pair -> {
                    NbtCompound itemNbt = new NbtCompound();
                    pair.getLeft().writeNbt(itemNbt);
                    itemNbt.putString("dropRule", pair.getRight().toString());
                    return itemNbt;
                }, pair -> pair.getLeft().isEmpty());

                nbt.put(entry.getKey(), itemsNbt);
            }
            return nbt;
        }
    }
}
