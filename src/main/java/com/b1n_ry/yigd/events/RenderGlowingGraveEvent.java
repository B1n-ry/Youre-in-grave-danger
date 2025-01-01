package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.player.LocalPlayer;

public interface RenderGlowingGraveEvent {
    Event<RenderGlowingGraveEvent> EVENT = EventFactory.createArrayBacked(RenderGlowingGraveEvent.class, events -> (be, player) -> {
        boolean allow = false;
        for (RenderGlowingGraveEvent event : events) {
            allow = allow || event.canRenderOutline(be, player);
        }
        return allow;
    });

    boolean canRenderOutline(GraveBlockEntity be, LocalPlayer player);
}
