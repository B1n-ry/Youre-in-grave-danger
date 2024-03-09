package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import com.beansgalaxy.backpacks.core.BackData;
import com.beansgalaxy.backpacks.platform.Services;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

public class BeansBackpacksCompat implements InvModCompat<BeansBackpacksCompat.BeansBackpackInv> {
    @Override
    public String getModName() {
        return "beansbackpacks";
    }

    @Override
    public void clear(ServerPlayerEntity player) {
        BackData backData = BackData.get(player);

        backData.set(ItemStack.EMPTY);
        backData.backpackInventory.clear();
    }

    @Override
    public CompatComponent<BeansBackpackInv> readNbt(NbtCompound nbt) {
        ItemStack stack = ItemStack.fromNbt(nbt.getCompound("stack"));
        DropRule rule = DropRule.valueOf(nbt.getString("dropRule"));
        NbtCompound backpackContentsNbt = nbt.getCompound("backpackContents");

        DefaultedList<ItemStack> backpackContents = InventoryComponent.listFromNbt(backpackContentsNbt, ItemStack::fromNbt, ItemStack.EMPTY);

        BeansBackpackInv inv = new BeansBackpackInv(stack, rule, backpackContents);
        return new BeansBackpacksComponent(inv);
    }

    @Override
    public CompatComponent<BeansBackpackInv> getNewComponent(ServerPlayerEntity player) {
        return new BeansBackpacksComponent(player);
    }

    public static class BeansBackpackInv {
        private ItemStack stack;
        private DropRule dropRule;
        private DefaultedList<ItemStack> backpackContents;

        public BeansBackpackInv(ItemStack stack, DropRule dropRule, DefaultedList<ItemStack> backpackContents) {
            this.stack = stack;
            this.dropRule = dropRule;
            this.backpackContents = backpackContents;
        }
        public ItemStack getBackpack() {
            return this.stack;
        }
        public DropRule getDropRule() {
            return this.dropRule;
        }
        public DefaultedList<ItemStack> getBackpackContents() {
            return this.backpackContents;
        }
        public void setBackpack(ItemStack stack) {
            this.stack = stack;
        }
        public void setDropRule(DropRule dropRule) {
            this.dropRule = dropRule;
        }
        public void setBackpackContents(DefaultedList<ItemStack> backpackContents) {
            this.backpackContents = backpackContents;
        }
    }

    private static class BeansBackpacksComponent extends CompatComponent<BeansBackpackInv> {

        public BeansBackpacksComponent(ServerPlayerEntity player) {
            super(player);
        }
        public BeansBackpacksComponent(BeansBackpackInv inventory) {
            super(inventory);
        }

        @Override
        public BeansBackpackInv getInventory(ServerPlayerEntity player) {
            BackData backData = BackData.get(player);
            ItemStack stack = backData.getStack().copy();
            DefaultedList<ItemStack> backpackContents = DefaultedList.of();
            for (ItemStack item : backData.backpackInventory.getItemStacks()) {
                backpackContents.add(item.copy());
            }
            return new BeansBackpackInv(stack, DropRule.PUT_IN_GRAVE, backpackContents);
        }

        @Override
        public DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            ItemStack backpack = this.inventory.getBackpack();
            DefaultedList<ItemStack> backpackContents = this.inventory.getBackpackContents();
            if (backpack.isEmpty()) {
                for (ItemStack extra : backpackContents) {
                    if (!extra.isEmpty())
                        extraItems.add(extra);
                }
                return extraItems;
            }

            BackData backData = BackData.get(player);
            backData.set(backpack);

            backData.backpackInventory.clear();
            for (ItemStack stack : backpackContents) {
                if (!stack.isEmpty()) {
                    ItemStack leftOver = backData.backpackInventory.insertItemSilent(stack, stack.getCount());
                    if (!leftOver.isEmpty()) {
                        extraItems.add(leftOver);
                    }
                }
            }

            Services.NETWORK.backpackInventory2C(player);

            return extraItems;
        }

        @Override
        public void handleDropRules(DeathContext context) {
            YigdConfig.CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;
            DropRule defaultDropRule = compatConfig.defaultBeansBackpacksDropRule;

            if (this.inventory.getBackpack().isEmpty()) {
                this.inventory.setDropRule(defaultDropRule);
                return;
            }

            if (defaultDropRule == DropRule.PUT_IN_GRAVE)
                this.inventory.setDropRule(DropRuleEvent.EVENT.invoker().getDropRule(this.inventory.getBackpack(), -1, context, true));
        }

        @Override
        public DefaultedList<Pair<ItemStack, DropRule>> getAsStackDropList() {
            DropRule dropRule = this.inventory.getDropRule();
            DefaultedList<Pair<ItemStack, DropRule>> stacks = DefaultedList.of();

            stacks.add(new Pair<>(this.inventory.getBackpack(), dropRule));
            for (ItemStack stack : this.inventory.getBackpackContents()) {
                stacks.add(new Pair<>(stack, dropRule));
            }
            return stacks;
        }

        @Override
        public void clear() {
            this.inventory.setBackpack(ItemStack.EMPTY);
            this.inventory.getBackpackContents().clear();
        }

        @Override
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.put("stack", this.inventory.stack.writeNbt(new NbtCompound()));
            nbt.putString("dropRule", this.inventory.dropRule.name());

            NbtCompound backpackContentsNbt = InventoryComponent.listToNbt(this.inventory.getBackpackContents(), itemStack -> itemStack.writeNbt(new NbtCompound()), ItemStack::isEmpty);
            nbt.put("backpackContents", backpackContentsNbt);
            return nbt;
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            for (ItemStack stack : this.inventory.getBackpackContents()) {
                if (predicate.test(stack)) {
                    stack.decrement(itemCount);
                    return true;
                }
            }
            return false;
        }

        // TODO: If it becomes possible, implement drop to drop the backpack entities
        @Override
        public void dropItems(ServerWorld world, Vec3d pos) {
            super.dropItems(world, pos);
        }
        @Override
        public void dropGraveItems(ServerWorld world, Vec3d pos) {
            super.dropGraveItems(world, pos);
        }

        @Override
        public CompatComponent<BeansBackpackInv> filterInv(Predicate<DropRule> predicate) {
            if (predicate.test(this.inventory.getDropRule())) {
                return new BeansBackpacksComponent(this.inventory);
            } else {
                return new BeansBackpacksComponent(new BeansBackpackInv(ItemStack.EMPTY, DropRule.PUT_IN_GRAVE, DefaultedList.of()));
            }
        }

        @Override
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayerEntity merger) {
            DefaultedList<ItemStack> extraItems = DefaultedList.of();

            BeansBackpackInv mergingInventory = (BeansBackpackInv) mergingComponent.inventory;
            if (this.inventory.getBackpack().isEmpty()) {
                for (ItemStack withoutBackpack : this.inventory.getBackpackContents()) {
                    if (!withoutBackpack.isEmpty())
                        extraItems.add(withoutBackpack);
                }
                this.inventory.setBackpack(mergingInventory.getBackpack());
                this.inventory.setDropRule(mergingInventory.getDropRule());
                this.inventory.setBackpackContents(mergingInventory.getBackpackContents());
            } else {
                extraItems.add(mergingInventory.getBackpack());
                for (ItemStack stack : mergingInventory.getBackpackContents()) {
                    if (!stack.isEmpty()) {
                        extraItems.add(stack);
                    }
                }
            }

            return extraItems;
        }
    }
}
