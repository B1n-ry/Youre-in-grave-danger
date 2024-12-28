package com.b1n_ry.yigd.compat.misc_compat_mods;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.events.AllowBlockUnderGraveGenerationEvent;
import com.b1n_ry.yigd.events.AllowGraveGenerationEvent;
import com.b1n_ry.yigd.events.DropRuleEvent;
import com.b1n_ry.yigd.util.DropRule;
import com.b1n_ry.yigd.util.GraveOverrideAreas;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.common.protection.impl.ProtectionImpl;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class CommonProtectionApiCompat {
    public static void init() {
        AllowBlockUnderGraveGenerationEvent.EVENT.register((grave, currentUnder) -> {
            if (CommonProtection.canPlaceBlock(grave.getWorld(), grave.getPos().down(), grave.getOwner().gameProfile(), null)) {
                return YigdConfig.getConfig().graveConfig.blockUnderGrave.generateInOwnClaim;
            } else {
                return YigdConfig.getConfig().graveConfig.blockUnderGrave.generateOnProtectedLand;
            }
        });

        AllowGraveGenerationEvent.EVENT.register((context, grave) -> {
            if (CommonProtection.canPlaceBlock(context.world(), grave.getPos(), grave.getOwner().gameProfile(), context.player())) {
                return YigdConfig.getConfig().compatConfig.standardDropRuleInOwnClaim == DropRule.PUT_IN_GRAVE;
            } else {
                return YigdConfig.getConfig().compatConfig.standardDropRuleInClaim == DropRule.PUT_IN_GRAVE;
            }

            return true;
        });

        DropRuleEvent.EVENT.register((item, slot, context, modify) -> {
            if (context == null || !modify) return GraveOverrideAreas.INSTANCE.defaultDropRule;

            if (ProtectionImpl.isProtected(context.world(), BlockPos.ofFloored(context.deathPos())))
                return YigdConfig.getConfig().compatConfig.standardDropRuleInClaim;

            ServerPlayerEntity player = context.player();
            if (CommonProtection.canPlaceBlock(context.world(), BlockPos.ofFloored(context.deathPos()), player.getGameProfile(), player)) {
                return YigdConfig.getConfig().compatConfig.standardDropRuleInOwnClaim;
            } else {
                return YigdConfig.getConfig().compatConfig.standardDropRuleInClaim;
            }
        });
    }
}
