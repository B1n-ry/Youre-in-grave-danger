package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.events.AllowBlockUnderGraveGenerationEvent;
import com.b1n_ry.yigd.events.AllowGraveGenerationEvent;
import eu.pb4.common.protection.impl.ProtectionImpl;

public class CommonProtectionApiCompat {
    public static void init() {
        // TODO: Change this to check a config too
        AllowBlockUnderGraveGenerationEvent.EVENT.register((grave, currentUnder) -> !ProtectionImpl.isProtected(grave.getWorld(), grave.getPos().down()));
        AllowGraveGenerationEvent.EVENT.register((context, grave) -> !ProtectionImpl.isProtected(context.getWorld(), grave.getPos()));
    }
}
