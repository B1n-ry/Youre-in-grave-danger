package com.b1n_ry.yigd.client;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import com.b1n_ry.yigd.events.YigdClientEventHandler;
import com.b1n_ry.yigd.packets.ClientPacketHandler;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;

public class YigdClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRendererFactories.register(Yigd.GRAVE_BLOCK_ENTITY, GraveBlockEntityRenderer::new);

        ClientPacketHandler.registerReceivers();
        YigdClientEventHandler.registerEventCallbacks();
    }
}
