package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.CompatConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.data.GraveItem;
import com.b1n_ry.yigd.events.YigdEvents;
import com.b1n_ry.yigd.util.DropRule;
import com.tiviacz.travelersbackpack.TravelersBackpack;
import com.tiviacz.travelersbackpack.capability.AttachmentUtils;
import com.tiviacz.travelersbackpack.capability.ITravelersBackpack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Predicate;

public class TravelersBackpackCompat implements InvModCompat<GraveItem> {
    public static boolean isIntegrationEnabled() {
        try {
            return TravelersBackpack.enableIntegration();
        }
        catch (Exception | Error e) {
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
    public CompatComponent<GraveItem> readNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        ItemStack stack = ItemStack.parse(registries, nbt).orElse(ItemStack.EMPTY);

        DropRule dropRule;
        if (nbt.contains("dropRule")) {
            dropRule = DropRule.valueOf(nbt.getString("dropRule"));
        } else {
            dropRule = YigdConfig.getConfig().compatConfig.defaultTravelersBackpackDropRule;
        }
        return new TBCompatComponent(new GraveItem(stack, dropRule));
    }

    @Override
    public CompatComponent<GraveItem> getNewComponent(ServerPlayer player) {
        return new TBCompatComponent(player);
    }

    private static class TBCompatComponent extends CompatComponent<GraveItem> {

        public TBCompatComponent(ServerPlayer player) {
            super(player);
        }

        public TBCompatComponent(GraveItem inventory) {
            super(inventory);
        }

        @Override
        public GraveItem getInventory(ServerPlayer player) {
            DropRule defaultDropRule = YigdConfig.getConfig().compatConfig.defaultTravelersBackpackDropRule;
            ItemStack stack = AttachmentUtils.getWearingBackpack(player);
            return stack == null ? InventoryComponent.EMPTY_GRAVE_ITEM : new GraveItem(stack, defaultDropRule);
        }

        @Override
        public NonNullList<GraveItem> merge(CompatComponent<?> mergingComponent, ServerPlayer merger) {
            NonNullList<GraveItem> extraItems = NonNullList.create();

            GraveItem graveItem = (GraveItem) mergingComponent.inventory;
            ItemStack mergingStack = graveItem.stack;
            ItemStack currentStack = this.inventory.stack;

            if (mergingStack.isEmpty()) return extraItems;

            if (!currentStack.isEmpty()) {
                extraItems.add(graveItem);
                return extraItems;
            }

            this.inventory = new GraveItem(mergingStack, graveItem.dropRule);
            return extraItems;
        }

        @Override
        public NonNullList<ItemStack> storeToPlayer(ServerPlayer player) {
            if (this.inventory.stack.isEmpty()) return NonNullList.create();

            AttachmentUtils.equipBackpack(player, this.inventory.stack.copy());

            return NonNullList.create();
        }

        @Override
        public void handleDropRules(DeathContext context) {
            CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

            DropRule dropRule = compatConfig.defaultTravelersBackpackDropRule;

            ItemStack stack = this.inventory.stack;
            if (stack.isEmpty()) return;

            if (dropRule == DropRule.PUT_IN_GRAVE)
                dropRule = NeoForge.EVENT_BUS.post(new YigdEvents.DropRuleEvent(stack, -1, context, true)).getDropRule();

            this.inventory.dropRule = dropRule;
        }

        @Override
        public NonNullList<GraveItem> getAsGraveItemList() {
            NonNullList<GraveItem> stacks = NonNullList.create();
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
                stack.shrink(itemCount);
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            this.inventory = InventoryComponent.EMPTY_GRAVE_ITEM;
        }

        @Override
        public CompoundTag writeNbt(HolderLookup.Provider registries) {
            CompoundTag nbt = (CompoundTag) this.inventory.stack.save(registries);

            nbt.putString("dropRule", this.inventory.dropRule.name());
            return nbt;
        }
    }
}
