package com.b1n_ry.yigd.util;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.block.GraveBlock;
import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.data.GraveyardData;
import com.google.gson.*;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3i;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class YigdResourceHandler {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Identifier.class, (JsonDeserializer<Identifier>) (elem, type, context) -> new Identifier(elem.getAsString()))
            .registerTypeAdapter(Vec3i.class, (JsonDeserializer<Vec3i>) (elem, type, context) -> new Vec3i(
                    elem.getAsJsonArray().get(0).getAsInt(),
                    elem.getAsJsonArray().get(1).getAsInt(),
                    elem.getAsJsonArray().get(2).getAsInt()))
            .create();

    public static void init() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new GraveResourceLoader());
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new GraveServerModelLoader());
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new GraveyardDataLoader());
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new GraveAreaOverrideLoader());
    }

    private static class GraveResourceLoader implements SimpleSynchronousResourceReloadListener {
        @Override
        public Identifier getFabricId() {
            return new Identifier(Yigd.MOD_ID, "custom_grave_model");
        }

        @Override
        public void reload(ResourceManager manager) {
            Identifier resourceLocation = new Identifier(Yigd.MOD_ID, "models/block/grave.json");
            List<Resource> resources = manager.getAllResources(resourceLocation);

            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    Yigd.LOGGER.info("Reloading grave model (client)");
                    JsonObject resourceJson = (JsonObject) JsonParser.parseReader(new InputStreamReader(is));
                    GraveBlockEntityRenderer.reloadModelFromJson(resourceJson);
                    GraveBlock.reloadShapeFromJson(resourceJson);

                    Yigd.LOGGER.info("Grave model and shape reload successful (client)");
                }
                catch (IOException | ClassCastException | NullPointerException e) {
                    Yigd.LOGGER.error("Could not load resource `%s` from resource pack `%s`".formatted(resourceLocation, resource.getPackId()), e);
                }
            }
        }
    }
    private static class GraveServerModelLoader implements SimpleSynchronousResourceReloadListener {
        @Override
        public Identifier getFabricId() {
            return new Identifier(Yigd.MOD_ID, "custom_server_grave_shape");
        }

        @Override
        public void reload(ResourceManager manager) {
            Identifier resourceLocation = new Identifier(Yigd.MOD_ID, "custom/grave_shape.json");
            List<Resource> resources = manager.getAllResources(resourceLocation);

            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    Yigd.LOGGER.info("Reloading grave shape (server)");
                    JsonObject resourceJson = (JsonObject) JsonParser.parseReader(new InputStreamReader(is));
                    GraveBlock.reloadShapeFromJson(resourceJson);

                    Yigd.LOGGER.info("Grave model and shape reload successful (server)");
                }
                catch (IOException | ClassCastException | NullPointerException e) {
                    Yigd.LOGGER.error("Could not load resource `%s` from datapack `%s`".formatted(resourceLocation, resource.getPackId()), e);
                }
            }
        }
    }
    private static class GraveyardDataLoader implements SimpleSynchronousResourceReloadListener {
        @Override
        public Identifier getFabricId() {
            return new Identifier(Yigd.MOD_ID, "graveyard");
        }

        @Override
        public void reload(ResourceManager manager) {
            Identifier resourceLocation = new Identifier(Yigd.MOD_ID, "custom/graveyard.json");
            List<Resource> resources = manager.getAllResources(resourceLocation);

            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    Yigd.LOGGER.info("Reloading YIGD graveyard data (server)");
                    GraveComponent.graveyardData = GSON.fromJson(new InputStreamReader(is), GraveyardData.class);
                    GraveComponent.graveyardData.handlePoint2Point();

                    Yigd.LOGGER.info("Graveyard data successfully reloaded (server)");
                }
                catch (IOException | ClassCastException | NullPointerException e) {
                    Yigd.LOGGER.error("Could not load resource `%s` from datapack `%s`".formatted(resourceLocation, resource.getPackId()), e);
                }
            }
        }
    }
    private static class GraveAreaOverrideLoader implements SimpleSynchronousResourceReloadListener {
        @Override
        public Identifier getFabricId() {
            return new Identifier(Yigd.MOD_ID, "grave_area_override");
        }

        @Override
        public void reload(ResourceManager manager) {
            Identifier resourceLocation = new Identifier(Yigd.MOD_ID, "custom/grave_areas.json");
            List<Resource> resources = manager.getAllResources(resourceLocation);

            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    Yigd.LOGGER.info("Reloading YIGD grave area overrides (server)");
                    GraveOverrideAreas.INSTANCE = GSON.fromJson(new InputStreamReader(is), GraveOverrideAreas.class);

                    Yigd.LOGGER.info("Grave area overrides successfully reloaded (server)");
                }
                catch (IOException | ClassCastException | NullPointerException e) {
                    Yigd.LOGGER.error("Could not load resource `%s` from datapack `%s`".formatted(resourceLocation, resource.getPackId()), e);
                }
            }
        }
    }
}
