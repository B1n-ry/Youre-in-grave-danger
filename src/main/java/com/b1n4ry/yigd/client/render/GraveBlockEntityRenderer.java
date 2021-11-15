package com.b1n4ry.yigd.client.render;

import com.b1n4ry.yigd.block.GraveBlock;
import com.b1n4ry.yigd.block.entity.GraveBlockEntity;
import com.b1n4ry.yigd.config.YigdConfig;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.*;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FallingBlockEntityRenderer;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class GraveBlockEntityRenderer implements BlockEntityRenderer<GraveBlockEntity> {
    private final TextRenderer textRenderer;
    private final EntityModelLoader renderLayer;
//    private final GraveModel model;

    public GraveBlockEntityRenderer(Context ctx) {
        this.textRenderer = ctx.getTextRenderer();
        this.renderLayer = ctx.getLayerRenderDispatcher();


//        model = new GraveModel(ctx.getLayerModelPart(new EntityModelLayer(new Identifier("yigd", "block/grave"), "main")));
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

                this.textRenderer.draw(customName, 0, 0, 0xFFFFFF, true, matrices.peek().getModel(), vertexConsumers, false, 0, light);

                matrices.pop();
            }
        }

        BlockPos pos = blockEntity.getPos();
        BlockPos under = new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ());
        World world = blockEntity.getWorld();

        BlockState blockUnder;
        if (YigdConfig.getConfig().graveSettings.adaptRenderer) {
            blockUnder = world.getBlockState(under);
        } else {
            blockUnder = Blocks.GRAVEL.getDefaultState();
        }

        matrices.push();
//        SpriteIdentifier spriteIdentifier = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, id);
//        VertexConsumer vertexConsumer = spriteIdentifier.getVertexConsumer(vertexConsumers, model::getLayer);
//        model.render(matrices, vertexConsumer, light, overlay, 0f, 0f, 0f, 1f);
//
//        Tessellator tessellator = Tessellator.getInstance();
//        BufferBuilder bufferBuilder = tessellator.getBuffer();
//
//        RenderSystem.enableTexture();
//        RenderSystem.enableCull();
//
//        RenderSystem.setShaderTexture(0, id);
//        bufferBuilder.vertex(0, 0, 0).color(1f, 1f, 1f, 1f).normal(16f, 1, 16f);
//
        matrices.scale(1f, 0.0625f, 1f);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlock(blockUnder, pos, world, matrices, vertexConsumers.getBuffer(RenderLayer.getCutout()), true, new Random());

        matrices.pop();
    }

    @Environment(EnvType.CLIENT)
    public static final class GraveModel extends Model {
        public final ModelPart root;

        public GraveModel(ModelPart ground) {
            super(RenderLayer::getEntityCutoutNoCull);
            this.root = ground;
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {

//            this.root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
            ModelPart.Cuboid cuboid = new ModelPart.Cuboid(0, 0, 0, 0, 0, 16, 1, 16, 0, 0, 0, false, 64, 64);
            cuboid.renderCuboid(matrices.peek(), vertices, light, overlay, red, green, blue, alpha);
        }

        public static @NotNull TexturedModelData getTexturedModelData() {
            ModelData modelData = new ModelData();
            ModelPartData modelPartData = modelData.getRoot();
            modelPartData.addChild("main", ModelPartBuilder.create().uv(0, 0).cuboid(0f, 0f, 0f, 16f, 1f, 16f), ModelTransform.NONE);
            return TexturedModelData.of(modelData, 64, 64);
        }
    }
}
