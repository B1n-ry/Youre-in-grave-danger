package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.YigdConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.SERVER)
@Mixin(MinecraftDedicatedServer.class)
public class MinecraftDedicatedServerMixin {
    @Inject(method = "isSpawnProtected", at = @At(value = "RETURN"), cancellable = true)
    private void isSpawnProtected(ServerWorld world, BlockPos pos, PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || !YigdConfig.getConfig().graveConfig.overrideSpawnProtection) return;

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof GraveBlockEntity) cir.setReturnValue(false);
    }
}
