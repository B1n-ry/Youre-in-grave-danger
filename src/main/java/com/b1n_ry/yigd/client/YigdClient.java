package com.b1n_ry.yigd.client;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.client.render.GraveBlockEntityRenderer;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.events.YigdClientEventHandler;
import com.b1n_ry.yigd.networking.packets.UpdateConfigC2SPacket;
import com.b1n_ry.yigd.util.YigdResourceHandler;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(value = Yigd.MOD_ID, dist = Dist.CLIENT)
public class YigdClient {
    public YigdClient(IEventBus modBus, ModContainer modContainer) {
        modBus.addListener(this::clientModInitializer);

        modBus.addListener(YigdResourceHandler::clientResourceEvent);
        NeoForge.EVENT_BUS.register(new YigdClientEventHandler());

        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, screen) -> AutoConfig.getConfigScreen(YigdConfig.class, screen).get());

        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, event -> {
            YigdConfig.GraveConfig graveConfig = YigdConfig.getConfig().graveConfig;
            PacketDistributor.sendToServer(new UpdateConfigC2SPacket(graveConfig.claimPriority, graveConfig.graveRobbing.robPriority));
        });
    }

    public void clientModInitializer(FMLClientSetupEvent event) {
        BlockEntityRenderers.register(Yigd.GRAVE_BLOCK_ENTITY.get(), GraveBlockEntityRenderer::new);
    }
}
