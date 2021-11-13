package com.b1n4ry.yigd.mixin;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.config.YigdConfig;
import com.b1n4ry.yigd.core.DeadPlayerData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.UUID;

@Mixin(value = PlayerEntity.class, priority = 9001)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Shadow @Final private PlayerInventory inventory;

    @Shadow public abstract PlayerInventory getInventory();

    @Shadow protected abstract void vanishCursedItems();

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Override // Overrides the method, which will cause the LivingEntity dropInventory function not to run, which is used by trinkets to drop all trinket items. This way trinket items are not deleted before this mixin method executes
    public void dropInventory() {
        if (this.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
            super.dropInventory();
            return;
        }

        DefaultedList<ItemStack> items = DefaultedList.of();
        items.addAll(inventory.main);
        items.addAll(inventory.armor);
        items.addAll(inventory.offHand);

        List<String> soulboundEnchantments = YigdConfig.getConfig().graveSettings.soulboundEnchantments; // Get a string array with all soulbound enchantment names
        DefaultedList<ItemStack> soulboundInventory = Yigd.getEnchantedItems(items, soulboundEnchantments); // Get all soulbound enchanted items in inventory

        if (!YigdConfig.getConfig().graveSettings.generateGraves) {
            super.dropInventory();

            this.vanishCursedItems();
            this.inventory.dropAll();
            return;
        }

        int dimId = this.world.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).getRawId(this.world.getDimension());
        if (YigdConfig.getConfig().graveSettings.blacklistDimensions.contains(dimId)) {
            this.inventory.dropAll();
            return;
        }

        items = Yigd.removeFromList(items, soulboundInventory); // Keep soulbound items from appearing in both player inventory and grave

        List<String> removeEnchantments = YigdConfig.getConfig().graveSettings.deleteEnchantments; // List with enchantments to delete
        DefaultedList<ItemStack> removeFromGrave = Yigd.getEnchantedItems(items, removeEnchantments); // Find all items to be removed
        items = Yigd.removeFromList(items, removeFromGrave); // Delete items with set enchantment

        this.inventory.clear(); // Make sure your items are in fact deleted, and not accidentally retrieved when you respawn

        List<ItemStack> syncHotbar = soulboundInventory.subList(0, 10);
        for (int i = 0; i < 10; i++) {
            this.getInventory().setStack(i, syncHotbar.get(i));
        }

        UUID playerId = this.getUuid();

        DeadPlayerData.setSoulboundInventories(playerId, soulboundInventory); // Stores the soulbound items
        DeadPlayerData.setDeathPlayerInventories(playerId, items); // Backup your items in case of mod failure

        Yigd.placeDeathGrave(this.world, this.getPos(), this.inventory.player, items);
    }
}
