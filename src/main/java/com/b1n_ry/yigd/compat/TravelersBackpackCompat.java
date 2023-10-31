package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import com.tiviacz.travelersbackpack.component.ComponentUtils;
import com.tiviacz.travelersbackpack.component.ITravelersBackpackComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

public class TravelersBackpackCompat implements InvModCompat<ItemStack> {
    @Override
    public String getModName() {
        return "travelers backpack";
    }

    @Override
    public void clear(ServerPlayerEntity player) {
        ComponentUtils.getComponent(player).removeWearable();
    }

    @Override
    public CompatComponent<ItemStack> readNbt(NbtCompound nbt) {
        ItemStack stack = ItemStack.fromNbt(nbt);
        return new TBCompatComponent(stack);
    }

    @Override
    public CompatComponent<ItemStack> getNewComponent(ServerPlayerEntity player) {
        return new TBCompatComponent(player);
    }

    private static class TBCompatComponent extends CompatComponent<ItemStack> {

        public TBCompatComponent(ServerPlayerEntity player) {
            super(player);
        }

        public TBCompatComponent(ItemStack inventory) {
            super(inventory);
        }

        @Override
        public ItemStack getInventory(ServerPlayerEntity player) {
            ItemStack stack = ComponentUtils.getComponent(player).getWearable();
            return stack == null ? ItemStack.EMPTY : stack;
        }

        @Override
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            if (mergingComponent.isEmpty()) return extraItems;

            ItemStack mergingStack = (ItemStack) mergingComponent.inventory;
            if (!this.isEmpty()) {
                extraItems.add(mergingStack);
                return extraItems;
            }

            this.inventory = mergingStack;
            return extraItems;
        }

        @Override
        public DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player) {
            ITravelersBackpackComponent component = ComponentUtils.getComponent(player);

            component.setWearable(this.inventory);
            component.setContents(this.inventory);
            component.sync();
            component.syncToTracking(player);

            return DefaultedList.of();
        }

        @Override
        public CompatComponent<ItemStack> handleDropRules(DeathContext context) {
            DropRule dropRule = DropRuleEvent.EVENT.invoker().getDropRule(this.inventory, -1, context);
            TBCompatComponent soulboundComponent = new TBCompatComponent(dropRule == DropRule.KEEP ? this.inventory : ItemStack.EMPTY);

            Vec3d deathPos = context.getDeathPos();
            if (dropRule == DropRule.DROP)
                ItemScatterer.spawn(context.getWorld(), deathPos.x, deathPos.y, deathPos.z, this.inventory);

            if (dropRule != DropRule.PUT_IN_GRAVE)
                this.inventory = ItemStack.EMPTY;

            return soulboundComponent;
        }

        @Override
        public DefaultedList<ItemStack> getAsStackList() {
            DefaultedList<ItemStack> stacks = DefaultedList.of();
            stacks.add(this.inventory);
            return stacks;
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            if (predicate.test(this.inventory)) {
                this.inventory.decrement(itemCount);
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            this.inventory = ItemStack.EMPTY;
        }

        @Override
        public boolean isEmpty() {
            return this.inventory.isEmpty();
        }

        @Override
        public NbtCompound writeNbt() {
            return this.inventory.writeNbt(new NbtCompound());
        }
    }
}
