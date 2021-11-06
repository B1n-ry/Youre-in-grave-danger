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
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
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

    @Shadow public int experienceLevel;

    @Shadow public int totalExperience;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Redirect(method = "dropInventory", at = @At(value = "INVOKE", target = "net.minecraft.entity.player.PlayerInventory.dropAll()V"))
    private void dropAll(PlayerInventory inventory) {
        if (this.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return;

        if (!YigdConfig.getConfig().graveSettings.generateGraves) {
            this.inventory.dropAll();
            return;
        }

        DefaultedList<ItemStack> items = DefaultedList.of();
        items.addAll(inventory.main);
        items.addAll(inventory.armor);
        items.addAll(inventory.offHand);

        List<String> soulboundEnchantments = YigdConfig.getConfig().graveSettings.soulboundEnchantments; // Get a string array with all soulbound enchantment names
        soulboundInventory = Yigd.getEnchantedItems(items, soulboundEnchantments); // Get all soulbound enchanted items in inventory

        items = Yigd.removeFromList(items, soulboundInventory); // Keep soulbound items from appearing in both player inventory and grave
        this.inventory.clear();

        Yigd.placeDeathGrave(this.world, this.getPos(), this.inventory.player, items);
    }

    public DefaultedList<ItemStack> getSoulboundInventory() {
        return soulboundInventory;
    }
}
