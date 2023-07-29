package com.b1n_ry.yigd.events;

import com.b1n_ry.yigd.components.GraveComponent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface GraveClaimEvent {
    Event<GraveClaimEvent> EVENT = EventFactory.createArrayBacked(GraveClaimEvent.class, graveClaimEvents -> (player, world, pos, grave, tool) -> {
        for (GraveClaimEvent claimEvent : graveClaimEvents) {
            if (claimEvent.canClaim(player, world, pos, grave, tool))
                return true;
        }
        return false;
    });

    boolean canClaim(ServerPlayerEntity player, ServerWorld world, BlockPos pos, GraveComponent grave, ItemStack tool);
}
