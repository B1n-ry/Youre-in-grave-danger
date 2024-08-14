package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.YigdEvents;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.compat.CuriosCompat.CuriosSlotEntry;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.*;
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
            NonNullList<Tuple<ItemStack, DropRule>> normalSlot = InventoryComponent.listFromNbt(slotNbt.getCompound("normal"), itemNbt -> {
                ItemStack stack = ItemStack.parse(registries, itemNbt).orElse(ItemStack.EMPTY);
                DropRule dropRule;
                if (itemNbt.contains("dropRule")) {
                    // We need to check in case the drop rule is a trinket drop rule (only has one difference and that is trinkets have DEFAULT)
                    String dropRuleString = itemNbt.getString("dropRule");
                    if (dropRuleString.equals("DEFAULT")) {
                        dropRule = YigdConfig.getConfig().compatConfig.defaultTrinketsDropRule;
                    } else {
                        dropRule = DropRule.valueOf(dropRuleString);
                    }
                } else {
                    dropRule = YigdConfig.getConfig().compatConfig.defaultTrinketsDropRule;
                }

                return new Tuple<>(stack, dropRule);
            }, InventoryComponent.EMPTY_ITEM_PAIR);
            NonNullList<Tuple<ItemStack, DropRule>> cosmeticSlot = InventoryComponent.listFromNbt(slotNbt.getCompound("cosmetic"), itemNbt -> {
                ItemStack stack = ItemStack.parse(registries, itemNbt).orElse(ItemStack.EMPTY);
                DropRule dropRule;
                if (itemNbt.contains("dropRule")) {
                    // We need to check in case the drop rule is a trinket drop rule (only has one difference and that is trinkets have DEFAULT)
                    String dropRuleString = itemNbt.getString("dropRule");
                    if (dropRuleString.equals("DEFAULT")) {
                        dropRule = YigdConfig.getConfig().compatConfig.defaultTrinketsDropRule;
                    } else {
                        dropRule = DropRule.valueOf(dropRuleString);
                    }
                } else {
                    dropRule = YigdConfig.getConfig().compatConfig.defaultTrinketsDropRule;
                }

                return new Tuple<>(stack, dropRule);
            }, InventoryComponent.EMPTY_ITEM_PAIR);

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
                NonNullList<Tuple<ItemStack, DropRule>> normalItems = NonNullList.create();
                NonNullList<Tuple<ItemStack, DropRule>> cosmeticItems = NonNullList.create();

                IDynamicStackHandler normalEquipped = stacksHandler.getStacks();
                for (int i = 0; i < normalEquipped.getSlots(); i++) {
                    normalItems.add(new Tuple<>(normalEquipped.getStackInSlot(i).copy(), DropRule.PUT_IN_GRAVE));
                }
                IDynamicStackHandler cosmeticEquipped = stacksHandler.getCosmeticStacks();
                for (int i = 0; i < cosmeticEquipped.getSlots(); i++) {
                    cosmeticItems.add(new Tuple<>(cosmeticEquipped.getStackInSlot(i).copy(), DropRule.PUT_IN_GRAVE));
                }

                inventory.put(entry.getKey(), new CuriosSlotEntry(normalItems, cosmeticItems));
            }
            return inventory;
        }

        @Override
        public NonNullList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayer merger) {
            NonNullList<ItemStack> extraItems = NonNullList.create();
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
                    Tuple<ItemStack, DropRule> mergingTuple = mergingSlot.normal.get(i);
                    ItemStack mergingStack = mergingTuple.getA();
                    if (mergingStack.isEmpty()) continue;

                    ItemStack thisStack = thisSlot.normal.get(i).getA();
                    if (!thisStack.isEmpty()) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    thisSlot.normal.set(i, mergingTuple);
                }
                for (int i = 0; i < mergingSlot.cosmetic.size(); i++) {
                    Tuple<ItemStack, DropRule> mergingTuple = mergingSlot.cosmetic.get(i);
                    ItemStack mergingStack = mergingTuple.getA();
                    if (mergingStack.isEmpty()) continue;

                    ItemStack thisStack = thisSlot.cosmetic.get(i).getA();
                    if (!thisStack.isEmpty()) {
                        extraItems.add(mergingStack);
                        continue;
                    }

                    thisSlot.cosmetic.set(i, mergingTuple);
                }
            }
            return extraItems;
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
                    entry.getValue().addAllNonEmptyToList(extraItems);
                    continue;
                }
                ICurioStacksHandler stacksHandler = stacksHandlerMap.get(key);
                CuriosSlotEntry slotEntry = entry.getValue();

                IDynamicStackHandler normalEquipped = stacksHandler.getStacks();
                IDynamicStackHandler cosmeticEquipped = stacksHandler.getCosmeticStacks();
                for (int i = 0; i < slotEntry.normal.size(); i++) {
                    Tuple<ItemStack, DropRule> tuple = slotEntry.normal.get(i);
                    if (i > normalEquipped.getSlots()) {
                        extraItems.add(tuple.getA());
                    }

                    normalEquipped.setStackInSlot(i, tuple.getA());
                }
                for (int i = 0; i < slotEntry.cosmetic.size(); i++) {
                    Tuple<ItemStack, DropRule> tuple = slotEntry.cosmetic.get(i);
                    if (i > cosmeticEquipped.getSlots()) {
                        extraItems.add(tuple.getA());
                    }

                    cosmeticEquipped.setStackInSlot(i, tuple.getA());
                }
            }
            return extraItems;
        }

        @Override
        public void handleDropRules(DeathContext context) {
            for (Map.Entry<String, CuriosSlotEntry> entry : this.inventory.entrySet()) {
                String key = entry.getKey();
                CuriosSlotEntry inventorySlot = entry.getValue();
                for (int i = 0; i < inventorySlot.normal.size(); i++) {
                    Tuple<ItemStack, DropRule> pair = inventorySlot.normal.get(i);
                    ItemStack stack = pair.getA();
                    DropRule dropRule = switch(AccessoriesAPI.getOrDefaultAccessory(stack)
                            .getDropRule(stack, SlotReference.of(context.player(), key, i), context.deathSource())) {
                        case DESTROY -> DropRule.DESTROY;
                        case KEEP -> DropRule.KEEP;
                        default -> {
                            DropRule defaultDropRule = YigdConfig.getConfig().compatConfig.defaultAccessoriesDropRule;
                            if (defaultDropRule == DropRule.PUT_IN_GRAVE)
                                yield NeoForge.EVENT_BUS.post(new YigdEvents.DropRuleEvent(stack, -1, context, true)).getDropRule();
                            else
                                yield defaultDropRule;
                        }
                    };

                    pair.setB(dropRule);
                }
            }
        }

        @Override
        public NonNullList<Tuple<ItemStack, DropRule>> getAsStackDropList() {
            NonNullList<Tuple<ItemStack, DropRule>> allItems = NonNullList.create();
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
                NonNullList<Tuple<ItemStack, DropRule>> normalSlot = NonNullList.create();
                NonNullList<Tuple<ItemStack, DropRule>> cosmeticSlot = NonNullList.create();
                for (Tuple<ItemStack, DropRule> pair : inventorySlot.normal) {
                    if (predicate.test(pair.getB())) {
                        normalSlot.add(pair);
                    } else {
                        normalSlot.add(InventoryComponent.EMPTY_ITEM_PAIR);
                    }
                }
                for (Tuple<ItemStack, DropRule> pair : inventorySlot.cosmetic) {
                    if (predicate.test(pair.getB())) {
                        cosmeticSlot.add(pair);
                    } else {
                        cosmeticSlot.add(InventoryComponent.EMPTY_ITEM_PAIR);
                    }
                }

                filtered.put(entry.getKey(), new CuriosSlotEntry(normalSlot, cosmeticSlot));
            }
            return new CuriosCompatComponent(filtered);
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            for (CuriosSlotEntry inventorySlot : this.inventory.values()) {
                for (Tuple<ItemStack, DropRule> pair : inventorySlot.normal) {
                    ItemStack stack = pair.getA();
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
                Collections.fill(inventorySlot.normal, InventoryComponent.EMPTY_ITEM_PAIR);
                Collections.fill(inventorySlot.cosmetic, InventoryComponent.EMPTY_ITEM_PAIR);
            }
        }

        @Override
        public CompoundTag writeNbt(HolderLookup.Provider registries) {
            CompoundTag nbt = new CompoundTag();
            for (Map.Entry<String, CuriosSlotEntry> entry : this.inventory.entrySet()) {
                CuriosSlotEntry inventorySlot = entry.getValue();
                CompoundTag slotNbt = new CompoundTag();
                CompoundTag normalNbt = InventoryComponent.listToNbt(inventorySlot.normal, pair -> {
                    CompoundTag itemNbt = (CompoundTag) pair.getA().save(registries);
                    itemNbt.putString("dropRule", pair.getB().name());

                    return itemNbt;
                }, pair -> pair.getA().isEmpty());
                CompoundTag cosmeticNbt = InventoryComponent.listToNbt(inventorySlot.cosmetic, pair -> {
                    CompoundTag itemNbt = (CompoundTag) pair.getA().save(registries);
                    itemNbt.putString("dropRule", pair.getB().name());

                    return itemNbt;
                }, pair -> pair.getA().isEmpty());

                slotNbt.put("normal", normalNbt);
                slotNbt.put("cosmetic", cosmeticNbt);

                nbt.put(entry.getKey(), slotNbt);
            }

            return nbt;
        }
    }

    public record CuriosSlotEntry(NonNullList<Tuple<ItemStack, DropRule>> normal, NonNullList<Tuple<ItemStack, DropRule>> cosmetic) {
        private void addAllNonEmptyToList(Collection<ItemStack> list) {
            for (Tuple<ItemStack, DropRule> pair : this.normal) {
                list.add(pair.getA().copy());
            }
            for (Tuple<ItemStack, DropRule> pair : this.cosmetic) {
                list.add(pair.getA().copy());
            }
        }
    }
}
