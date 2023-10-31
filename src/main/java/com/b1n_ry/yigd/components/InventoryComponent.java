package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.compat.CompatComponent;
import com.b1n_ry.yigd.compat.InvModCompat;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropItemEvent;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.util.YigdTags;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.function.Predicate;

public class InventoryComponent {
    private final DefaultedList<ItemStack> items;
    private final Map<String, CompatComponent<?>> modInventoryItems;
    public final int mainSize;
    public final int armorSize;
    public final int offHandSize;

    public InventoryComponent(ServerPlayerEntity player) {
        // Avoiding list being immutable in case items should be added on death
        this.items = DefaultedList.of();
        this.items.addAll(this.getInventoryItems(player));

        this.modInventoryItems = this.getModInventoryItems(player);

        PlayerInventory inventory = player.getInventory();
        this.mainSize = inventory.main.size();
        this.armorSize = inventory.armor.size();
        this.offHandSize = inventory.offHand.size();
    }
    private InventoryComponent(DefaultedList<ItemStack> items, Map<String, CompatComponent<?>> modInventoryItems, int mainSize, int armorSize, int offHandSize) {
        // Avoiding list being immutable in case items should be added on death
        this.items = DefaultedList.of();
        this.items.addAll(items);

        this.modInventoryItems = modInventoryItems;
        this.mainSize = mainSize;
        this.armorSize = armorSize;
        this.offHandSize = offHandSize;
    }

