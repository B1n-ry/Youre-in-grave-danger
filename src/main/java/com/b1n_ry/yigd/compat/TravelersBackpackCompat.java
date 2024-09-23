package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.YigdEvents;
import com.b1n_ry.yigd.util.DropRule;
import com.tiviacz.travelersbackpack.capability.AttachmentUtils;
import com.tiviacz.travelersbackpack.capability.ITravelersBackpack;
import com.tiviacz.travelersbackpack.config.TravelersBackpackConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Predicate;

public class TravelersBackpackCompat implements InvModCompat<Tuple<ItemStack, DropRule>> {
    public static boolean isAccessoriesIntegrationEnabled() {
        try {
            return TravelersBackpackConfig.SERVER.backpackSettings.accessoriesIntegration.get();
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
    public void clear(ServerPlayer player) {
        AttachmentUtils.getAttachment(player).ifPresent(ITravelersBackpack::removeWearable);
    }

    @Override
    public CompatComponent<Tuple<ItemStack, DropRule>> readNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        ItemStack stack = ItemStack.parse(registries, nbt).orElse(ItemStack.EMPTY);

        DropRule dropRule;
        if (nbt.contains("dropRule")) {
            dropRule = DropRule.valueOf(nbt.getString("dropRule"));
        } else {
            dropRule = YigdConfig.getConfig().compatConfig.defaultTravelersBackpackDropRule;
        }
        return new TBCompatComponent(new Tuple<>(stack, dropRule));
    }

    @Override
    public CompatComponent<Tuple<ItemStack, DropRule>> getNewComponent(ServerPlayer player) {
        return new TBCompatComponent(player);
    }

    private static class TBCompatComponent extends CompatComponent<Tuple<ItemStack, DropRule>> {

        public TBCompatComponent(ServerPlayer player) {
            super(player);
        }

        public TBCompatComponent(Tuple<ItemStack, DropRule> inventory) {
            super(inventory);
        }

        @Override
        public Tuple<ItemStack, DropRule> getInventory(ServerPlayer player) {
            DropRule defaultDropRule = YigdConfig.getConfig().compatConfig.defaultTravelersBackpackDropRule;
            ItemStack stack = AttachmentUtils.getWearingBackpack(player);
            return stack == null ? InventoryComponent.EMPTY_ITEM_PAIR : new Tuple<>(stack, defaultDropRule);
        }

        @Override
        public NonNullList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayer merger) {
            NonNullList<ItemStack> extraItems = NonNullList.create();

            @SuppressWarnings("unchecked")
            Tuple<ItemStack, DropRule> pair = (Tuple<ItemStack, DropRule>) mergingComponent.inventory;
            ItemStack mergingStack = pair.getA();
            ItemStack currentStack = this.inventory.getA();

            if (mergingStack.isEmpty()) return extraItems;

            if (!currentStack.isEmpty()) {
                extraItems.add(mergingStack);
                return extraItems;
            }

            this.inventory = new Tuple<>(mergingStack, pair.getB());
            return extraItems;
        }

        @Override
        public NonNullList<ItemStack> storeToPlayer(ServerPlayer player) {
            if (this.inventory.getA().isEmpty()) return NonNullList.create();

            AttachmentUtils.equipBackpack(player, this.inventory.getA());

            return NonNullList.create();
        }

        @Override
        public void handleDropRules(DeathContext context) {
            YigdConfig.CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

            DropRule dropRule = compatConfig.defaultTravelersBackpackDropRule;

            ItemStack stack = this.inventory.getA();
            if (stack.isEmpty()) return;

            if (dropRule == DropRule.PUT_IN_GRAVE)
                dropRule = NeoForge.EVENT_BUS.post(new YigdEvents.DropRuleEvent(stack, -1, context, true)).getDropRule();

            this.inventory.setB(dropRule);
        }

        @Override
        public NonNullList<Tuple<ItemStack, DropRule>> getAsStackDropList() {
            NonNullList<Tuple<ItemStack, DropRule>> stacks = NonNullList.create();
            stacks.add(this.inventory);
            return stacks;
        }

        @Override
        public CompatComponent<Tuple<ItemStack, DropRule>> filterInv(Predicate<DropRule> predicate) {
            Tuple<ItemStack, DropRule> pair;
            if (predicate.test(this.inventory.getB())) {
                pair = this.inventory;
            } else {
                pair = InventoryComponent.EMPTY_ITEM_PAIR;
            }
            return new TBCompatComponent(pair);
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            ItemStack stack = this.inventory.getA();
            if (predicate.test(stack)) {
                stack.shrink(itemCount);
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            this.inventory = InventoryComponent.EMPTY_ITEM_PAIR;
        }

        @Override
        public CompoundTag writeNbt(HolderLookup.Provider registries) {
            CompoundTag nbt = (CompoundTag) this.inventory.getA().save(registries);

            nbt.putString("dropRule", this.inventory.getB().name());
            return nbt;
        }
    }
}
