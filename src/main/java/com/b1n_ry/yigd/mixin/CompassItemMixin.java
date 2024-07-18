package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.util.GraveCompassHelper;
import net.minecraft.entity.Entity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(CompassItem.class)
public class CompassItemMixin {
    @Inject(method = "inventoryTick", at = @At("TAIL"))
    private void onInventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected, CallbackInfo ci) {
        if (world.isClient()) return;

        if (!stack.isOf(Items.COMPASS)) return;

        // This mixin is to make the grave compass disappear when the grave is broken (or redirect to the closest grave)
        if (!YigdConfig.getConfig().extraFeatures.graveCompass.deleteWhenUnlinked) return;
        UUID graveId = stack.get(GraveComponent.GRAVE_ID);
        if (graveId == null) return;

        if (world.getTime() % 200 == 0) {
            GraveCompassHelper.updateClosestNbt(world.getRegistryKey(), entity.getBlockPos(), entity.getUuid(), stack);
        }

        DeathInfoManager.INSTANCE.getGrave(graveId).ifPresent(grave -> {
            if (grave.getStatus() != GraveStatus.UNCLAIMED) {
                stack.setCount(0);
            }
        });
    }
}
