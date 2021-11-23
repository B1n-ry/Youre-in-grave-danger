package com.b1n4ry.yigd.mixin;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.config.YigdConfig;
import com.b1n4ry.yigd.core.GraveHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mixin(value = LivingEntity.class, priority = 9001)
public abstract class LivingEntityMixin {
    @Shadow protected abstract void dropInventory();

    @Redirect(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropInventory()V"))
    private void generateGrave(LivingEntity livingEntity) {
        if (!(livingEntity instanceof PlayerEntity player)) {
            this.dropInventory();
            return;
        }
        PlayerInventory inventory = player.getInventory();

        DefaultedList<ItemStack> items = DefaultedList.of();
        items.addAll(inventory.main);
        items.addAll(inventory.armor);
        items.addAll(inventory.offHand);

        if (inventory.size() > 41) {
            for (int i = 41; i < inventory.size(); i++) {
                items.add(inventory.getStack(i));
            }
        }

        List<Object> modInventories = new ArrayList<>();
        for (YigdApi yigdApi : Yigd.apiMods) {
            modInventories.add(yigdApi.getInventory(player, true));

            yigdApi.dropAll(player);
        }

        List<String> soulboundEnchantments = YigdConfig.getConfig().graveSettings.soulboundEnchantments; // Get a string array with all soulbound enchantment names
        DefaultedList<ItemStack> soulboundInventory = GraveHelper.getEnchantedItems(items, soulboundEnchantments); // Get all soulbound enchanted items in inventory


        items = GraveHelper.removeFromList(items, soulboundInventory); // Keep soulbound items from appearing in both player inventory and grave

        List<String> removeEnchantments = YigdConfig.getConfig().graveSettings.deleteEnchantments; // List with enchantments to delete
        DefaultedList<ItemStack> removeFromGrave = GraveHelper.getEnchantedItems(items, removeEnchantments); // Find all items to be removed
        items = GraveHelper.removeFromList(items, removeFromGrave); // Delete items with set enchantment

        UUID playerId = player.getUuid();

        Yigd.deadPlayerData.setSoulboundInventories(playerId, soulboundInventory); // Stores the soulbound items
        Yigd.deadPlayerData.setDeathPlayerInventories(playerId, items); // Backup your items in case of mod failure
        Yigd.deadPlayerData.setModdedInventories(playerId, modInventories); // Backup modded items

        inventory.clear(); // Make sure no items are accidentally dropped, and will render gone from your inventory

        int dimId = player.world.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).getRawId(player.world.getDimension());
        if (!YigdConfig.getConfig().graveSettings.generateGraves || YigdConfig.getConfig().graveSettings.blacklistDimensions.contains(dimId)) {
            for (int i = 0; i < Yigd.apiMods.size(); i++) {
                YigdApi yigdApi = Yigd.apiMods.get(i);
                items.addAll(yigdApi.toStackList(modInventories.get(i)));
            }

            ItemScatterer.spawn(player.world, player.getBlockPos(), items);
            return;
        }

        for (int i = 0; i < soulboundInventory.size(); i++) {
            inventory.setStack(i, soulboundInventory.get(i));
        }
        List<Object> modSoulbound = Yigd.deadPlayerData.getModdedSoulbound(playerId);
        if (modSoulbound != null) {
            for (int i = 0; i < modSoulbound.size(); i++) {
                Yigd.apiMods.get(i).setInventory(modSoulbound.get(i), player);
            }
        }

        final DefaultedList<ItemStack> graveItems = items; // Necessary for following line to work
        Yigd.NEXT_TICK.add(() -> GraveHelper.placeDeathGrave(player.world, player.getPos(), inventory.player, graveItems, modInventories));
    }
}
