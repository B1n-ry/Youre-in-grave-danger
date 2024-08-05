package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import com.beansgalaxy.backpacks.platform.FabricCompatHelper;
import com.beansgalaxy.backpacks.platform.services.CompatHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;
import java.util.function.Predicate;

public class BeansBackpacksCompat implements InvModCompat<BeansBackpacksCompat.BeansBackpackInv> {
    public BeansBackpacksCompat() {
        FabricCompatHelper.OnDeathCallback.EVENT.register(FabricCompatHelper.Context::cancel);
    }

    public static void prepForTrinkets() {
        FabricCompatHelper.OnDeathCallback.EVENT.register(FabricCompatHelper.Context::cancel);
    }

    @Override
    public String getModName() {
        return "beansbackpacks";
    }

    @Override
    public void clear(ServerPlayerEntity player) {
        CompatHelper.setBackStack(player, ItemStack.EMPTY);
        CompatHelper.getBackpackInventory(player).clear();
    }

    @Override
    public CompatComponent<BeansBackpackInv> readNbt(NbtCompound nbt) {
        ItemStack stack = ItemStack.fromNbt(nbt.getCompound("stack"));
        DropRule rule = DropRule.valueOf(nbt.getString("dropRule"));
        NbtCompound backpackContentsNbt = nbt.getCompound("backpackContents");
        UUID ownerId = nbt.getUuid("ownerId");
        float yaw = nbt.getFloat("yaw");
        Direction direction = Direction.byId(nbt.getInt("direction"));

        DefaultedList<ItemStack> backpackContents = InventoryComponent.listFromNbt(backpackContentsNbt, ItemStack::fromNbt, ItemStack.EMPTY);

        BeansBackpackInv inv = new BeansBackpackInv(stack, rule, backpackContents, ownerId, yaw, direction);
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
        private final UUID ownerId;

        private final float yaw;
        private final Direction direction;

        public BeansBackpackInv(ItemStack stack, DropRule dropRule, DefaultedList<ItemStack> backpackContents,
                                UUID ownerId, float yaw, Direction direction) {
            this.stack = stack;
            this.dropRule = dropRule;
            this.backpackContents = backpackContents;
            this.ownerId = ownerId;

            // less important values, but they're cool I guess
            this.yaw = yaw;
            this.direction = direction;
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
            ItemStack stack = CompatHelper.getBackStack(player).copy();
            DefaultedList<ItemStack> backpackContents = CompatHelper.getBackpackInventory(player);
            DefaultedList<ItemStack> copied = DefaultedList.of();
            for (ItemStack i : backpackContents) {
                copied.add(i.copy());
            }

            return new BeansBackpackInv(stack, DropRule.PUT_IN_GRAVE, copied, player.getUuid(),
                    player.headYaw, player.getHorizontalFacing());
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

            CompatHelper.setBackStack(player, backpack);

            DefaultedList<ItemStack> backpackInventory = CompatHelper.getBackpackInventory(player);
            backpackInventory.clear();
            for (ItemStack stack : backpackContents) {
                if (!stack.isEmpty()) {
                    backpackInventory.add(stack);
                }
            }

            CompatHelper.updateBackpackInventory2C(player);

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
            nbt.putUuid("ownerId", this.inventory.ownerId);

            nbt.putFloat("yaw", this.inventory.yaw);
            nbt.putInt("direction", this.inventory.direction.getId());

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

        @Override
        public void dropItems(ServerWorld world, Vec3d pos) {
            CompatHelper.createBackpackEntity(this.inventory.getBackpack(), (int) pos.x, (int) pos.y, (int) pos.z,
                    this.inventory.yaw, true, this.inventory.direction, world,
                    this.inventory.ownerId, this.inventory.getBackpackContents());
        }
        @Override
        public void dropGraveItems(ServerWorld world, Vec3d pos) {
            if (this.inventory.getDropRule() == DropRule.KEEP || this.inventory.getDropRule() == DropRule.DESTROY) return;

            this.inventory.setDropRule(DropRule.DROP);
            CompatHelper.createBackpackEntity(this.inventory.getBackpack(), (int) pos.x, (int) pos.y, (int) pos.z,
                    this.inventory.yaw, true, this.inventory.direction, world,
                    this.inventory.ownerId, this.inventory.getBackpackContents());
        }

        @Override
        public CompatComponent<BeansBackpackInv> filterInv(Predicate<DropRule> predicate) {
            if (predicate.test(this.inventory.getDropRule())) {
                return new BeansBackpacksComponent(this.inventory);
            } else {
                return new BeansBackpacksComponent(new BeansBackpackInv(ItemStack.EMPTY, DropRule.PUT_IN_GRAVE,
                        DefaultedList.of(), this.inventory.ownerId, this.inventory.yaw, this.inventory.direction));
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
