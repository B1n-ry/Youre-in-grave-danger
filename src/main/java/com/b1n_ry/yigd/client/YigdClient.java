package com.b1n_ry.yigd.client;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.client.gui.ConfigWarningScreen;
import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import com.b1n_ry.yigd.config.PriorityInventoryConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.ClientPacketReceivers;
import com.b1n_ry.yigd.core.PacketIdentifiers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.network.PacketByteBuf;

import static com.b1n_ry.yigd.Yigd.GRAVE_BLOCK_ENTITY;

public class YigdClient implements ClientModInitializer {
    public static PriorityInventoryConfig normalPriority = PriorityInventoryConfig.GRAVE;
    public static PriorityInventoryConfig robbingPriority = PriorityInventoryConfig.INVENTORY;

    @Override
    public void onInitializeClient() {
        BlockEntityRendererFactories.register(GRAVE_BLOCK_ENTITY, GraveBlockEntityRenderer::new);

        ClientPacketReceivers.register();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            if (Yigd.defaultConfig != null)
            client.setScreen(new ConfigWarningScreen(client.currentScreen));
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            YigdConfig config = YigdConfig.getConfig();
            PriorityInventoryConfig normal = config.graveSettings.priority;
            PriorityInventoryConfig robbing = config.graveSettings.graveRobbing.robPriority;

            PacketByteBuf buf = PacketByteBufs.create()
                    .writeEnumConstant(normal)
                    .writeEnumConstant(robbing);
            try {
                ClientPlayNetworking.send(PacketIdentifiers.CONFIG_UPDATE, buf);

                normalPriority = normal;
                robbingPriority = robbing;
            }
            catch (IllegalStateException e) {
                Yigd.LOGGER.warn("Tried to sync client config, but didn't find a server to sync to");
            }
        });
    }
}