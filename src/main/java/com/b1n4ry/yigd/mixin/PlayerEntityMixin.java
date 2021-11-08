package com.b1n4ry.yigd.mixin;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.config.YigdConfig;
import com.b1n4ry.yigd.core.PlayerEntityExt;
import jdk.nashorn.internal.ir.annotations.Reference;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements PlayerEntityExt {
    private DefaultedList<ItemStack> soulboundInventory; // Not necessary as the game mechanic is currently not working

    @Shadow @Final public PlayerInventory inventory;

    @Shadow public abstract boolean equip(int slot, ItemStack item);

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Redirect(method = "dropInventory", at = @At(value = "INVOKE", target = "net.minecraft.entity.player.PlayerInventory.dropAll()V"))
    private void dropAll(PlayerInventory inventory) {
        if (this.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return;

        DefaultedList<ItemStack> items = DefaultedList.of();
        items.addAll(inventory.main);
        items.addAll(inventory.armor);
        items.addAll(inventory.offHand);

        List<String> soulboundEnchantments = YigdConfig.getConfig().graveSettings.soulboundEnchantments; // Get a string array with all soulbound enchantment names
        soulboundInventory = Yigd.getEnchantedItems(items, soulboundEnchantments); // Get all soulbound enchanted items in inventory

        if (!YigdConfig.getConfig().graveSettings.generateGraves) {
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
            this.equip(i, syncHotbar.get(i));
        }

        Yigd.placeDeathGrave(this.world, this.getPos(), this.inventory.player, items);
    }

    public DefaultedList<ItemStack> getSoulboundInventory() {
        return soulboundInventory;
    }
}
