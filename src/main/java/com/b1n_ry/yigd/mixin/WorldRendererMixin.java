package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.mixin.accessors.PostEffectProcessorAccessor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Shadow private @Nullable PostEffectProcessor entityOutlinePostProcessor;
    @Shadow @Final private MinecraftClient client;
    @Unique
    private boolean yigd$renderEntityOutlineProcessor = false;

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderDispatcher;render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V", ordinal = 0))
    private void calcIfShouldRenderShader(BlockEntityRenderDispatcher instance, BlockEntity blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        if (blockEntity instanceof GraveBlockEntity) {
            this.yigd$renderEntityOutlineProcessor = true;
        }

        instance.render(blockEntity, tickDelta, matrices, vertexConsumers);
    }

    @Inject(method = "render", at = @At(value = "RETURN"))
    private void renderGlowingShader(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f projectionMatrix, CallbackInfo ci) {
        if (this.entityOutlinePostProcessor == null || !this.yigd$renderEntityOutlineProcessor) return;

        if (((PostEffectProcessorAccessor) this.entityOutlinePostProcessor).getLastTickDelta() != tickDelta) {
            this.entityOutlinePostProcessor.render(tickDelta);
            this.client.getFramebuffer().beginWrite(false);
        }

        this.yigd$renderEntityOutlineProcessor = false;
    }
}
