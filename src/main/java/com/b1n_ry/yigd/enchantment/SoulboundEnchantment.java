package com.b1n_ry.yigd.enchantment;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.util.YigdTags;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class SoulboundEnchantment extends Enchantment {
    public SoulboundEnchantment(Rarity weight, EquipmentSlot... slotTypes) {
        super(weight, EnchantmentTarget.BREAKABLE, slotTypes);
    }

    @Override
    public boolean isTreasure() {
        return YigdConfig.getConfig().extraFeatures.soulboundEnchant.isTreasure;
    }
    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return YigdConfig.getConfig().extraFeatures.soulboundEnchant.isAvailableForEnchantedBookOffer;
    }
    @Override
    public boolean isAvailableForRandomSelection() {
        return YigdConfig.getConfig().extraFeatures.soulboundEnchant.isAvailableForRandomSelection;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return !stack.isIn(YigdTags.SOULBOUND_BLACKLIST);
    }
}
