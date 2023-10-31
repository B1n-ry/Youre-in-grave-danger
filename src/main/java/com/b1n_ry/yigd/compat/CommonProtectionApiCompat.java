package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.events.AllowBlockUnderGraveGenerationEvent;
import com.b1n_ry.yigd.events.AllowGraveGenerationEvent;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import eu.pb4.common.protection.impl.ProtectionImpl;
import net.minecraft.util.math.BlockPos;

public class CommonProtectionApiCompat {
    public static void init() {
        AllowBlockUnderGraveGenerationEvent.EVENT.register(
                (grave, currentUnder) -> YigdConfig.getConfig().graveConfig.blockUnderGrave.generateOnProtectedLand || !ProtectionImpl.isProtected(grave.getWorld(), grave.getPos().down()));

        AllowGraveGenerationEvent.EVENT.register((context, grave) -> {
            if (ProtectionImpl.isProtected(context.getWorld(), grave.getPos()))
                return YigdConfig.getConfig().compatConfig.standardDropRuleInClaim == DropRule.PUT_IN_GRAVE;

            return true;
        });

        DropRuleEvent.EVENT.register((item, slot, context) -> {
            if (context == null) return DropRule.PUT_IN_GRAVE;

            if (ProtectionImpl.isProtected(context.getWorld(), BlockPos.ofFloored(context.getDeathPos())))
                return YigdConfig.getConfig().compatConfig.standardDropRuleInClaim;

            return DropRule.PUT_IN_GRAVE;
        });
    }
}
