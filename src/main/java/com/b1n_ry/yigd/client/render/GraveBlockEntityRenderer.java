package com.b1n_ry.yigd.client.render;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.events.YigdEvents;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

public class GraveBlockEntityRenderer implements BlockEntityRenderer<GraveBlockEntity> {
    private static final Gson GSON = new Gson();

    private final Map<SkullBlock.Type, SkullModelBase> skullModels;
    private final Font textRenderer;
    private final Minecraft client;
    private final boolean adaptRenderer;

    private static ModelPart graveModel;
    @Nullable
    private static TextRenderInfo textRenderInfo = null;
    @Nullable
    private static SkullRenderInfo skullRenderInfo = null;
    private static final Map<String, Material> CUBOID_SPRITES = new HashMap<>();
    private static final RenderType OUTLINE_RENDER_LAYER;

    public GraveBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.skullModels = SkullBlockRenderer.createSkullRenderers(context.getModelSet());
        this.textRenderer = context.getFont();
        this.client = Minecraft.getInstance();

        this.adaptRenderer = YigdConfig.getConfig().graveRendering.adaptRenderer;
    }
    @Override
    public void render(@NotNull GraveBlockEntity entity, float tickDelta, @NotNull PoseStack poseStack, @NotNull MultiBufferSource multiBufferSource, int light, int overlay) {
        YigdConfig.GraveRendering config = YigdConfig.getConfig().graveRendering;
        if (!config.useCustomFeatureRenderer) return;

        BlockState state = entity.getBlockState();
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        float rotation = (float) switch (direction) {
            case SOUTH -> Math.PI;
            case WEST -> Math.PI * 0.5D;
            case EAST -> Math.PI * 1.5D;
            default -> 0;  // North (can't be up/down)
        };

        poseStack.pushPose();

        poseStack.rotateAround(Axis.YP.rotation(rotation), .5f, .5f, .5f);

        if (config.useGlowingEffect && entity.isUnclaimed()) {
            // Get the actual outline vertex consumer, instead of the normal one
            OutlineBufferSource consumerProvider = this.client.levelRenderer.renderBuffers.outlineBufferSource();
            this.renderGlowingOutline(entity, tickDelta, poseStack, consumerProvider, light, overlay);
        }

        if (config.useSkullRenderer)
            this.renderOwnerSkull(entity, tickDelta, poseStack, multiBufferSource, light, overlay);
        if (config.useTextRenderer)
            this.renderGraveText(entity, tickDelta, poseStack, multiBufferSource, light, overlay);
        this.renderGraveModel(entity, tickDelta, poseStack, multiBufferSource, light, overlay);

        poseStack.popPose();
    }

    private void renderOwnerSkull(GraveBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        ResolvableProfile skullOwner = entity.getGraveSkull();
        if (skullOwner == null) return;

        SkullBlock.Type type = SkullBlock.Types.PLAYER;
        RenderType renderLayer = SkullBlockRenderer.getRenderType(type, skullOwner);

        this.renderSkull(entity, tickDelta, matrices, vertexConsumers, light, overlay, renderLayer);
    }
    /**
     * Render the model with given RenderLayer. This way it can be rendered with both the skin texture and outline shader
     */
    private void renderSkull(GraveBlockEntity ignoredEntity, float ignoredTickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int ignoredOverlay, RenderType renderLayer) {
        if (skullRenderInfo == null) return;
        SkullBlock.Type type = SkullBlock.Types.PLAYER;

        SkullModelBase model = this.skullModels.get(type);

        matrices.pushPose();
        matrices.translate(0.5f, 0.25f, 0.5f);  // Required for calculations of rotation and scale to not change position

        int[] rotation = skullRenderInfo.rotation;

        matrices.translate(0D, -(4 - skullRenderInfo.height) / 16D, -(8 - skullRenderInfo.depth) / 16D);

        Quaternionf angle = new Quaternionf().rotateXYZ((float) Math.toRadians(rotation[0]), (float) Math.toRadians(rotation[1]), (float) Math.toRadians(rotation[2]));
        matrices.mulPose(angle);
        matrices.scale(skullRenderInfo.scaleFace, skullRenderInfo.scaleFace, skullRenderInfo.scaleDepth);

        matrices.translate(-0.5f, -0.25f, -0.5f);  // Move back to actual position. Calculations of scale and rotation are now done

        SkullBlockRenderer.renderSkull(null, 0, 0, matrices, vertexConsumers, light, model, renderLayer);
        matrices.popPose();
    }

    private void renderGraveText(GraveBlockEntity entity, float ignoredTickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int ignoredOverlay) {
        Component graveText = entity.getGraveText();
        if (graveText == null || textRenderInfo == null) return;

        matrices.pushPose();

        matrices.translate(.5, textRenderInfo.height / 16f, textRenderInfo.depth / 16f - 0.0001f);
        matrices.scale(-1, -1, 0);

        int textWidth = this.textRenderer.width(graveText.getString());
        float scale = textRenderInfo.width / (textWidth * 16f);
        matrices.scale(scale, scale, scale);

        matrices.translate(-textWidth / 2.0, -4.5, 0);

        this.textRenderer.drawInBatch(graveText, 0f, 0f, 0xFFFFFF, false, matrices.last().pose(), vertexConsumers, Font.DisplayMode.NORMAL, 0x0, light);

        matrices.popPose();
    }

    private void renderGraveModel(GraveBlockEntity entity, float ignoredTickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        for (Map.Entry<String, Material> cuboid : CUBOID_SPRITES.entrySet()) {
            String key = cuboid.getKey();
            ModelPart part = graveModel.getChild(key);
            if (this.adaptRenderer && key.equals("ground")) {
                Level world = entity.getLevel();
                if (world != null) {
                    BlockPos underPos = entity.getBlockPos().below();
                    BlockState blockUnder = entity.getLevel().getBlockState(underPos);

                    if (blockUnder != null && blockUnder.isSolidRender(world, underPos)) {
                        ModelPart.Cube cuboidPart = part.getRandomCube(world.random);  // Only contains 1 cuboid, so we'll get that one
                        float scaleX = cuboidPart.maxX - cuboidPart.minX;
                        float scaleZ = cuboidPart.maxZ - cuboidPart.minZ;

                        matrices.pushPose();

                        matrices.translate(cuboidPart.minX / 16f + .0005f, cuboidPart.maxY / 16f - 1f, cuboidPart.minZ / 16f + .0005f);
                        matrices.scale(.999f * (scaleX / 16f), 1f, .999f * (scaleZ / 16f));

                        this.client.getBlockRenderer()
                                .renderBatched(blockUnder, underPos, world, matrices, vertexConsumers.getBuffer(
                                        RenderType.cutout()), false, world.random);
                        matrices.popPose();

                        continue;
                    }
                }
            }
            VertexConsumer consumer = cuboid.getValue().buffer(vertexConsumers, RenderType::entityCutout);

            part.render(matrices, consumer, light, overlay);
        }
    }

    private void renderGlowingOutline(GraveBlockEntity entity, float ignoredTickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {
        LocalPlayer player = this.client.player;
        YigdEvents.RenderGlowingGraveEvent event = NeoForge.EVENT_BUS.post(new YigdEvents.RenderGlowingGraveEvent(entity, player));
        if (!event.isRenderGlowing()) return;

        VertexConsumer consumer = vertexConsumers.getBuffer(OUTLINE_RENDER_LAYER);

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
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition root = modelData.getRoot();

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
            ResourceLocation texture = ResourceLocation.parse(textureName);
            Material sprite = new Material(InventoryMenu.BLOCK_ATLAS, texture);

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
        graveModel = LayerDefinition.create(modelData, uvX, uvY).bakeRoot();
    }
    private static ModelPart getGraveModel() {
        MeshDefinition modelData = new MeshDefinition();
        PartDefinition root = modelData.getRoot();
        addChildPart(root, "ground", 0, 0, 0, 0, 0, 16, 1, 16);
        addChildPart(root, "base", 0, 21, 2, 1, 10, 12, 2, 5);
        addChildPart(root, "bust", 0, 28, 3, 3, 11, 10, 12, 3);
        addChildPart(root, "top", 0, 17, 4, 15, 11, 8, 1, 3);

        return LayerDefinition.create(modelData, 64, 64).bakeRoot();
    }
    private static void addChildPart(PartDefinition root, String name, int uvX, int uvY, float minX, float minY, float minZ, float sizeX, float sizeY, float sizeZ) {
        root.addOrReplaceChild(
                name,
                CubeListBuilder.create().texOffs(uvX, uvY).addBox(minX, minY, minZ, sizeX, sizeY, sizeZ),
                PartPose.offsetAndRotation(sizeX + minX * 2, sizeY + minY * 2, 0, 0, 0, (float) Math.PI));
    }
    static {
        graveModel = getGraveModel();
        OUTLINE_RENDER_LAYER = RenderType.outline(ResourceLocation.parse("textures/misc/white.png"));
    }

    private record TextRenderInfo(float depth, float height, float width) { }
    private record SkullRenderInfo(float depth, float height, int[] rotation, float scaleFace, float scaleDepth) { }
}
