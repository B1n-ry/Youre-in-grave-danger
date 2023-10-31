package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import me.lizardofoz.inventorio.api.InventorioAPI;
import me.lizardofoz.inventorio.player.PlayerInventoryAddon;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

import java.util.Collections;
import java.util.function.Predicate;

public class InventorioCompat implements InvModCompat<DefaultedList<ItemStack>> {
    @Override
    public String getModName() {
        return "inventorio";
    }

    @Override
    public void clear(ServerPlayerEntity player) {
        PlayerInventoryAddon inventoryAddon = InventorioAPI.getInventoryAddon(player);
        if (inventoryAddon == null) return;

        inventoryAddon.clear();
    }

    @Override
    public CompatComponent<DefaultedList<ItemStack>> readNbt(NbtCompound nbt) {
        int size = nbt.getInt("size");
        DefaultedList<ItemStack> items = DefaultedList.ofSize(size, ItemStack.EMPTY);

        Inventories.readNbt(nbt, items);
        return new InventorioCompatComponent(items);
    }

    @Override
    public CompatComponent<DefaultedList<ItemStack>> getNewComponent(ServerPlayerEntity player) {
        return new InventorioCompatComponent(player);
    }

    private static class InventorioCompatComponent extends CompatComponent<DefaultedList<ItemStack>> {

        public InventorioCompatComponent(ServerPlayerEntity player) {
            super(player);
        }
        public InventorioCompatComponent(DefaultedList<ItemStack> inventory) {
            super(inventory);
        }

        @Override
        public DefaultedList<ItemStack> getInventory(ServerPlayerEntity player) {
            PlayerInventoryAddon addon = InventorioAPI.getInventoryAddon(player);

            DefaultedList<ItemStack> items = DefaultedList.of();
            if (addon == null) return items;

            for (int i = 0; i < addon.size(); i++) {
                ItemStack stack = addon.getStack(i);
                items.add(stack);
            }

            return items;
        }

        @Override
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            @SuppressWarnings("unchecked")
            DefaultedList<ItemStack> mergingInventory = (DefaultedList<ItemStack>) mergingComponent.inventory;
            for (int i = 0; i < mergingInventory.size(); i++) {
                ItemStack mergingItem = mergingInventory.get(i);
                if (mergingItem.isEmpty()) continue;

                if (!this.inventory.get(i).isEmpty()) {
                    extraItems.add(mergingItem);
                } else {
                    this.inventory.set(i, mergingItem);
                }
            }
            return extraItems;
        }

        @Override
        public DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            PlayerInventoryAddon addon = InventorioAPI.getInventoryAddon(player);
            if (addon == null) return extraItems;

            for (int i = 0; i < this.inventory.size(); i++) {
                ItemStack item = this.inventory.get(i);
                if (i >= addon.size()) {
                    extraItems.add(item);
                } else {
                    addon.setStack(i, item);
                }
            }

            return extraItems;
        }

        @Override
        public CompatComponent<DefaultedList<ItemStack>> handleDropRules(DeathContext context) {
            DefaultedList<ItemStack> soulboundItems = DefaultedList.ofSize(this.inventory.size(), ItemStack.EMPTY);

            for (int i = 0; i < this.inventory.size(); i++) {
                ItemStack stack = this.inventory.get(i);

                DropRule dropRule = DropRuleEvent.EVENT.invoker().getDropRule(stack, -1, context);
                Vec3d deathPos = context.getDeathPos();
                switch (dropRule) {
                    case KEEP -> soulboundItems.set(i, stack);
                    case DROP -> ItemScatterer.spawn(context.getWorld(), deathPos.x, deathPos.y, deathPos.z, stack);
                }
                if (dropRule != DropRule.PUT_IN_GRAVE)
                    this.inventory.set(i, ItemStack.EMPTY);
            }
            return new InventorioCompatComponent(soulboundItems);
        }

        @Override
        public DefaultedList<ItemStack> getAsStackList() {
            return this.inventory;
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            for (ItemStack stack : this.inventory) {
                if (predicate.test(stack)) {
                    stack.decrement(itemCount);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            Collections.fill(this.inventory, ItemStack.EMPTY);
        }

        @Override
        public boolean isEmpty() {
            DefaultedList<ItemStack> items = DefaultedList.of();
            items.addAll(this.inventory);
            items.removeIf(ItemStack::isEmpty);
            return items.isEmpty();
        }

        @Override
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putInt("size", this.inventory.size());
            return Inventories.writeNbt(nbt, this.inventory);
        }
    }
}
