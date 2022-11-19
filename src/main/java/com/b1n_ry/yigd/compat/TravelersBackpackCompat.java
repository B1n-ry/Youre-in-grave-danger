package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.DeathEffectConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.b1n_ry.yigd.core.GraveHelper;
import com.b1n_ry.yigd.core.ModTags;
import com.tiviacz.travelersbackpack.component.ComponentUtils;
import com.tiviacz.travelersbackpack.config.TravelersBackpackConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TravelersBackpackCompat implements YigdApi {
    public static boolean isTrinketIntegrationOn() {
        return TravelersBackpackConfig.trinketsIntegration;
    }

    @Override
    public String getModName() {
        return "travelers_backpack";
    }

    @Override
    public Object getInventory(PlayerEntity player, boolean onDeath, @Nullable DeathEffectConfig onDeathHandling) {
        YigdConfig.GraveSettings config = YigdConfig.getConfig().graveSettings;
        List<String> soulboundEnchantments = config.soulboundEnchantments;
        List<String> deleteEnchantments = config.deleteEnchantments;

        ItemStack backpack = ComponentUtils.getWearingBackpack(player);

        if (onDeath) {
            ItemStack soulbound;
            boolean shouldDelete = false;
            if (GraveHelper.hasEnchantments(soulboundEnchantments, backpack)) {
                if (config.loseSoulboundLevelOnDeath) {
                    GraveHelper.removeEnchantmentLevel(backpack, soulboundEnchantments);
                }
                soulbound = backpack;
                shouldDelete = true;
            } else if (backpack.isIn(ModTags.SOULBOUND_ITEM) || onDeathHandling == DeathEffectConfig.KEEP_ITEMS || GraveHelper.hasBotaniaKeepIvy(backpack, true)) {
                soulbound = backpack;
                shouldDelete = true;
            } else {
                soulbound = ItemStack.EMPTY;
            }
            DeadPlayerData.Soulbound.addModdedSoulbound(player.getUuid(), soulbound, this.getModName());

            if (GraveHelper.hasEnchantments(deleteEnchantments, backpack)) {
                shouldDelete = true;
            }

            if (shouldDelete) return ItemStack.EMPTY;
        }

        return backpack;
    }

    @Override
    public DefaultedList<ItemStack> setInventory(Object inventory, PlayerEntity player) {
        DefaultedList<ItemStack> extraItems = DefaultedList.of();
        if (!(inventory instanceof ItemStack stack) || stack.isEmpty()) return extraItems;

        if (ComponentUtils.isWearingBackpack(player)) {
            extraItems.add(stack);
        } else {
            ComponentUtils.equipBackpack(player, stack);
        }

        return extraItems;
    }

    @Override
    public int getInventorySize(Object inventory) {
        if (inventory instanceof ItemStack stack && !stack.isEmpty()) return 1;
        return 0;
    }

    @Override
    public void dropAll(PlayerEntity player) {
        ComponentUtils.getComponent(player).removeWearable();
    }

    @Override
    public List<ItemStack> toStackList(Object inventory) {
        List<ItemStack> stacks = new ArrayList<>();
        if (!(inventory instanceof ItemStack stack)) return stacks;
        stacks.add(stack);
        return stacks;
    }

    @Override
    public NbtCompound writeNbt(Object o) {
        NbtCompound nbt = new NbtCompound();
        if (o instanceof ItemStack stack) {
            return stack.writeNbt(nbt);
        }
        return nbt;
    }

    @Override
    public Object readNbt(NbtCompound nbt) {
        return ItemStack.fromNbt(nbt);
    }
}
