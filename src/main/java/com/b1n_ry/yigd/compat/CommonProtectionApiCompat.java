package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.events.AllowBlockUnderGraveGenerationEvent;
import com.b1n_ry.yigd.events.AllowGraveGenerationEvent;
import eu.pb4.common.protection.impl.ProtectionImpl;

public class CommonProtectionApiCompat {
    public static void init() {
        AllowBlockUnderGraveGenerationEvent.EVENT.register(
                (grave, currentUnder) -> YigdConfig.getConfig().graveConfig.blockUnderGrave.generateOnProtectedLand || !ProtectionImpl.isProtected(grave.getWorld(), grave.getPos().down()));
        // TODO: Change this to check a config too
        AllowGraveGenerationEvent.EVENT.register((context, grave) -> !ProtectionImpl.isProtected(context.getWorld(), grave.getPos()));
    }
}
