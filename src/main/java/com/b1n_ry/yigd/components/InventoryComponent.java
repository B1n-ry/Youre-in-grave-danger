package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.compat.CompatComponent;
import com.b1n_ry.yigd.compat.InvModCompat;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.AdjustDropRuleEvent;
import com.b1n_ry.yigd.events.DropItemEvent;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.util.GraveOverrideAreas;
import com.b1n_ry.yigd.util.PairModificationConsumer;
import com.b1n_ry.yigd.util.YigdTags;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class InventoryComponent {
    private final DefaultedList<Pair<ItemStack, DropRule>> items;
    private final Map<String, CompatComponent<?>> modInventoryItems;
    public final int mainSize;
    public final int armorSize;
    public final int offHandSize;

    private static final Random RANDOM = new Random();
    public static final Pair<ItemStack, DropRule> EMPTY_ITEM_PAIR = new Pair<>(ItemStack.EMPTY, GraveOverrideAreas.INSTANCE.defaultDropRule);

    public InventoryComponent(ServerPlayerEntity player) {
        // Avoiding list being immutable in case items should be added on death
        this.items = DefaultedList.of();
        for (ItemStack stack : this.getInventoryItems(player)) {
            this.items.add(new Pair<>(stack, DropRule.PUT_IN_GRAVE));
        }

        this.modInventoryItems = this.getModInventoryItems(player);

        PlayerInventory inventory = player.getInventory();
        this.mainSize = inventory.main.size();
        this.armorSize = inventory.armor.size();
        this.offHandSize = inventory.offHand.size();
    }
    private InventoryComponent(DefaultedList<Pair<ItemStack, DropRule>> items, Map<String, CompatComponent<?>> modInventoryItems, int mainSize, int armorSize, int offHandSize) {
        // Avoiding list being immutable in case items should be added on death
        this.items = DefaultedList.of();
        this.items.addAll(items);

        this.modInventoryItems = modInventoryItems;
        this.mainSize = mainSize;
        this.armorSize = armorSize;
        this.offHandSize = offHandSize;
    }

    public DefaultedList<Pair<ItemStack, DropRule>> getItems() {
        return this.items;
    }

    /**
     * Get all items in component, excluding vanilla inventory
     * @param withoutEmpty Whether empty items should be included
     * @return All items in component, excluding vanilla inventory
     */
    public DefaultedList<ItemStack> getAllExtraItems(boolean withoutEmpty) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();

        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            for (Pair<ItemStack, DropRule> pair : compatComponent.getAsStackDropList()) {
                ItemStack stack = pair.getLeft();
                if (withoutEmpty && stack.isEmpty()) continue;

                stacks.add(stack);
            }
        }

        return stacks;
    }
    public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
        predicate = predicate.and(stack -> stack.getCount() >= itemCount);

        for (Pair<ItemStack, DropRule> pair : this.items) {
            ItemStack stack = pair.getLeft();
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

    public void onDeath(DeathContext context) {
        YigdConfig config = YigdConfig.getConfig();
        if (config.inventoryConfig.dropPlayerHead) {
            ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);
            NbtCompound profileNbt = NbtHelper.writeGameProfile(new NbtCompound(), context.player().getGameProfile());
            playerHead.setSubNbt("SkullOwner", profileNbt);
            this.items.add(new Pair<>(playerHead, GraveOverrideAreas.INSTANCE.defaultDropRule));  // Drop rules should not yet be handled, so default one is used
        }

        this.handleDropRules(context);

        // Drop all dropped items
        InventoryComponent dropInventory = this.filteredInv(dropRule -> dropRule == DropRule.DROP);
        dropInventory.dropAll(context.world(), context.deathPos());
    }

    /**
     * Handles the drop rule for each item in the component
     * @param context extra info about the death
     */
    private void handleDropRules(DeathContext context) {
        // Handle drop rules for vanilla inventory
        for (int i = 0; i < this.items.size(); i++) {
            Pair<ItemStack, DropRule> pair = this.items.get(i);

            ItemStack item = pair.getLeft();
            if (item.isEmpty()) continue;

            DropRule dropRule = DropRuleEvent.EVENT.invoker().getDropRule(item, i, context, true);
            pair.setRight(dropRule);
        }

        // Handle drop rules for mod compat inventories
        for (InvModCompat<?> compatMod : InvModCompat.invCompatMods) {
            String modName = compatMod.getModName();
            CompatComponent<?> compatComponent = this.modInventoryItems.get(modName);
            compatComponent.handleDropRules(context);
        }

        AdjustDropRuleEvent.EVENT.invoker().adjustDropRules(this, context);
    }

    public void applyLoss() {
        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.InventoryConfig.ItemLossConfig itemLoss = config.inventoryConfig.itemLoss;

        int from, to;
        if (itemLoss.usePercentRange) {
            DefaultedList<ItemStack> vanillaStacks = DefaultedList.of();
            for (Pair<ItemStack, DropRule> pair : this.items) {
                if (!pair.getLeft().isEmpty() && pair.getRight() != DropRule.DESTROY)
                    vanillaStacks.add(pair.getLeft());
            }

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

            this.loseRandomItem();
        }
    }
    private void loseRandomItem() {
        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.InventoryConfig.ItemLossConfig itemLoss = config.inventoryConfig.itemLoss;

        List<Integer> itemSlots = new ArrayList<>();
        for (int i = 0; i < this.items.size(); i++) {
            Pair<ItemStack, DropRule> pair = this.items.get(i);
            ItemStack stack = pair.getLeft();
            if (stack.isEmpty()) continue;
            if (pair.getRight() == DropRule.KEEP && !itemLoss.canLoseSoulbound) continue;
            if (stack.isIn(YigdTags.LOSS_IMMUNE)) continue;

            itemSlots.add(i);
        }

        if (itemSlots.isEmpty()) return;

        int random = RANDOM.nextInt(itemSlots.size());

        int slot = itemSlots.get(random);
        if (itemLoss.affectStacks) {
            this.items.get(slot).setRight(DropRule.DESTROY);
        } else {
            ItemStack stack = this.items.get(slot).getLeft();

            stack.decrement(1);
        }
    }

    public void dropAll(ServerWorld world, Vec3d pos) {
        for (Pair<ItemStack, DropRule> pair : this.items) {
            ItemStack stack = pair.getLeft();
            if (stack.isEmpty()) continue;
            InventoryComponent.dropItemIfToBeDropped(stack, pos.x, pos.y, pos.z, world);
        }

        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            compatComponent.dropItems(world, pos);
        }
    }
    public void dropGraveItems(ServerWorld world, Vec3d pos) {
        for (Pair<ItemStack, DropRule> pair : this.items) {
            ItemStack stack = pair.getLeft();
            if (stack.isEmpty() || pair.getRight() != DropRule.PUT_IN_GRAVE) continue;
            pair.setRight(DropRule.DROP);  // Make sure item are marked as dropped, and not in a non-existent grave
            InventoryComponent.dropItemIfToBeDropped(stack, pos.x, pos.y, pos.z, world);
        }

        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            compatComponent.dropGraveItems(world, pos);
        }
    }

    /**
     * Adds a new inventory component on top of the currently existing one
     * @param mergingComponent Inventory component to merge with. This will be added to existing component
     * @return All items that wouldn't fit in inventory
     */
    public DefaultedList<ItemStack> merge(InventoryComponent mergingComponent, ServerPlayerEntity merger) {
        YigdConfig config = YigdConfig.getConfig();
        DefaultedList<ItemStack> extraItems = DefaultedList.of();

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

            ItemStack mergingStack = mergingComponent.items.get(i).getLeft().copy();  // Copy to avoid a problem in the case when merging and current stack are the same object
            if (currentComponentIndex > this.items.size()) {
                extraItems.add(mergingStack);
                continue;
            }

            ItemStack currentStack = this.items.get(currentComponentIndex).getLeft();

            if (config.graveConfig.treatBindingCurse && i >= mergingComponent.mainSize && i < mergingComponent.mainSize + mergingComponent.armorSize) {  // If merging stack is armor, check for curse of binding
                if (EnchantmentHelper.hasBindingCurse(mergingStack)) {  // If merging stack has curse of binding, force in that slot
                    // If grave inventory got all curse of binding items pulled, only the player equipped ones should be forced on the player (because that makes sense)
                    // This is done by clearing the current slot, moving the item to extraItems, and the curse of binding item will be applied in the slot later
                    if (!currentStack.isEmpty()) extraItems.add(currentStack);

                    this.items.set(currentComponentIndex, EMPTY_ITEM_PAIR);  // Item will get put here later
                    currentStack = this.items.get(currentComponentIndex).getLeft();
                }
            }
            if (config.graveConfig.mergeStacksOnRetrieve) {
                // Merge ItemStacks, and modify sizes accordingly if possible
                int combinationSlot = this.findMatchingStackSlot(mergingStack);
                if (combinationSlot != -1) {
                    this.mergeItemInSlot(mergingStack, combinationSlot);
                }
            }
            if (!mergingStack.isEmpty()) {  // Can be due to merging (count could be 0 if merge was "fully completed")
                if (currentStack.isEmpty()) {
                    this.items.set(currentComponentIndex, new Pair<>(mergingStack, DropRule.PUT_IN_GRAVE));  // Drop rule does not matter
                } else {
                    extraItems.add(mergingStack);
                }
            }
        }

        for (InvModCompat<?> modCompat : InvModCompat.invCompatMods) {
            String modName = modCompat.getModName();
            if (!mergingComponent.modInventoryItems.containsKey(modName)) continue;

            CompatComponent<?> mergingCompatComponent = mergingComponent.modInventoryItems.get(modName);
            if (!this.modInventoryItems.containsKey(modName)) {
                for (Pair<ItemStack, DropRule> pair : mergingCompatComponent.getAsStackDropList()) {
                    ItemStack item = pair.getLeft();
                    if (!item.isEmpty())
                        extraItems.add(item);
                }
                continue;
            }
            CompatComponent<?> compatComponent = this.modInventoryItems.get(modName);

            DefaultedList<ItemStack> extraModItems = compatComponent.merge(mergingCompatComponent, merger);
            extraItems.addAll(extraModItems);
        }

        this.addStacksToMain(extraItems);

        return extraItems;
    }

    private int findMatchingStackSlot(ItemStack stack) {
        for (int i = 0; i < this.mainSize; i++) {
            ItemStack iStack = this.items.get(i).getLeft();
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
        ItemStack mergeTo = this.items.get(slot).getLeft();
        int remaining = mergeTo.getMaxCount() - mergeTo.getCount();

        int ableToAdd = Math.min(toMerge.getCount(), remaining);
        mergeTo.increment(ableToAdd);
        toMerge.decrement(ableToAdd);
    }

    /**
     * Pull all curse of binding items from component (that would get stuck) and deletes them from component
     * @param playerRef Player reference for some modded inventories to know if an item can be unequipped or not
     * @return all curse of binding items in armor slots
     */
    public DefaultedList<ItemStack> pullBindingCurseItems(ServerPlayerEntity playerRef) {
        DefaultedList<ItemStack> bindingItems = DefaultedList.of();
        for (int i = 0; i < this.armorSize; i++) {
            ItemStack armorStack = this.items.get(this.mainSize + i).getLeft();  // Current armor item
            if (EnchantmentHelper.hasBindingCurse(armorStack)) {
                bindingItems.add(armorStack);
                this.items.set(this.mainSize + i, EMPTY_ITEM_PAIR);  // Moving the item in  this slot
            }
        }
        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            bindingItems.addAll(compatComponent.pullBindingCurseItems(playerRef));
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
            ItemStack addToStack = this.items.get(addToSlot).getLeft();
            if (addToStack.isEmpty()) {
                this.items.set(addToSlot, new Pair<>(stack, GraveOverrideAreas.INSTANCE.defaultDropRule));
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
            if (this.items.get(i).getLeft().isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Allows for checking for specific items in specific slots and inventories
     * @param itemPredicate Predicate to check for items
     * @param modPredicate Predicate to check for mod name ("vanilla" for vanilla)
     * @param slotPredicate Predicate to check for slot index (only applies to vanilla inventory)
     * @return Whether any item in the component matches the predicates
     */
    public boolean containsAny(Predicate<ItemStack> itemPredicate, Predicate<String> modPredicate, Predicate<Integer> slotPredicate) {
        if (modPredicate.test("vanilla")) {
            for (int i = 0; i < this.items.size(); i++) {
                Pair<ItemStack, DropRule> pair = this.items.get(i);
                if (slotPredicate.test(i) && itemPredicate.test(pair.getLeft())) return true;
            }
        }
        for (Map.Entry<String, CompatComponent<?>> entry : this.modInventoryItems.entrySet()) {
            CompatComponent<?> compatComponent = entry.getValue();
            if (modPredicate.test(entry.getKey()) && compatComponent.containsAny(itemPredicate)) return true;  // Modded inventories don't use slot IDs
        }
        return false;
    }

    /**
     * Executes code to modify items in the component based on predicate filters
     * @param modPredicate Mods matching this are affected ("vanilla" for vanilla inventory). This is used as a separate
     *                     predicate instead of inside the consumer, to optimize performance
     * @param modification Modification done to pair
     */
    public void handleItemPairs(Predicate<String> modPredicate, PairModificationConsumer modification) {
        if (modPredicate.test("vanilla")) {
            for (int i = 0; i < this.items.size(); i++) {
                Pair<ItemStack, DropRule> pair = this.items.get(i);
                modification.accept(pair.getLeft(), i, pair);
            }
        }
        for (Map.Entry<String, CompatComponent<?>> entry : this.modInventoryItems.entrySet()) {
            CompatComponent<?> compatComponent = entry.getValue();
            if (modPredicate.test(entry.getKey())) compatComponent.handleItemPairs(modification);
        }
    }

    /**
     * Checks if the grave is empty, meaning no items are stored to be retrieved from the grave when it's opened
     * @return Whether the grave is empty
     */
    public boolean isGraveEmpty() {
        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            if (compatComponent.containsGraveItems()) return false;
        }

        for (Pair<ItemStack, DropRule> pair : this.items) {
            if (!pair.getLeft().isEmpty() && pair.getRight() == DropRule.PUT_IN_GRAVE)
                return false;
        }
        return true;
    }

    /**
     * Get the number of occupied slots with drop rule PUT_IN_GRAVE
     * @return Number of occupied slots with drop rule PUT_IN_GRAVE
     */
    public int graveSize() {
        int size = 0;

        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            for (Pair<ItemStack, DropRule> pair : compatComponent.getAsStackDropList()) {
                if (!pair.getLeft().isEmpty() && pair.getRight() == DropRule.PUT_IN_GRAVE)
                    ++size;
            }
        }
        for (Pair<ItemStack, DropRule> pair : this.items) {
            if (!pair.getLeft().isEmpty() && pair.getRight() == DropRule.PUT_IN_GRAVE)
                ++size;
        }

        return size;
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

            ItemStack stack = this.items.get(i).getLeft();

            if (playerInvIndex >= inventory.size() || playerInvIndex == -1) {
                extraItems.add(stack);
            } else {
                inventory.setStack(playerInvIndex, stack);
            }
        }

        // Set mod inventories
        for (InvModCompat<?> modCompat : InvModCompat.invCompatMods) {
            String modName = modCompat.getModName();
            if (!this.modInventoryItems.containsKey(modName)) continue;
            DefaultedList<ItemStack> extraModItems = this.modInventoryItems.get(modName).storeToPlayer(player);
            extraItems.addAll(extraModItems);
        }

        return extraItems;
    }

    public InventoryComponent filteredInv(Predicate<DropRule> filter) {
        DefaultedList<Pair<ItemStack, DropRule>> filteredItems = DefaultedList.of();
        for (Pair<ItemStack, DropRule> pair : this.items) {
            if (filter.test(pair.getRight())) {
                filteredItems.add(pair);
            } else {
                filteredItems.add(EMPTY_ITEM_PAIR);
            }
        }

        Map<String, CompatComponent<?>> filteredModInventories = new HashMap<>();
        for (InvModCompat<?> compatMod : InvModCompat.invCompatMods) {
            String modName = compatMod.getModName();
            if (!this.modInventoryItems.containsKey(modName)) continue;
            CompatComponent<?> compatInv = this.modInventoryItems.get(modName);
            CompatComponent<?> filteredCompatInv = compatInv.filterInv(filter);
            filteredModInventories.put(modName, filteredCompatInv);
        }

        return new InventoryComponent(filteredItems, filteredModInventories, this.mainSize, this.armorSize, this.offHandSize);
    }

    public void clear() {
        Collections.fill(this.items, EMPTY_ITEM_PAIR);

        for (CompatComponent<?> component : this.modInventoryItems.values()) {
            component.clear();
        }
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtCompound vanillaInventoryNbt = listToNbt(this.items, pair -> {
            NbtCompound itemNbt = new NbtCompound();
            pair.getLeft().writeNbt(itemNbt);
            itemNbt.putString("dropRule", pair.getRight().name());

            return itemNbt;
        }, pair -> pair.getLeft().isEmpty());
        
        vanillaInventoryNbt.putInt("mainSize", this.mainSize);
        vanillaInventoryNbt.putInt("armorSize", this.armorSize);
        vanillaInventoryNbt.putInt("offHandSize", this.offHandSize);

        NbtCompound modInventoriesNbt = new NbtCompound();
        for (InvModCompat<?> compatMod : InvModCompat.invCompatMods) {
            String modName = compatMod.getModName();
            if (!this.modInventoryItems.containsKey(modName)) continue;

            CompatComponent<?> compatInv = this.modInventoryItems.get(modName);
            if (!compatInv.isEmpty())
                modInventoriesNbt.put(modName, compatInv.writeNbt());
        }

        nbt.put("vanilla", vanillaInventoryNbt);
        nbt.put("mods", modInventoriesNbt);

        return nbt;
    }

    public static InventoryComponent fromNbt(NbtCompound nbt) {
        NbtCompound vanillaInvNbt = nbt.getCompound("vanilla");
        DefaultedList<Pair<ItemStack, DropRule>> items = listFromNbt(vanillaInvNbt, itemNbt -> {
            ItemStack stack = ItemStack.fromNbt(itemNbt);
            DropRule dropRule = GraveOverrideAreas.INSTANCE.defaultDropRule;
            if (itemNbt.contains("dropRule")) {
                dropRule = DropRule.valueOf(itemNbt.getString("dropRule"));
            }
            return new Pair<>(stack, dropRule);
        }, EMPTY_ITEM_PAIR);

        int mainSize = vanillaInvNbt.getInt("mainSize");
        int armorSize = vanillaInvNbt.getInt("armorSize");
        int offHandSize = vanillaInvNbt.getInt("offHandSize");

        NbtCompound modInventoriesNbt = nbt.getCompound("mods");
        Map<String, CompatComponent<?>> compatComponents = new HashMap<>();
        for (InvModCompat<?> compatMod : InvModCompat.invCompatMods) {
            String modName = compatMod.getModName();
            if (!modInventoriesNbt.contains(modName)) continue;
            NbtCompound modNbt = modInventoriesNbt.getCompound(modName);
            compatComponents.put(modName, compatMod.readNbt(modNbt));
        }

        return new InventoryComponent(items, compatComponents, mainSize, armorSize, offHandSize);
    }

    public static <T> NbtCompound listToNbt(DefaultedList<T> list, Function<T, NbtCompound> mappingFunction, Predicate<T> isEmpty) {
        return listToNbt(list, mappingFunction, isEmpty, "Items", "Slot");
    }
    public static <T> NbtCompound listToNbt(DefaultedList<T> list, Function<T, NbtCompound> mappingFunction, Predicate<T> isEmpty, String listName, String itemName) {
        NbtCompound nbt = new NbtCompound();
        int size = list.size();
        nbt.putInt("size", size);

        NbtList nbtList = new NbtList();
        for (int i = 0; i < size; i++) {
            T item = list.get(i);
            if (isEmpty.test(item)) continue;

            NbtCompound itemNbt = mappingFunction.apply(item);
            itemNbt.putInt(itemName, i);
            nbtList.add(itemNbt);
        }
        nbt.put(listName, nbtList);
        return nbt;
    }
    public static <T> DefaultedList<T> listFromNbt(NbtCompound nbt, Function<NbtCompound, T> mappingFunction, T emptyValue) {
        return listFromNbt(nbt, mappingFunction, emptyValue, "Items", "Slot");
    }
    public static <T> DefaultedList<T> listFromNbt(NbtCompound nbt, Function<NbtCompound, T> mappingFunction, T emptyValue, String listName, String itemName) {
        int size = nbt.getInt("size");
        DefaultedList<T> list = DefaultedList.ofSize(size, emptyValue);

        NbtList nbtList = nbt.getList(listName, NbtElement.COMPOUND_TYPE);
        for (NbtElement element : nbtList) {
            NbtCompound itemNbt = (NbtCompound) element;
            int index = itemNbt.getInt(itemName);
            T item = mappingFunction.apply(itemNbt);
            list.set(index, item);
        }

        return list;
    }

    public static void dropItemIfToBeDropped(ItemStack stack, double x, double y, double z, ServerWorld world) {
        if (DropItemEvent.EVENT.invoker().shouldDropItem(stack, x, y, z, world))
            ItemScatterer.spawn(world, x, y, z, stack.copy());
    }

    public static void clearPlayer(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            inventory.setStack(i, ItemStack.EMPTY);
        }

        for (InvModCompat<?> invModCompat : InvModCompat.invCompatMods) {
            invModCompat.clear(player);
        }
    }
}
