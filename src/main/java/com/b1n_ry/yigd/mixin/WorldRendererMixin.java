package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Inject(method = "canDrawEntityOutlines", at = @At("HEAD"), cancellable = true)
    private void canDrawOutline(CallbackInfoReturnable<Boolean> cir) {
        if (GraveBlockEntityRenderer.renderGraveGlowing) cir.setReturnValue(true);
    }
}
