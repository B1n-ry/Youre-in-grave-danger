package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class ExpComponent {
    private int storedXp;

    public ExpComponent(ServerPlayerEntity player) {
        this.storedXp = this.getXpDropAmount(player);
    }
    private ExpComponent(int storedXp) {
        this.storedXp = storedXp;
    }

    public int getStoredXp() {
        return this.storedXp;
    }

    public int getXpDropAmount(ServerPlayerEntity player) {
        YigdConfig config = YigdConfig.getConfig();

        double totalExperience = this.getTotalExperience(player);
        int percentageDrop = (int) ((config.expConfig.dropPercentage / 100f) * totalExperience);
        int vanillaDrop = player.getXpToDrop();
        return switch (config.expConfig.dropBehaviour) {
            case BEST_OF_BOTH -> Math.max(vanillaDrop, percentageDrop);
            case WORST_OF_BOTH -> Math.min(vanillaDrop, percentageDrop);
            case PERCENTAGE -> percentageDrop;
            case VANILLA -> vanillaDrop;
        };
    }

    private double getTotalExperience(ServerPlayerEntity player) {
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
        totalExperience += player.getNextLevelExperience() * player.experienceProgress;
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

    public void onDeath(RespawnComponent respawnComponent, DeathContext ignoredContext) {
        ExpComponent keepExp = this.getSoulboundExp();
        respawnComponent.setSoulboundExp(keepExp);
    }

    public boolean isEmpty() {
        return this.storedXp == 0;
    }

    private ExpComponent getSoulboundExp() {
        YigdConfig config = YigdConfig.getConfig();
        float soulboundFactor = config.expConfig.keepPercentage / 100f;
        int keepXp = (int) (this.storedXp * soulboundFactor);
        this.storedXp -= keepXp;

        if (this.storedXp < 0) this.storedXp = 0;

        return new ExpComponent(keepXp);
    }

    public void dropAll(ServerWorld world, Vec3d pos) {
        ExperienceOrbEntity.spawn(world, pos, this.storedXp);
    }

    public void applyToPlayer(ServerPlayerEntity player) {
        player.addExperience(this.storedXp);
    }

    public void clear() {
        this.storedXp = 0;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("value", this.storedXp);
        return nbt;
    }

    public static ExpComponent fromNbt(NbtCompound nbt) {
        int xpToDrop = nbt.getInt("value");
        return new ExpComponent(xpToDrop);
    }

    public static void clearXp(ServerPlayerEntity player) {
        player.totalExperience = 0;
        player.experienceLevel = 0;
        player.experienceProgress = 0;
    }
}
