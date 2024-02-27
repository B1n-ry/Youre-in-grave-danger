package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.util.DropRule;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

import java.util.function.Consumer;
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
     * If curse of binding has any effect in this inventory, this method should be overwritten to remove those items
     * @param playerRef Player reference to check items that can or can't be unequipped
     * @return The removed curse of binding items
     */
    public DefaultedList<ItemStack> pullBindingCurseItems(ServerPlayerEntity playerRef) {
        return DefaultedList.of();
    }

    /**
     * Slaps the merging component on top of the current one. If any item is occupied in current component, merging
     * component should add the item that would've gone in that slot to the returning list
     * Component should be filtered based on drop rule before calling this, so different drop rules on items does not matter in this method
     * @param mergingComponent Component that will merge. REQUIRED TO BE OF SAME INSTANCE AS THIS COMPONENT
     * @param merger The player that is merging the components. DO NOT MODIFY THE PLAYER INVENTORY IN THIS METHOD
     * @return A list with all items that couldn't be merged from merging component
     */
    public abstract DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayerEntity merger);
    public abstract DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player);

    /**
     * Handle drop rules for each item or whatever the component holds
     *
     * @param context How the player died
     */
    public abstract void handleDropRules(DeathContext context);

    /**
     * Get all items as a {@link DefaultedList<Pair>} of {@link Pair<>} containing {@link ItemStack} and {@link DropRule} in the component
     * The drop rule refers to what drop rule was/will be applied on death
     * @return Pairs containing all items in the component <b>INCLUDING EMPTY ITEMS</b>
     */
    public abstract DefaultedList<Pair<ItemStack, DropRule>> getAsStackDropList();
    public abstract CompatComponent<T> filterInv(Predicate<DropRule> predicate);
    public abstract boolean removeItem(Predicate<ItemStack> predicate, int itemCount);

    /**
     * Drop all items in the component to the world
     * @param world The world to drop items in
     * @param pos The position to drop items at
     */
    public void dropItems(ServerWorld world, Vec3d pos) {
        DefaultedList<Pair<ItemStack, DropRule>> items = this.getAsStackDropList();
        for (Pair<ItemStack, DropRule> pair : items) {
            ItemStack stack = pair.getLeft();
            if (stack.isEmpty()) continue;

            InventoryComponent.dropItemIfToBeDropped(pair.getLeft(), pos.x, pos.y, pos.z, world);
        }
    }

    /**
     * Drop all items in the component to the world, but only items that should be placed in a grave or dropped anyway
     * @param world The world to drop items in
     * @param pos The position to drop items at
     */
    public void dropGraveItems(ServerWorld world, Vec3d pos) {
        DefaultedList<Pair<ItemStack, DropRule>> items = this.getAsStackDropList();
        for (Pair<ItemStack, DropRule> pair : items) {
            ItemStack stack = pair.getLeft();
            if (stack.isEmpty() || pair.getRight() == DropRule.KEEP || pair.getRight() == DropRule.DESTROY) continue;
            pair.setRight(DropRule.DROP);  // Make sure item are marked as dropped, and not in a non-existent grave

            InventoryComponent.dropItemIfToBeDropped(pair.getLeft(), pos.x, pos.y, pos.z, world);
        }
    }
    public abstract void clear();

    /**
     * Check if the component contains any items that should be placed in a grave (according to drop rules)
     * @return Whether the component contains any items that should be placed in a grave
     */
    public boolean containsGraveItems() {
        for (Pair<ItemStack, DropRule> pair : this.getAsStackDropList()) {
            if (!pair.getLeft().isEmpty() && pair.getRight() == DropRule.PUT_IN_GRAVE) return true;
        }
        return false;
    }

    /**
     * Check if the component contains at least one item that matches the predicate. Will stop at first match
     * @param predicate Predicate to test items against
     * @return Whether the component contains at least one item that matches the predicate
     */
    public boolean containsAny(Predicate<ItemStack> predicate) {
        for (Pair<ItemStack, DropRule> pair : this.getAsStackDropList()) {
            if (predicate.test(pair.getLeft())) return true;
        }
        return false;
    }
    public void handleItemPairs(Predicate<ItemStack> predicate, Consumer<Pair<ItemStack, DropRule>> modification) {
        for (Pair<ItemStack, DropRule> pair : this.getAsStackDropList()) {
            if (predicate.test(pair.getLeft())) {
                modification.accept(pair);
            }
        }
    }
    public abstract NbtCompound writeNbt();
}
