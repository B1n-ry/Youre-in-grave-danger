package com.b1n_ry.yigd.client.render;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import org.joml.Quaternionf;

import java.util.Map;

public class GraveBlockEntityRenderer implements BlockEntityRenderer<GraveBlockEntity> {
    private final Map<SkullBlock.SkullType, SkullBlockEntityModel> skullModels;

    public GraveBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.skullModels = SkullBlockEntityRenderer.getModels(context.getLayerRenderDispatcher());
    }

    @Override
    public void render(GraveBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        GameProfile graveOwner = entity.getGraveOwner();
        if (graveOwner == null) return;

        BlockState state = entity.getCachedState();
        Direction direction = state.get(Properties.HORIZONTAL_FACING);

        float rotation = (float) switch (direction) {
            case SOUTH -> Math.PI;
            case EAST -> Math.PI * 0.5D;
            case WEST -> Math.PI * 1.5D;
            default -> 0;  // North (can't be up/down)
        };

        SkullBlock.SkullType type = SkullBlock.Type.PLAYER;
        SkullBlockEntityModel model = this.skullModels.get(type);
        RenderLayer renderLayer = SkullBlockEntityRenderer.getRenderLayer(type, graveOwner);

        matrices.push();
        matrices.multiply(new Quaternionf().rotateXYZ((float) Math.PI / 2f, 0f, rotation), .5f, .25f, .5f);
        matrices.translate(0D, -.1875D, .5);
        matrices.scale(1f, 1f, .25f);

        SkullBlockEntityRenderer.renderSkull(null, 0, 0, matrices, vertexConsumers, light, model, renderLayer);
        matrices.pop();
    }
}
