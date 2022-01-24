package com.b1n4ry.yigd.client;

import com.b1n4ry.yigd.client.gui.GraveSelectScreen;
import com.b1n4ry.yigd.client.gui.PlayerSelectScreen;
import com.b1n4ry.yigd.client.render.GraveBlockEntityRenderer;
import com.b1n4ry.yigd.core.DeadPlayerData;
import com.b1n4ry.yigd.mixin.WorldRendererAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.*;

import static com.b1n4ry.yigd.Yigd.GRAVE_BLOCK_ENTITY;

public class YigdClient implements ClientModInitializer {
    public static boolean isRenderingGlowingShader = false;

    @Override
    public void onInitializeClient() {
        BlockEntityRendererRegistry.register(GRAVE_BLOCK_ENTITY, GraveBlockEntityRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(new Identifier("yigd", "single_dead_guy"), (client, handler, buf, responseSender) -> {
            if (client.player == null) return;

            int listSize = buf.readInt();
            List<DeadPlayerData> deadUserData = new ArrayList<>();
            for (int i = 0; i < listSize; i++) {
                NbtCompound nbtData = buf.readNbt();
                deadUserData.add(DeadPlayerData.fromNbt(nbtData));
            }

            client.execute(() -> {
                GraveSelectScreen screen = new GraveSelectScreen(deadUserData, 1, null);
                MinecraftClient.getInstance().setScreen(screen);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(new Identifier("yigd", "all_dead_people"), (client, handler, buf, responseSender) -> {
            if (client == null) return;

            int mapSize = buf.readInt();
            Map<UUID, List<DeadPlayerData>> data = new HashMap<>();
            for (int i = 0; i < mapSize; i++) {
                UUID uuid = buf.readUuid();
                int listSize = buf.readInt();
                List<DeadPlayerData> userData = new ArrayList<>();
                for (int n = 0; n < listSize; n++) {
                    NbtCompound nbt = buf.readNbt();
                    userData.add(DeadPlayerData.fromNbt(nbt));
                }
                data.put(uuid, userData);
            }

            client.execute(() -> {
                PlayerSelectScreen screen = new PlayerSelectScreen(data, 1);
                client.setScreen(screen);
            });
        });

        // Makes sure just before render is applied, that if any graves should glow they will be able to glow
        WorldRenderEvents.LAST.register(context -> {
            WorldRendererAccessor worldRenderer = (WorldRendererAccessor) context.worldRenderer();
            MinecraftClient client = MinecraftClient.getInstance();
            if (GraveBlockEntityRenderer.renderGraveGlowing && !isRenderingGlowingShader) { // Makes sure that glowing shader is not applied twice
                worldRenderer.getEntityOutlineShader().render(context.tickDelta());
                client.getFramebuffer().beginWrite(false);

                GraveBlockEntityRenderer.renderGraveGlowing = false;
            }
            isRenderingGlowingShader = false;
        });
    }
}