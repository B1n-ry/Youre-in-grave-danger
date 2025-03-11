package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.CompatConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.data.GraveItem;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import de.rubixdev.inventorio.api.InventorioAPI;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

import java.util.Collections;
import java.util.function.Predicate;

public class InventorioCompat implements InvModCompat<DefaultedList<GraveItem>> {
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
    public CompatComponent<DefaultedList<GraveItem>> readNbt(NbtCompound nbt) {
        DefaultedList<GraveItem> items = InventoryComponent.listFromNbt(nbt, itemNbt -> {
            ItemStack stack = ItemStack.fromNbt(itemNbt);

            DropRule dropRule;
            if (itemNbt.contains("dropRule")) {
                dropRule = DropRule.valueOf(itemNbt.getString("dropRule"));
            } else {
                dropRule = YigdConfig.getConfig().compatConfig.defaultInventorioDropRule;
            }

            return new GraveItem(stack, dropRule);
        }, InventoryComponent.EMPTY_GRAVE_ITEM);

        return new InventorioCompatComponent(items);
    }

    @Override
    public CompatComponent<DefaultedList<GraveItem>> getNewComponent(ServerPlayerEntity player) {
        return new InventorioCompatComponent(player);
    }

    private static class InventorioCompatComponent extends CompatComponent<DefaultedList<GraveItem>> {

        public InventorioCompatComponent(ServerPlayerEntity player) {
            super(player);
        }
        public InventorioCompatComponent(DefaultedList<GraveItem> inventory) {
            super(inventory);
        }

        @Override
        public DefaultedList<GraveItem> getInventory(ServerPlayerEntity player) {
            PlayerInventoryAddon addon = InventorioAPI.getInventoryAddon(player);

            DefaultedList<GraveItem> items = DefaultedList.of();
            if (addon == null) return items;

            DropRule defaultDropRule = YigdConfig.getConfig().compatConfig.defaultInventorioDropRule;

            for (int i = 0; i < addon.size(); i++) {
                ItemStack stack = addon.getStack(i);
                items.add(new GraveItem(stack, defaultDropRule));
            }

            return items;
        }

        @Override
        public DefaultedList<GraveItem> merge(CompatComponent<?> mergingComponent, ServerPlayerEntity merger) {
            DefaultedList<GraveItem> extraItems = DefaultedList.of();

            @SuppressWarnings("unchecked")
            DefaultedList<GraveItem> mergingInventory = (DefaultedList<GraveItem>) mergingComponent.inventory;
            for (int i = 0; i < mergingInventory.size(); i++) {
                GraveItem mergingItem = mergingInventory.get(i).copy();  // Solves the issue where the itemstacks are the same instance
                if (mergingItem.stack.isEmpty()) continue;

                GraveItem graveItem = this.inventory.get(i);
                if (!graveItem.stack.isEmpty()) {
                    extraItems.add(mergingItem);
                } else {
                    // Can't set the ItemStack directly because if it's the empty one we change the empty pair to a non-empty value
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
                ItemStack item = this.inventory.get(i).stack.copy();
                if (i >= addon.size()) {
                    extraItems.add(item);
                } else {
                    addon.setStack(i, item);
                }
            }

            return extraItems;
        }

        @Override
        public void handleDropRules(DeathContext context) {
            CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

            for (GraveItem graveItem : this.inventory) {
                ItemStack stack = graveItem.stack;
                if (stack.isEmpty()) continue;

                DropRule dropRule = compatConfig.defaultInventorioDropRule;
                if (dropRule == DropRule.PUT_IN_GRAVE)
                    dropRule = DropRuleEvent.EVENT.invoker().getDropRule(stack, -1, context, true);

                graveItem.dropRule = dropRule;
            }
        }

        @Override
        public DefaultedList<GraveItem> getAsGraveItemList() {
            DefaultedList<GraveItem> items = DefaultedList.of();
            items.addAll(this.inventory);
            return items;
        }

        @Override
        public CompatComponent<DefaultedList<GraveItem>> filterInv(Predicate<DropRule> predicate) {
            DefaultedList<GraveItem> filteredItems = DefaultedList.of();

            for (int i = 0; i < this.inventory.size(); i++) {
                GraveItem graveItem = this.inventory.get(i);
                ItemStack stack = graveItem.stack;
                DropRule dropRule = graveItem.dropRule;

                if (predicate.test(dropRule)) {
                    filteredItems.add(i, new GraveItem(stack, dropRule));
                } else {
                    filteredItems.add(i, InventoryComponent.EMPTY_GRAVE_ITEM);
                }
            }

            return new InventorioCompatComponent(filteredItems);
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            for (GraveItem graveItem : this.inventory) {
                ItemStack stack = graveItem.stack;
                if (predicate.test(stack)) {
                    stack.decrement(itemCount);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            Collections.fill(this.inventory, InventoryComponent.EMPTY_GRAVE_ITEM);
        }

        @Override
        public NbtCompound writeNbt() {
            return InventoryComponent.listToNbt(this.inventory, graveItem -> {
                NbtCompound itemNbt = new NbtCompound();
                graveItem.stack.writeNbt(itemNbt);
                itemNbt.putString("dropRule", graveItem.dropRule.name());

                return itemNbt;
            }, graveItem -> graveItem.stack.isEmpty());
        }
    }
}
