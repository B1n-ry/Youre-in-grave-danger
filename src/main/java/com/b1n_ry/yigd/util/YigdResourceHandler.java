package com.b1n_ry.yigd.util;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.block.GraveBlock;
import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.data.GraveyardData;
import com.google.gson.*;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class YigdResourceHandler {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, (JsonDeserializer<ResourceLocation>) (elem, type, context) -> ResourceLocation.parse(elem.getAsString()))
            .registerTypeAdapter(Vec3i.class, (JsonDeserializer<Vec3i>) (elem, type, context) -> new Vec3i(
                    elem.getAsJsonArray().get(0).getAsInt(),
                    elem.getAsJsonArray().get(1).getAsInt(),
                    elem.getAsJsonArray().get(2).getAsInt()))
            .create();

    public static void serverDataEvent(AddReloadListenerEvent event) {
        event.addListener(new GraveServerModelLoader());
        event.addListener(new GraveyardDataLoader());
        event.addListener(new GraveAreaOverrideLoader());
    }
    public static void clientResourceEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new GraveResourceLoader());
    }

    public static class GraveResourceLoader implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "models/block/grave.json");
            List<Resource> resources = manager.getResourceStack(resourceLocation);

            for (Resource resource : resources) {
                try (InputStream is = resource.open()) {
                    Yigd.LOGGER.info("Reloading grave model (client)");
                    JsonObject resourceJson = (JsonObject) JsonParser.parseReader(new InputStreamReader(is));
                    GraveBlockEntityRenderer.reloadModelFromJson(resourceJson);
                    GraveBlock.reloadShapeFromJson(resourceJson);

                    Yigd.LOGGER.info("Grave model and shape reload successful (client)");
                }
                catch (IOException | ClassCastException | NullPointerException e) {
                    Yigd.LOGGER.error("Could not load resource `%s` from resource pack `%s`".formatted(resourceLocation, resource.sourcePackId()), e);
                }
            }
        }
    }
    private static class GraveServerModelLoader implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "custom/grave_shape.json");
            List<Resource> resources = manager.getResourceStack(resourceLocation);

            for (Resource resource : resources) {
                try (InputStream is = resource.open()) {
                    Yigd.LOGGER.info("Reloading grave shape (server)");
                    JsonObject resourceJson = (JsonObject) JsonParser.parseReader(new InputStreamReader(is));
                    GraveBlock.reloadShapeFromJson(resourceJson);

                    Yigd.LOGGER.info("Grave model and shape reload successful (server)");
                }
                catch (IOException | ClassCastException | NullPointerException e) {
                    Yigd.LOGGER.error("Could not load resource `%s` from datapack `%s`".formatted(resourceLocation, resource.sourcePackId()), e);
                }
            }
        }
    }
    private static class GraveyardDataLoader implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "custom/graveyard.json");
            List<Resource> resources = manager.getResourceStack(resourceLocation);

            for (Resource resource : resources) {
                try (InputStream is = resource.open()) {
                    Yigd.LOGGER.info("Reloading YIGD graveyard data (server)");
                    GraveComponent.graveyardData = GSON.fromJson(new InputStreamReader(is), GraveyardData.class);
                    GraveComponent.graveyardData.handlePoint2Point();

                    Yigd.LOGGER.info("Graveyard data successfully reloaded (server)");
                }
                catch (IOException | ClassCastException | NullPointerException e) {
                    Yigd.LOGGER.error("Could not load resource `%s` from datapack `%s`".formatted(resourceLocation, resource.sourcePackId()), e);
                }
            }
        }
    }
    private static class GraveAreaOverrideLoader implements ResourceManagerReloadListener {
        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(Yigd.MOD_ID, "custom/grave_areas.json");
            List<Resource> resources = manager.getResourceStack(resourceLocation);

            for (Resource resource : resources) {
                try (InputStream is = resource.open()) {
                    Yigd.LOGGER.info("Reloading YIGD grave area overrides (server)");
                    GraveOverrideAreas.INSTANCE = GSON.fromJson(new InputStreamReader(is), GraveOverrideAreas.class);

                    Yigd.LOGGER.info("Grave area overrides successfully reloaded (server)");
                }
                catch (IOException | ClassCastException | NullPointerException e) {
                    Yigd.LOGGER.error("Could not load resource `%s` from datapack `%s`".formatted(resourceLocation, resource.sourcePackId()), e);
                }
            }
        }
    }
}
