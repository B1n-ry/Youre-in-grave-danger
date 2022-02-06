package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.b1n_ry.yigd.core.GraveHelper;
import com.tiviacz.travelersbackpack.component.ComponentUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.List;

public class TravelersBackpackCompat implements YigdApi {
    @Override
    public String getModName() {
        return "travelers_backpack";
    }

    @Override
    public Object getInventory(PlayerEntity player) {
        return this.getInventory(player, false);
    }

    @Override
    public Object getInventory(PlayerEntity player, boolean onDeath) {
        List<String> soulboundEnchantments = YigdConfig.getConfig().graveSettings.soulboundEnchantments;
        List<String> deleteEnchantments = YigdConfig.getConfig().graveSettings.deleteEnchantments;

        ItemStack backpack = ComponentUtils.getWearingBackpack(player);

        if (onDeath) {
            ItemStack soulbound;
            boolean shouldDelete = false;
            if (GraveHelper.hasEnchantments(soulboundEnchantments, backpack) || player.world.getTagManager().getItems().getTagsFor(backpack.getItem()).contains((new Identifier("yigd", "soulbound_item")))) {
                soulbound = backpack;
                shouldDelete = true;
            } else {
                soulbound = ItemStack.EMPTY;
            }
            DeadPlayerData.Soulbound.addModdedSoulbound(player.getUuid(), soulbound);

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
        if (!(inventory instanceof ItemStack)) return extraItems;
        ItemStack stack = (ItemStack) inventory;

        if (stack.isEmpty()) return extraItems;

        if (ComponentUtils.isWearingBackpack(player)) {
            extraItems.add(stack);
        } else {
            ComponentUtils.equipBackpack(player, stack);
        }

        return extraItems;
    }

    @Override
    public int getInventorySize(Object inventory) {
        if (inventory instanceof ItemStack && !((ItemStack) inventory).isEmpty()) return 1;
        return 0;
    }

    @Override
    public void dropAll(PlayerEntity player) {
        ComponentUtils.getComponent(player).removeWearable();
    }

    @Override
    public List<ItemStack> toStackList(Object inventory) {
        List<ItemStack> stacks = new ArrayList<>();
        if (!(inventory instanceof ItemStack)) return stacks;
        ItemStack stack = (ItemStack) inventory;
        stacks.add(stack);
        return stacks;
    }

    @Override
    public NbtCompound writeNbt(Object o) {
        NbtCompound nbt = new NbtCompound();
        if (o instanceof ItemStack) {
            ItemStack stack = (ItemStack) o;
            return stack.writeNbt(nbt);
        }
        return nbt;
    }

    @Override
    public Object readNbt(NbtCompound nbt) {
        return ItemStack.fromNbt(nbt);
    }
}
