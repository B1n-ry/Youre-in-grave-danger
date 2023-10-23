package com.b1n_ry.yigd.util;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new GraveResourceLoader());
    }

    @Environment(EnvType.CLIENT)
    private static class GraveResourceLoader implements SimpleSynchronousResourceReloadListener {
        @Override
        public Identifier getFabricId() {
            return new Identifier(Yigd.MOD_ID, "custom_grave_model");
        }

        @Override
        public void reload(ResourceManager manager) {
            List<Resource> resources = manager.getAllResources(new Identifier(Yigd.MOD_ID, "models/block/grave.json"));

            for (Resource resource : resources) {
                try (InputStream is = resource.getInputStream()) {
                    Yigd.LOGGER.info("Reloading grave model (client)");
                    JsonObject resourceJson = (JsonObject) JsonParser.parseReader(new InputStreamReader(is));
                    GraveBlockEntityRenderer.loadModelFromJson(resourceJson);
                }
                catch (IOException | ClassCastException | NullPointerException e) {
                    Yigd.LOGGER.error("Could not load resource %s from resource pack `%s`".formatted(this.getFabricId(), resource.getResourcePackName()), e);
                }
            }
        }
    }
}
