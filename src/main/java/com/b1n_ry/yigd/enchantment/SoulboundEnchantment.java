package com.b1n_ry.yigd.enchantment;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.util.YigdTags;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;

public class SoulboundEnchantment extends Enchantment {
    public SoulboundEnchantment(Properties properties) {
        super(properties);
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
