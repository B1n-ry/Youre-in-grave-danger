package com.b1n_ry.yigd.mixin;

import net.minecraft.client.gl.ShaderEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShaderEffect.class)
public interface ShaderEffectAccessor {
    @Accessor("lastTickDelta")
    float getLastTickDelta();
}
