package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.config.YigdConfig.ExtraFeatures.DeathSightConfig;
import com.b1n_ry.yigd.util.YigdTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public class YigdClientEventHandler {
    public static void registerEventCallbacks() {
        RenderGlowingGraveEvent.EVENT.register((be, player) -> {
            YigdConfig config = YigdConfig.getConfig();

            ResolvableProfile graveOwner = be.getGraveSkull();

            double distance = config.graveRendering.glowingDistance;
            boolean isOwner = graveOwner != null && graveOwner.gameProfile().equals(player.getGameProfile());
            DeathSightConfig deathSightConfig = config.extraFeatures.deathSightEnchant;

            ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!headStack.isEmpty() && EnchantmentHelper.hasTag(headStack, YigdTags.DEATH_SIGHT)) {
                distance = deathSightConfig.range;

                // This doesn't actually mean that the user is the grave owner, but that the graves should light up
                isOwner = deathSightConfig.targets == DeathSightConfig.GraveTargets.ALL_GRAVES
                        || (graveOwner != null && deathSightConfig.targets == DeathSightConfig.GraveTargets.PLAYER_GRAVES);
                // If targets are OWN_GRAVES, the owner is already correct
            }

            boolean inRange = be.getBlockPos().closerToCenterThan(player.position(), distance);

            return isOwner && inRange;
        });
    }
}
