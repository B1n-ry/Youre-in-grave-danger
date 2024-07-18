package com.b1n_ry.yigd.enchantment;

import com.b1n_ry.yigd.config.YigdConfig;
import net.minecraft.enchantment.Enchantment;

public class DeathSightEnchantment extends Enchantment {
    public DeathSightEnchantment(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isTreasure() {
        return YigdConfig.getConfig().extraFeatures.deathSightEnchant.isTreasure;
    }
    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return YigdConfig.getConfig().extraFeatures.deathSightEnchant.isAvailableForEnchantedBookOffer;
    }
    @Override
    public boolean isAvailableForRandomSelection() {
        return YigdConfig.getConfig().extraFeatures.deathSightEnchant.isAvailableForRandomSelection;
    }
}
