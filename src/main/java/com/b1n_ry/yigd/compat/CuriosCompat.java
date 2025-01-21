package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.data.GraveItem;
import com.b1n_ry.yigd.events.YigdEvents;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.compat.CuriosCompat.CuriosSlotEntry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class CuriosCompat implements InvModCompat<Map<String, CuriosSlotEntry>> {
    @Override
    public String getModName() {
        return "curios";
    }

    @Override
    public void clear(ServerPlayer player) {
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            for (ICurioStacksHandler stacksHandler : handler.getCurios().values()) {
                IDynamicStackHandler[] stackHandlers = { stacksHandler.getStacks(), stacksHandler.getCosmeticStacks() };
                for (IDynamicStackHandler dynamicStackHandler : stackHandlers) {
                    for (int i = 0; i < dynamicStackHandler.getSlots(); i++) {
                        dynamicStackHandler.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }
        });
    }

    @Override
    public CompatComponent<Map<String, CuriosSlotEntry>> readNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        Map<String, CuriosSlotEntry> inventory = new HashMap<>();

        for (String key : nbt.getAllKeys()) {
            CompoundTag slotNbt = nbt.getCompound(key);
            NonNullList<GraveItem> normalSlot = InventoryComponent.listFromNbt(slotNbt.getCompound("normal"), itemNbt -> {
                ItemStack stack = ItemStack.parse(registries, itemNbt).orElse(ItemStack.EMPTY);
                DropRule dropRule;
                if (itemNbt.contains("dropRule")) {
                    // We need to check in case the drop rule is a trinket drop rule (only has one difference and that is trinkets have DEFAULT)
                    String dropRuleString = itemNbt.getString("dropRule");
                    if (dropRuleString.equals("DEFAULT")) {
                        dropRule = YigdConfig.getConfig().compatConfig.defaultCuriosDropRule;
                    } else {
                        dropRule = DropRule.valueOf(dropRuleString);
                    }
                } else {
                    dropRule = YigdConfig.getConfig().compatConfig.defaultCuriosDropRule;
                }

                return new GraveItem(stack, dropRule);
            }, InventoryComponent.EMPTY_GRAVE_ITEM);
            NonNullList<GraveItem> cosmeticSlot = InventoryComponent.listFromNbt(slotNbt.getCompound("cosmetic"), itemNbt -> {
                ItemStack stack = ItemStack.parse(registries, itemNbt).orElse(ItemStack.EMPTY);
                DropRule dropRule;
                if (itemNbt.contains("dropRule")) {
                    // We need to check in case the drop rule is a trinket drop rule (only has one difference and that is trinkets have DEFAULT)
                    String dropRuleString = itemNbt.getString("dropRule");
                    if (dropRuleString.equals("DEFAULT")) {
                        dropRule = YigdConfig.getConfig().compatConfig.defaultCuriosDropRule;
                    } else {
                        dropRule = DropRule.valueOf(dropRuleString);
                    }
                } else {
                    dropRule = YigdConfig.getConfig().compatConfig.defaultCuriosDropRule;
                }

                return new GraveItem(stack, dropRule);
            }, InventoryComponent.EMPTY_GRAVE_ITEM);

            inventory.put(key, new CuriosSlotEntry(normalSlot, cosmeticSlot));
        }
        return new CuriosCompatComponent(inventory);
    }

    @Override
    public CompatComponent<Map<String, CuriosSlotEntry>> getNewComponent(ServerPlayer player) {
        return new CuriosCompatComponent(player);
    }

    private static class CuriosCompatComponent extends CompatComponent<Map<String, CuriosSlotEntry>> {
        public CuriosCompatComponent(ServerPlayer player) {
            super(player);
        }
        public CuriosCompatComponent(Map<String, CuriosSlotEntry> inventory) {
            super(inventory);
        }

        @Override
        public Map<String, CuriosSlotEntry> getInventory(ServerPlayer player) {
            Map<String, CuriosSlotEntry> inventory = new HashMap<>();
            Optional<ICuriosItemHandler> optionalHandler = CuriosApi.getCuriosInventory(player);
            if (optionalHandler.isEmpty()) return inventory;

            for (Map.Entry<String, ICurioStacksHandler> entry : optionalHandler.get().getCurios().entrySet()) {
                ICurioStacksHandler stacksHandler = entry.getValue();
                NonNullList<GraveItem> normalItems = NonNullList.create();
                NonNullList<GraveItem> cosmeticItems = NonNullList.create();

                IDynamicStackHandler normalEquipped = stacksHandler.getStacks();
                for (int i = 0; i < normalEquipped.getSlots(); i++) {
                    normalItems.add(new GraveItem(normalEquipped.getStackInSlot(i).copy(), DropRule.PUT_IN_GRAVE));
                }
                IDynamicStackHandler cosmeticEquipped = stacksHandler.getCosmeticStacks();
                for (int i = 0; i < cosmeticEquipped.getSlots(); i++) {
                    cosmeticItems.add(new GraveItem(cosmeticEquipped.getStackInSlot(i).copy(), DropRule.PUT_IN_GRAVE));
                }

                inventory.put(entry.getKey(), new CuriosSlotEntry(normalItems, cosmeticItems));
            }
            return inventory;
        }

        @Override
        public NonNullList<GraveItem> merge(CompatComponent<?> mergingComponent, ServerPlayer merger) {
            NonNullList<GraveItem> extraItems = NonNullList.create();
            @SuppressWarnings("unchecked")
            Map<String, CuriosSlotEntry> mergingInventory = (Map<String, CuriosSlotEntry>) mergingComponent.inventory;

            for (Map.Entry<String, CuriosSlotEntry> entry : mergingInventory.entrySet()) {
                String key = entry.getKey();
                if (!this.inventory.containsKey(key)) {
                    entry.getValue().addAllNonEmptyToList(extraItems);
                    continue;
                }
                CuriosSlotEntry mergingSlot = entry.getValue();
                CuriosSlotEntry thisSlot = this.inventory.get(key);

                for (int i = 0; i < mergingSlot.normal.size(); i++) {
                    GraveItem mergingGraveItem = mergingSlot.normal.get(i).copy();
                    ItemStack mergingStack = mergingGraveItem.stack;
                    if (mergingStack.isEmpty()) continue;

                    if (thisSlot.normal.size() <= i) {
                        extraItems.add(mergingGraveItem);
                        continue;
                    }

                    GraveItem currentGraveItem = thisSlot.normal.get(i);
                    ItemStack thisStack = currentGraveItem.stack;
                    if (YigdConfig.getConfig().graveConfig.treatBindingCurse && this.blockUnequip(mergingStack, key, i, merger, false)) {
                        extraItems.add(currentGraveItem);  // Add the current item to extraItems (as it's being replaced)
                        thisSlot.normal.set(i, mergingGraveItem);  // Can't be unequipped, so it's prioritized
                        continue;  // Already set the item, so we can skip the rest
                    }
                    if (!thisStack.isEmpty()) {
                        extraItems.add(mergingGraveItem);
                        continue;
                    }

                    thisSlot.normal.set(i, mergingGraveItem);
                }
                for (int i = 0; i < mergingSlot.cosmetic.size(); i++) {
                    GraveItem mergingGraveItem = mergingSlot.cosmetic.get(i).copy();
                    ItemStack mergingStack = mergingGraveItem.stack;
                    if (mergingStack.isEmpty()) continue;

                    if (thisSlot.cosmetic.size() <= i) {
                        extraItems.add(mergingGraveItem);
                        continue;
                    }

                    GraveItem currentGraveItem = thisSlot.cosmetic.get(i);
                    ItemStack thisStack = currentGraveItem.stack;
                    if (YigdConfig.getConfig().graveConfig.treatBindingCurse && this.blockUnequip(mergingStack, key, i, merger, true)) {
                        extraItems.add(currentGraveItem);  // Add the current item to extraItems (as it's being replaced)
                        thisSlot.cosmetic.set(i, mergingGraveItem);  // Can't be unequipped, so it's prioritized
                        continue;  // Already set the item, so we can skip the rest
                    }
                    if (!thisStack.isEmpty()) {
                        extraItems.add(mergingGraveItem);
                        continue;
                    }

                    thisSlot.cosmetic.set(i, mergingGraveItem);
                }
            }
            return extraItems;
        }

        @Override
        public NonNullList<ItemStack> pullBindingCurseItems(ServerPlayer playerRef) {
            NonNullList<ItemStack> noUnequipItems = NonNullList.create();

            if (!YigdConfig.getConfig().graveConfig.treatBindingCurse) return noUnequipItems;

            for (Map.Entry<String, CuriosSlotEntry> entry : this.inventory.entrySet()) {
                CuriosSlotEntry inventorySlot = entry.getValue();
                for (int i = 0; i < inventorySlot.normal.size(); i++) {
                    GraveItem graveItem = inventorySlot.normal.get(i);
                    ItemStack stack = graveItem.stack;
                    boolean isBound = this.blockUnequip(stack, entry.getKey(), i, playerRef, false);
                    if (isBound) {
                        noUnequipItems.add(stack);
                        graveItem.stack = ItemStack.EMPTY;
                    }
                }
                for (int i = 0; i < inventorySlot.cosmetic.size(); i++) {
                    GraveItem graveItem = inventorySlot.cosmetic.get(i);
                    ItemStack stack = graveItem.stack;
                    boolean isBound = this.blockUnequip(stack, entry.getKey(), i, playerRef, true);
                    if (isBound) {
                        noUnequipItems.add(stack);
                        graveItem.stack = ItemStack.EMPTY;
                    }
                }
            }

            return noUnequipItems;
        }

        private boolean blockUnequip(ItemStack stack, String key, int index, ServerPlayer playerRef, boolean cosmetic) {
            Optional<ICurio> iCurio = CuriosApi.getCurio(stack);
            return !iCurio.map(curio -> curio.canUnequip(new SlotContext(key, playerRef, index, cosmetic, false))).orElse(true);
        }

        @Override
        public NonNullList<ItemStack> storeToPlayer(ServerPlayer player) {
            NonNullList<ItemStack> extraItems = NonNullList.create();
            Optional<ICuriosItemHandler> optional = CuriosApi.getCuriosInventory(player);
            if (optional.isEmpty()) return extraItems;

            Map<String, ICurioStacksHandler> stacksHandlerMap = optional.get().getCurios();
            for (Map.Entry<String, CuriosSlotEntry> entry : this.inventory.entrySet()) {
                String key = entry.getKey();
                if (!stacksHandlerMap.containsKey(key)) {
                    entry.getValue().addAllNonEmptyToStackList(extraItems);
                    continue;
                }
                ICurioStacksHandler stacksHandler = stacksHandlerMap.get(key);
                CuriosSlotEntry slotEntry = entry.getValue();

                IDynamicStackHandler normalEquipped = stacksHandler.getStacks();
                IDynamicStackHandler cosmeticEquipped = stacksHandler.getCosmeticStacks();
                for (int i = 0; i < slotEntry.normal.size(); i++) {
                    GraveItem graveItem = slotEntry.normal.get(i).copy();
                    if (i >= normalEquipped.getSlots()) {
                        extraItems.add(graveItem.stack);
                        continue;
                    }

                    normalEquipped.setStackInSlot(i, graveItem.stack);
                }
                for (int i = 0; i < slotEntry.cosmetic.size(); i++) {
                    GraveItem graveItem = slotEntry.cosmetic.get(i).copy();
                    if (i >= cosmeticEquipped.getSlots()) {
                        extraItems.add(graveItem.stack);
                        continue;
                    }

                    cosmeticEquipped.setStackInSlot(i, graveItem.stack);
                }
            }
            return extraItems;
        }

        private ICurio.DropRule getDropRule(ItemStack stack, String key, int index, DeathContext context, boolean cosmetic, List<Tuple<Predicate<ItemStack>, ICurio.DropRule>> overrides) {
            for (Tuple<Predicate<ItemStack>, ICurio.DropRule> t : overrides) {
                if (t.getA().test(stack)) {
                    return t.getB();
                }
            }
            Optional<ICurio> iCurio = CuriosApi.getCurio(stack);
            return iCurio.map(curio -> curio.getDropRule(new SlotContext(key, context.player(), index, cosmetic, false), context.deathSource(), true)).orElse(ICurio.DropRule.DEFAULT);
        }

        @Override
        public void handleDropRules(DeathContext context) {
            ServerPlayer player = context.player();
            List<Tuple<Predicate<ItemStack>, ICurio.DropRule>> overrides = new ArrayList<>();
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                DropRulesEvent event = NeoForge.EVENT_BUS.post(new DropRulesEvent(player, handler, context.deathSource(), 0, false));
                overrides.addAll(event.getOverrides());
            });
            for (Map.Entry<String, CuriosSlotEntry> entry : this.inventory.entrySet()) {
                String key = entry.getKey();
                CuriosSlotEntry inventorySlot = entry.getValue();
                for (int i = 0; i < inventorySlot.normal.size(); i++) {
                    GraveItem graveItem = inventorySlot.normal.get(i);
                    ItemStack stack = graveItem.stack;

                    graveItem.dropRule = switch(this.getDropRule(stack, key, i, context, false, overrides)) {
                        case DESTROY -> DropRule.DESTROY;
                        case ALWAYS_KEEP -> DropRule.KEEP;
                        default -> {
                            DropRule defaultDropRule = YigdConfig.getConfig().compatConfig.defaultCuriosDropRule;
                            if (defaultDropRule == DropRule.PUT_IN_GRAVE)
                                yield NeoForge.EVENT_BUS.post(new YigdEvents.DropRuleEvent(stack, -1, context, true)).getDropRule();
                            else
                                yield defaultDropRule;
                        }
                    };
                }
                for (int i = 0; i < inventorySlot.cosmetic.size(); i++) {
                    GraveItem graveItem = inventorySlot.cosmetic.get(i);
                    ItemStack stack = graveItem.stack;

                    graveItem.dropRule = switch(this.getDropRule(stack, key, i, context, true, overrides)) {
                        case DESTROY -> DropRule.DESTROY;
                        case ALWAYS_KEEP -> DropRule.KEEP;
                        default -> {
                            DropRule defaultDropRule = YigdConfig.getConfig().compatConfig.defaultCuriosDropRule;
                            if (defaultDropRule == DropRule.PUT_IN_GRAVE)
                                yield NeoForge.EVENT_BUS.post(new YigdEvents.DropRuleEvent(stack, -1, context, true)).getDropRule();
                            else
                                yield defaultDropRule;
                        }
                    };
                }
            }
        }

        @Override
        public NonNullList<GraveItem> getAsGraveItemList() {
            NonNullList<GraveItem> allItems = NonNullList.create();
            for (CuriosSlotEntry slotEntry : this.inventory.values()) {
                allItems.addAll(slotEntry.normal);
                allItems.addAll(slotEntry.cosmetic);
            }
            return allItems;
        }

        @Override
        public CompatComponent<Map<String, CuriosSlotEntry>> filterInv(Predicate<DropRule> predicate) {
            Map<String, CuriosSlotEntry> filtered = new HashMap<>();
            for (Map.Entry<String, CuriosSlotEntry> entry : this.inventory.entrySet()) {
                CuriosSlotEntry inventorySlot = entry.getValue();
                NonNullList<GraveItem> normalSlot = NonNullList.create();
                NonNullList<GraveItem> cosmeticSlot = NonNullList.create();
                for (GraveItem graveItem : inventorySlot.normal) {
                    if (predicate.test(graveItem.dropRule)) {
                        normalSlot.add(graveItem);
                    } else {
                        normalSlot.add(InventoryComponent.EMPTY_GRAVE_ITEM);
                    }
                }
                for (GraveItem graveItem : inventorySlot.cosmetic) {
                    if (predicate.test(graveItem.dropRule)) {
                        cosmeticSlot.add(graveItem);
                    } else {
                        cosmeticSlot.add(InventoryComponent.EMPTY_GRAVE_ITEM);
                    }
                }

                filtered.put(entry.getKey(), new CuriosSlotEntry(normalSlot, cosmeticSlot));
            }
            return new CuriosCompatComponent(filtered);
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            for (CuriosSlotEntry inventorySlot : this.inventory.values()) {
                for (GraveItem graveItem : inventorySlot.normal) {
                    ItemStack stack = graveItem.stack;
                    if (predicate.test(stack)) {
                        stack.shrink(itemCount);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void clear() {
            for (CuriosSlotEntry inventorySlot : this.inventory.values()) {
                Collections.fill(inventorySlot.normal, InventoryComponent.EMPTY_GRAVE_ITEM);
                Collections.fill(inventorySlot.cosmetic, InventoryComponent.EMPTY_GRAVE_ITEM);
            }
        }

        @Override
        public CompoundTag writeNbt(HolderLookup.Provider registries) {
            CompoundTag nbt = new CompoundTag();
            for (Map.Entry<String, CuriosSlotEntry> entry : this.inventory.entrySet()) {
                CuriosSlotEntry inventorySlot = entry.getValue();
                CompoundTag slotNbt = new CompoundTag();
                CompoundTag normalNbt = InventoryComponent.listToNbt(inventorySlot.normal, graveItem -> {
                    CompoundTag itemNbt = (CompoundTag) graveItem.stack.save(registries);
                    itemNbt.putString("dropRule", graveItem.dropRule.name());

                    return itemNbt;
                }, graveItem -> graveItem.stack.isEmpty());
                CompoundTag cosmeticNbt = InventoryComponent.listToNbt(inventorySlot.cosmetic, graveItem -> {
                    CompoundTag itemNbt = (CompoundTag) graveItem.stack.save(registries);
                    itemNbt.putString("dropRule", graveItem.dropRule.name());

                    return itemNbt;
                }, graveItem -> graveItem.stack.isEmpty());

                slotNbt.put("normal", normalNbt);
                slotNbt.put("cosmetic", cosmeticNbt);

                nbt.put(entry.getKey(), slotNbt);
            }

            return nbt;
        }
    }

    public record CuriosSlotEntry(NonNullList<GraveItem> normal, NonNullList<GraveItem> cosmetic) {
        private void addAllNonEmptyToList(Collection<GraveItem> list) {
            this.addAllNonEmptyToList(list, graveItem -> graveItem);
        }
        private void addAllNonEmptyToStackList(Collection<ItemStack> list) {
            this.addAllNonEmptyToList(list, graveItem -> graveItem.stack);
        }
        private<T> void addAllNonEmptyToList(Collection<T> list, Function<GraveItem, T> mapFunc) {
            for (GraveItem graveItem : this.normal) {
                if (!graveItem.stack.isEmpty())
                    list.add(mapFunc.apply(graveItem.copy()));
            }
            for (GraveItem graveItem : this.cosmetic) {
                if (!graveItem.stack.isEmpty())
                    list.add(mapFunc.apply(graveItem.copy()));
            }
        }
    }
}
