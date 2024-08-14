package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.util.PairModificationConsumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

public abstract class CompatComponent<T> {
    protected T inventory;

    public CompatComponent(ServerPlayer player) {
        this.inventory = this.getInventory(player);
    }
    public CompatComponent(T inventory) {
        this.inventory = inventory;
    }

    public abstract T getInventory(ServerPlayer player);

    /**
     * If curse of binding has any effect in this inventory, this method should be overwritten to remove those items
     * @param playerRef Player reference to check items that can or can't be unequipped
     * @return The removed curse of binding items
     */
    public NonNullList<ItemStack> pullBindingCurseItems(ServerPlayer playerRef) {
        return NonNullList.create();
    }

    /**
     * Slaps the merging component on top of the current one. If any item is occupied in current component, merging
     * component should add the item that would've gone in that slot to the returning list
     * Component should be filtered based on drop rule before calling this, so different drop rules on items does not matter in this method
     * @param mergingComponent Component that will merge. REQUIRED TO BE OF SAME INSTANCE AS THIS COMPONENT
     * @param merger The player that is merging the components. DO NOT MODIFY THE PLAYER INVENTORY IN THIS METHOD
     * @return A list with all items that couldn't be merged from merging component
     */
    public abstract NonNullList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayer merger);
    public abstract NonNullList<ItemStack> storeToPlayer(ServerPlayer player);

    /**
     * Handle drop rules for each item or whatever the component holds
     *
     * @param context How the player died
     */
    public abstract void handleDropRules(DeathContext context);

    /**
     * Get all items as a {@link NonNullList<Tuple>} of {@link Tuple<>} containing {@link ItemStack} and {@link DropRule} in the component
     * The drop rule refers to what drop rule was/will be applied on death
     * @return Pairs containing all items in the component <b>INCLUDING EMPTY ITEMS</b>
     */
    public abstract NonNullList<Tuple<ItemStack, DropRule>> getAsStackDropList();
    public abstract CompatComponent<T> filterInv(Predicate<DropRule> predicate);
    public abstract boolean removeItem(Predicate<ItemStack> predicate, int itemCount);

    /**
     * Drop all items in the component to the world
     * @param world The world to drop items in
     * @param pos The position to drop items at
     */
    public void dropItems(ServerLevel world, Vec3 pos) {
        NonNullList<Tuple<ItemStack, DropRule>> items = this.getAsStackDropList();
        for (Tuple<ItemStack, DropRule> pair : items) {
            ItemStack stack = pair.getA();
            if (stack.isEmpty()) continue;

            InventoryComponent.dropItemIfToBeDropped(pair.getA(), pos.x, pos.y, pos.z, world);
        }
    }

    /**
     * Drop items in the component to the world, but only items that should be placed in a grave or dropped anyway
     * @param world The world to drop items in
     * @param pos The position to drop items at
     */
    public void dropGraveItems(ServerLevel world, Vec3 pos) {
        NonNullList<Tuple<ItemStack, DropRule>> items = this.getAsStackDropList();
        for (Tuple<ItemStack, DropRule> pair : items) {
            ItemStack stack = pair.getA();
            if (stack.isEmpty() || pair.getB() == DropRule.KEEP || pair.getB() == DropRule.DESTROY) continue;
            pair.setB(DropRule.DROP);  // Make sure item are marked as dropped, and not in a non-existent grave

            InventoryComponent.dropItemIfToBeDropped(pair.getA(), pos.x, pos.y, pos.z, world);
        }
    }
    public abstract void clear();

    /**
     * Check if the component contains any items that should be placed in a grave (according to drop rules)
     * @return Whether the component contains any items that should be placed in a grave
     */
    public boolean containsGraveItems() {
        for (Tuple<ItemStack, DropRule> pair : this.getAsStackDropList()) {
            if (!pair.getA().isEmpty() && pair.getB() == DropRule.PUT_IN_GRAVE) return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return !this.containsAny(stack -> !stack.isEmpty());
    }

    /**
     * Check if the component contains at least one item that matches the predicate. Will stop at first match
     * @param predicate Predicate to test items against
     * @return Whether the component contains at least one item that matches the predicate
     */
    public boolean containsAny(Predicate<ItemStack> predicate) {
        for (Tuple<ItemStack, DropRule> pair : this.getAsStackDropList()) {
            if (predicate.test(pair.getA())) return true;
        }
        return false;
    }
    public void handleItemPairs(PairModificationConsumer modification) {
        for (Tuple<ItemStack, DropRule> pair : this.getAsStackDropList()) {
            modification.accept(pair.getA(), -1, pair);
        }
    }
    public abstract CompoundTag writeNbt(HolderLookup.Provider registries);
}
