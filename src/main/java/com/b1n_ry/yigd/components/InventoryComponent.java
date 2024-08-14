package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.compat.CompatComponent;
import com.b1n_ry.yigd.compat.InvModCompat;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.YigdEvents;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.util.GraveOverrideAreas;
import com.b1n_ry.yigd.util.PairModificationConsumer;
import com.b1n_ry.yigd.util.YigdTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class InventoryComponent {
    private final NonNullList<Tuple<ItemStack, DropRule>> items;
    private final Map<String, CompatComponent<?>> modInventoryItems;
    public final int mainSize;
    public final int armorSize;
    public final int offHandSize;

    private static final Random RANDOM = new Random();
    public static final Tuple<ItemStack, DropRule> EMPTY_ITEM_PAIR = new Tuple<>(ItemStack.EMPTY, GraveOverrideAreas.INSTANCE.defaultDropRule);

    public InventoryComponent(ServerPlayer player) {
        // Avoiding list being immutable in case items should be added on death
        this.items = NonNullList.create();
        for (ItemStack stack : this.getInventoryItems(player)) {
            this.items.add(new Tuple<>(stack, DropRule.PUT_IN_GRAVE));
        }

        this.modInventoryItems = this.getModInventoryItems(player);

        Inventory inventory = player.getInventory();
        this.mainSize = inventory.items.size();
        this.armorSize = inventory.armor.size();
        this.offHandSize = inventory.offhand.size();
    }
    private InventoryComponent(NonNullList<Tuple<ItemStack, DropRule>> items, Map<String, CompatComponent<?>> modInventoryItems, int mainSize, int armorSize, int offHandSize) {
        // Avoiding list being immutable in case items should be added on death
        this.items = NonNullList.create();
        this.items.addAll(items);

        this.modInventoryItems = modInventoryItems;
        this.mainSize = mainSize;
        this.armorSize = armorSize;
        this.offHandSize = offHandSize;
    }

    public NonNullList<Tuple<ItemStack, DropRule>> getItems() {
        return this.items;
    }

    /**
     * Get all items in component, excluding vanilla inventory
     * @param withoutEmpty Whether empty items should be included
     * @return All items in component, excluding vanilla inventory
     */
    public NonNullList<ItemStack> getAllExtraItems(boolean withoutEmpty) {
        NonNullList<ItemStack> stacks = NonNullList.create();

        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            for (Tuple<ItemStack, DropRule> pair : compatComponent.getAsStackDropList()) {
                ItemStack stack = pair.getA();
                if (withoutEmpty && stack.isEmpty()) continue;

                stacks.add(stack);
            }
        }

        return stacks;
    }
    public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
        predicate = predicate.and(stack -> stack.getCount() >= itemCount);

        for (Tuple<ItemStack, DropRule> pair : this.items) {
            ItemStack stack = pair.getA();
            if (predicate.test(stack)) {
                stack.shrink(itemCount);
                return true;
            }
        }
        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            if (compatComponent.removeItem(predicate, itemCount))
                return true;
        }

        return false;
    }
    private NonNullList<ItemStack> getInventoryItems(ServerPlayer player) {
        Inventory inventory = player.getInventory();

        NonNullList<ItemStack> items = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);

        // Save all items in vanilla inventory
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            items.set(i, inventory.getItem(i));
        }

        return items;
    }
    private Map<String, CompatComponent<?>> getModInventoryItems(ServerPlayer player) {
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
            playerHead.set(DataComponents.PROFILE, new ResolvableProfile(context.player().getGameProfile()));

            this.items.add(new Tuple<>(playerHead, GraveOverrideAreas.INSTANCE.defaultDropRule));  // Drop rules should not yet be handled, so default one is used
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
            Tuple<ItemStack, DropRule> pair = this.items.get(i);

            ItemStack item = pair.getA();
            if (item.isEmpty()) continue;

            YigdEvents.DropRuleEvent event = NeoForge.EVENT_BUS.post(new YigdEvents.DropRuleEvent(item, i, context, true));
            DropRule dropRule = event.getDropRule();
            pair.setB(dropRule);
        }

        // Handle drop rules for mod compat inventories
        for (InvModCompat<?> compatMod : InvModCompat.invCompatMods) {
            String modName = compatMod.getModName();
            CompatComponent<?> compatComponent = this.modInventoryItems.get(modName);
            compatComponent.handleDropRules(context);
        }

        NeoForge.EVENT_BUS.post(new YigdEvents.AdjustDropRuleEvent(this, context));
    }

    public void applyLoss() {
        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.InventoryConfig.ItemLossConfig itemLoss = config.inventoryConfig.itemLoss;

        int from, to;
        if (itemLoss.usePercentRange) {
            NonNullList<ItemStack> vanillaStacks = NonNullList.create();
            for (Tuple<ItemStack, DropRule> pair : this.items) {
                if (!pair.getA().isEmpty() && pair.getB() != DropRule.DESTROY)
                    vanillaStacks.add(pair.getA());
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
            Tuple<ItemStack, DropRule> pair = this.items.get(i);
            ItemStack stack = pair.getA();
            if (stack.isEmpty()) continue;
            if (pair.getB() == DropRule.KEEP && !itemLoss.canLoseSoulbound) continue;
            if (stack.is(YigdTags.LOSS_IMMUNE)) continue;

            itemSlots.add(i);
        }

        if (itemSlots.isEmpty()) return;

        int random = RANDOM.nextInt(itemSlots.size());

        int slot = itemSlots.get(random);
        if (itemLoss.affectStacks) {
            this.items.get(slot).setB(DropRule.DESTROY);
        } else {
            ItemStack stack = this.items.get(slot).getA();

            stack.shrink(1);
        }
    }

    public void dropAll(ServerLevel world, Vec3 pos) {
        for (Tuple<ItemStack, DropRule> pair : this.items) {
            ItemStack stack = pair.getA();
            if (stack.isEmpty()) continue;
            InventoryComponent.dropItemIfToBeDropped(stack, pos.x, pos.y, pos.z, world);
        }

        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            compatComponent.dropItems(world, pos);
        }
    }
    public void dropGraveItems(ServerLevel world, Vec3 pos) {
        for (Tuple<ItemStack, DropRule> pair : this.items) {
            ItemStack stack = pair.getA();
            if (stack.isEmpty() || pair.getB() != DropRule.PUT_IN_GRAVE) continue;
            pair.setB(DropRule.DROP);  // Make sure item are marked as dropped, and not in a non-existent grave
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
    public NonNullList<ItemStack> merge(InventoryComponent mergingComponent, ServerPlayer merger) {
        YigdConfig config = YigdConfig.getConfig();
        NonNullList<ItemStack> extraItems = NonNullList.create();

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

            ItemStack mergingStack = mergingComponent.items.get(i).getA().copy();  // Copy to avoid a problem in the case when merging and current stack are the same object
            if (currentComponentIndex > this.items.size()) {
                extraItems.add(mergingStack);
                continue;
            }

            ItemStack currentStack = this.items.get(currentComponentIndex).getA();

            if (config.graveConfig.treatBindingCurse && i >= mergingComponent.mainSize && i < mergingComponent.mainSize + mergingComponent.armorSize) {  // If merging stack is armor, check for curse of binding
                if (EnchantmentHelper.has(mergingStack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {  // If merging stack has curse of binding, force in that slot
                    // If grave inventory got all curse of binding items pulled, only the player equipped ones should be forced on the player (because that makes sense)
                    // This is done by clearing the current slot, moving the item to extraItems, and the curse of binding item will be applied in the slot later
                    if (!currentStack.isEmpty()) extraItems.add(currentStack);
                    this.items.set(currentComponentIndex, EMPTY_ITEM_PAIR);  // Item will get put here later
                    currentStack = this.items.get(currentComponentIndex).getA();
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
                    this.items.set(currentComponentIndex, new Tuple<>(mergingStack, DropRule.PUT_IN_GRAVE));  // Drop rule does not matter
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
                for (Tuple<ItemStack, DropRule> pair : mergingCompatComponent.getAsStackDropList()) {
                    ItemStack item = pair.getA();
                    if (!item.isEmpty())
                        extraItems.add(item);
                }
                continue;
            }
            CompatComponent<?> compatComponent = this.modInventoryItems.get(modName);

            NonNullList<ItemStack> extraModItems = compatComponent.merge(mergingCompatComponent, merger);
            extraItems.addAll(extraModItems);
        }

        this.addStacksToMain(extraItems);

        return extraItems;
    }

    private int findMatchingStackSlot(ItemStack stack) {
        for (int i = 0; i < this.mainSize; i++) {
            ItemStack iStack = this.items.get(i).getA();
            if (ItemStack.isSameItemSameComponents(stack, iStack) && iStack.isStackable() && iStack.getMaxStackSize() > iStack.getCount()) {
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
        ItemStack mergeTo = this.items.get(slot).getA();
        int remaining = mergeTo.getMaxStackSize() - mergeTo.getCount();

        int ableToAdd = Math.min(toMerge.getCount(), remaining);
        mergeTo.grow(ableToAdd);
        toMerge.shrink(ableToAdd);
    }

    /**
     * Pull all curse of binding items from component (that would get stuck) and deletes them from component
     * @param playerRef Player reference for some modded inventories to know if an item can be unequipped or not
     * @return all curse of binding items in armor slots
     */
    public NonNullList<ItemStack> pullBindingCurseItems(ServerPlayer playerRef) {
        NonNullList<ItemStack> bindingItems = NonNullList.create();
        for (int i = 0; i < this.armorSize; i++) {
            ItemStack armorStack = this.items.get(this.mainSize + i).getA();  // Current armor item
            if (EnchantmentHelper.has(armorStack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
                bindingItems.add(armorStack);
                this.items.set(this.mainSize + i, EMPTY_ITEM_PAIR);  // Moving the item in  this slot
            }
        }
        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            bindingItems.addAll(compatComponent.pullBindingCurseItems(playerRef));
        }
        return bindingItems;
    }

    private void addStacksToMain(NonNullList<ItemStack> extraItems) {
        YigdConfig config = YigdConfig.getConfig();
        while (!extraItems.isEmpty()) {
            ItemStack stack = extraItems.getFirst();

            int addToSlot = -1;
            if (config.graveConfig.mergeStacksOnRetrieve) {
                addToSlot = this.findMatchingStackSlot(stack);
            }
            if (addToSlot == -1) {
                addToSlot = this.findEmptySlot();
                if (addToSlot == -1) return;  // Inventory is full
            }
            ItemStack addToStack = this.items.get(addToSlot).getA();
            if (addToStack.isEmpty()) {
                this.items.set(addToSlot, new Tuple<>(stack, GraveOverrideAreas.INSTANCE.defaultDropRule));
                extraItems.removeFirst();
            } else {
                this.mergeItemInSlot(stack, addToSlot);
                if (stack.isEmpty()) {
                    extraItems.removeFirst();
                }
            }
        }
    }

    private int findEmptySlot() {
        for (int i = 0; i < this.mainSize; i++) {
            if (this.items.get(i).getA().isEmpty()) {
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
                Tuple<ItemStack, DropRule> pair = this.items.get(i);
                if (slotPredicate.test(i) && itemPredicate.test(pair.getA())) return true;
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
                Tuple<ItemStack, DropRule> pair = this.items.get(i);
                modification.accept(pair.getA(), i, pair);
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

        for (Tuple<ItemStack, DropRule> pair : this.items) {
            if (!pair.getA().isEmpty() && pair.getB() == DropRule.PUT_IN_GRAVE)
                return false;
        }
        return true;
    }

    public boolean isEmpty() {
        for (CompatComponent<?> compatComponent : this.modInventoryItems.values()) {
            if (!compatComponent.isEmpty())
                return false;
        }

        for (Tuple<ItemStack, DropRule> pair : this.items) {
            if (!pair.getA().isEmpty())
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
            for (Tuple<ItemStack, DropRule> pair : compatComponent.getAsStackDropList()) {
                if (!pair.getA().isEmpty() && pair.getB() == DropRule.PUT_IN_GRAVE)
                    ++size;
            }
        }
        for (Tuple<ItemStack, DropRule> pair : this.items) {
            if (!pair.getA().isEmpty() && pair.getB() == DropRule.PUT_IN_GRAVE)
                ++size;
        }

        return size;
    }

    public NonNullList<ItemStack> applyToPlayer(ServerPlayer player) {
        NonNullList<ItemStack> extraItems = NonNullList.create();
        Inventory inventory = player.getInventory();

        // Set vanilla inventory
        int invMainSize = inventory.items.size();
        int invArmorSize = inventory.armor.size();
        int invOffHandSize = inventory.offhand.size();
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

            ItemStack stack = this.items.get(i).getA();

            if (playerInvIndex >= inventory.getContainerSize() || playerInvIndex == -1) {
                extraItems.add(stack);
            } else {
                inventory.setItem(playerInvIndex, stack);
            }
        }

        // Set mod inventories
        for (InvModCompat<?> modCompat : InvModCompat.invCompatMods) {
            String modName = modCompat.getModName();
            if (!this.modInventoryItems.containsKey(modName)) continue;
            NonNullList<ItemStack> extraModItems = this.modInventoryItems.get(modName).storeToPlayer(player);
            extraItems.addAll(extraModItems);
        }

        return extraItems;
    }

    public InventoryComponent filteredInv(Predicate<DropRule> filter) {
        NonNullList<Tuple<ItemStack, DropRule>> filteredItems = NonNullList.create();
        for (Tuple<ItemStack, DropRule> pair : this.items) {
            if (filter.test(pair.getB())) {
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

    public CompoundTag toNbt(HolderLookup.Provider lookupRegistry) {
        CompoundTag nbt = new CompoundTag();
        CompoundTag vanillaInventoryNbt = listToNbt(this.items, pair -> {
            CompoundTag itemNbt = (CompoundTag) pair.getA().save(lookupRegistry);
            itemNbt.putString("dropRule", pair.getB().name());

            return itemNbt;
        }, pair -> pair.getA().isEmpty());

        vanillaInventoryNbt.putInt("mainSize", this.mainSize);
        vanillaInventoryNbt.putInt("armorSize", this.armorSize);
        vanillaInventoryNbt.putInt("offHandSize", this.offHandSize);

        CompoundTag modInventoriesNbt = new CompoundTag();
        for (InvModCompat<?> compatMod : InvModCompat.invCompatMods) {
            String modName = compatMod.getModName();
            if (!this.modInventoryItems.containsKey(modName)) continue;

            CompatComponent<?> compatInv = this.modInventoryItems.get(modName);
            if (!compatInv.isEmpty())
                modInventoriesNbt.put(modName, compatInv.writeNbt(lookupRegistry));
        }

        nbt.put("vanilla", vanillaInventoryNbt);
        nbt.put("mods", modInventoriesNbt);

        return nbt;
    }

    public static InventoryComponent fromNbt(CompoundTag nbt, HolderLookup.Provider lookupRegistry) {
        CompoundTag vanillaInvNbt = nbt.getCompound("vanilla");
        NonNullList<Tuple<ItemStack, DropRule>> items = listFromNbt(vanillaInvNbt, itemNbt -> {
            ItemStack stack = ItemStack.parseOptional(lookupRegistry, itemNbt);
            DropRule dropRule = GraveOverrideAreas.INSTANCE.defaultDropRule;
            if (itemNbt.contains("dropRule")) {
                dropRule = DropRule.valueOf(itemNbt.getString("dropRule"));
            }
            return new Tuple<>(stack, dropRule);
        }, EMPTY_ITEM_PAIR);

        int mainSize = vanillaInvNbt.getInt("mainSize");
        int armorSize = vanillaInvNbt.getInt("armorSize");
        int offHandSize = vanillaInvNbt.getInt("offHandSize");

        CompoundTag modInventoriesNbt = nbt.getCompound("mods");
        Map<String, CompatComponent<?>> compatComponents = new HashMap<>();
        for (InvModCompat<?> compatMod : InvModCompat.invCompatMods) {
            String modName = compatMod.getModName();
            if (!modInventoriesNbt.contains(modName)) continue;
            CompoundTag modNbt = modInventoriesNbt.getCompound(modName);
            compatComponents.put(modName, compatMod.readNbt(modNbt, lookupRegistry));
        }

        return new InventoryComponent(items, compatComponents, mainSize, armorSize, offHandSize);
    }

    public static <T> CompoundTag listToNbt(NonNullList<T> list, Function<T, CompoundTag> mappingFunction, Predicate<T> isEmpty) {
        return listToNbt(list, mappingFunction, isEmpty, "Items", "Slot");
    }
    public static <T> CompoundTag listToNbt(NonNullList<T> list, Function<T, CompoundTag> mappingFunction, Predicate<T> isEmpty, String listName, String itemName) {
        CompoundTag nbt = new CompoundTag();
        int size = list.size();
        nbt.putInt("size", size);

        ListTag nbtList = new ListTag();
        for (int i = 0; i < size; i++) {
            T item = list.get(i);
            if (isEmpty.test(item)) continue;

            CompoundTag itemNbt = mappingFunction.apply(item);
            itemNbt.putInt(itemName, i);
            nbtList.add(itemNbt);
        }
        nbt.put(listName, nbtList);
        return nbt;
    }
    public static <T> NonNullList<T> listFromNbt(CompoundTag nbt, Function<CompoundTag, T> mappingFunction, T emptyValue) {
        return listFromNbt(nbt, mappingFunction, emptyValue, "Items", "Slot");
    }
    public static <T> NonNullList<T> listFromNbt(CompoundTag nbt, Function<CompoundTag, T> mappingFunction, T emptyValue, String listName, String itemName) {
        int size = nbt.getInt("size");
        NonNullList<T> list = NonNullList.withSize(size, emptyValue);

        ListTag nbtList = nbt.getList(listName, Tag.TAG_COMPOUND);
        for (Tag element : nbtList) {
            CompoundTag itemNbt = (CompoundTag) element;
            int index = itemNbt.getInt(itemName);
            T item = mappingFunction.apply(itemNbt);
            list.set(index, item);
        }

        return list;
    }

    public static void dropItemIfToBeDropped(ItemStack stack, double x, double y, double z, ServerLevel world) {
        YigdEvents.DropItemEvent event = NeoForge.EVENT_BUS.post(new YigdEvents.DropItemEvent(stack, x, y, z, world));
        if (event.shouldDrop())
            Containers.dropItemStack(world, x, y, z, stack.copy());
    }

    public static void clearPlayer(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            inventory.setItem(i, ItemStack.EMPTY);
        }

        for (InvModCompat<?> invModCompat : InvModCompat.invCompatMods) {
            invModCompat.clear(player);
        }
    }
}
