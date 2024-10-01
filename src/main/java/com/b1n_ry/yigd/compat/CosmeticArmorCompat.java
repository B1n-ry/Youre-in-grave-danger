package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.YigdEvents;
import com.b1n_ry.yigd.util.DropRule;
import lain.mods.cos.impl.ModObjects;
import lain.mods.cos.impl.inventory.InventoryCosArmor;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;

import java.util.function.Predicate;

public class CosmeticArmorCompat implements InvModCompat<NonNullList<Tuple<ItemStack, DropRule>>> {
    @Override
    public String getModName() {
        return "cosmeticarmor";
    }

    @Override
    public void clear(ServerPlayer player) {
        InventoryCosArmor inv = ModObjects.invMan.getCosArmorInventory(player.getUUID());
        inv.clearContent();
    }

    @Override
    public CompatComponent<NonNullList<Tuple<ItemStack, DropRule>>> readNbt(CompoundTag nbt, HolderLookup.Provider registries) {
        NonNullList<Tuple<ItemStack, DropRule>> items = InventoryComponent.listFromNbt(nbt, itemTag -> {
            DropRule dropRule = DropRule.valueOf(itemTag.getString("dropRule"));
            ItemStack stack = ItemStack.parse(registries, itemTag).orElse(ItemStack.EMPTY);
            return new Tuple<>(stack, dropRule);
        }, InventoryComponent.EMPTY_ITEM_PAIR);
        return new CosmeticArmorCompatComponent(items);
    }

    @Override
    public CompatComponent<NonNullList<Tuple<ItemStack, DropRule>>> getNewComponent(ServerPlayer player) {
        return new CosmeticArmorCompatComponent(player);
    }

    static class CosmeticArmorCompatComponent extends CompatComponent<NonNullList<Tuple<ItemStack, DropRule>>> {
        public CosmeticArmorCompatComponent(ServerPlayer player) {
            super(player);
        }
        public CosmeticArmorCompatComponent(NonNullList<Tuple<ItemStack, DropRule>> inventory) {
            super(inventory);
        }

        @Override
        public NonNullList<Tuple<ItemStack, DropRule>> getInventory(ServerPlayer player) {
            InventoryCosArmor inventory = ModObjects.invMan.getCosArmorInventory(player.getUUID());
            NonNullList<Tuple<ItemStack, DropRule>> list = NonNullList.create();
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                list.add(new Tuple<>(stack, DropRule.PUT_IN_GRAVE));
            }
            return list;
        }

        @Override
        public NonNullList<ItemStack> storeToPlayer(ServerPlayer player) {
            NonNullList<ItemStack> extraItems = NonNullList.create();
            InventoryCosArmor cosArmor = ModObjects.invMan.getCosArmorInventory(player.getUUID());

            for (int i = 0; i < cosArmor.getContainerSize(); i++) {
                if (i >= this.inventory.size()) break;
                ItemStack stack = this.inventory.get(i).getA();
                if (cosArmor.getItem(i).isEmpty()) {
                    cosArmor.setItem(i, stack);
                } else {
                    extraItems.add(stack);
                }
            }
            return extraItems;
        }

        @Override
        public void handleDropRules(DeathContext context) {
            YigdConfig.CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

            for (Tuple<ItemStack, DropRule> tuple : this.inventory) {
                if (tuple.getA().isEmpty()) continue;
                DropRule dropRule = compatConfig.defaultCosmeticArmorDropRule;

                if (dropRule != DropRule.PUT_IN_GRAVE) continue;
                dropRule = NeoForge.EVENT_BUS.post(new YigdEvents.DropRuleEvent(tuple.getA(), -1, context, true)).getDropRule();

                tuple.setB(dropRule);
            }
        }

        @Override
        public NonNullList<Tuple<ItemStack, DropRule>> getAsStackDropList() {
            return NonNullList.copyOf(this.inventory);
        }

        @Override
        public void clear() {
            this.inventory.clear();
        }

        @Override
        public CompoundTag writeNbt(HolderLookup.Provider registries) {
            return InventoryComponent.listToNbt(this.inventory, tuple -> {
                CompoundTag itemTag = (CompoundTag) tuple.getA().save(registries);
                itemTag.putString("dropRule", tuple.getB().toString());
                return itemTag;
            }, tuple -> tuple.getA().isEmpty());
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            for (Tuple<ItemStack, DropRule> tuple : this.inventory) {
                ItemStack stack = tuple.getA();
                if (predicate.test(stack)) {
                    stack.shrink(itemCount);
                    return true;
                }
            }
            return false;
        }

        @Override
        public CompatComponent<NonNullList<Tuple<ItemStack, DropRule>>> filterInv(Predicate<DropRule> predicate) {
            NonNullList<Tuple<ItemStack, DropRule>> list = NonNullList.create();
            for (Tuple<ItemStack, DropRule> tuple : this.inventory) {
                if (predicate.test(tuple.getB())) {
                    list.add(tuple);
                } else {
                    list.add(InventoryComponent.EMPTY_ITEM_PAIR);
                }
            }
            return new CosmeticArmorCompatComponent(list);
        }

        @Override
        public NonNullList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayer merger) {
            NonNullList<ItemStack> extraItems = NonNullList.create();

            @SuppressWarnings("unchecked")
            NonNullList<Tuple<ItemStack, DropRule>> mergingItems = (NonNullList<Tuple<ItemStack, DropRule>>) mergingComponent.inventory;
            for (int i = 0; i < mergingItems.size(); i++) {
                Tuple<ItemStack, DropRule> tuple = mergingItems.get(i);
                ItemStack mergingStack = tuple.getA();
                if (i >= this.inventory.size()) {
                    extraItems.add(mergingStack);
                    continue;
                }
                ItemStack thisStack = this.inventory.get(i).getA();
                if (thisStack.isEmpty()) {
                    this.inventory.set(i, new Tuple<>(mergingStack, tuple.getB()));
                } else {
                    extraItems.add(mergingStack);
                }
            }
            return extraItems;
        }
    }
}
