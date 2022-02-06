package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.b1n_ry.yigd.core.GraveHelper;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Mixin(value = LivingEntity.class, priority = 9001)
public abstract class LivingEntityMixin {
    @Shadow protected abstract void dropInventory();

    @Shadow protected abstract void dropXp();

    @Redirect(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropInventory()V"))
    private void generateGrave(LivingEntity livingEntity, DamageSource source) {
        if (!(livingEntity instanceof PlayerEntity player) || livingEntity.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
            this.dropInventory();
            return;
        }
        Yigd.NEXT_TICK.add(() -> {
            PlayerInventory inventory = player.getInventory();
            DefaultedList<ItemStack> allItems = DefaultedList.of();

            DefaultedList<ItemStack> items = DefaultedList.of();
            items.addAll(inventory.main);
            items.addAll(inventory.armor);
            items.addAll(inventory.offHand);

            if (inventory.size() > 41) {
                for (int i = 41; i < inventory.size(); i++) {
                    ItemStack stack = inventory.getStack(i);
                    items.add(stack);
                }
            }

            List<Object> modInventories = new ArrayList<>();
            for (YigdApi yigdApi : Yigd.apiMods) {
                Object modInv = yigdApi.getInventory(player, true);
                modInventories.add(modInv);
                allItems.addAll(yigdApi.toStackList(modInv));

                yigdApi.dropAll(player);
            }

            List<String> soulboundEnchantments = YigdConfig.getConfig().graveSettings.soulboundEnchantments; // Get a string array with all soulbound enchantment names
            DefaultedList<ItemStack> soulboundInventory = GraveHelper.getEnchantedItems(items, soulboundEnchantments); // Get all soulbound enchanted items in inventory

            // Add defaulted soulbound items
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i);

                Collection<Identifier> tags = player.world.getTagManager().getOrCreateTagGroup(Registry.ITEM_KEY).getTagsFor(stack.getItem());
                if (tags.contains(new Identifier("yigd", "soulbound_item"))) soulboundInventory.set(i, stack);
            }

            GraveHelper.removeFromList(items, soulboundInventory); // Keep soulbound items from appearing in both player inventory and grave

            List<String> removeEnchantments = YigdConfig.getConfig().graveSettings.deleteEnchantments; // List with enchantments to delete
            DefaultedList<ItemStack> removeFromGrave = GraveHelper.getEnchantedItems(items, removeEnchantments); // Find all items to be removed
            GraveHelper.removeFromList(items, removeFromGrave); // Delete items with set enchantment

            allItems.addAll(items);
            allItems.removeIf(ItemStack::isEmpty);

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

            DeadPlayerData.Soulbound.setSoulboundInventories(playerId, soulboundInventory); // Stores the soulbound items

            inventory.clear(); // Make sure no items are accidentally dropped, and will render gone from your inventory

            player.totalExperience = 0;
            player.experienceProgress = 0;
            player.experienceLevel = 0;

            if (YigdConfig.getConfig().graveSettings.dropPlayerHead) {
                ItemStack stack = new ItemStack(Items.PLAYER_HEAD, 1);
                NbtCompound nbt = new NbtCompound();
                nbt.putString("SkullOwner", player.getName().asString());
                stack.setNbt(nbt);
                items.add(stack);
            }

            boolean canGenerate = true;
            if (YigdConfig.getConfig().graveSettings.requireGraveItem) {
                canGenerate = false;
                for (ItemStack stack : items) {
                    if (stack.getItem() == Yigd.GRAVE_BLOCK.asItem()) {
                        canGenerate = true;
                        stack.decrement(1);
                    }
                }
            }

            int dimId = player.world.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY).getRawId(player.world.getDimension());
            YigdConfig.GraveSettings config = YigdConfig.getConfig().graveSettings;
            if (!config.generateGraves || config.blacklistDimensions.contains(dimId) || config.ignoreDeathTypes.contains(source.name) || !canGenerate) {
                for (int i = 0; i < Yigd.apiMods.size(); i++) {
                    YigdApi yigdApi = Yigd.apiMods.get(i);
                    items.addAll(yigdApi.toStackList(modInventories.get(i)));
                }

                ItemScatterer.spawn(player.world, player.getBlockPos(), items);
                ExperienceOrbEntity.spawn((ServerWorld) player.world, player.getPos(), xpPoints);
                return;
            } else if (!config.putXpInGrave) {
                ExperienceOrbEntity.spawn((ServerWorld) player.world, player.getPos(), xpPoints);
                xpPoints = 0;
            }

            for (int i = 0; i < soulboundInventory.size(); i++) {
                inventory.setStack(i, soulboundInventory.get(i));
            }

            if (allItems.size() > 0 || xpPoints > 0 || YigdConfig.getConfig().graveSettings.generateEmptyGraves) {
                GraveHelper.placeDeathGrave(player.world, player.getPos(), inventory.player, items, modInventories, xpPoints, source);
            }

            this.dropInventory();
        });
    }

    @Redirect(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropXp()V"))
    private void overwriteXp(LivingEntity instance) {
        if (!(instance instanceof PlayerEntity)) this.dropXp();
    }
}
