package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemProperties.class)
public class CompassModelMixin {
    @Inject(method = "lambda$static$11", at = @At("HEAD"), cancellable = true)
    private static void changeCompassDirection(ClientLevel world, ItemStack stack, Entity entity, CallbackInfoReturnable<GlobalPos> cir) {
        GlobalPos gravePos = stack.get(Yigd.GRAVE_LOCATION);

        if (gravePos != null) {
            cir.setReturnValue(gravePos);
        }
    }
}
