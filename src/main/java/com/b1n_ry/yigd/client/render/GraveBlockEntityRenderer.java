package com.b1n_ry.yigd.client.render;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.events.RenderGlowingGraveEvent;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

public class GraveBlockEntityRenderer implements BlockEntityRenderer<GraveBlockEntity> {
    private static final Gson GSON = new Gson();

    private final Map<SkullBlock.SkullType, SkullBlockEntityModel> skullModels;
    private final TextRenderer textRenderer;
    private final MinecraftClient client;

    private static ModelPart graveModel;
    @Nullable
    private static TextRenderInfo textRenderInfo = null;
    @Nullable
    private static SkullRenderInfo skullRenderInfo = null;
    private static final Map<String, SpriteIdentifier> CUBOID_SPRITES = new HashMap<>();
    private static final RenderLayer OUTLINE_RENDER_LAYER;

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

    private void renderOwnerSkull(GraveBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        GameProfile graveOwner = entity.getGraveOwner();
        if (graveOwner == null) return;

        SkullBlock.SkullType type = SkullBlock.Type.PLAYER;
        RenderLayer renderLayer = SkullBlockEntityRenderer.getRenderLayer(type, graveOwner);

        this.renderSkull(entity, tickDelta, matrices, vertexConsumers, light, overlay, renderLayer);
    }
    /**
     * Render the model with given RenderLayer. This way it can be rendered with both the skin texture and outline shader
     */
    private void renderSkull(GraveBlockEntity ignoredEntity, float ignoredTickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int ignoredOverlay, RenderLayer renderLayer) {
        if (skullRenderInfo == null) return;
        SkullBlock.SkullType type = SkullBlock.Type.PLAYER;

        SkullBlockEntityModel model = this.skullModels.get(type);

        matrices.push();
        matrices.translate(0.5f, 0.25f, 0.5f);  // Required for calculations of rotation and scale to not change position

        int[] rotation = skullRenderInfo.rotation;

        matrices.translate(0D, -(4 - skullRenderInfo.height) / 16D, -(8 - skullRenderInfo.depth) / 16D);

        Quaternionf angle = new Quaternionf().rotateXYZ((float) Math.toRadians(rotation[0]), (float) Math.toRadians(rotation[1]), (float) Math.toRadians(rotation[2]));
        matrices.multiply(angle);
        matrices.scale(skullRenderInfo.scaleFace, skullRenderInfo.scaleFace, skullRenderInfo.scaleDepth);

        matrices.translate(-0.5f, -0.25f, -0.5f);  // Move back to actual position. Calculations of scale and rotation are now done

        SkullBlockEntityRenderer.renderSkull(null, 0, 0, matrices, vertexConsumers, light, model, renderLayer);
        matrices.pop();
    }

    private void renderGraveText(GraveBlockEntity entity, float ignoredTickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int ignoredOverlay) {
        Text graveText = entity.getGraveText();
        if (graveText == null || textRenderInfo == null) return;

        matrices.push();

        matrices.translate(.5, textRenderInfo.height / 16f, textRenderInfo.depth / 16f - 0.0001f);
        matrices.scale(-1, -1, 0);

        int textWidth = this.textRenderer.getWidth(graveText.getString());
        float scale = textRenderInfo.width / (textWidth * 16f);
        matrices.scale(scale, scale, scale);

        matrices.translate(-textWidth / 2.0, -4.5, 0);

        this.textRenderer.draw(graveText, 0f, 0f, 0xFFFFFF, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0x0, light);

        matrices.pop();
    }

    private void renderGraveModel(GraveBlockEntity ignoredEntity, float ignoredTickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        for (Map.Entry<String, SpriteIdentifier> cuboid : CUBOID_SPRITES.entrySet()) {
            ModelPart part = graveModel.getChild(cuboid.getKey());
            VertexConsumer consumer = cuboid.getValue().getVertexConsumer(vertexConsumers, RenderLayer::getEntityCutout);

            part.render(matrices, consumer, light, overlay);
        }
    }

