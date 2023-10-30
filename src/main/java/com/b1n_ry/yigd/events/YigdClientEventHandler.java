package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.config.YigdConfig.ExtraFeatures.DeathSightConfig;
import com.mojang.authlib.GameProfile;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public class YigdClientEventHandler {
    public static void registerEventCallbacks() {
        RenderGlowingGraveEvent.EVENT.register((be, player) -> {
            YigdConfig config = YigdConfig.getConfig();

            GameProfile graveOwner = be.getGraveOwner();

            double distance = config.graveRendering.glowingDistance;
            boolean isOwner = graveOwner != null && graveOwner.equals(player.getGameProfile());
            DeathSightConfig deathSightConfig = config.extraFeatures.deathSightEnchant;
            if (deathSightConfig.enabled) {
                ItemStack headStack = player.getEquippedStack(EquipmentSlot.HEAD);
                if (!headStack.isEmpty() && EnchantmentHelper.get(headStack).containsKey(Yigd.DEATH_SIGHT_ENCHANTMENT)) {
                    distance = deathSightConfig.range;

                    // This doesn't actually mean that the user is the grave owner, but that the graves should light up
                    isOwner = deathSightConfig.targets == DeathSightConfig.GraveTargets.ALL_GRAVES
                            || (graveOwner != null && deathSightConfig.targets == DeathSightConfig.GraveTargets.PLAYER_GRAVES);
                    // If targets are OWN_GRAVES, the owner is already correct
                }
            }

            boolean inRange = be.getPos().isWithinDistance(player.getPos(), distance);

            return isOwner && inRange;
        });
    }
}
