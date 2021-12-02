package com.b1n4ry.yigd.mixin;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.config.YigdConfig;
import com.b1n4ry.yigd.core.GraveHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;
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
    private void generateGrave(LivingEntity livingEntity, DamageSource source) {
        if (!(livingEntity instanceof PlayerEntity player) || livingEntity.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
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

        int xpPoints;
        YigdConfig.GraveSettings graveSettings = YigdConfig.getConfig().graveSettings;
        if (graveSettings.defaultXpDrop) {
            xpPoints = Math.min(7 * player.experienceLevel, 100);
        } else {
            int currentLevel = player.experienceLevel;
            int totalExperience = (int) (Math.pow(currentLevel, 2) + 6 * currentLevel + player.experienceProgress);
            xpPoints = (int) ((graveSettings.xpDropPercent / 100f) * totalExperience);
        }

        player.totalExperience = 0;
        player.experienceProgress = 0;
        player.experienceLevel = 0;

        Yigd.deadPlayerData.setDeathXp(playerId, xpPoints);
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
            ExperienceOrbEntity.spawn((ServerWorld) player.world, new Vec3d(player.getX(), player.getY(), player.getZ()), xpPoints);
            return;
        }

        for (int i = 0; i < soulboundInventory.size(); i++) {
            inventory.setStack(i, soulboundInventory.get(i));
        }
        List<Object> modSoulbound = Yigd.deadPlayerData.getModdedSoulbound(playerId);
        if (modSoulbound != null) {
            for (int i = 0; i < modSoulbound.size(); i++) {
                Yigd.apiMods.get(i).dropAll(player);
                Yigd.apiMods.get(i).setInventory(modSoulbound.get(i), player);
            }
        }

        // Get killer if killed by a player
        UUID killerId;
        Entity e = source.getSource();
        if (e instanceof PlayerEntity) {
            killerId = e.getUuid();
        } else {
            killerId = null;
        }

        // All variables passed into placeGrave method has to be final to be executed on the end of the tick
        final int xp = xpPoints;
        final DefaultedList<ItemStack> graveItems = items;
        final UUID killer = killerId;
        Yigd.NEXT_TICK.add(() -> GraveHelper.placeDeathGrave(player.world, player.getPos(), inventory.player, graveItems, modInventories, xp, killer));
    }
}
