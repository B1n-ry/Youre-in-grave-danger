package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropItemEvent;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

public class InventoryComponent {
    private final DefaultedList<ItemStack> items;
    private final int mainSize;
    private final int armorSize;
    private final int offHandSize;

    public InventoryComponent(ServerPlayerEntity player) {
        this.items = this.getInventoryItems(player);
        PlayerInventory inventory = player.getInventory();
        this.mainSize = inventory.main.size();
        this.armorSize = inventory.armor.size();
        this.offHandSize = inventory.offHand.size();
    }
    private InventoryComponent(DefaultedList<ItemStack> items, int mainSize, int armorSize, int offHandSize) {
        this.items = items;
        this.mainSize = mainSize;
        this.armorSize = armorSize;
        this.offHandSize = offHandSize;
    }

    private DefaultedList<ItemStack> getInventoryItems(ServerPlayerEntity player) {
        Inventory inventory = player.getInventory();

        DefaultedList<ItemStack> items = DefaultedList.ofSize(inventory.size());

        // Save all items in vanilla inventory
        for (int i = 0; i < inventory.size(); i++) {
            items.set(i, inventory.getStack(i));
        }

        return items;
    }

    public void onDeath(RespawnComponent respawnComponent, DeathContext context) {
        DefaultedList<ItemStack> soulboundItems = this.handleDropRules(this.items, context);

        respawnComponent.setSoulboundItems(soulboundItems);
    }

    /**
     * Handles the drop rule for each item in the list
     * @param items all items that should be filtered
     * @return new list with all items that should be kept
     */
    private DefaultedList<ItemStack> handleDropRules(DefaultedList<ItemStack> items, DeathContext context) {
        DefaultedList<ItemStack> soulboundList = DefaultedList.ofSize(items.size(), ItemStack.EMPTY);

        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            DropRule dropRule = DropRuleEvent.DESTROY_ITEM_EVENT.invoker().getDropRule(item, i, context);
            switch (dropRule) {
                case DESTROY -> items.set(i, ItemStack.EMPTY);
                case KEEP -> {
                    items.set(i, ItemStack.EMPTY);
                    soulboundList.set(i, item);
                }
            }
        }

