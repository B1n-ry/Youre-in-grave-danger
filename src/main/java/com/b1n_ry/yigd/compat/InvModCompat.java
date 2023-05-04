package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.data.DeathContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

public interface InvModCompat<T> {
    String getModName();
    T getInventory(ServerPlayerEntity player);

    /**
     * Stores the mod inventory to the player
     * @param player The player who should receive the items or whatever the inventory contains
     * @param inventory The inventory to be received
     * @return all items that wouldn't fit in the inventory
     */
    DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player, T inventory);

    /**
     * Handle things like soulbound and curse of vanishing
     * @param inventory The inventory to handle
     * @param context Context to the death
     * @return Inventory containing all items that are applied with soulbound
     */
    T handleDropRules(T inventory, DeathContext context);
    DefaultedList<ItemStack> getAsStackList(T inventory);
    default void dropItems(T inventory, ServerWorld world, Vec3d pos) {
        DefaultedList<ItemStack> stacks = this.getAsStackList(inventory);
        for (ItemStack stack : stacks) {
            ItemScatterer.spawn(world, pos.x, pos.y, pos.z, stack);
        }
    }
    void clear(ServerPlayerEntity player);

    NbtCompound writeNbt(T inventory);
    T readNbt(NbtCompound nbt);
}
