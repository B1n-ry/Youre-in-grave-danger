package com.b1n4ry.yigd.client;

import com.b1n4ry.yigd.client.render.GraveBlockEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.util.Identifier;

import static com.b1n4ry.yigd.Yigd.GRAVE_BLOCK_ENTITY;

public class YigdClient implements ClientModInitializer {
    public static final EntityModelLayer GRAVE_GROUND = new EntityModelLayer(new Identifier("yigd", "block/grave"), "main");

    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.INSTANCE.register(GRAVE_BLOCK_ENTITY, GraveBlockEntityRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(GRAVE_GROUND, GraveBlockEntityRenderer.GraveModel::getTexturedModelData);
    }
}
