package com.b1n_ry.yigd.client.render;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.block.GraveBlock;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.YigdConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.model.*;
import net.minecraft.client.render.OverlayTexture;
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
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.World;

import java.util.*;

@Environment(EnvType.CLIENT)
public class GraveBlockEntityRenderer implements BlockEntityRenderer<GraveBlockEntity> {
    private final TextRenderer textRenderer;
    private final EntityModelLoader renderLayer;

    private final Identifier SHADER_TEXTURE = new Identifier("yigd", "textures/shader/glowing.png");

    private static Map<String, String> modelTextures;
    private static ModelPart graveModel;

    public static boolean renderGraveGlowing = false;

    public GraveBlockEntityRenderer(Context ctx) {
        this.textRenderer = ctx.getTextRenderer();
        this.renderLayer = ctx.getLayerRenderDispatcher();

        if (GraveBlock.customModel == null || !GraveBlock.customModel.isJsonObject()) {
            ModelPartData data = new ModelData().getRoot();
            data.addChild("Base_Layer", ModelPartBuilder.create().uv(0, 0).cuboid(0, 0, 0, 16, 1, 16).mirrored(false), ModelTransform.NONE);
            data.addChild("grave_base", ModelPartBuilder.create().uv(0, 21).cuboid(2, 1, 10, 12, 2, 5).mirrored(false), ModelTransform.NONE);
            data.addChild("grave_core", ModelPartBuilder.create().uv(0, 28).cuboid(3, 3, 11, 10, 12, 3).mirrored(false), ModelTransform.NONE);
            data.addChild("grave_top", ModelPartBuilder.create().uv(0, 17).cuboid(4, 15, 11, 8, 1, 3).mirrored(false), ModelTransform.NONE);

            List<String> modelNames = Arrays.asList("Base_Layer", "grave_base", "grave_core", "grave_top");
            List<String> textureLocations = Arrays.asList("yigd:block/grave", "yigd:block/grave", "yigd:block/grave", "yigd:block/grave");
            modelTextures = new HashMap<>();
            for (int i = 0; i < modelNames.size(); i++)
                modelTextures.put(modelNames.get(i), textureLocations.get(i));
            graveModel = data.createPart(64, 64);
        } else {
            reloadCustomModel();
        }
    }

    public static void reloadCustomModel() {
        ModelPartData data = new ModelData().getRoot();
        modelTextures = new HashMap<>();

        JsonObject modelObject = GraveBlock.customModel.getAsJsonObject();
        JsonArray textureSize = GraveBlock.customModel.getAsJsonArray("texture_size");
        JsonObject textures = GraveBlock.customModel.getAsJsonObject("textures");
        JsonArray elements = modelObject.getAsJsonArray("elements");
        int i = 0;
        for (JsonElement element : elements) {
            i++;
            JsonObject o = element.getAsJsonObject();
            JsonObject faces = o.getAsJsonObject("faces");
            float minX = Float.NaN;
            float maxX = Float.NaN;
            float minY = Float.NaN;
            float maxY = Float.NaN;
            String textureName = "";
            for (Map.Entry<String, JsonElement> entry : faces.entrySet()) {
                textureName = entry.getValue().getAsJsonObject().get("texture").getAsString();
                JsonArray uv = entry.getValue().getAsJsonObject().getAsJsonArray("uv");
                float localMinX = Math.min(uv.get(0).getAsFloat(), uv.get(2).getAsFloat());
                float localMaxX = Math.max(uv.get(0).getAsFloat(), uv.get(2).getAsFloat());

                float localMinY = Math.min(uv.get(1).getAsFloat(), uv.get(3).getAsFloat());
                float localMaxY = Math.max(uv.get(1).getAsFloat(), uv.get(3).getAsFloat());

                minX = !Float.isNaN(minX) ? Math.min(minX, localMinX) : localMinX;
                maxX = !Float.isNaN(maxX) ? Math.max(maxX, localMaxX) : localMaxX;
                minY = !Float.isNaN(minY) ? Math.min(minY, localMinY) : localMinY;
                maxY = !Float.isNaN(maxY) ? Math.max(maxY, localMaxY) : localMaxY;
            }
            textureName = textureName.replaceFirst("#", "");

            if (Float.isNaN(minX) || Float.isNaN(minY) || Float.isNaN(maxX) || Float.isNaN(maxY)) continue;
            minX *= (textureSize.get(0).getAsFloat() / 16f);
            minY *= (textureSize.get(1).getAsFloat() / 16f);

            JsonArray from = o.getAsJsonArray("from");
            JsonArray to = o.getAsJsonArray("to");
            float x1 = from.get(0).getAsFloat();
            float y1 = from.get(1).getAsFloat();
            float z1 = from.get(2).getAsFloat();

            float x2 = to.get(0).getAsFloat();
            float y2 = to.get(1).getAsFloat();
            float z2 = to.get(2).getAsFloat();

            String name = o.get("name") != null ? o.get("name").getAsString() : "" + i;
            String textureLocation = textures.get(textureName).getAsString();
            modelTextures.put(name, textureLocation);

            // For some reason all shapes generates upside down, so they have to be re-rotated to be as they should
            data.addChild(name, ModelPartBuilder.create().uv((int) minX, (int) minY).cuboid(x1, y1, z1, x2 - x1, y2 - y1, z2 - z1), ModelTransform.of((x1 + x2), (y1 + y2), 0, 0, 0, (float) Math.PI));
        }
        graveModel = data.createPart(textureSize.get(0).getAsInt(), textureSize.get(1).getAsInt());
    }

