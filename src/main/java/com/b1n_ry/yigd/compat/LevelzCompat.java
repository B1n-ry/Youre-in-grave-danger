package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.data.DeathContext;
import net.levelz.access.PlayerStatsManagerAccess;
import net.levelz.stats.PlayerStatsManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
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
        public CompatComponent<Float> handleDropRules(DeathContext context) {
            return new LevelzCompatComponent(0f);
        }

        @Override
        public DefaultedList<ItemStack> getAsStackList() {
            return DefaultedList.of();
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
        public boolean isEmpty() {
            return this.inventory == 0f;
        }

        @Override
        public NbtCompound writeNbt() {
            NbtCompound nbt = new NbtCompound();
            nbt.putFloat("value", this.inventory);
            return nbt;
        }
    }
}
