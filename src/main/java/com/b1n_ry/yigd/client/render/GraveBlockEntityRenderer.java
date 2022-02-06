package com.b1n_ry.yigd.client.render;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.block.GraveBlock;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.mixin.WorldRendererAccessor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;

import java.util.*;

@Environment(EnvType.CLIENT)
public class GraveBlockEntityRenderer extends BlockEntityRenderer<GraveBlockEntity> {
    private final Identifier SHADER_TEXTURE = new Identifier("yigd", "textures/shader/glowing.png");

    private static final Map<String, String> modelTextures = new HashMap<>();
    private static final Map<String, ModelPart> parts = new HashMap<>();

    public static boolean renderGraveGlowing = false;

    public GraveBlockEntityRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);

        if (GraveBlock.customModel == null || !GraveBlock.customModel.isJsonObject()) {
            ModelPart baseLayer = new ModelPart(64, 64, 0, 0);
            baseLayer.addCuboid(0, 0, 0, 16, 1, 16, false);
            ModelPart graveBase = new ModelPart(64, 64, 0, 21);
            graveBase.addCuboid(2, 1, 10, 12, 2, 5, false);
            ModelPart graveCore = new ModelPart(64, 64, 0, 28);
            graveCore.addCuboid(3, 3, 11, 10, 12, 3, false);
            ModelPart graveTop = new ModelPart(64, 64, 0, 17);
            graveTop.addCuboid(4, 15, 11, 8, 1, 3, false);

            // For some reason all shapes generates upside down, so they have to be re-rotated to be as they should
            baseLayer.setPivot(16, 1, 0);
            baseLayer.roll += Math.PI;
            graveBase.setPivot(16, 4, 0);
            graveBase.roll += Math.PI;
            graveCore.setPivot(16, 18, 0);
            graveCore.roll += Math.PI;
            graveTop.setPivot(16, 31, 0);
            graveTop.roll += Math.PI;

            parts.put("Base_Layer", baseLayer);
            parts.put("grave_base", graveBase);
            parts.put("grave_core", graveCore);
            parts.put("grave_top", graveTop);

            List<String> modelNames = Arrays.asList("Base_Layer", "grave_base", "grave_core", "grave_top");
            List<String> textureLocations = Arrays.asList("yigd:block/grave", "yigd:block/grave", "yigd:block/grave", "yigd:block/grave");
            modelTextures.clear();
            for (int i = 0; i < modelNames.size(); i++)
                modelTextures.put(modelNames.get(i), textureLocations.get(i));
        } else {
            reloadCustomModel();
        }
    }

    public static void reloadCustomModel() {
        if (GraveBlock.customModel == null) return;
        parts.clear();
        modelTextures.clear();

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

            ModelPart iPart = new ModelPart(textureSize.get(0).getAsInt(), textureSize.get(1).getAsInt(), (int) minX, (int) minY);
            iPart.addCuboid(x1, y1, z1, x2 - x1, y2 - y1, z2 - z1, false);

            // For some reason all shapes generates upside down, so they have to be re-rotated to be as they should
            iPart.setPivot((x1 + x2), (y1 + y2), 0);
            iPart.roll += Math.PI;

            parts.put(name, iPart);
        }
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

        ItemStack headItem = client.player != null ? client.player.inventory.armor.get(3) : ItemStack.EMPTY;
        boolean wearingGraveXray = !headItem.isEmpty() && EnchantmentHelper.get(headItem).containsKey(Yigd.DEATH_SIGHT);

        boolean canGlow = wearingGraveXray || (config.graveSettings.graveRenderSettings.glowingGrave && blockEntity.canGlow() && client.player != null && blockEntity.getGraveOwner() != null && client.player.getUuid().equals(blockEntity.getGraveOwner().getId()) && !pos.isWithinDistance(client.player.getPos(), config.graveSettings.graveRenderSettings.glowMinDistance));
        GameProfile graveOwner = blockEntity.getGraveOwner();
        if (graveOwner != null && config.graveSettings.graveRenderSettings.renderGraveSkull) {
            matrices.push();

            matrices.translate(0.5f, 0.25f, 0.5f);
            float yRotation;
            switch (direction) {
                case SOUTH: yRotation = 180f; break;
                case WEST: yRotation = 90f; break;
                case EAST: yRotation = 270f; break;
                default: yRotation = 0;
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
                SkullBlockEntityRenderer.render(null, 0f, SkullBlock.Type.PLAYER, blockEntity.getGraveOwner(), 0f, matrices, vertexConsumers, light);

                if (canGlow) {
                    SkullBlockEntityRenderer.render(null, 0f, SkullBlock.Type.PLAYER, blockEntity.getGraveOwner(), 0f, matrices, ((WorldRendererAccessor)(MinecraftClient.getInstance().worldRenderer)).getBufferBuilders().getOutlineVertexConsumers(), light);
                }
            }

            matrices.pop();
        }

        String customName = blockEntity.getCustomName();
        if (customName != null) {
            boolean renderText = config.graveSettings.graveRenderSettings.renderGraveOwner;
            if (renderText || blockEntity.getGraveOwner() == null) {
                matrices.push();

                int width = dispatcher.getTextRenderer().getWidth(customName);

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
                    case SOUTH: {
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180));
                        matrices.translate(-1, 0, -1);
                        break;
                    }
                    case WEST: {
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90));
                        matrices.translate(-1, 0, 0);
                        break;
                    }
                    case EAST: {
                        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(270));
                        matrices.translate(0, 0, -1);
                        break;
                    }
                }

                matrices.translate(0.5, textHeight / 16, (textDepth / 16f) - 0.001f); // Render text 0.001 from the edge of the grave to avoid clipping
                matrices.scale(-1, -1, 0);

                matrices.scale(scale, scale, scale);
                matrices.translate(-width / 2.0, -4.5, 0);

                if (showText) dispatcher.getTextRenderer().draw(customName, 0, 0, 0xFFFFFF, config.graveSettings.graveRenderSettings.textShadow, matrices.peek().getModel(), vertexConsumers, false, 0, light);

                matrices.pop();
            }
        }
        matrices.push();

        switch (direction) {
            case EAST: {
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(270f));
                matrices.translate(0, 0, -1);
                break;
            }
            case WEST: {
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90f));
                matrices.translate(-1, 0, 0);
                break;
            }
            case SOUTH: {
                matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180));
                matrices.translate(-1, 0, -1);
                break;
            }
        }
        BlockState blockUnder = null;
        if (world != null) blockUnder = world.getBlockState(under);

        if (canGlow) {
            parts.forEach((name, modelPart) -> modelPart.render(matrices, vertexConsumers.getBuffer(RenderLayer.getOutline(SHADER_TEXTURE)), light, OverlayTexture.DEFAULT_UV, 1, 1, 1, 1));

            renderGraveGlowing = true;
        }


        for (Map.Entry<String, String> entry : modelTextures.entrySet()) {
            if (entry.getKey().equals("Base_Layer")) continue;

            Identifier identifier = new Identifier(entry.getValue());
            SpriteIdentifier texture = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, identifier);
            VertexConsumer vertexConsumer = texture.getVertexConsumer(vertexConsumers, RenderLayer::getEntityCutout);

            ModelPart child = parts.get(entry.getKey());
            if (child != null) child.render(matrices, vertexConsumer, light, overlay);
        }

        if (parts.containsKey("Base_Layer")) {
            if (config.graveSettings.graveRenderSettings.adaptRenderer && blockUnder != null && blockUnder.isOpaqueFullCube(world, pos)) {
                ModelPart baseLayer = parts.get("Base_Layer");
                ModelPart.Cuboid cuboid = baseLayer.getRandomCuboid(new Random());
                float scaleX = cuboid.maxX - cuboid.minX;
                float scaleZ = cuboid.maxZ - cuboid.minZ;

                matrices.translate((cuboid.minX / 16f) + 0.0005f, (cuboid.maxY / 16f) - 1, (cuboid.minZ / 16f) + 0.0005f);
                matrices.scale(0.999f * (scaleX / 16f), 1, 0.999f * (scaleZ / 16f));
                client.getBlockRenderManager().renderBlock(blockUnder, pos, world, matrices, vertexConsumers.getBuffer(RenderLayer.getCutout()), true, new Random());
            } else if (modelTextures.containsKey("Base_Layer")) {
                Identifier identifier = new Identifier(modelTextures.get("Base_Layer"));
                SpriteIdentifier texture = new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, identifier);
                VertexConsumer vertexConsumer = texture.getVertexConsumer(vertexConsumers, RenderLayer::getEntityCutout);

                parts.get("Base_Layer").render(matrices, vertexConsumer, light, overlay);
            }
        }

        matrices.pop();
    }
}