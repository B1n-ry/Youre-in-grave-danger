package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.config.ExpDropBehaviour;
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

    public int getXpDropAmount(ServerPlayerEntity player) {
        YigdConfig config = YigdConfig.getConfig();

        if (config.expConfig.dropBehaviour == ExpDropBehaviour.VANILLA) {
            return Math.min(7 * player.experienceLevel, 100);
        } else if (config.expConfig.dropBehaviour == ExpDropBehaviour.PERCENTAGE) {
            int currentLevel = player.experienceLevel;
            int totalExperience;
            if (currentLevel >= 32) {
                totalExperience = (int) (4.5 * Math.pow(currentLevel, 2) - 162.5 * currentLevel + 2220);
            } else if (currentLevel >= 17) {
                totalExperience = (int) (2.5 * Math.pow(currentLevel, 2) - 40.5 * currentLevel + 360);
            } else {
                totalExperience = (int) (Math.pow(currentLevel, 2) + 6 * currentLevel);
            }
            totalExperience += player.experienceProgress;

            return (int) ((config.expConfig.dropPercentage / 100f) * (float) totalExperience);
        }

        return 0;
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
