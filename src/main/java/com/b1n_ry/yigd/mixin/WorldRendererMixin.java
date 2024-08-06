package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import net.minecraft.client.render.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    /**
     * It just works  -Tod Howard
     * I *think* this will render the block entities on the entity rendering profile, allowing them to have entity outlines
     * I feel like this is kinda cursed if this is the case, but I don't know how else to do it ¯\_(ツ)_/¯
     * Also it seems to work, as long as the rendering on the outline render layer is an actual outline vertex consumer
     */
    @ModifyVariable(method = "render", at = @At(value = "CONSTANT", args = "stringValue=blockentities", ordinal = 0), ordinal = 3)
    private boolean modify(boolean value) {
        boolean res = value || GraveBlockEntityRenderer.renderOutlineShader;
        GraveBlockEntityRenderer.renderOutlineShader = false;
        return res;
    }
}
