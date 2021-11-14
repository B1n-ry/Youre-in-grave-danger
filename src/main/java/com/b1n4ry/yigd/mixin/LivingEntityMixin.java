package com.b1n4ry.yigd.mixin;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.config.YigdConfig;
import com.b1n4ry.yigd.core.DeadPlayerData;
import com.b1n4ry.yigd.core.GraveHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(value = LivingEntity.class, priority = 9001)
public abstract class LivingEntityMixin {
    @Inject(method = "dropInventory", at = @At("HEAD"))
    private void generateGrave(CallbackInfo ci) {
        if (!((LivingEntity)(Object)this instanceof PlayerEntity player)) return;
        PlayerInventory inventory = player.getInventory();

        DefaultedList<ItemStack> items = DefaultedList.of();
        items.addAll(inventory.main);
        items.addAll(inventory.armor);
        items.addAll(inventory.offHand);

        List<String> soulboundEnchantments = YigdConfig.getConfig().graveSettings.soulboundEnchantments; // Get a string array with all soulbound enchantment names
        DefaultedList<ItemStack> soulboundInventory = GraveHelper.getEnchantedItems(items, soulboundEnchantments); // Get all soulbound enchanted items in inventory


        items = GraveHelper.removeFromList(items, soulboundInventory); // Keep soulbound items from appearing in both player inventory and grave

        List<String> removeEnchantments = YigdConfig.getConfig().graveSettings.deleteEnchantments; // List with enchantments to delete
        DefaultedList<ItemStack> removeFromGrave = GraveHelper.getEnchantedItems(items, removeEnchantments); // Find all items to be removed
        items = GraveHelper.removeFromList(items, removeFromGrave); // Delete items with set enchantment

        UUID playerId = player.getUuid();

        DeadPlayerData.setSoulboundInventories(playerId, soulboundInventory); // Stores the soulbound items
        DeadPlayerData.setDeathPlayerInventories(playerId, items); // Backup your items in case of mod failure

        inventory.clear(); // Make sure no items are accidentally dropped, and will render gone from your inventory

        int dimId = player.world.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).getRawId(player.world.getDimension());
        if (!YigdConfig.getConfig().graveSettings.generateGraves || YigdConfig.getConfig().graveSettings.blacklistDimensions.contains(dimId)) {
            ItemScatterer.spawn(player.world, player.getBlockPos(), items);
            return;
        }

        GraveHelper.placeDeathGrave(player.world, player.getPos(), inventory.player, items);
    }
}
