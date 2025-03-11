package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.CompatConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.data.GraveItem;
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

public class TravelersBackpackCompat implements InvModCompat<GraveItem> {
    public static boolean isTrinketIntegrationEnabled() {
        try {
            return TravelersBackpackConfig.getConfig().backpackSettings.trinketsIntegration;
        }
        catch (Exception e) {
            return false;
        }
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
    public CompatComponent<GraveItem> readNbt(NbtCompound nbt) {
        ItemStack stack = ItemStack.fromNbt(nbt);

        DropRule dropRule;
        if (nbt.contains("dropRule")) {
            dropRule = DropRule.valueOf(nbt.getString("dropRule"));
        } else {
            dropRule = YigdConfig.getConfig().compatConfig.defaultTravelersBackpackDropRule;
        }
        return new TBCompatComponent(new GraveItem(stack, dropRule));
    }

    @Override
    public CompatComponent<GraveItem> getNewComponent(ServerPlayerEntity player) {
        return new TBCompatComponent(player);
    }

    private static class TBCompatComponent extends CompatComponent<GraveItem> {

        public TBCompatComponent(ServerPlayerEntity player) {
            super(player);
        }

        public TBCompatComponent(GraveItem inventory) {
            super(inventory);
        }

        @Override
        public GraveItem getInventory(ServerPlayerEntity player) {
            DropRule defaultDropRule = YigdConfig.getConfig().compatConfig.defaultTravelersBackpackDropRule;
            ItemStack stack = ComponentUtils.getComponent(player).getWearable();
            return stack == null ? InventoryComponent.EMPTY_GRAVE_ITEM : new GraveItem(stack, defaultDropRule);
        }

        @Override
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayerEntity merger) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            @SuppressWarnings("unchecked")
            Pair<ItemStack, DropRule> pair = (Pair<ItemStack, DropRule>) mergingComponent.inventory;
            ItemStack mergingStack = pair.getLeft();
            ItemStack currentStack = this.inventory.stack;

            if (mergingStack.isEmpty()) return extraItems;

            if (!currentStack.isEmpty()) {
                extraItems.add(mergingStack);
                return extraItems;
            }

            this.inventory = new GraveItem(mergingStack, pair.getRight());
            return extraItems;
        }

        @Override
        public DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player) {
            if (this.inventory.stack.isEmpty()) return DefaultedList.of();

            ComponentUtils.equipBackpack(player, this.inventory.stack.copy());

            return DefaultedList.of();
        }

        @Override
        public void handleDropRules(DeathContext context) {
            CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

            DropRule dropRule = compatConfig.defaultTravelersBackpackDropRule;

            ItemStack stack = this.inventory.stack;
            if (stack.isEmpty()) return;

            if (dropRule == DropRule.PUT_IN_GRAVE)
                dropRule = DropRuleEvent.EVENT.invoker().getDropRule(stack, -1, context, true);

            this.inventory.dropRule = dropRule;
        }

        @Override
        public DefaultedList<GraveItem> getAsGraveItemList() {
            DefaultedList<GraveItem> stacks = DefaultedList.of();
            stacks.add(this.inventory);
            return stacks;
        }

        @Override
        public CompatComponent<GraveItem> filterInv(Predicate<DropRule> predicate) {
            GraveItem graveItem;
            if (predicate.test(this.inventory.dropRule)) {
                graveItem = this.inventory;
            } else {
                graveItem = InventoryComponent.EMPTY_GRAVE_ITEM;
            }
            return new TBCompatComponent(graveItem);
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            ItemStack stack = this.inventory.stack;
            if (predicate.test(stack)) {
                stack.decrement(itemCount);
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            this.inventory = InventoryComponent.EMPTY_GRAVE_ITEM;
        }

        @Override
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();
            this.inventory.stack.writeNbt(nbt);

            nbt.putString("dropRule", this.inventory.dropRule.name());
            return nbt;
        }
    }
}
