package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.util.DropRule;
import net.levelz.access.LevelManagerAccess;
import net.levelz.level.LevelManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public class LevelzCompat implements InvModCompat<Float> {
    @Override
    public String getModName() {
        return "levelz";
    }

    @Override
    public void clear(ServerPlayer player) {
        LevelManager manager = ((LevelManagerAccess) player).getLevelManager();
        manager.setLevelProgress(0);
    }

    @Override
    public CompatComponent<Float> readNbt(CompoundTag nbt, HolderLookup.Provider registryLookup) {
        float value = nbt.getFloat("value");
        return new LevelzCompatComponent(value);
    }

    @Override
    public CompatComponent<Float> getNewComponent(ServerPlayer player) {
        return new LevelzCompatComponent(player);
    }

    private static class LevelzCompatComponent extends CompatComponent<Float> {

        public LevelzCompatComponent(ServerPlayer player) {
            super(player);
        }

        public LevelzCompatComponent(Float inventory) {
            super(inventory);
        }

        @Override
        public Float getInventory(ServerPlayer player) {
            LevelManager manager = ((LevelManagerAccess) player).getLevelManager();

            return manager.getLevelProgress();
        }

        @Override
        public NonNullList<ItemStack> merge(CompatComponent<?> mergingComponent, ServerPlayer merger) {
            this.inventory += (float) mergingComponent.inventory;
            return NonNullList.create();
        }

        @Override
        public NonNullList<ItemStack> storeToPlayer(ServerPlayer player) {
            LevelManager manager = ((LevelManagerAccess) player).getLevelManager();
            manager.setLevelProgress(this.inventory);
            return NonNullList.create();
        }

        @Override
        public void handleDropRules(DeathContext context) {
            // Only default drop rule should matter, which is tested in #filterInv()
            // There are no specific drop rules for this component (except drop). Only a set standard from the config
        }

        @Override
        public NonNullList<Tuple<ItemStack, DropRule>> getAsStackDropList() {
            return NonNullList.create();
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
        public CompoundTag writeNbt(HolderLookup.Provider registryLookup) {
            CompoundTag nbt = new CompoundTag();
            nbt.putFloat("value", this.inventory);
            return nbt;
        }
    }
}
