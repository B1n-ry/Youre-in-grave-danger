package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import io.github.apace100.apoli.component.PowerHolderComponent;
import io.github.apace100.apoli.power.Active;
import io.github.apace100.apoli.power.InventoryPower;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.function.Predicate;

public class OriginsCompat implements InvModCompat<Map<String, DefaultedList<ItemStack>>> {
    @Override
    public String getModName() {
        return "apoli";
    }

    @Override
    public void clear(ServerPlayerEntity player) {
        PowerHolderComponent.getPowers(player, InventoryPower.class).forEach(InventoryPower::clear);
    }

    @Override
    public CompatComponent<Map<String, DefaultedList<ItemStack>>> readNbt(NbtCompound nbt) {
        Map<String, DefaultedList<ItemStack>> inventory = new HashMap<>();

        for (String key : nbt.getKeys()) {
            NbtCompound inventoryNbt = nbt.getCompound(key);

            int size = inventoryNbt.getInt("size");
            DefaultedList<ItemStack> items = DefaultedList.ofSize(size, ItemStack.EMPTY);

            Inventories.readNbt(inventoryNbt, items);

            inventory.put(key, items);
        }
        return new OriginsCompatComponent(inventory);
    }

    @Override
    public CompatComponent<Map<String, DefaultedList<ItemStack>>> getNewComponent(ServerPlayerEntity player) {
        return new OriginsCompatComponent(player);
    }

    private static class OriginsCompatComponent extends CompatComponent<Map<String, DefaultedList<ItemStack>>> {

        public OriginsCompatComponent(ServerPlayerEntity player) {
            super(player);
        }

        public OriginsCompatComponent(Map<String, DefaultedList<ItemStack>> inventory) {
            super(inventory);
        }

        @Override
        public Map<String, DefaultedList<ItemStack>> getInventory(ServerPlayerEntity player) {
            Map<String, DefaultedList<ItemStack>> inventory = new HashMap<>();

            List<InventoryPower> powers =  PowerHolderComponent.getPowers(player, InventoryPower.class);
            for (InventoryPower inventoryPower : powers) {
                Active.Key key = inventoryPower.getKey();
                DefaultedList<ItemStack> stacks = DefaultedList.of();

                for (int i = 0; i < inventoryPower.size(); i++) {
                    stacks.add(inventoryPower.getStack(i));
                }

                inventory.put(key.key, stacks);
            }

            return inventory;
        }

        @Override
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            @SuppressWarnings("unchecked")
            Map<String, DefaultedList<ItemStack>> mergingInventory = (Map<String, DefaultedList<ItemStack>>) mergingComponent.inventory;
            for (Map.Entry<String, DefaultedList<ItemStack>> entry : mergingInventory.entrySet()) {
                DefaultedList<ItemStack> currentItems = this.inventory.getOrDefault(entry.getKey(), DefaultedList.of());
                DefaultedList<ItemStack> mergingItems = entry.getValue();

                for (int i = 0; i < mergingItems.size(); i++) {
                    ItemStack mergingStack = mergingItems.get(i);

                    if (i >= currentItems.size()) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    ItemStack currentStack = currentItems.get(i);
                    if (!currentStack.isEmpty()) {
                        extraItems.add(mergingStack);
                    } else {
                        currentItems.set(i, mergingStack);
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

                DefaultedList<ItemStack> inventoryItems = this.inventory.get(key);
                if (inventoryItems == null)
                    continue;

                for (int i = 0; i < inventoryItems.size(); i++) {
                    ItemStack currentStack = inventoryItems.get(i);

                    if (i >= power.size()) {
                        extraItems.add(currentStack);
                    } else {
                        power.setStack(i, currentStack);
                    }
                }
            }

            for (String key : unhandledPowers) {
                extraItems.addAll(this.inventory.get(key));
            }


            return extraItems;
        }

        @Override
        public CompatComponent<Map<String, DefaultedList<ItemStack>>> handleDropRules(DeathContext context) {
            Map<String, DefaultedList<ItemStack>> soulbound = new HashMap<>();

            for (Map.Entry<String, DefaultedList<ItemStack>> entry : this.inventory.entrySet()) {
                String key = entry.getKey();
                DefaultedList<ItemStack> items = entry.getValue();

                DefaultedList<ItemStack> soulboundStacks = DefaultedList.ofSize(items.size(), ItemStack.EMPTY);

                for (int i = 0; i < items.size(); i++) {
                    ItemStack item = items.get(i);
                    Vec3d deathPos = context.getDeathPos();
                    DropRule dropRule = DropRuleEvent.EVENT.invoker().getDropRule(item, -1, context, true);
                    switch (dropRule) {
                        case KEEP -> soulboundStacks.set(i, item);
                        case DROP -> ItemScatterer.spawn(context.getWorld(), deathPos.x, deathPos.y, deathPos.z, item);
                    }
                    if (dropRule != DropRule.PUT_IN_GRAVE)
                        items.set(i, ItemStack.EMPTY);
                }

                soulbound.put(key, soulboundStacks);
            }
            return new OriginsCompatComponent(soulbound);
        }

        @Override
        public DefaultedList<ItemStack> getAsStackList() {
            DefaultedList<ItemStack> allItems = DefaultedList.of();
            for (DefaultedList<ItemStack> stacks : this.inventory.values())
                allItems.addAll(stacks);

            return allItems;
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            for (DefaultedList<ItemStack> stacks : this.inventory.values()) {
                for (ItemStack stack : stacks) {
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
            for (DefaultedList<ItemStack> stacks : this.inventory.values()) {
                Collections.fill(stacks, ItemStack.EMPTY);
            }
        }

        @Override
        public boolean isEmpty() {
            DefaultedList<ItemStack> items = DefaultedList.of();
            items.addAll(this.getAsStackList());

            items.removeIf(ItemStack::isEmpty);
            return items.isEmpty();
        }

        @Override
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();
            for (Map.Entry<String, DefaultedList<ItemStack>> entry : this.inventory.entrySet()) {
                DefaultedList<ItemStack> items = entry.getValue();
                NbtCompound itemsNbt = Inventories.writeNbt(new NbtCompound(), items);

                itemsNbt.putInt("size", items.size());

                nbt.put(entry.getKey(), itemsNbt);
            }
            return nbt;
        }
    }
}
