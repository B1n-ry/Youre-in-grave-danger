package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderEffect;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow private @Nullable ShaderEffect entityOutlineShader;
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "render", at = @At("RETURN"))
    private void renderGlowingShader(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f positionMatrix, CallbackInfo ci) {
        if (this.entityOutlineShader == null || !GraveBlockEntityRenderer.renderGraveGlowing) return;
        if (tickDelta != ((ShaderEffectAccessor) this.entityOutlineShader).getLastTickDelta()) {
            this.entityOutlineShader.render(tickDelta);
            this.client.getFramebuffer().beginWrite(false);
        }
    }
}