    public SkullBlockEntityModel getSkull() {
        return new SkullEntityModel(renderLayer.getModelPart(EntityModelLayers.PLAYER_HEAD));
    }

    @Override
    public void render(GraveBlockEntity blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        YigdConfig config = YigdConfig.getConfig();
        if (!config.graveSettings.graveRenderSettings.useRenderFeatures) return;
        if (blockEntity == null) {
            return;
        }
        Direction direction = blockEntity.getCachedState().get(Properties.HORIZONTAL_FACING);
        MinecraftClient client = MinecraftClient.getInstance();

        BlockPos pos = blockEntity.getPos();
        BlockPos under = pos.down();
        World world = blockEntity.getWorld();

        JsonObject featureRenders = GraveBlock.customModel.getAsJsonObject("features");

        ItemStack headItem = client.player != null ? client.player.getInventory().armor.get(3) : ItemStack.EMPTY;
        boolean wearingGraveXray = !headItem.isEmpty() && EnchantmentHelper.get(headItem).containsKey(Yigd.DEATH_SIGHT);
        boolean withingGlowDistance = client.player != null && pos.isWithinDistance(client.player.getPos(), config.graveSettings.graveRenderSettings.glowMaxDistance) && !pos.isWithinDistance(client.player.getPos(), config.graveSettings.graveRenderSettings.glowMinDistance);

        boolean canGlow = wearingGraveXray || (config.graveSettings.graveRenderSettings.glowingGrave && blockEntity.canGlow() && client.player != null && blockEntity.getGraveOwner() != null && client.player.getUuid().equals(blockEntity.getGraveOwner().getId()) && withingGlowDistance);
        GameProfile graveOwner = blockEntity.getGraveOwner();
        if (graveOwner != null && config.graveSettings.graveRenderSettings.renderGraveSkull) {
            matrices.push();

            matrices.translate(0.5f, 0.25f, 0.5f);
            float yRotation;
            switch (direction) {
                case SOUTH -> yRotation = 180f;
                case WEST -> yRotation = 90f;
                case EAST -> yRotation = 270f;
                default -> yRotation = 0;
            }
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(yRotation));


            float midY = 2f;
            float midZ = 5f;

            float rotX = 90f;
            float rotY = 0;
            float rotZ = 0;

            float scaleXY = 1f;
            float scaleZ = 0.25f;

            boolean showSkull = false;
            if (featureRenders != null) {
                JsonObject headRender = featureRenders.getAsJsonObject("skull");
                if (headRender != null) {
                    midY = headRender.get("height").getAsFloat();
                    midZ = headRender.get("depth").getAsFloat();

                    if (headRender.getAsJsonArray("rotation") != null) {
                        rotX = headRender.getAsJsonArray("rotation").get(0).getAsFloat();
                        rotY = headRender.getAsJsonArray("rotation").get(1).getAsFloat();
                        rotZ = headRender.getAsJsonArray("rotation").get(2).getAsFloat();
                    } else {
                        rotX = 0;
                        rotY = 0;
                        rotZ = 0;
                    }

                    if (headRender.get("scaleFace") != null) {
                        scaleXY = headRender.get("scaleFace").getAsFloat();
                    }
                    if (headRender.get("scaleDepth") != null) {
                        scaleZ = headRender.get("scaleDepth").getAsFloat();
                    } else {
                        scaleZ = 1;
                    }

                    showSkull = true;
                }
            }

            matrices.translate(0, -(4f - midY) / 16f, -(8f - midZ) / 16f);
            matrices.multiply(new Quaternion(rotX, rotY, rotZ, true));

            matrices.scale(scaleXY, scaleXY, scaleZ);

            matrices.translate(-0.5f, -0.25f, -0.5f);

            if (showSkull) {
                SkullBlockEntityModel skull = getSkull();
                SkullBlockEntityRenderer.renderSkull(null, 0, 0, matrices, vertexConsumers, light, skull, SkullBlockEntityRenderer.getRenderLayer(SkullBlock.Type.PLAYER, blockEntity.getGraveOwner()));

                if (canGlow) {
                    SkullBlockEntityRenderer.renderSkull(null, 0, 0, matrices, vertexConsumers, light, skull, RenderLayer.getOutline(SHADER_TEXTURE));
                }
            }

            matrices.pop();
        }

