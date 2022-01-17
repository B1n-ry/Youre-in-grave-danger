package com.b1n4ry.yigd.mixin;

import com.b1n4ry.yigd.block.entity.GraveBlockEntity;
import com.b1n4ry.yigd.config.YigdConfig;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {
    @Shadow @Final private MinecraftServer server;

    @Redirect(method = "canPlayerModifyAt", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isSpawnProtected(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;)Z"))
    private boolean isSpawnProtected(MinecraftServer instance, ServerWorld world, BlockPos pos, PlayerEntity player) {
        boolean shouldBeSpawnProtected = server.isSpawnProtected(world, pos, player);
        if (!shouldBeSpawnProtected) return false;
        if (!YigdConfig.getConfig().graveSettings.ignoreSpawnProtection) return true;
        BlockEntity be = world.getBlockEntity(pos);
        return !(be instanceof GraveBlockEntity grave) || grave.getGraveOwner() != null;
    }
}
