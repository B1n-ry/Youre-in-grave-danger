package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.YigdConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public class DedicatedServerMixin {
    @Inject(method = "isUnderSpawnProtection", at = @At(value = "RETURN"), cancellable = true)
    private void isSpawnProtected(ServerLevel world, BlockPos pos, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || !YigdConfig.getConfig().graveConfig.overrideSpawnProtection) return;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof GraveBlockEntity) cir.setReturnValue(false);
    }
}
