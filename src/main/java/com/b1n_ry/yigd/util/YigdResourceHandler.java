package com.b1n_ry.yigd.util;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.block.GraveBlock;
import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class YigdResourceHandler {
    public static void init() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new GraveResourceLoader());
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new GraveServerModelLoader());
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
                    Yigd.LOGGER.error("Could not load resource `%s` from resource pack `%s`".formatted(resourceLocation, resource.getResourcePackName()), e);
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
                    Yigd.LOGGER.error("Could not load resource `%s` from resource pack `%s`".formatted(resourceLocation, resource.getResourcePackName()), e);
                }
            }
        }
    }
}
