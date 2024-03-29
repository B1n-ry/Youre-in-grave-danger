package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.glisco.numismaticoverhaul.currency.CurrencyConverter;
import com.glisco.numismaticoverhaul.item.CoinItem;
import com.glisco.numismaticoverhaul.item.CurrencyItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;

import java.util.function.Predicate;

public class NumismaticOverhaulCompat implements InvModCompat<Long> {
    @Override
    public String getModName() {
        return "numismatic overhaul";
    }

    @Override
    public void clear(ServerPlayerEntity player) {
        CurrencyComponent component = ModComponents.CURRENCY.get(player);
        long value = component.getValue();
        component.silentModify(-value);
    }

    @Override
    public CompatComponent<Long> readNbt(NbtCompound nbt) {
        long value = nbt.getLong("value");
        long dropValue = nbt.contains("dropValue") ? nbt.getLong("dropValue") : 0L;
        long keepValue = nbt.contains("keepValue") ? nbt.getLong("keepValue") : 0L;
        long destroyValue = nbt.contains("destroyValue") ? nbt.getLong("destroyValue") : 0L;
        long graveValue = nbt.contains("graveValue") ? nbt.getLong("graveValue") : 0L;

        return new NumismaticCompatComponent(value, dropValue, keepValue, destroyValue, graveValue);
    }

    @Override
    public CompatComponent<Long> getNewComponent(ServerPlayerEntity player) {
        return new NumismaticCompatComponent(player);
    }

    private static class NumismaticCompatComponent extends CompatComponent<Long> {
        private long dropValue = 0;
        private long keepValue = 0;
        private long destroyValue = 0;
        private long graveValue = 0;

        public NumismaticCompatComponent(ServerPlayerEntity player) {
            super(player);
        }
        public NumismaticCompatComponent(long inventory, long dropValue, long keepValue, long destroyValue, long graveValue) {
            super(inventory);
            this.dropValue = dropValue;
            this.keepValue = keepValue;
            this.destroyValue = destroyValue;
            this.graveValue = graveValue;
        }

        @Override
        public Long getInventory(ServerPlayerEntity player) {
            return ModComponents.CURRENCY.get(player).getValue();
        }

        @Override
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayerEntity merger) {
            this.inventory += (long) mergingComponent.inventory;
            return DefaultedList.of();
        }

        @Override
        public DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player) {
            ModComponents.CURRENCY.get(player).modify(this.inventory);
            return DefaultedList.of();
        }

        @Override
        public void handleDropRules(DeathContext context) {
            YigdConfig.CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

            int dropRate = context.world().getGameRules().get(NumismaticOverhaul.MONEY_DROP_PERCENTAGE).get();
            float dropFactor = dropRate * 0.01f;
            float keepFactor = Math.max(1 - dropFactor, 0);  // In case someone set the gamerule to like 10000, so negative numbers won't be reached

            this.dropValue = 0;
            this.destroyValue = 0;
            this.graveValue = 0;

            this.keepValue = (long) (this.inventory * keepFactor);
            this.inventory -= this.keepValue;

            for (ItemStack stack : CurrencyConverter.getAsItemStackArray(this.inventory)) {
                DropRule dropRule = compatConfig.defaultNumismaticDropRule;
                long itemValue;
                if (stack.getItem() instanceof CurrencyItem cItem) {
                    itemValue = cItem.getValue(stack);
                } else {
                    continue;  // Should not be the case, but if the item doesn't have a currency in this component, we ignore it
                }

                if (dropRule == DropRule.PUT_IN_GRAVE)
                    dropRule = DropRuleEvent.EVENT.invoker().getDropRule(stack, -1, context, true);
                switch (dropRule) {
                    case DROP -> {
                        this.inventory -= itemValue;
                        this.dropValue += itemValue;
                    }
                    case DESTROY -> {
                        this.inventory -= itemValue;
                        this.destroyValue += itemValue;
                    }
                    case KEEP -> {
                        this.inventory -= itemValue;
                        this.keepValue += itemValue;
                    }
                    case PUT_IN_GRAVE -> {
                        this.inventory -= itemValue;
                        this.graveValue += itemValue;
                    }
                }
            }
        }

        @Override
        public DefaultedList<Pair<ItemStack, DropRule>> getAsStackDropList() {
            DefaultedList<Pair<ItemStack, DropRule>> list = DefaultedList.of();
            if (this.graveValue != 0) {
                for (ItemStack stack : CurrencyConverter.getAsItemStackArray(this.graveValue)) {
                    list.add(new Pair<>(stack, DropRule.PUT_IN_GRAVE));
                }
            }
            if (this.dropValue != 0) {
                for (ItemStack stack : CurrencyConverter.getAsItemStackArray(this.dropValue)) {
                    list.add(new Pair<>(stack, DropRule.DROP));
                }
            }
            if (this.keepValue != 0) {
                for (ItemStack stack : CurrencyConverter.getAsItemStackArray(this.keepValue)) {
                    list.add(new Pair<>(stack, DropRule.KEEP));
                }
            }
            if (this.destroyValue != 0) {
                for (ItemStack stack : CurrencyConverter.getAsItemStackArray(this.destroyValue)) {
                    list.add(new Pair<>(stack, DropRule.DESTROY));
                }
            }

            return list;
        }

        @Override
        public CompatComponent<Long> filterInv(Predicate<DropRule> predicate) {
            long totalValue = 0;
            long dropValue = 0;
            long keepValue = 0;
            long destroyValue = 0;
            long graveValue = 0;

            if (predicate.test(DropRule.DROP)) {
                totalValue += this.dropValue;
                dropValue = this.dropValue;
            }
            if (predicate.test(DropRule.KEEP)) {
                totalValue += this.keepValue;
                keepValue = this.keepValue;
            }
            if (predicate.test(DropRule.DESTROY)) {
                totalValue += this.destroyValue;
                destroyValue = this.destroyValue;
            }
            if (predicate.test(DropRule.PUT_IN_GRAVE)) {
                totalValue += this.graveValue;
                graveValue = this.graveValue;
            }

            return new NumismaticCompatComponent(totalValue, dropValue, keepValue, destroyValue, graveValue);
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            ItemStack[] stacks = CurrencyConverter.getAsItemStackArray(this.inventory);
            for (ItemStack stack : stacks) {
                if (predicate.test(stack)) {
                    this.inventory -= ((CoinItem) stack.getItem()).currency.getRawValue(itemCount);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            this.inventory = 0L;
        }

        @Override
        public boolean containsGraveItems() {
            return this.graveValue != 0L;
        }

        @Override
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putLong("value", this.inventory);
            nbt.putLong("dropValue", this.dropValue);
            nbt.putLong("keepValue", this.keepValue);
            nbt.putLong("destroyValue", this.destroyValue);
            nbt.putLong("graveValue", this.graveValue);

            return nbt;
        }
    }
}
