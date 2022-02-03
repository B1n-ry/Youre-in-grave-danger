package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @ModifyVariable(method = "render", at = @At(value = "STORE"), ordinal = 4)
    private boolean willRenderOutlineShader(boolean bl4) {
        boolean glowingGrave = GraveBlockEntityRenderer.renderGraveGlowing;
        GraveBlockEntityRenderer.renderGraveGlowing = false;
        return glowingGrave || bl4;
    }
}
