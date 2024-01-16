package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import com.tiviacz.travelersbackpack.component.ComponentUtils;
import com.tiviacz.travelersbackpack.component.ITravelersBackpackComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

public class TravelersBackpackCompat implements InvModCompat<Pair<ItemStack, DropRule>> {
    @Override
    public String getModName() {
        return "travelers backpack";
    }

    @Override
    public void clear(ServerPlayerEntity player) {
        ComponentUtils.getComponent(player).removeWearable();
    }

    @Override
    public CompatComponent<Pair<ItemStack, DropRule>> readNbt(NbtCompound nbt) {
        ItemStack stack = ItemStack.fromNbt(nbt);

        DropRule dropRule;
        if (nbt.contains("dropRule")) {
            dropRule = DropRule.valueOf(nbt.getString("dropRule"));
        } else {
            dropRule = YigdConfig.getConfig().compatConfig.defaultTravelersBackpackDropRule;
        }
        return new TBCompatComponent(new Pair<>(stack, dropRule));
    }

    @Override
    public CompatComponent<Pair<ItemStack, DropRule>> getNewComponent(ServerPlayerEntity player) {
        return new TBCompatComponent(player);
    }

    private static class TBCompatComponent extends CompatComponent<Pair<ItemStack, DropRule>> {

        public TBCompatComponent(ServerPlayerEntity player) {
            super(player);
        }

        public TBCompatComponent(Pair<ItemStack, DropRule> inventory) {
            super(inventory);
        }

        @Override
        public Pair<ItemStack, DropRule> getInventory(ServerPlayerEntity player) {
            DropRule defaultDropRule = YigdConfig.getConfig().compatConfig.defaultTravelersBackpackDropRule;
            ItemStack stack = ComponentUtils.getComponent(player).getWearable();
            return stack == null ? InventoryComponent.EMPTY_ITEM_PAIR : new Pair<>(stack, defaultDropRule);
        }

        @Override
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            if (mergingComponent.isEmpty()) return extraItems;

            @SuppressWarnings("unchecked")
            Pair<ItemStack, DropRule> pair = (Pair<ItemStack, DropRule>) mergingComponent.inventory;
            ItemStack mergingStack = pair.getLeft();  // Solves the case where the merging component is the same as this component

            if (!this.isEmpty()) {
                extraItems.add(mergingStack);
                return extraItems;
            }

            this.inventory.setLeft(mergingStack);
            return extraItems;
        }

        @Override
        public DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player) {
            if (this.inventory.getLeft().isEmpty()) return DefaultedList.of();

            ITravelersBackpackComponent component = ComponentUtils.getComponent(player);

            component.setWearable(this.inventory.getLeft());
            component.setContents(this.inventory.getLeft());
            component.sync();

            return DefaultedList.of();
        }

        @Override
        public void handleDropRules(DeathContext context) {
            YigdConfig.CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

            DropRule dropRule = compatConfig.defaultTravelersBackpackDropRule;

            ItemStack stack = this.inventory.getLeft();

            if (dropRule == DropRule.PUT_IN_GRAVE)
                dropRule = DropRuleEvent.EVENT.invoker().getDropRule(stack, -1, context, true);

            TBCompatComponent soulboundComponent = new TBCompatComponent(dropRule == DropRule.KEEP ? this.inventory : InventoryComponent.EMPTY_ITEM_PAIR);

            Vec3d deathPos = context.deathPos();
            if (dropRule == DropRule.DROP)
                InventoryComponent.dropItemIfToBeDropped(stack, deathPos.x, deathPos.y, deathPos.z, context.world());

        }

        @Override
        public DefaultedList<ItemStack> getAsStackList() {
            DefaultedList<ItemStack> stacks = DefaultedList.of();
            stacks.add(this.inventory.getLeft());
            return stacks;
        }

        @Override
        public CompatComponent<Pair<ItemStack, DropRule>> filterInv(Predicate<DropRule> predicate) {
            Pair<ItemStack, DropRule> pair;
            if (predicate.test(this.inventory.getRight())) {
                pair = this.inventory;
            } else {
                pair = InventoryComponent.EMPTY_ITEM_PAIR;
            }
            return new TBCompatComponent(pair);
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            ItemStack stack = this.inventory.getLeft();
            if (predicate.test(stack)) {
                stack.decrement(itemCount);
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            this.inventory.setLeft(ItemStack.EMPTY);
        }

        @Override
        public boolean isEmpty() {
            return this.inventory.getLeft().isEmpty();
        }

        @Override
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();
            this.inventory.getLeft().writeNbt(nbt);

            nbt.putString("dropRule", this.inventory.getRight().name());
            return nbt;
        }
    }
}
