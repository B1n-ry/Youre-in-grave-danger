package com.b1n_ry.yigd.enchantment;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.GraveHelper;
import com.b1n_ry.yigd.core.ModTags;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class SoulboundEnchantment extends Enchantment {
    public SoulboundEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentTarget.VANISHABLE, EquipmentSlot.values());
    }

    public boolean isAcceptableItem(ItemStack stack) {
        return !stack.isIn(ModTags.SOULBOUND_BLACKLIST) && !stack.isIn(ModTags.SOULBOUND_ITEM) && !GraveHelper.hasBotaniaKeepIvy(stack, false); // Enchantment is always applicable unless it can't be or always is soulbound
    }

    public int getMinPower(int level) {
        return level * 25;
    }

    public int getMaxPower(int level) {
        return this.getMinPower(level) + 50;
    }

    public boolean isTreasure() {
        return YigdConfig.getConfig().utilitySettings.soulboundEnchant.isTreasure;
    }
    public boolean isAvailableForEnchantedBookOffer() {
        return YigdConfig.getConfig().utilitySettings.soulboundEnchant.villagerTrade;
    }
    public boolean isAvailableForRandomSelection() {
        return YigdConfig.getConfig().utilitySettings.soulboundEnchant.tableAndLoot;
    }

    public int getMaxLevel() {
        return 1;
    }

    @Override
    protected boolean canAccept(Enchantment other) {
        if (other == this || other == Enchantments.VANISHING_CURSE) {
            return false;
        }
        return super.canAccept(other);
    }
}
