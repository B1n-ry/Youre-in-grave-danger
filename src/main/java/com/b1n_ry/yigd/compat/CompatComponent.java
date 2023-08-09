package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropItemEvent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
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
    public abstract DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent);
    public abstract DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player);
    public abstract CompatComponent<T> handleDropRules(DeathContext context);
    public abstract DefaultedList<ItemStack> getAsStackList();
    public abstract boolean removeItem(Predicate<ItemStack> predicate, int itemCount);
    public void dropItems(ServerWorld world, Vec3d pos) {
        DefaultedList<ItemStack> items = this.getAsStackList();
        for (ItemStack stack : items) {
            if (DropItemEvent.EVENT.invoker().shouldDropItem(stack, pos.x, pos.y, pos.z, world))
                ItemScatterer.spawn(world, pos.x, pos.y, pos.z, stack);
        }
    }
    public abstract void clear();
    public abstract boolean isEmpty();
    public abstract NbtCompound writeNbt();
}
