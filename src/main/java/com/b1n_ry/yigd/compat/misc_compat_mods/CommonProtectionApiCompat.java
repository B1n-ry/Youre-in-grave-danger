package com.b1n_ry.yigd.compat.misc_compat_mods;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.events.AllowBlockUnderGraveGenerationEvent;
import com.b1n_ry.yigd.events.AllowGraveGenerationEvent;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.util.GraveOverrideAreas;
import eu.pb4.common.protection.impl.ProtectionImpl;
import net.minecraft.util.math.BlockPos;

public class CommonProtectionApiCompat {
    public static void init() {
        AllowBlockUnderGraveGenerationEvent.EVENT.register((grave, currentUnder) -> {
            if (ProtectionImpl.canPlaceBlock(grave.getWorld(), grave.getPos().down(), grave.getOwner(), null)) {
                return YigdConfig.getConfig().graveConfig.blockUnderGrave.generateInOwnClaim;
            } else {
                return YigdConfig.getConfig().graveConfig.blockUnderGrave.generateOnProtectedLand;
            }
        });

        AllowGraveGenerationEvent.EVENT.register((context, grave) -> {
            if (ProtectionImpl.canPlaceBlock(context.world(), grave.getPos(), grave.getOwner(), context.player())) {
                return YigdConfig.getConfig().compatConfig.standardDropRuleInOwnClaim == DropRule.PUT_IN_GRAVE;
            } else {
                return YigdConfig.getConfig().compatConfig.standardDropRuleInClaim == DropRule.PUT_IN_GRAVE;
            }
        });

        DropRuleEvent.EVENT.register((item, slot, context, modify) -> {
            if (context == null || !modify) return GraveOverrideAreas.INSTANCE.defaultDropRule;

            var player = context.player();
            if (ProtectionImpl.canPlaceBlock(context.world(), BlockPos.ofFloored(context.deathPos()), player.getGameProfile(), player)) {
                return YigdConfig.getConfig().compatConfig.standardDropRuleInOwnClaim;
            } else {
                return YigdConfig.getConfig().compatConfig.standardDropRuleInClaim;
            }
        });
    }
}
