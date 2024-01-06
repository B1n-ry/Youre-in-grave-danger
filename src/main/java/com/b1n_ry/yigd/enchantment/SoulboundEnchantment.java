package com.b1n_ry.yigd.enchantment;

import com.b1n_ry.yigd.util.YigdTags;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import com.b1n_ry.yigd.config.YigdConfig;

public class SoulboundEnchantment extends Enchantment {
    public SoulboundEnchantment(Rarity weight, EquipmentSlot... slotTypes) {
        super(weight, EnchantmentTarget.BREAKABLE, slotTypes);
    }

    YigdConfig config = YigdConfig.getConfig();

    @Override
    public boolean isTreasure() {
        return config.extraFeatures.soulbound.isTreasure;
    }

    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return config.extraFeatures.soulbound.isAvailableForEnchantedBookOffer;
    }
    @Override
    public boolean isAvailableForRandomSelection() {
        return config.extraFeatures.soulbound.isAvailableForRandomSelection;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return !stack.isIn(YigdTags.SOULBOUND_BLACKLIST);
    }
}
