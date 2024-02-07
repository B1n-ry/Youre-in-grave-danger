package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.util.DropRule;
import net.levelz.access.PlayerStatsManagerAccess;
import net.levelz.stats.PlayerStatsManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;

import java.util.function.Predicate;

public class LevelzCompat implements InvModCompat<Float> {
    @Override
    public String getModName() {
        return "levelz";
    }

    @Override
    public void clear(ServerPlayerEntity player) {
        PlayerStatsManager manager = ((PlayerStatsManagerAccess) player).getPlayerStatsManager();
        manager.setLevelProgress(0);
    }

    @Override
    public CompatComponent<Float> readNbt(NbtCompound nbt) {
        float value = nbt.getFloat("value");
        return new LevelzCompatComponent(value);
    }

    @Override
    public CompatComponent<Float> getNewComponent(ServerPlayerEntity player) {
        return new LevelzCompatComponent(player);
    }

    private static class LevelzCompatComponent extends CompatComponent<Float> {

        public LevelzCompatComponent(ServerPlayerEntity player) {
            super(player);
        }

        public LevelzCompatComponent(Float inventory) {
            super(inventory);
        }

        @Override
        public Float getInventory(ServerPlayerEntity player) {
            PlayerStatsManager manager = ((PlayerStatsManagerAccess) player).getPlayerStatsManager();

            return manager.getLevelProgress();
        }

        @Override
        public DefaultedList<ItemStack> merge(CompatComponent<?> mergingComponent) {
            this.inventory += (float) mergingComponent.inventory;
            return DefaultedList.of();
        }

        @Override
        public DefaultedList<ItemStack> storeToPlayer(ServerPlayerEntity player) {
            PlayerStatsManager manager = ((PlayerStatsManagerAccess) player).getPlayerStatsManager();
            manager.setLevelProgress(this.inventory);
            return DefaultedList.of();
        }

        @Override
        public void handleDropRules(DeathContext context) {
            // Only default drop rule should matter, which is tested in #filterInv()
            // There are no specific drop rules for this component (except drop). Only a set standard from the config
        }

        @Override
        public DefaultedList<Pair<ItemStack, DropRule>> getAsStackDropList() {
            return DefaultedList.of();
        }

        @Override
        public CompatComponent<Float> filterInv(Predicate<DropRule> predicate) {
            if (predicate.test(YigdConfig.getConfig().compatConfig.defaultLevelzDropRule)) {
                return new LevelzCompatComponent(this.inventory);
            } else {
                return new LevelzCompatComponent(0f);
            }
        }

        @Override
        public boolean removeItem(Predicate<ItemStack> predicate, int itemCount) {
            return false;
        }

        @Override
        public void clear() {
            this.inventory = 0f;
        }

        @Override
        public boolean containsGraveItems() {
            return this.inventory != 0f;
        }

        @Override
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putFloat("value", this.inventory);
            return nbt;
        }
    }
}
