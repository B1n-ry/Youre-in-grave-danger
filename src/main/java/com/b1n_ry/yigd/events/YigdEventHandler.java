package com.b1n_ry.yigd.events;

public class YigdEventHandler {
    public static void registerEventCallbacks() {
        GraveClaimEvent.EVENT.register((player, world, pos, grave, tool) -> player.getUuid().equals(grave.getOwner().getId()));
    }
}
