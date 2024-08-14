package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.config.YigdConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;

public class ExpComponent {
    private final double originalXp;
    private int storedXp;

    public ExpComponent(ServerPlayer player) {
        this.originalXp = this.getTotalExperience(player);
        this.storedXp = this.getXpDropAmount(player);
    }
    private ExpComponent(int storedXp, double originalXp) {
        this.storedXp = storedXp;
        this.originalXp = originalXp;
    }

    public void setStoredXp(int storedXp) {
        this.storedXp = storedXp;
    }

    public int getStoredXp() {
        return this.storedXp;
    }
    public double getOriginalXp() {
        return this.originalXp;
    }

    public int getXpDropAmount(ServerPlayer player) {
        YigdConfig config = YigdConfig.getConfig();

        double totalExperience = this.getTotalExperience(player);
        int percentageDrop = (int) ((config.expConfig.dropPercentage / 100f) * totalExperience);
        int vanillaDrop = player.getExperienceReward(player.serverLevel(), null);
        return switch (config.expConfig.dropBehaviour) {
            case BEST_OF_BOTH -> Math.max(vanillaDrop, percentageDrop);
            case WORST_OF_BOTH -> Math.min(vanillaDrop, percentageDrop);
            case PERCENTAGE -> percentageDrop;
            case VANILLA -> vanillaDrop;
        };
    }

    private double getTotalExperience(ServerPlayer player) {
        // This for some reason works more reliably than to get player.totalExperience directly
        int currentLevel = player.experienceLevel;
        double totalExperience;
        if (currentLevel >= 32) {
            totalExperience = 4.5 * Math.pow(currentLevel, 2) - 162.5 * currentLevel + 2220;
        } else if (currentLevel >= 17) {
            totalExperience = 2.5 * Math.pow(currentLevel, 2) - 40.5 * currentLevel + 360;
        } else {
            totalExperience = Math.pow(currentLevel, 2) + 6 * currentLevel;
        }
        totalExperience += player.getXpNeededForNextLevel() * player.experienceProgress;
        return totalExperience;
    }

    public int getXpLevel() {
        int totalXp = this.storedXp;

        int i;
        for (i = 0; totalXp >= 0; i++) {
            if (i < 16) {
                totalXp -= (2 * i) + 7;
            } else if(i < 31) {
                totalXp -= (5 * i) - 38;
            } else {
                totalXp -= (9 * i) - 158;
            }
        }

        return i - 1;
    }

    public boolean isEmpty() {
        return this.storedXp == 0;
    }

    public ExpComponent getSoulboundExp() {
        YigdConfig config = YigdConfig.getConfig();
        float soulboundFactor = config.expConfig.keepPercentage / 100f;
        int keepXp = (int) (this.storedXp * soulboundFactor);
        this.storedXp -= keepXp;

        if (this.storedXp < 0) this.storedXp = 0;

        return new ExpComponent(keepXp, this.originalXp);
    }

    public void dropAll(ServerLevel world, Vec3 pos) {
        ExperienceOrb.award(world, pos, this.storedXp);
    }

    public void applyToPlayer(ServerPlayer player) {
        player.giveExperiencePoints(this.storedXp);
    }

    public void clear() {
        this.storedXp = 0;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("value", this.storedXp);
        nbt.putDouble("original", this.originalXp);
        return nbt;
    }

    public static ExpComponent fromNbt(CompoundTag nbt) {
        int xpToDrop = nbt.getInt("value");
        double originalXp = nbt.contains("original") ? nbt.getDouble("original") : xpToDrop;
        return new ExpComponent(xpToDrop, originalXp);
    }

    public static void clearXp(ServerPlayer player) {
        player.totalExperience = 0;
        player.experienceLevel = 0;
        player.experienceProgress = 0;
    }

    public ExpComponent copy() {
        return new ExpComponent(this.storedXp, this.originalXp);
    }
}