        String customName = blockEntity.getCustomName();
        if (customName != null) {
            boolean renderText = config.graveSettings.graveRenderSettings.renderGraveOwner;
            if (renderText || blockEntity.getGraveOwner() == null) {
                matrices.push();

                int width = this.textRenderer.getWidth(customName);

                float scale = 1f / width;
                float textWidth = 8.8f;
                float textDepth = 11f;
                float textHeight = 9.6f;

                boolean showText = false;
                if (featureRenders != null) {
                    JsonObject textRender = featureRenders.getAsJsonObject("text");
                    if (textRender != null) {
                        textWidth = textRender.get("width").getAsFloat();
                        textDepth = textRender.get("depth").getAsFloat();
                        textHeight = textRender.get("height").getAsFloat();

                        showText = true;
                    }
                }

                scale *= (textWidth / 16f);

                switch (direction) {
                    case SOUTH -> {
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180));
                        matrices.translate(-1, 0, -1);
                    }
                    case WEST -> {
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90));
                        matrices.translate(-1, 0, 0);
                    }
                    case EAST -> {
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(270));
                        matrices.translate(0, 0, -1);
                    }
                }

                matrices.translate(0.5, textHeight / 16, (textDepth / 16f) - 0.001f); // Render text 0.001 from the edge of the grave to avoid clipping
                matrices.scale(-1, -1, 0);

                matrices.scale(scale, scale, scale);
                matrices.translate(-width / 2.0, -4.5, 0);

                if (showText) this.textRenderer.draw(customName, 0, 0, 0xFFFFFF, config.graveSettings.graveRenderSettings.textShadow, matrices.peek().getModel(), vertexConsumers, false, 0, light);

                matrices.pop();
            }
        }
        matrices.push();

        switch (direction) {
            case EAST -> {
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(270f));
                matrices.translate(0, 0, -1);
            }
            case WEST -> {
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90f));
                matrices.translate(-1, 0, 0);
            }
            case SOUTH -> {
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180));
                matrices.translate(-1, 0, -1);
            }
        }
        BlockState blockUnder = null;
        if (world != null) blockUnder = world.getBlockState(under);

        if (canGlow) {
            graveModel.forEachCuboid(matrices, (matrix, path, index, cuboid) -> cuboid.renderCuboid(matrix, vertexConsumers.getBuffer(RenderLayer.getOutline(SHADER_TEXTURE)), light, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1));

            renderGraveGlowing = true;
        }


        for (Map.Entry<String, String> entry : modelTextures.entrySet()) {
            if (entry.getKey().equals("Base_Layer")) continue;

            Identifier identifier = new Identifier(entry.getValue());
            SpriteIdentifier texture = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, identifier);
            VertexConsumer vertexConsumer = texture.getVertexConsumer(vertexConsumers, RenderLayer::getEntityCutout);

            ModelPart child = graveModel.getChild(entry.getKey());
            child.render(matrices, vertexConsumer, light, overlay);
        }

        if (modelTextures.containsKey("Base_Layer")) {
            if (config.graveSettings.graveRenderSettings.adaptRenderer && blockUnder != null && blockUnder.isOpaqueFullCube(world, pos)) {
                ModelPart baseLayer = graveModel.getChild("Base_Layer");
                ModelPart.Cuboid cuboid = baseLayer.getRandomCuboid(new Random());
                float scaleX = cuboid.maxX - cuboid.minX;
                float scaleZ = cuboid.maxZ - cuboid.minZ;

                matrices.translate((cuboid.minX / 16f) + 0.0005f, (cuboid.maxY / 16f) - 1, (cuboid.minZ / 16f) + 0.0005f);
                matrices.scale(0.999f * (scaleX / 16f), 1, 0.999f * (scaleZ / 16f));
                client.getBlockRenderManager().renderBlock(blockUnder, pos, world, matrices, vertexConsumers.getBuffer(RenderLayer.getCutout()), true, new Random());
            } else {
                Identifier identifier = new Identifier(modelTextures.get("Base_Layer"));
                SpriteIdentifier texture = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, identifier);
                VertexConsumer vertexConsumer = texture.getVertexConsumer(vertexConsumers, RenderLayer::getEntityCutout);

                graveModel.getChild("Base_Layer").render(matrices, vertexConsumer, light, overlay);
            }
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