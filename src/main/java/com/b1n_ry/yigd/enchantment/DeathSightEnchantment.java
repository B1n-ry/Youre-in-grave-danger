package com.b1n_ry.yigd.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;

public class DeathSightEnchantment extends Enchantment {
    public DeathSightEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.ARMOR_HEAD, new EquipmentSlot[] { EquipmentSlot.HEAD });
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }
}
