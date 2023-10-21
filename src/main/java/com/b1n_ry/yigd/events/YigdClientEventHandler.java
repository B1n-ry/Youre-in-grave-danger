package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.config.YigdConfig;
import com.mojang.authlib.GameProfile;

public class YigdClientEventHandler {
    public static void registerEventCallbacks() {
        RenderGlowingGraveEvent.EVENT.register((be, player) -> {
            YigdConfig.GraveRendering config = YigdConfig.getConfig().graveRendering;

            GameProfile graveOwner = be.getGraveOwner();

            boolean inRange = be.getPos().isWithinDistance(player.getPos(), config.glowingDistance);
            boolean isOwner = graveOwner != null && graveOwner.equals(player.getGameProfile());

            // TODO: insert more configs and checks (like enchantment)
            return isOwner && inRange;
        });
    }
}
