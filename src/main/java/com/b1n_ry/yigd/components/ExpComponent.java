package com.b1n_ry.yigd.components;

import com.b1n_ry.yigd.config.ExpDropBehaviour;
import com.b1n_ry.yigd.config.YigdConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

public class ExpComponent {
    private final int xpToDrop;

    public ExpComponent(ServerPlayerEntity player) {
        this.xpToDrop = this.getXpDropAmount(player);
    }
    private ExpComponent(int xpToDrop) {
        this.xpToDrop = xpToDrop;
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

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("value", this.xpToDrop);
        return nbt;
    }

    public static ExpComponent fromNbt(NbtCompound nbt) {
        int xpToDrop = nbt.getInt("value");
        return new ExpComponent(xpToDrop);
    }
}
