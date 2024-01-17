package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import me.lizardofoz.inventorio.api.InventorioAPI;
import me.lizardofoz.inventorio.player.PlayerInventoryAddon;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

public class InventorioCompat implements InvModCompat<DefaultedList<Pair<ItemStack, DropRule>>> {
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
    public CompatComponent<DefaultedList<Pair<ItemStack, DropRule>>> readNbt(NbtCompound nbt) {
        int size = nbt.getInt("size");
        DefaultedList<Pair<ItemStack, DropRule>> items = DefaultedList.ofSize(size, InventoryComponent.EMPTY_ITEM_PAIR);

        InventoryComponent.listFromNbt(nbt, itemNbt -> {
            ItemStack stack = ItemStack.fromNbt(itemNbt);

            DropRule dropRule;
            if (itemNbt.contains("dropRule")) {
                dropRule = DropRule.valueOf(itemNbt.getString("dropRule"));
            } else {
                dropRule = YigdConfig.getConfig().compatConfig.defaultInventorioDropRule;
            }

            return new Pair<>(stack, dropRule);
        }, InventoryComponent.EMPTY_ITEM_PAIR);

        return new InventorioCompatComponent(items);
    }

    @Override
    public CompatComponent<DefaultedList<Pair<ItemStack, DropRule>>> getNewComponent(ServerPlayerEntity player) {
        return new InventorioCompatComponent(player);
    }

    private static class InventorioCompatComponent extends CompatComponent<DefaultedList<Pair<ItemStack, DropRule>>> {

        public InventorioCompatComponent(ServerPlayerEntity player) {
            super(player);
        }
        public InventorioCompatComponent(DefaultedList<Pair<ItemStack, DropRule>> inventory) {
            super(inventory);
        }

        @Override
        public DefaultedList<Pair<ItemStack, DropRule>> getInventory(ServerPlayerEntity player) {
            PlayerInventoryAddon addon = InventorioAPI.getInventoryAddon(player);

            DefaultedList<Pair<ItemStack, DropRule>> items = DefaultedList.of();
            if (addon == null) return items;

            DropRule defaultDropRule = YigdConfig.getConfig().compatConfig.defaultInventorioDropRule;

            for (int i = 0; i < addon.size(); i++) {
                ItemStack stack = addon.getStack(i);
                items.add(new Pair<>(stack, defaultDropRule));
            }

            return items;
        }

        @Override
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            @SuppressWarnings("unchecked")
            DefaultedList<Pair<ItemStack, DropRule>> mergingInventory = (DefaultedList<Pair<ItemStack, DropRule>>) mergingComponent.inventory;
            for (int i = 0; i < mergingInventory.size(); i++) {
                ItemStack mergingItem = mergingInventory.get(i).getLeft().copy();  // Solves the issue where the itemstacks are the same instance
                if (mergingItem.isEmpty()) continue;

                if (!this.inventory.get(i).getLeft().isEmpty()) {
                    extraItems.add(mergingItem);
                } else {
                    this.inventory.get(i).setLeft(mergingItem);
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
                ItemStack item = this.inventory.get(i).getLeft();
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
            YigdConfig.CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

            Vec3d deathPos = context.deathPos();
            for (Pair<ItemStack, DropRule> pair : this.inventory) {
                ItemStack stack = pair.getLeft();

                DropRule dropRule = compatConfig.defaultInventorioDropRule;
                if (dropRule == DropRule.PUT_IN_GRAVE)
                    dropRule = DropRuleEvent.EVENT.invoker().getDropRule(stack, -1, context, true);

                if (dropRule == DropRule.DROP) {
                    InventoryComponent.dropItemIfToBeDropped(stack, deathPos.x, deathPos.y, deathPos.z, context.world());
                }

                pair.setRight(dropRule);
            }
        }

        @Override
        public DefaultedList<Pair<ItemStack, DropRule>> getAsStackDropList() {
            DefaultedList<Pair<ItemStack, DropRule>> items = DefaultedList.of();
            items.addAll(this.inventory);
            return items;
        }

        @Override
        public CompatComponent<DefaultedList<Pair<ItemStack, DropRule>>> filterInv(Predicate<DropRule> predicate) {
            DefaultedList<Pair<ItemStack, DropRule>> filteredItems = DefaultedList.of();

            for (int i = 0; i < this.inventory.size(); i++) {
                Pair<ItemStack, DropRule> pair = this.inventory.get(i);
                ItemStack stack = pair.getLeft();
                DropRule dropRule = pair.getRight();

                if (predicate.test(dropRule)) {
                    filteredItems.add(i, new Pair<>(stack, dropRule));
                } else {
                    filteredItems.add(i, InventoryComponent.EMPTY_ITEM_PAIR);
                }
            }

            return new InventorioCompatComponent(filteredItems);
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            for (Pair<ItemStack, DropRule> pair : this.inventory) {
                ItemStack stack = pair.getLeft();
                if (predicate.test(stack)) {
                    stack.decrement(itemCount);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            for (Pair<ItemStack, DropRule> pair : this.inventory) {
                pair.setLeft(ItemStack.EMPTY);
            }
        }

        @Override
        public boolean containsGraveItems() {
            for (Pair<ItemStack, DropRule> pair : this.inventory) {
                if (!pair.getLeft().isEmpty() && pair.getRight() == DropRule.PUT_IN_GRAVE) return true;
            }

            return false;
        }

        @Override
        public NbtCompound writeNbt() {
            return InventoryComponent.listToNbt(this.inventory, pair -> {
                NbtCompound itemNbt = new NbtCompound();
                pair.getLeft().writeNbt(itemNbt);
                itemNbt.putString("dropRule", pair.getRight().name());

                return itemNbt;
            }, pair -> pair.getLeft().isEmpty());
        }
    }
}
