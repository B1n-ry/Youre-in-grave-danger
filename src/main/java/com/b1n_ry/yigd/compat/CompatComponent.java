package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.data.GraveItem;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.util.GraveItemModificationConsumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    public abstract NonNullList<GraveItem> merge(CompatComponent<?> mergingComponent, ServerPlayer merger);
    public abstract NonNullList<ItemStack> storeToPlayer(ServerPlayer player);

    /**
     * Handle drop rules for each item or whatever the component holds
     *
     * @param context How the player died
     */
    public abstract void handleDropRules(DeathContext context);

    /**
     * Get all items as a {@link NonNullList<GraveItem>} of {@link GraveItem} containing {@link ItemStack} and {@link DropRule} in the component.
     * The drop rule refers to what drop rule was/will be applied on death.
     * @return GraveItems containing all items in the component <b>INCLUDING EMPTY ITEMS</b>
     */
    public abstract NonNullList<GraveItem> getAsGraveItemList();
    public abstract CompatComponent<T> filterInv(Predicate<DropRule> predicate);
    public abstract boolean removeItem(Predicate<ItemStack> predicate, int itemCount);

    /**
     * Drop all items in the component to the world
     * @param world The world to drop items in
     * @param pos The position to drop items at
     */
    public void dropItems(ServerLevel world, Vec3 pos) {
        NonNullList<GraveItem> items = this.getAsGraveItemList();
        for (GraveItem graveItem : items) {
            ItemStack stack = graveItem.stack;
            if (stack.isEmpty()) continue;

            InventoryComponent.dropItemIfToBeDropped(graveItem.stack, pos.x, pos.y, pos.z, world);
        }
    }

    /**
     * Drop items in the component to the world, but only items that should be placed in a grave or dropped anyway
     * @param world The world to drop items in
     * @param pos The position to drop items at
     */
    public void dropGraveItems(ServerLevel world, Vec3 pos) {
        NonNullList<GraveItem> items = this.getAsGraveItemList();
        for (GraveItem graveItem : items) {
            ItemStack stack = graveItem.stack;
            if (stack.isEmpty() || graveItem.dropRule == DropRule.KEEP || graveItem.dropRule == DropRule.DESTROY) continue;
            graveItem.dropRule = DropRule.DROP;  // Make sure item are marked as dropped, and not in a non-existent grave

            InventoryComponent.dropItemIfToBeDropped(graveItem.stack, pos.x, pos.y, pos.z, world);
        }
    }
    public abstract void clear();

    /**
     * Check if the component contains any items that should be placed in a grave (according to drop rules)
     * @return Whether the component contains any items that should be placed in a grave
     */
    public boolean containsGraveItems() {
        for (GraveItem graveItem : this.getAsGraveItemList()) {
            if (!graveItem.stack.isEmpty() && graveItem.dropRule == DropRule.PUT_IN_GRAVE) return true;
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
        for (GraveItem graveItem : this.getAsGraveItemList()) {
            if (predicate.test(graveItem.stack)) return true;
        }
        return false;
    }
    public void handleGraveItems(GraveItemModificationConsumer modification) {
        for (GraveItem graveItem : this.getAsGraveItemList()) {
            modification.accept(graveItem.stack, -1, graveItem);
        }
    }
    public abstract CompoundTag writeNbt(HolderLookup.Provider registries);
}
