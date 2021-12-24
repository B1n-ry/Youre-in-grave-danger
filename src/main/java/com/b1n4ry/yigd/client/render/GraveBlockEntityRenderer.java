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
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory.Context;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.SkullEntityModel;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;

import java.util.Random;

@Environment(EnvType.CLIENT)
public class GraveBlockEntityRenderer implements BlockEntityRenderer<GraveBlockEntity> {
    private final TextRenderer textRenderer;
    private final EntityModelLoader renderLayer;

    private final ModelPart graveModel;

    public GraveBlockEntityRenderer(Context ctx) {
        this.textRenderer = ctx.getTextRenderer();
        this.renderLayer = ctx.getLayerRenderDispatcher();

        ModelPartData data = new ModelData().getRoot();
        data.addChild("ground_base", ModelPartBuilder.create().uv(0, 0).cuboid(0, 0, 0, 16, 1, 16), ModelTransform.NONE);
        data.addChild("grave_base", ModelPartBuilder.create().uv(0, 21).cuboid(2, 1, 1, 12, 2, 5), ModelTransform.NONE);
        data.addChild("grave_core", ModelPartBuilder.create().uv(0, 28).cuboid(3, 3, 2, 10, 12, 3), ModelTransform.NONE);
        data.addChild("grave_top", ModelPartBuilder.create().uv(0, 17).cuboid(4, 15, 2, 8, 1, 3), ModelTransform.NONE);
        graveModel = data.createPart(64, 64);
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
        MinecraftClient client = MinecraftClient.getInstance();

        BlockPos pos = blockEntity.getPos();
        BlockPos under = pos.down();
        World world = blockEntity.getWorld();

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
        matrices.push();

        switch (direction) {
            case WEST -> {
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(270f));
                matrices.translate(0, 0, -1);
            }
            case EAST -> {
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90f));
                matrices.translate(-1, 0, 0);
            }
            case NORTH -> {
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180));
                matrices.translate(-1, 0, -1);
            }
        }
        BlockState blockUnder = null;
        if (world != null) blockUnder = world.getBlockState(under);

        if (YigdConfig.getConfig().graveSettings.glowingGrave && blockEntity.canGlow() && client.player != null && blockEntity.getGraveOwner() != null && client.player.getUuid().equals(blockEntity.getGraveOwner().getId()) && !pos.isWithinDistance(client.player.getPos(), 10)) {
            graveModel.render(matrices, vertexConsumers.getBuffer(RenderLayer.getOutline(new Identifier("yigd", "textures/block/grave.png"))), light, overlay);
        }

        Identifier identifier = new Identifier("yigd", "block/grave");
        SpriteIdentifier texture = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, identifier);
        VertexConsumer vertexConsumer = texture.getVertexConsumer(vertexConsumers, RenderLayer::getEntityCutout);

        graveModel.getChild("grave_base").render(matrices, vertexConsumer, light, overlay);
        graveModel.getChild("grave_core").render(matrices, vertexConsumer, light, overlay);
        graveModel.getChild("grave_top").render(matrices, vertexConsumer, light, overlay);

        if (YigdConfig.getConfig().graveSettings.adaptRenderer && blockUnder != null && blockUnder.isOpaqueFullCube(world, pos)) {
            matrices.scale(0.999f, 1, 0.999f);
            matrices.translate(0.0005f, -0.9375f, 0.0005f);
            client.getBlockRenderManager().renderBlock(blockUnder, pos, world, matrices, vertexConsumers.getBuffer(RenderLayer.getCutout()), true, new Random());
        } else {
            graveModel.getChild("ground_base").render(matrices, vertexConsumer, light, overlay);
        }

        matrices.pop();
    }

    @Override
    public boolean isInRenderDistance(GraveBlockEntity blockEntity, Vec3d pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        double viewDistance = client.worldRenderer.getViewDistance();
        BlockPos blockPos = blockEntity.getPos();

        if (client.player == null) return false;
        return pos.isInRange(new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()), (viewDistance + 1) * 16);
    }
}