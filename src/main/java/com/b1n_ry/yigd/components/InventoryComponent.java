package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

public class InventoryComponent {
    private final DefaultedList<ItemStack> items;

    public InventoryComponent(ServerPlayerEntity player) {
        this.items = this.getInventoryItems(player);
    }
    private InventoryComponent(DefaultedList<ItemStack> items) {
        this.items = items;
    }

    private DefaultedList<ItemStack> getInventoryItems(ServerPlayerEntity player) {
        Inventory inventory = player.getInventory();

        DefaultedList<ItemStack> items = DefaultedList.ofSize(inventory.size());

        // Save all items in vanilla inventory
        for (int i = 0; i < inventory.size(); i++) {
            items.set(i, inventory.getStack(i));
        }

        return items;
    }

    public void onDeath(RespawnComponent respawnComponent, DeathContext context) {
        DefaultedList<ItemStack> soulboundItems = this.handleDropRules(this.items, context);

        respawnComponent.setSoulboundItems(soulboundItems);
    }

    /**
     * Handles the drop rule for each item in the list
     * @param items all items that should be filtered
     * @return new list with all items that should be kept
     */
    private DefaultedList<ItemStack> handleDropRules(DefaultedList<ItemStack> items, DeathContext context) {
        DefaultedList<ItemStack> soulboundList = DefaultedList.ofSize(items.size(), ItemStack.EMPTY);

        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            DropRule dropRule = DropRuleEvent.DESTROY_ITEM_EVENT.invoker().getDropRule(item, i, context);
            switch (dropRule) {
                case DESTROY -> items.set(i, ItemStack.EMPTY);
                case KEEP -> {
                    items.set(i, ItemStack.EMPTY);
                    soulboundList.set(i, item);
                }
            }
        }

        return soulboundList;
    }

    public void applyLoss() {

    }

    public void dropAll(ServerWorld world, Vec3d pos) {
        for (ItemStack stack : this.items) {
            if (stack.isEmpty()) continue;
            ItemScatterer.spawn(world, pos.x, pos.y, pos.z, stack);
        }
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtCompound vanillaInventoryNbt = Inventories.writeNbt(new NbtCompound(), this.items);
        vanillaInventoryNbt.putInt("size", this.items.size());

        nbt.put("vanilla", vanillaInventoryNbt);

        return nbt;
    }

    public static InventoryComponent fromNbt(NbtCompound nbt) {
        int listSize = nbt.getInt("size");
        DefaultedList<ItemStack> items = DefaultedList.ofSize(listSize, ItemStack.EMPTY);
        Inventories.readNbt(nbt, items);

        return new InventoryComponent(items);
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        player.getInventory().clear();
    }
}
