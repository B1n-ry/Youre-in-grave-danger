package com.b1n4ry.yigd.client.render;

import com.b1n4ry.yigd.block.entity.GraveBlockEntity;
import com.b1n4ry.yigd.config.YigdConfig;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3f;

public class GraveBlockEntityRenderer extends BlockEntityRenderer<GraveBlockEntity> {
    public GraveBlockEntityRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(GraveBlockEntity blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (blockEntity == null) {
            return;
        }
        Direction direction = blockEntity.getCachedState().get(Properties.HORIZONTAL_FACING);

        if (blockEntity.getGraveOwner() != null && YigdConfig.getConfig().graveSettings.renderGraveSkull) {
            matrices.push();

            switch (direction) {
                case SOUTH:
                    matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180f));
                    matrices.translate(-1.0, 0.25, -0.9375);
                    break;
                case NORTH:
                    matrices.translate(0.0, 0.25, 0.0625);
                    break;
                case WEST:
                    matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90f));
                    matrices.translate(-1.0, 0.25, 0.0625);
                    break;
                case EAST:
                    matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(270f));
                    matrices.translate(0.0, 0.25, -0.9375);
                    break;
            }
            matrices.multiply(Vec3f.NEGATIVE_X.getDegreesQuaternion(270f));

            matrices.scale(1f, 1f, 0.25f);

            SkullBlockEntityRenderer.render(null, 0f, SkullBlock.Type.PLAYER, blockEntity.getGraveOwner(), 0f, matrices, vertexConsumers, light);
            matrices.pop();
        }

        String customName = blockEntity.getCustomName();

        if (customName != null) {
            boolean renderText = YigdConfig.getConfig().graveSettings.renderGraveOwner;
            if (!renderText || blockEntity.getGraveOwner() == null) {
                matrices.push();

                int width = this.dispatcher.getTextRenderer().getWidth(customName);

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

                this.dispatcher.getTextRenderer().draw(customName, 0, 0, 0xFFFFFF, true, matrices.peek().getModel(), vertexConsumers, false, 0, light);

                matrices.pop();
            }
        }
    }
}
