package com.b1n_ry.yigd.enchantment;

import com.b1n_ry.yigd.config.YigdConfig;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;

public class DeathSightEnchantment extends Enchantment {
    public DeathSightEnchantment(Rarity weight, EquipmentSlot... slotTypes) {
        super(weight, EnchantmentTarget.ARMOR_HEAD, slotTypes);
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
