package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.NumismaticOverhaul;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import com.glisco.numismaticoverhaul.currency.CurrencyConverter;
import com.glisco.numismaticoverhaul.item.CoinItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

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
        component.modify(-value);
    }

    @Override
    public CompatComponent<Long> readNbt(NbtCompound nbt) {
        long value = nbt.getLong("value");
        return new NumismaticCompatComponent(value);
    }

    @Override
    public CompatComponent<Long> getNewComponent(ServerPlayerEntity player) {
        return new NumismaticCompatComponent(player);
    }

    private static class NumismaticCompatComponent extends CompatComponent<Long> {

        public NumismaticCompatComponent(ServerPlayerEntity player) {
            super(player);
        }
        public NumismaticCompatComponent(Long inventory) {
            super(inventory);
        }

        @Override
        public Long getInventory(ServerPlayerEntity player) {
            return ModComponents.CURRENCY.get(player).getValue();
        }

        @Override
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent) {
            this.inventory += (long) mergingComponent.inventory;
            return DefaultedList.of();
        }

        @Override
        public DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player) {
            ModComponents.CURRENCY.get(player).modify(this.inventory);
            return DefaultedList.of();
        }

        @Override
        public CompatComponent<Long> handleDropRules(DeathContext context) {
            YigdConfig.CompatConfig compatConfig = YigdConfig.getConfig().compatConfig;

            int dropRate = context.getWorld().getGameRules().get(NumismaticOverhaul.MONEY_DROP_PERCENTAGE).get();
            float dropFactor = dropRate * 0.01f;
            float keepFactor = Math.max(1 - dropFactor, 0);  // In case someone set the gamerule to like 10000, so negative numbers won't be reached

            long soulbound = (long) (this.inventory * keepFactor);
            this.inventory -= soulbound;

            Vec3d deathPos = context.getDeathPos();
            for (ItemStack stack : CurrencyConverter.getAsItemStackArray(this.inventory)) {
                DropRule dropRule = compatConfig.defaultNumismaticDropRule;
                if (dropRule == DropRule.PUT_IN_GRAVE)
                    dropRule = DropRuleEvent.EVENT.invoker().getDropRule(stack, -1, context, true);
                switch (dropRule) {
                    case DROP -> InventoryComponent.dropItemIfToBeDropped(stack, deathPos.x, deathPos.y, deathPos.z, context.getWorld());
                    case DESTROY -> this.inventory = 0L;
                    case KEEP -> soulbound += this.inventory;
                }
            }

            return new NumismaticCompatComponent(soulbound);
        }

        @Override
        public DefaultedList<ItemStack> getAsStackList() {
            return DefaultedList.of();
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
        public boolean isEmpty() {
            return this.inventory == 0L;
        }

        @Override
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putLong("value", this.inventory);
            return nbt;
        }
    }
}