    private void renderGlowingOutline(GraveBlockEntity entity, float ignoredTickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        ClientPlayerEntity player = this.client.player;
        if (!RenderGlowingGraveEvent.EVENT.invoker().canRenderOutline(entity, player)) return;

        VertexConsumer consumer = vertexConsumers.getBuffer(OUTLINE_RENDER_LAYER);

        renderOutlineShader = true;

        graveModel.render(matrices, consumer, light, overlay);
        if (YigdConfig.getConfig().graveRendering.useSkullRenderer)
            this.renderSkull(entity, ignoredTickDelta, matrices, vertexConsumers, light, overlay, OUTLINE_RENDER_LAYER);
    }


    /**
     * Takes JSON and reloads current grave model to what the JSON describes
     * @param json Model json (same the baked block model uses)
     * @throws IllegalStateException if the model json is incomplete or wrong
     */
    public static void reloadModelFromJson(JsonObject json) throws IllegalStateException {
        CUBOID_SPRITES.clear();
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        JsonArray textureSize = json.getAsJsonArray("texture_size");
        JsonObject textures = json.getAsJsonObject("textures");
        JsonArray elements = json.getAsJsonArray("elements");
        JsonObject features = json.has("features") ? json.getAsJsonObject("features") : null;

        int uvX = textureSize.get(0).getAsInt();
        int uvY = textureSize.get(1).getAsInt();
        Map<String, String> nameIds = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : textures.entrySet()) {
            String key = e.getKey();
            String value = e.getValue().getAsString();

            nameIds.put(key, value);
        }
        int i = 0;
        for (JsonElement e : elements) {
            JsonObject o = e.getAsJsonObject();
            String name = o.has("name") ? o.get("name").getAsString() : String.valueOf(i++);
            JsonArray from = o.getAsJsonArray("from");
            JsonArray to = o.getAsJsonArray("to");
            JsonObject faces = o.getAsJsonObject("faces");

            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            String textureName = "";
            for (Map.Entry<String, JsonElement> face : faces.entrySet()) {
                JsonObject value = face.getValue().getAsJsonObject();
                JsonArray uv = value.getAsJsonArray("uv");
                textureName = value.get("texture").getAsString();

                minX = Math.min(minX, uv.get(0).getAsFloat());
                minY = Math.min(minY, uv.get(1).getAsFloat());
            }
            minX *= uvX / 16f;
            minY *= uvY / 16f;

            textureName = textureName.replaceFirst("#", "");
            if (nameIds.containsKey(textureName)) {
                textureName = nameIds.get(textureName);
            }
            Identifier texture = new Identifier(textureName);
            SpriteIdentifier sprite = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, texture);

            CUBOID_SPRITES.put(name, sprite);

            float fromX = from.get(0).getAsFloat();
            float fromY = from.get(1).getAsFloat();
            float fromZ = from.get(2).getAsFloat();
            float toX = to.get(0).getAsFloat();
            float toY = to.get(1).getAsFloat();
            float toZ = to.get(2).getAsFloat();

            // Min no longer have to be in from
            float lowerX = Math.min(fromX, toX);
            float lowerY = Math.min(fromY, toY);
            float lowerZ = Math.min(fromZ, toZ);
            float higherX = Math.max(fromX, toX);
            float higherY = Math.max(fromY, toY);
            float higherZ = Math.max(fromZ, toZ);
            addChildPart(root, name, (int) minX, (int) minY, lowerX, lowerY, lowerZ, higherX - lowerX, higherY - lowerY, higherZ - lowerZ);
        }
        if (features != null) {
            if (features.has("text")) {
                textRenderInfo = GSON.fromJson(features.get("text"), TextRenderInfo.class);
            }
            if (features.has("skull")) {
                skullRenderInfo = GSON.fromJson(features.get("skull"), SkullRenderInfo.class);
            }
        }
        graveModel = TexturedModelData.of(modelData, uvX, uvY).createModel();
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
        graveModel = getGraveModel();
        OUTLINE_RENDER_LAYER = RenderLayer.getOutline(new Identifier("textures/misc/white.png"));
    }

    private record TextRenderInfo(float depth, float height, float width) { }
    private record SkullRenderInfo(float depth, float height, int[] rotation, float scaleFace, float scaleDepth) { }
}
