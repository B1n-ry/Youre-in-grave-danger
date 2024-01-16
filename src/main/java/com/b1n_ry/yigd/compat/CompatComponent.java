package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.util.DropRule;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

public abstract class CompatComponent<T> {
    protected T inventory;

    public CompatComponent(ServerPlayerEntity player) {
        this.inventory = this.getInventory(player);
    }
    public CompatComponent(T inventory) {
        this.inventory = inventory;
    }

    public abstract T getInventory(ServerPlayerEntity player);

    /**
     * Slaps the merging component on top of the current one. If any item is occupied in current component, merging
     * component should add the item that would've gone in that slot to the returning list
     * @param mergingComponent Component that will merge. REQUIRED TO BE OF SAME INSTANCE AS THIS COMPONENT
     * @return A list with all items that couldn't be merged from merging component
     */
    public abstract DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent);
    public abstract DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player);

    /**
     * Handle drop rules for each item or whatever the component holds
     *
     * @param context How the player died
     */
    public abstract void handleDropRules(DeathContext context);

    /**
     * Get all items as a {@link DefaultedList<ItemStack>} in the component
     * @return All items in the component <b>INCLUDING EMPTY ITEMS</b>
     */
    public abstract DefaultedList<ItemStack> getAsStackList();
    public abstract CompatComponent<T> filterInv(Predicate<DropRule> predicate);
    public abstract boolean removeItem(Predicate<ItemStack> predicate, int itemCount);
    public void dropItems(ServerWorld world, Vec3d pos) {
        DefaultedList<ItemStack> items = this.getAsStackList();
        for (ItemStack stack : items) {
            InventoryComponent.dropItemIfToBeDropped(stack, pos.x, pos.y, pos.z, world);
        }
    }
    public abstract void clear();
    public abstract boolean isEmpty();
    public abstract NbtCompound writeNbt();
}
