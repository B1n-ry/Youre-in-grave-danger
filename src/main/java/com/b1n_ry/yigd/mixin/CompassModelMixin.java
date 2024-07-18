package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.util.GraveCompassHelper;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.GlobalPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelPredicateProviderRegistry.class)
public class CompassModelMixin {
    @Inject(method = "method_43220", at = @At("HEAD"), cancellable = true)
    private static void changeCompassDirection(ClientWorld world, ItemStack stack, Entity entity, CallbackInfoReturnable<GlobalPos> cir) {
        GlobalPos gravePos = stack.get(GraveCompassHelper.GRAVE_LOCATION);

        if (gravePos != null) {
            cir.setReturnValue(gravePos);
        }
    }
}
