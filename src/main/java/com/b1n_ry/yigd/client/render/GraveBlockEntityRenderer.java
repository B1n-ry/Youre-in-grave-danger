package com.b1n_ry.yigd.client.render;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.events.RenderGlowingGraveEvent;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.*;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.SkullBlockEntityModel;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

import java.util.Map;

public class GraveBlockEntityRenderer implements BlockEntityRenderer<GraveBlockEntity> {
    private final Map<SkullBlock.SkullType, SkullBlockEntityModel> skullModels;
    private final TextRenderer textRenderer;
    private final MinecraftClient client;

    private static ModelPart GRAVE_MODEL;
    private static Identifier TEXTURE_LOCATION;
    private static SpriteIdentifier SPRITE_IDENTIFIER;

    public static boolean renderOutlineShader = false;

    public GraveBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.skullModels = SkullBlockEntityRenderer.getModels(context.getLayerRenderDispatcher());
        this.textRenderer = context.getTextRenderer();
        this.client = MinecraftClient.getInstance();
    }

    @Override
    public void render(GraveBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        YigdConfig.GraveRendering config = YigdConfig.getConfig().graveRendering;
        if (!config.useCustomFeatureRenderer) return;

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

        if (config.useGlowingEffect)
            this.renderGlowingOutline(entity, tickDelta, matrices, vertexConsumers, light, overlay);

        if (config.useSkullRenderer)
            this.renderOwnerSkull(entity, tickDelta, matrices, vertexConsumers, light, overlay);
        if (config.useTextRenderer)
            this.renderGraveText(entity, tickDelta, matrices, vertexConsumers, light, overlay);
        this.renderGraveModel(entity, tickDelta, matrices, vertexConsumers, light, overlay);

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

    private void renderGraveModel(GraveBlockEntity ignoredEntity, float ignoredTickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        VertexConsumer consumer = SPRITE_IDENTIFIER.getVertexConsumer(vertexConsumers, RenderLayer::getEntityCutout);

        GRAVE_MODEL.getChild("ground").render(matrices, consumer, light, overlay);
        GRAVE_MODEL.getChild("base").render(matrices, consumer, light, overlay);
        GRAVE_MODEL.getChild("bust").render(matrices, consumer, light, overlay);
        GRAVE_MODEL.getChild("top").render(matrices, consumer, light, overlay);
    }

    private void renderGlowingOutline(GraveBlockEntity entity, float ignoredTickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        ClientPlayerEntity player = this.client.player;
        if (!RenderGlowingGraveEvent.EVENT.invoker().canRenderOutline(entity, player)) return;

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getOutline(new Identifier(Yigd.MOD_ID, "textures/block/grave.png")));

        renderOutlineShader = true;

        GRAVE_MODEL.render(matrices, consumer, light, overlay);
    }

    private static ModelPart getGraveModel() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        addChildPart(root, "ground", 0, 0, 0, 0, 0, 16, 1, 16);
        addChildPart(root, "base", 0, 21, 2, 1, 10, 12, 2, 5);
        addChildPart(root, "bust", 0, 28, 3, 3, 11, 10, 12, 3);
        addChildPart(root, "top", 0, 17, 4, 15, 11, 8, 1, 3);

        return TexturedModelData.of(modelData, 64, 64).createModel();
    }
    private static void addChildPart(ModelPartData root, String name, int uvX, int uvY, float minX, float minY, float minZ, float sizeX, float sizeY, float sizeZ) {
        root.addChild(
                name,
                ModelPartBuilder.create().uv(uvX, uvY).cuboid(minX, minY, minZ, sizeX, sizeY, sizeZ),
                ModelTransform.of(sizeX + minX * 2, sizeY + minY * 2, 0, 0, 0, (float) Math.PI));
    }

    static {
        GRAVE_MODEL = getGraveModel();
        TEXTURE_LOCATION = new Identifier(Yigd.MOD_ID, "block/grave");
        SPRITE_IDENTIFIER = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, TEXTURE_LOCATION);
    }
}