    public DefaultedList<ItemStack> getItems() {
        return this.items;
    }
    public DefaultedList<ItemStack> getAllExtraItems(boolean withoutEmpty) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();

        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            stacks.addAll(compatComponent.getAsStackList());
        }

        if (withoutEmpty) {
            stacks.removeIf(ItemStack::isEmpty);
        }

        return stacks;
    }
    public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
        predicate = predicate.and(stack -> stack.getCount() >= itemCount);

        for (ItemStack stack : this.items) {
            if (predicate.test(stack)) {
                stack.decrement(itemCount);
                return true;
            }
        }
        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            if (compatComponent.removeItem(predicate, itemCount))
                return true;
        }

        return false;
    }
    private DefaultedList<ItemStack> getInventoryItems(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();

        DefaultedList<ItemStack> items = DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY);

        // Save all items in vanilla inventory
        for (int i = 0; i < inventory.size(); i++) {
            items.set(i, inventory.getStack(i));
        }

        return items;
    }
    private Map<String, CompatComponent<?>> getModInventoryItems(ServerPlayerEntity player) {
        Map<String, CompatComponent<?>> modInventories = new HashMap<>();

        for (InvModCompat<?> compatMod : InvModCompat.invCompatMods) {
            modInventories.put(compatMod.getModName(), compatMod.getNewComponent(player));
        }

        return modInventories;
    }

    public void onDeath(RespawnComponent respawnComponent, DeathContext context) {
        YigdConfig config = YigdConfig.getConfig();
        if (config.inventoryConfig.dropPlayerHead) {
            ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);
            NbtCompound profileNbt = NbtHelper.writeGameProfile(new NbtCompound(), context.getPlayer().getGameProfile());
            playerHead.setSubNbt("SkullOwner", profileNbt);
            this.items.add(playerHead);
        }

        InventoryComponent soulboundInventory = this.handleDropRules(context);

        respawnComponent.setSoulboundInventory(soulboundInventory);
    }

    /**
     * Handles the drop rule for each item in the component
     * @param context extra info about the death
     * @return new inventory component with all items that should be kept
     */
    private InventoryComponent handleDropRules(DeathContext context) {
        InventoryComponent soulboundInventory = new InventoryComponent(DefaultedList.ofSize(this.items.size(), ItemStack.EMPTY), new HashMap<>(), this.mainSize, this.armorSize, this.offHandSize);

        // Handle drop rules for vanilla inventory
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack item = this.items.get(i);

            Vec3d deathPos = context.getDeathPos();

            DropRule dropRule = DropRuleEvent.EVENT.invoker().getDropRule(item, i, context);
            switch (dropRule) {
                case KEEP -> soulboundInventory.items.set(i, item);
                case DROP -> ItemScatterer.spawn(context.getWorld(), deathPos.x, deathPos.y, deathPos.z, item);
            }

            if (dropRule != DropRule.PUT_IN_GRAVE)
                this.items.set(i, ItemStack.EMPTY);
        }

        // Handle drop rules for mod compat inventories
        for (InvModCompat<?> compatMod : InvModCompat.invCompatMods) {
            String modName = compatMod.getModName();
            CompatComponent<?> compatComponent = this.modInventoryItems.get(modName);
            CompatComponent<?> soulboundComponent = compatComponent.handleDropRules(context);

            soulboundInventory.modInventoryItems.put(modName, soulboundComponent);
        }

        return soulboundInventory;
    }

    public void applyLoss(DeathContext context) {
        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.InventoryConfig.ItemLossConfig itemLoss = config.inventoryConfig.itemLoss;

        int from, to;
        if (itemLoss.usePercentRange) {
            DefaultedList<ItemStack> vanillaStacks = DefaultedList.of();
            vanillaStacks.addAll(this.items);
            vanillaStacks.removeIf(ItemStack::isEmpty);

            int itemCount = vanillaStacks.size();
            if (!itemLoss.affectStacks) {
                itemCount = 0;
                for (ItemStack stack : vanillaStacks) {
                    itemCount += stack.getCount();
                }
            }

            from = (int) (itemCount * itemLoss.lossRangeFrom / 100f);
            to = (int) (itemCount * itemLoss.lossRangeTo / 100f);
        } else {
            from = itemLoss.lossRangeFrom;
            to = itemLoss.lossRangeTo;
        }
        int amount = from < to ? new Random().nextInt(from, to + 1) : from;

        for (int i = 0; i < amount; i++) {
            if (Math.random() > itemLoss.percentChanceOfLoss / 100D) continue;

            this.loseRandomItem(context);
        }
    }
    private void loseRandomItem(DeathContext context) {
        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.InventoryConfig.ItemLossConfig itemLoss = config.inventoryConfig.itemLoss;

        List<Integer> itemSlots = new ArrayList<>();
        for (int i = 0; i < this.items.size(); i++) {
            ItemStack stack = this.items.get(i);
            if (stack.isEmpty()) continue;
            if (DropRuleEvent.EVENT.invoker().getDropRule(stack, i, context) == DropRule.KEEP && !itemLoss.canLoseSoulbound) continue;
            if (stack.isIn(YigdTags.LOSS_IMMUNE)) continue;

            itemSlots.add(i);
        }

        if (itemSlots.isEmpty()) return;

        int random = new Random().nextInt(itemSlots.size());

        if (itemLoss.affectStacks) {
            this.items.set(random, ItemStack.EMPTY);
        } else {
            int slot = itemSlots.get(random);
            ItemStack stack = this.items.get(slot);

            stack.decrement(1);
        }
    }

    public void dropAll(ServerWorld world, Vec3d pos) {
        for (ItemStack stack : this.items) {
            if (stack.isEmpty()) continue;
            if (DropItemEvent.EVENT.invoker().shouldDropItem(stack, pos.x, pos.y, pos.z, world))
                ItemScatterer.spawn(world, pos.x, pos.y, pos.z, stack);
        }

        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            compatComponent.dropItems(world, pos);
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
        if (config.graveConfig.treatBindingCurse) {
            if (isGraveInventory) {
                extraItems.addAll(this.pullBindingCurseItems());
            } else {
                extraItems.addAll(mergingComponent.pullBindingCurseItems());
            }
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
                currentComponentIndex = groupIndex < this.offHandSize ? groupIndex + this.mainSize + this.armorSize : this.mainSize + this.armorSize;
            } else {  // Can be reached if initial inventory size is increased
                groupIndex = i - (mergingComponent.mainSize + mergingComponent.armorSize + mergingComponent.offHandSize);
                currentComponentIndex = groupIndex + this.mainSize + this.armorSize + this.offHandSize;
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

        for (InvModCompat<?> modCompat : InvModCompat.invCompatMods) {
            String modName = modCompat.getModName();
            CompatComponent<?> compatComponent = this.modInventoryItems.get(modName);
            CompatComponent<?> mergingCompatComponent = mergingComponent.modInventoryItems.get(modName);

            DefaultedList<ItemStack> extraModItems = compatComponent.merge(mergingCompatComponent);
            extraItems.addAll(extraModItems);
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

    public boolean isEmpty() {
        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            if (!compatComponent.isEmpty()) return false;
        }

        List<ItemStack> allItems = new ArrayList<>(this.items);
        allItems.removeIf(ItemStack::isEmpty);

        return allItems.isEmpty();
    }
    public int size() {
        List<ItemStack> allItems = new ArrayList<>(this.items);

        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            allItems.addAll(compatComponent.getAsStackList());
        }

        allItems.removeIf(ItemStack::isEmpty);

        return allItems.size();
    }

    public DefaultedList<ItemStack> applyToPlayer(ServerPlayerEntity player) {
        DefaultedList<ItemStack> extraItems = DefaultedList.of();
        PlayerInventory inventory = player.getInventory();

        // Set vanilla inventory
        int invMainSize = inventory.main.size();
        int invArmorSize = inventory.armor.size();
        int invOffHandSize = inventory.offHand.size();
        for (int i = 0; i < this.items.size(); i++) {
            int groupIndex;
            int playerInvIndex;
            if (i < this.mainSize) {
                groupIndex = i;
                playerInvIndex = groupIndex < invMainSize ? groupIndex : -1;
            } else if (i < this.mainSize + this.armorSize) {
                groupIndex = i - this.mainSize;
                playerInvIndex = groupIndex < invArmorSize ? groupIndex + invMainSize : -1;
            } else if (i < this.mainSize + this.armorSize + this.offHandSize) {
                groupIndex = i - (this.mainSize + this.armorSize);
                playerInvIndex = groupIndex < invOffHandSize ? groupIndex + invMainSize + invArmorSize : -1;
            } else {  // Should is only reached if the inventory size is increased without any sub-inventory increasing
                groupIndex = i - (this.mainSize + this.armorSize + this.offHandSize);
                playerInvIndex = groupIndex + invMainSize + invArmorSize + invOffHandSize;
            }

            ItemStack stack = this.items.get(i);

            if (playerInvIndex >= inventory.size() || playerInvIndex == -1) {
                extraItems.add(stack);
            } else {
                inventory.setStack(playerInvIndex, stack);
            }
        }

        // Set mod inventories
        for (InvModCompat<?> modCompat : InvModCompat.invCompatMods) {
            DefaultedList<ItemStack> extraModItems = this.modInventoryItems.get(modCompat.getModName()).storeToPlayer(player);
            extraItems.addAll(extraModItems);
        }

        return extraItems;
    }
    public void clear() {
        Collections.fill(this.items, ItemStack.EMPTY);
        for (CompatComponent<?> component : this.modInventoryItems.values()) {
            component.clear();
        }
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtCompound vanillaInventoryNbt = Inventories.writeNbt(new NbtCompound(), this.items);
        vanillaInventoryNbt.putInt("size", this.items.size());
        vanillaInventoryNbt.putInt("mainSize", this.mainSize);
        vanillaInventoryNbt.putInt("armorSize", this.armorSize);
        vanillaInventoryNbt.putInt("offHandSize", this.offHandSize);

        NbtCompound modInventoriesNbt = new NbtCompound();
        for (InvModCompat<?> compatMod : InvModCompat.invCompatMods) {
            String modName = compatMod.getModName();
            CompatComponent<?> compatInv = this.modInventoryItems.get(modName);
            modInventoriesNbt.put(modName, compatInv.writeNbt());
        }

        nbt.put("vanilla", vanillaInventoryNbt);
        nbt.put("mods", modInventoriesNbt);

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

        NbtCompound modInventoriesNbt = nbt.getCompound("mods");
        Map<String, CompatComponent<?>> compatComponents = new HashMap<>();
        for (InvModCompat<?> compatMod : InvModCompat.invCompatMods) {
            String modName = compatMod.getModName();
            NbtCompound modNbt = modInventoriesNbt.getCompound(modName);
            compatComponents.put(modName, compatMod.readNbt(modNbt));
        }

        return new InventoryComponent(items, compatComponents, mainSize, armorSize, offHandSize);
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        player.getInventory().clear();

        for (InvModCompat<?> invModCompat : InvModCompat.invCompatMods) {
            invModCompat.clear(player);
        }
    }
}
