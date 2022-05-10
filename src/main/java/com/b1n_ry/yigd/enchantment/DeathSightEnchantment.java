package com.b1n_ry.yigd.enchantment;

import com.b1n_ry.yigd.config.YigdConfig;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;

public class DeathSightEnchantment extends Enchantment {
    public DeathSightEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.ARMOR_HEAD, new EquipmentSlot[] { EquipmentSlot.HEAD });
    }

    public boolean isTreasure() {
        return YigdConfig.getConfig().utilitySettings.deathSightEnchant.isTreasure;
    }
    public boolean isAvailableForEnchantedBookOffer() {
        return YigdConfig.getConfig().utilitySettings.deathSightEnchant.villagerTrade;
    }
    public boolean isAvailableForRandomSelection() {
        return YigdConfig.getConfig().utilitySettings.deathSightEnchant.tableAndLoot;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }
}
