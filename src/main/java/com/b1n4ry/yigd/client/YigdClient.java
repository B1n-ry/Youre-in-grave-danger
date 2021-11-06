package com.b1n4ry.yigd.client;

import com.b1n4ry.yigd.client.render.GraveBlockEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;

import static com.b1n4ry.yigd.Yigd.GRAVE_BLOCK_ENTITY;

public class YigdClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.INSTANCE.register(GRAVE_BLOCK_ENTITY, GraveBlockEntityRenderer::new);
    }
}
