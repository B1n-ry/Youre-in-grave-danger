package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.ClaimModsApi;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.DeathEffectConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.*;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
        YigdConfig config = YigdConfig.getConfig();
        Vec3d pos = player.getPos();
        World playerWorld = player.world;
        Yigd.NEXT_TICK.add(() -> {
            PlayerInventory inventory = player.getInventory();
            inventory.updateItems();

            DefaultedList<ItemStack> allItems = DefaultedList.of();

            DefaultedList<ItemStack> items = DefaultedList.of();
            items.addAll(inventory.main);
            items.addAll(inventory.armor);
            items.addAll(inventory.offHand);

            // These lines should never be triggered, but just in case there is any problem this is here
            int currentSize = items.size();
            if (inventory.size() > currentSize) {
                for (int i = currentSize; i < inventory.size(); i++) {
                    ItemStack stack = inventory.getStack(i);
                    items.add(stack);
                }
            }
            YigdConfig.GraveSettings graveConfig = config.graveSettings;

            List<String> soulboundEnchantments = graveConfig.soulboundEnchantments; // Get a string array with all soulbound enchantment names
            List<String> removeEnchantments = graveConfig.deleteEnchantments; // List with enchantments to delete

            YigdConfig.ItemLoss itemLoss = graveConfig.itemLoss;
            if (itemLoss.enableLoss) {
                boolean handleAsStacks = itemLoss.affectStacks;
                int from, to;
                if (itemLoss.usePercentRange) {
                    DefaultedList<ItemStack> vanillaStacks = DefaultedList.of();
                    vanillaStacks.addAll(items);
                    vanillaStacks.removeIf(ItemStack::isEmpty);

                    int totalItems = vanillaStacks.size();
                    if (!itemLoss.affectStacks) {
                        totalItems = 0;
                        for (ItemStack stack : vanillaStacks) {
                            totalItems += stack.getCount();
                        }
                    }
                    from = (int) (((float) itemLoss.lossRangeFrom / 100f) * totalItems);
                    to = (int) (((float) itemLoss.lossRangeTo / 100f) * totalItems);
                } else {
                    from = itemLoss.lossRangeFrom;
                    to = itemLoss.lossRangeTo;
                }
                int amount = from < to ? new Random().nextInt(from, ++to) : from;

                List<String> matchingEnchantment = new ArrayList<>();
                if (itemLoss.ignoreSoulboundItems) matchingEnchantment.addAll(soulboundEnchantments);

                for (int i = 0; i < amount; i++) {
                    if (Math.random() * 100 > (double) itemLoss.percentChanceOfLoss) continue;

                    GraveHelper.deleteItemFromList(items, handleAsStacks, matchingEnchantment);
                }
            }

            DefaultedList<ItemStack> removeFromGrave = GraveHelper.getEnchantedItems(items, removeEnchantments); // Find all items to be removed
            DefaultedList<ItemStack> soulboundInventory = GraveHelper.getEnchantedItems(items, soulboundEnchantments); // Get all soulbound enchanted items in inventory


            // Add defaulted soulbound items
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i);

                if (stack.isIn(ModTags.SOULBOUND_ITEM) || graveConfig.soulboundSlots.contains(i)) soulboundInventory.set(i, stack);
                if (GraveHelper.hasBotaniaKeepIvy(stack, false)) removeFromGrave.set(i, stack);
            }

            for (int i : graveConfig.voidSlots) {
                removeFromGrave.set(i, items.get(i));
            }

            GraveHelper.removeFromList(items, soulboundInventory); // Keep soulbound items from appearing in both player inventory and grave
            GraveHelper.removeFromList(items, removeFromGrave); // Delete items with set enchantment

            DimensionType playerDimension = playerWorld.getDimension();
            Registry<DimensionType> dimManager = playerWorld.getRegistryManager().get(Registry.DIMENSION_TYPE_KEY);

            BlockPos blockPos = new BlockPos(pos);
            boolean canGenerate = GraveAreaOverride.canGenerateOnPos(blockPos, dimManager.getId(playerDimension), graveConfig.generateGraves);

            DeathEffectConfig spawnProtectionRule = graveConfig.deathInSpawnProtection;
            DeathEffectConfig alteredSpawnRules = DeathEffectConfig.CREATE_GRAVE;
            ServerWorld serverWorld = (ServerWorld) playerWorld;
            if (spawnProtectionRule != DeathEffectConfig.CREATE_GRAVE) {
                boolean isSpawnProtected = serverWorld.getServer().isSpawnProtected(serverWorld, blockPos, player);
                if (isSpawnProtected && spawnProtectionRule == DeathEffectConfig.KEEP_ITEMS) {
                    alteredSpawnRules = spawnProtectionRule;
                    for (int i = 0; i < items.size(); i++) {
                        if (!soulboundInventory.get(i).isEmpty()) continue;
                        soulboundInventory.set(i, items.remove(i));
                    }
                } else if (isSpawnProtected && spawnProtectionRule == DeathEffectConfig.DROP_ITEMS) {
                    alteredSpawnRules = spawnProtectionRule;
                    canGenerate = false;
                }
            }
            DeathEffectConfig claimProtectionRule = graveConfig.graveCompatConfig.claimRuleOverride;
            if (claimProtectionRule != DeathEffectConfig.CREATE_GRAVE) {
                boolean isInClaim = false;
                for (ClaimModsApi claimMod : Yigd.claimMods) {
                    if (isInClaim) break;
                    isInClaim = claimMod.isInClaim(blockPos, serverWorld);
                }

                if (isInClaim && claimProtectionRule == DeathEffectConfig.KEEP_ITEMS) {
                    alteredSpawnRules = claimProtectionRule;
                    for (int i = 0; i < items.size(); i++) {
                        if (!soulboundInventory.get(i).isEmpty()) continue;
                        soulboundInventory.set(i, items.get(i));
                        items.set(i, ItemStack.EMPTY);
                    }
                } else if (isInClaim && claimProtectionRule == DeathEffectConfig.DROP_ITEMS) {
                    alteredSpawnRules = claimProtectionRule;
                    canGenerate = false;
                }
            }

            List<Object> modInventories = new ArrayList<>();
            for (YigdApi yigdApi : Yigd.apiMods) {
                Object modInv = yigdApi.getInventory(player, true, alteredSpawnRules);
                modInventories.add(modInv);
                allItems.addAll(yigdApi.toStackList(modInv));

                yigdApi.dropAll(player);
            }

            allItems.addAll(items);
            allItems.removeIf(ItemStack::isEmpty);

            UUID playerId = player.getUuid();

            int xpPoints;
            if (graveConfig.defaultXpDrop) {
                xpPoints = Math.min(7 * player.experienceLevel, 100);
            } else {
                int currentLevel = player.experienceLevel;
                int totalExperience;
                if (currentLevel >= 32) {
                    totalExperience = (int) (4.5 * Math.pow(currentLevel, 2) - 162.5 * currentLevel + 2220);
                } else if (currentLevel >= 17) {
                    totalExperience = (int) (2.5 * Math.pow(currentLevel, 2) - 40.5 * currentLevel + 360);
                } else {
                    totalExperience = (int) (Math.pow(currentLevel, 2) + 6 * currentLevel + player.experienceProgress);
                }
                xpPoints = (int) ((graveConfig.xpDropPercent / 100f) * totalExperience);
            }

            DeadPlayerData.Soulbound.setSoulboundInventories(playerId, soulboundInventory); // Stores the soulbound items

            inventory.clear(); // Make sure no items are accidentally dropped, and will render gone from your inventory

            player.totalExperience = 0;
            player.experienceProgress = 0;
            player.experienceLevel = 0;

            if (graveConfig.dropPlayerHead) {
                ItemStack stack = new ItemStack(Items.PLAYER_HEAD, 1);
                NbtCompound nbt = new NbtCompound();
                nbt.putString("SkullOwner", player.getName().getString());
                stack.setNbt(nbt);
                items.add(stack);
            }

            if (graveConfig.requireGraveItem) {
                canGenerate = false;
                for (ItemStack stack : items) {
                    if (stack.getItem() == Yigd.GRAVE_BLOCK.asItem()) {
                        canGenerate = true;
                        stack.decrement(1);
                    }
                }
            }

            List<UUID> whitelist = DeathInfoManager.INSTANCE.getGraveList();
            if ((!whitelist.contains(player.getUuid()) && DeathInfoManager.INSTANCE.isWhiteList()) || (whitelist.contains(player.getUuid()) && !DeathInfoManager.INSTANCE.isWhiteList())) {
                canGenerate = false;
            }

            int dimId = dimManager.getRawId(playerDimension);
            if (!graveConfig.generateGraves || graveConfig.blacklistDimensions.contains(dimId) || graveConfig.ignoreDeathTypes.contains(source.name) || !canGenerate) {
                for (int i = 0; i < Yigd.apiMods.size(); i++) {
                    YigdApi yigdApi = Yigd.apiMods.get(i);
                    Object o = modInventories.get(i);
                    items.addAll(yigdApi.toStackList(o));
                    yigdApi.dropOnGround(o, serverWorld, pos);
                }

                ItemScatterer.spawn(playerWorld, new BlockPos(pos), items);
                ExperienceOrbEntity.spawn((ServerWorld) playerWorld, pos, xpPoints);
                return;
            } else if (!graveConfig.putXpInGrave) {
                ExperienceOrbEntity.spawn((ServerWorld) playerWorld, pos, xpPoints);
                xpPoints = 0;
            }

            // Render items in your hotbar, offhand and armour
            for (int i = 0; i < soulboundInventory.size(); i++) {
                inventory.setStack(i, soulboundInventory.get(i));
            }

            if (allItems.size() > 0 || xpPoints > 0 || graveConfig.generateEmptyGraves) {
                GraveHelper.placeDeathGrave(playerWorld, pos, inventory.player, items, modInventories, xpPoints, source);
            } else {
                Yigd.LOGGER.info("Didn't generate grave as grave wouldn't contain anything");
            }

            this.dropInventory();
        });
    }

    @Redirect(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropXp()V"))
    private void overwriteXp(LivingEntity instance) {
        if (!(instance instanceof PlayerEntity)) this.dropXp();
    }
}
