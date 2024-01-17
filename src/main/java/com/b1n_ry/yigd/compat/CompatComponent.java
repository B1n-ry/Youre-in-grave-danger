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
     * Component should be filtered based on drop rule before calling this, so different drop rules on items does not matter in this method
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
     * Get all items as a {@link DefaultedList<Pair>} of {@link Pair<>} containing {@link ItemStack} and {@link DropRule} in the component
     * The drop rule refers to what drop rule was/will be applied on death
     * @return Pairs containing all items in the component <b>INCLUDING EMPTY ITEMS</b>
     */
    public abstract DefaultedList<Pair<ItemStack, DropRule>> getAsStackDropList();
    public abstract CompatComponent<T> filterInv(Predicate<DropRule> predicate);
    public abstract boolean removeItem(Predicate<ItemStack> predicate, int itemCount);
    public void dropItems(ServerWorld world, Vec3d pos) {
        DefaultedList<Pair<ItemStack, DropRule>> items = this.getAsStackDropList();
        for (Pair<ItemStack, DropRule> pair : items) {
            InventoryComponent.dropItemIfToBeDropped(pair.getLeft(), pos.x, pos.y, pos.z, world);
        }
    }
    public abstract void clear();

    /**
     * Check if the component contains any items that should be placed in a grave (according to drop rules)
     * @return Whether the component contains any items that should be placed in a grave
     */
    public abstract boolean containsGraveItems();
    public abstract NbtCompound writeNbt();
}
