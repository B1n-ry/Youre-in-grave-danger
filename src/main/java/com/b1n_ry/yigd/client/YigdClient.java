package com.b1n_ry.yigd.client;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.events.YigdClientEventHandler;
import com.b1n_ry.yigd.networking.ClientPacketHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class YigdClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererFactories.register(Yigd.GRAVE_BLOCK_ENTITY, GraveBlockEntityRenderer::new);

        ClientPacketHandler.registerReceivers();
        YigdClientEventHandler.registerEventCallbacks();

        // There is no event handler for standard minecraft client events, so this is used here
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ClientPacketHandler.sendConfigUpdate(YigdConfig.getConfig()));
    }
}