        return soulboundList;
    }

    public void applyLoss() {

    }

    public void dropAll(ServerWorld world, Vec3d pos) {
        for (ItemStack stack : this.items) {
            if (stack.isEmpty()) continue;
            if (DropItemEvent.DROP_ITEM_EVENT.invoker().shouldDropItem(stack, pos.x, pos.y, pos.z, world))
                ItemScatterer.spawn(world, pos.x, pos.y, pos.z, stack);
        }
    }

    /**
     * Adds a new inventory component on top of the currently existing one
     * @param mergingComponent Inventory component to merge with. This will be added to existing component
     * @param isGraveInventory If this component is connected to the grave. Depending on this
     * @return All items that wouldn't fit in inventory
     */
    public DefaultedList<ItemStack> merge(InventoryComponent mergingComponent, boolean isGraveInventory) {
        YigdConfig config = YigdConfig.getConfig();
        DefaultedList<ItemStack> extraItems = DefaultedList.of();

        // Move curse of binding items from equipped in grave, so they can't get stuck to the player even after death
        if (isGraveInventory) {
            extraItems.addAll(this.pullBindingCurseItems());
        } else {
            extraItems.addAll(mergingComponent.pullBindingCurseItems());
        }

        for (int i = 0; i < mergingComponent.items.size(); i++) {
            // Make sure to only add items from respective section to correct group. E.g. no sword should end up in the chest-plate slot
            // This is here in case some mod thinks it's funny to have different people with different inventory sizes,
            // or if you add/remove a mod with different inventory size than vanilla (so the mod will adapt)
            int groupIndex;
            int currentComponentIndex;
            if (i < mergingComponent.mainSize) {
                groupIndex = i;
                currentComponentIndex = groupIndex < this.mainSize ? groupIndex : 0;
            } else if (i < mergingComponent.mainSize + mergingComponent.armorSize) {
                groupIndex = i - mergingComponent.mainSize;
                currentComponentIndex = groupIndex < this.armorSize ? groupIndex + this.mainSize : this.mainSize;
            } else if (i < mergingComponent.mainSize + mergingComponent.armorSize + mergingComponent.offHandSize) {
                groupIndex = i - (mergingComponent.mainSize + mergingComponent.armorSize);
                currentComponentIndex = groupIndex < this.offHandSize ? groupIndex + this.mainSize + this.offHandSize : this.mainSize + this.armorSize;
            } else {  // Should never be reached unless some mod adds to the inventory in some weird way
                groupIndex = i - (mergingComponent.mainSize + mergingComponent.armorSize + mergingComponent.offHandSize);
                currentComponentIndex = groupIndex + this.mainSize + this.offHandSize + this.offHandSize;
            }

            ItemStack mergingStack = mergingComponent.items.get(i);
            if (currentComponentIndex > this.items.size()) {
                extraItems.add(mergingStack);
                continue;
            }

            ItemStack currentStack = this.items.get(currentComponentIndex);
            if (config.graveConfig.mergeStacksOnRetrieve) {
                int combinationSlot = this.findMatchingStackSlot(mergingStack);
                if (combinationSlot != -1) {
                    this.mergeItemInSlot(mergingStack, combinationSlot);
                }
            }
            if (!mergingStack.isEmpty()) {  // Can be due to merging (count could be 0)
                if (currentStack.isEmpty()) {
                    this.items.set(currentComponentIndex, mergingStack);
                } else {
                    extraItems.add(mergingStack);
                }
            }
        }

        this.addStacksToMain(extraItems);

        return extraItems;
    }

    private int findMatchingStackSlot(ItemStack stack) {
        for (int i = 0; i < this.mainSize; i++) {
            ItemStack iStack = this.items.get(i);
            if (ItemStack.canCombine(stack, iStack) && iStack.isStackable() && iStack.getMaxCount() > iStack.getCount()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Do not call on this method if you can't merge the item in this slot
     * @param toMerge Item that should merge into a slot
     * @param slot Which slot the item should merge into
     */
    private void mergeItemInSlot(ItemStack toMerge, int slot) {
        ItemStack mergeTo = this.items.get(slot);
        int remaining = mergeTo.getMaxCount() - mergeTo.getCount();

        int ableToAdd = Math.min(toMerge.getCount(), remaining);
        mergeTo.increment(ableToAdd);
        toMerge.decrement(ableToAdd);
    }

    /**
     * Pull all curse of binding items from component (that would get stuck) and deletes them from component
     * @return all curse of binding items in armor slots
     */
    private DefaultedList<ItemStack> pullBindingCurseItems() {
        DefaultedList<ItemStack> bindingItems = DefaultedList.of();
        for (int i = 0; i < this.armorSize; i++) {
            ItemStack armorStack = this.items.get(this.mainSize + i);  // Current armor item
            if (EnchantmentHelper.hasBindingCurse(armorStack)) {
                bindingItems.add(armorStack);
                this.items.set(this.mainSize + i, ItemStack.EMPTY);  // Moving the item in  this slot
            }
        }
        return bindingItems;
    }

    private void addStacksToMain(DefaultedList<ItemStack> extraItems) {
        YigdConfig config = YigdConfig.getConfig();
        while (!extraItems.isEmpty()) {
            ItemStack stack = extraItems.get(0);

            int addToSlot = -1;
            if (config.graveConfig.mergeStacksOnRetrieve) {
                addToSlot = this.findMatchingStackSlot(stack);
            }
            if (addToSlot == -1) {
                addToSlot = this.findEmptySlot();
                if (addToSlot == -1) return;  // Inventory is full
            }
            ItemStack addToStack = this.items.get(addToSlot);
            if (addToStack.isEmpty()) {
                this.items.set(addToSlot, stack);
                extraItems.remove(0);
            } else {
                this.mergeItemInSlot(stack, addToSlot);
                if (stack.isEmpty()) {
                    extraItems.remove(0);
                }
            }
        }
    }

    private int findEmptySlot() {
        for (int i = 0; i < this.mainSize; i++) {
            if (this.items.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }


    public void applyToPlayer(ServerPlayerEntity player) {

    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtCompound vanillaInventoryNbt = Inventories.writeNbt(new NbtCompound(), this.items);
        vanillaInventoryNbt.putInt("size", this.items.size());
        vanillaInventoryNbt.putInt("mainSize", this.mainSize);
        vanillaInventoryNbt.putInt("armorSize", this.armorSize);
        vanillaInventoryNbt.putInt("offHandSize", this.offHandSize);

        nbt.put("vanilla", vanillaInventoryNbt);

        return nbt;
    }

    public static InventoryComponent fromNbt(NbtCompound nbt) {
        NbtCompound vanillaInvNbt = nbt.getCompound("vanilla");
        int listSize = vanillaInvNbt.getInt("size");
        DefaultedList<ItemStack> items = DefaultedList.ofSize(listSize, ItemStack.EMPTY);
        Inventories.readNbt(vanillaInvNbt, items);

        int mainSize = vanillaInvNbt.getInt("mainSize");
        int armorSize = vanillaInvNbt.getInt("armorSize");
        int offHandSize = vanillaInvNbt.getInt("offHandSize");

        return new InventoryComponent(items, mainSize, armorSize, offHandSize);
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        player.getInventory().clear();
    }
}
