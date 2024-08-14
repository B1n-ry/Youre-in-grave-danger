package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.util.YigdTags;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;

public class YigdClientEventHandler {
    @SubscribeEvent
    public void renderGlowingGrave(YigdEvents.RenderGlowingGraveEvent event) {
        GraveBlockEntity be = event.getGrave();
        LocalPlayer player = event.getPlayer();
        YigdConfig config = YigdConfig.getConfig();

        ResolvableProfile graveOwner = be.getGraveSkull();

        double distance = config.graveRendering.glowingDistance;
        boolean isOwner = graveOwner != null && graveOwner.gameProfile().equals(player.getGameProfile());
        YigdConfig.ExtraFeatures.DeathSightConfig deathSightConfig = config.extraFeatures.deathSightEnchant;

        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!headStack.isEmpty() && EnchantmentHelper.hasTag(headStack, YigdTags.DEATH_SIGHT)) {
            distance = deathSightConfig.range;

            // This doesn't actually mean that the user is the grave owner, but that the graves should light up
            isOwner = deathSightConfig.targets == YigdConfig.ExtraFeatures.DeathSightConfig.GraveTargets.ALL_GRAVES
                    || (graveOwner != null && deathSightConfig.targets == YigdConfig.ExtraFeatures.DeathSightConfig.GraveTargets.PLAYER_GRAVES);
            // If targets are OWN_GRAVES, the owner is already correct
        }

        boolean inRange = be.getBlockPos().closerToCenterThan(player.position(), distance);

        if (isOwner && inRange)
            event.setRenderGlowing(true);
    }
}
