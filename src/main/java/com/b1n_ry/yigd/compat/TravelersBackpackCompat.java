package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import com.tiviacz.travelersbackpack.component.ComponentUtils;
import com.tiviacz.travelersbackpack.config.TravelersBackpackConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;

import java.util.function.Predicate;

public class TravelersBackpackCompat implements InvModCompat<Pair<ItemStack, DropRule>> {
    public static boolean isTrinketIntegrationEnabled() {
        return TravelersBackpackConfig.trinketsIntegration;
    }

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
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayerEntity merger) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            @SuppressWarnings("unchecked")
            Pair<ItemStack, DropRule> pair = (Pair<ItemStack, DropRule>) mergingComponent.inventory;
            ItemStack mergingStack = pair.getLeft();
            ItemStack currentStack = this.inventory.getLeft();

            if (mergingStack.isEmpty()) return extraItems;

            if (!currentStack.isEmpty()) {
                extraItems.add(mergingStack);
                return extraItems;
            }

            this.inventory = new Pair<>(mergingStack, pair.getRight());
            return extraItems;
        }

        @Override
        public DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player) {
            if (this.inventory.getLeft().isEmpty()) return DefaultedList.of();

            ComponentUtils.equipBackpack(player, this.inventory.getLeft());

            return DefaultedList.of();
        }

        @Override
        public void handleDropRules(DeathContext context) {
            YigdConfig.CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

            DropRule dropRule = compatConfig.defaultTravelersBackpackDropRule;

            ItemStack stack = this.inventory.getLeft();
            if (stack.isEmpty()) return;

            if (dropRule == DropRule.PUT_IN_GRAVE)
                dropRule = DropRuleEvent.EVENT.invoker().getDropRule(stack, -1, context, true);

            this.inventory.setRight(dropRule);
        }

        @Override
        public DefaultedList<Pair<ItemStack, DropRule>> getAsStackDropList() {
            DefaultedList<Pair<ItemStack, DropRule>> stacks = DefaultedList.of();
            stacks.add(this.inventory);
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
            this.inventory = InventoryComponent.EMPTY_ITEM_PAIR;
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
