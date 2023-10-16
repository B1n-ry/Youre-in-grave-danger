package com.b1n_ry.yigd.client.render;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

import java.util.Map;

public class GraveBlockEntityRenderer implements BlockEntityRenderer<GraveBlockEntity> {
    private final Map<SkullBlock.SkullType, SkullBlockEntityModel> skullModels;
    private final TextRenderer textRenderer;

    public GraveBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.skullModels = SkullBlockEntityRenderer.getModels(context.getLayerRenderDispatcher());
        this.textRenderer = context.getTextRenderer();
    }

    @Override
    public void render(GraveBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockState state = entity.getCachedState();
        Direction direction = state.get(Properties.HORIZONTAL_FACING);

        float rotation = (float) switch (direction) {
            case SOUTH -> Math.PI;
            case WEST -> Math.PI * 0.5D;
            case EAST -> Math.PI * 1.5D;
            default -> 0;  // North (can't be up/down)
        };

        matrices.push();

        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(rotation), .5f, .5f, .5f);
        this.renderOwnerSkull(entity, tickDelta, matrices, vertexConsumers, light, overlay);
        this.renderGraveText(entity, tickDelta, matrices, vertexConsumers, light, overlay);

        matrices.pop();
    }

    private void renderOwnerSkull(GraveBlockEntity entity, float ignoredTickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int ignoredOverlay) {
        GameProfile graveOwner = entity.getGraveOwner();
        if (graveOwner == null) return;

        SkullBlock.SkullType type = SkullBlock.Type.PLAYER;
        SkullBlockEntityModel model = this.skullModels.get(type);
        RenderLayer renderLayer = SkullBlockEntityRenderer.getRenderLayer(type, graveOwner);

        matrices.push();

        matrices.multiply(RotationAxis.POSITIVE_X.rotation((float) Math.PI / 2f), .5f, .25f, .5f);
        matrices.translate(0D, -.1875D, .5);
        matrices.scale(1f, 1f, .25f);

        SkullBlockEntityRenderer.renderSkull(null, 0, 0, matrices, vertexConsumers, light, model, renderLayer);
        matrices.pop();
    }

    private void renderGraveText(GraveBlockEntity entity, float ignoredTickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int ignoredOverlay) {
        Text graveText = entity.getGraveText();
        if (graveText == null) return;

        matrices.push();

        matrices.multiply(RotationAxis.NEGATIVE_Z.rotation((float) Math.PI));

        matrices.translate(-12f / 16, -10.6f / 16, 11f / 16 - 0.0001f);

        int textWidth = this.textRenderer.getWidth(entity.getGraveText().getString());
        float scalar = 8f / (textWidth * 16);
        matrices.scale(scalar, scalar, scalar);

        this.textRenderer.draw(graveText, 0f, 0f, 0xFFFFFF, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0x0, light);

        matrices.pop();
    }
}
