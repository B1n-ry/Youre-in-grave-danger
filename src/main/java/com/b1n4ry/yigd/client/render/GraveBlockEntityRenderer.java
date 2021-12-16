package com.b1n4ry.yigd.client.render;

import com.b1n4ry.yigd.block.entity.GraveBlockEntity;
import com.b1n4ry.yigd.config.YigdConfig;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.SkullEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;

import java.util.Random;

@Environment(EnvType.CLIENT)
public class GraveBlockEntityRenderer<T extends GraveBlockEntity> implements BlockEntityRenderer<T> {
    private final TextRenderer textRenderer;
    private final EntityModelLoader renderLayer;

    public GraveBlockEntityRenderer(Context ctx) {
        this.textRenderer = ctx.getTextRenderer();
        this.renderLayer = ctx.getLayerRenderDispatcher();

    }

    public SkullBlockEntityModel getSkull() {
        return new SkullEntityModel(renderLayer.getModelPart(EntityModelLayers.PLAYER_HEAD));
    }

    @Override
    public void render(GraveBlockEntity blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (blockEntity == null) {
            return;
        }
        Direction direction = blockEntity.getCachedState().get(Properties.HORIZONTAL_FACING);

        GameProfile graveOwner = blockEntity.getGraveOwner();
        if (graveOwner != null && YigdConfig.getConfig().graveSettings.renderGraveSkull) {
            matrices.push();

            switch (direction) {
                case SOUTH -> {
                    matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180f));
                    matrices.translate(-1.0, 0.25, -0.9375);
                }
                case NORTH -> matrices.translate(0.0, 0.25, 0.0625);
                case WEST -> {
                    matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90f));
                    matrices.translate(-1.0, 0.25, 0.0625);
                }
                case EAST -> {
                    matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(270f));
                    matrices.translate(0.0, 0.25, -0.9375);
                }
            }
            matrices.multiply(Vec3f.NEGATIVE_X.getDegreesQuaternion(270f));

            matrices.scale(1f, 1f, 0.25f);

            SkullBlockEntityModel skull = getSkull();
            SkullBlockEntityRenderer.renderSkull(null, 0, 0f, matrices, vertexConsumers, light, skull, SkullBlockEntityRenderer.getRenderLayer(SkullBlock.Type.PLAYER, blockEntity.getGraveOwner()));

            matrices.pop();
        }

        String customName = blockEntity.getCustomName();

        if (customName != null) {
            boolean renderText = YigdConfig.getConfig().graveSettings.renderGraveOwner;
            if (renderText || blockEntity.getGraveOwner() == null) {
                matrices.push();

                int width = this.textRenderer.getWidth(customName);

                float scale = 0.55f / width;


                switch (direction) {
                    case SOUTH:
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180));
                        matrices.translate(-1, 0, -1);
                        break;
                    case NORTH:
                        break;
                    case WEST:
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90));
                        matrices.translate(-1, 0, 0);
                        break;
                    case EAST:
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(270));
                        matrices.translate(0, 0, -1);
                        break;
                }

                matrices.translate(0.5, 0.6, 0.675); // Render text 0.0125 from the edge of the grave to avoid clipping
                matrices.scale(-1, -1, 0);

                matrices.scale(scale, scale, scale);
                matrices.translate(-width / 2.0, -4.5, 0);

                this.textRenderer.draw(customName, 0, 0, 0xFFFFFF, true, matrices.peek().getPositionMatrix(), vertexConsumers, false, 0, light);

                matrices.pop();
            }
        }

        BlockPos pos = blockEntity.getPos();
        BlockPos under = pos.down();
        World world = blockEntity.getWorld();

//        ClientPlayerEntity player = MinecraftClient.getInstance().player;
//        if (player != null) {
//            if (blockEntity.canGlow() && graveOwner != null && graveOwner.getId() == player.getUuid()) {
//                VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getSolid());
//                // Init stencil
//                GL20C.glEnable(GL20C.GL_STENCIL_TEST);
//                RenderSystem.clear(GL20C.GL_STENCIL_BUFFER_BIT, true);
//                RenderSystem.stencilMask(0xFF);
//                RenderSystem.clearStencil(0);
//
//                RenderSystem.stencilOp(GL20C.GL_KEEP, GL20C.GL_KEEP, GL20C.GL_REPLACE);
//
//                // Write modelPart to the stencil buffer
//                RenderSystem.stencilMask(0xFF);
//                RenderSystem.stencilFunc(GL20C.GL_ALWAYS, 1, 0xFF);
//                this.modelPart.render(matrices, consumer, light, overlay, 0, 0, 0, 255);
//
//                // Let's render the same model excluding its own pixels
//                // I.e., we should see nothing, I suppose
//                RenderSystem.stencilMask(0x00);
//                RenderSystem.stencilFunc(GL20C.GL_NOTEQUAL, 1, 0xFF);
//                matrices.scale(0.99f, 0.99f, 0.99f);
//                this.modelPart.render(matrices, consumer, light, overlay, 255, 255, 255, 255);
//
//                GL20C.glDisable(GL20C.GL_STENCIL_TEST);
//            }
//        }

        BlockState blockUnder = null;
        if (world != null) blockUnder = world.getBlockState(under);
        if (YigdConfig.getConfig().graveSettings.adaptRenderer && blockUnder != null) {

            matrices.push();

            matrices.scale(1.001f, 0.0626f, 1.001f);
            matrices.translate(-0.0005f, 0, -0.0005f);
            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(blockUnder, pos, world, matrices, vertexConsumers.getBuffer(RenderLayer.getCutout()), false, new Random());

            matrices.pop();
        }
    }
}