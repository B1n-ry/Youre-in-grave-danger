package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.DeathHandler;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.impl.ServerPlayerEntityImpl;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements ServerPlayerEntityImpl {
    @Unique
    private Vec3d youre_in_grave_danger$lastGroundPos = Vec3d.ZERO;  // Initial value. WILL change as soon as game ticks once

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void updateGroundPos(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        if (!player.isOnGround()) return;

        this.youre_in_grave_danger$lastGroundPos = player.getPos();
    }

    @Inject(method = "onDeath", at = @At(value = "HEAD"))
    private void onDeath(DamageSource damageSource, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        if (!player.isDead()) return;  // If some weird shit happens, this is a failsafe
        if (player.isSpectator()) return;  // Spectators don't generate graves

        DeathHandler deathHandler = new DeathHandler();
        deathHandler.onPlayerDeath(player, player.getServerWorld(), player.getPos(), damageSource);
    }

    @Redirect(method = "createEndSpawnPlatform", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private boolean createEndSpawnPlatform(ServerWorld world, BlockPos blockPos, BlockState blockState) {
        if (world.getBlockEntity(blockPos) instanceof GraveBlockEntity grave) {
            GraveComponent component = grave.getComponent();
            if (component != null)
                return false;
        }
        return world.setBlockState(blockPos, blockState);
    }

    @Override
    public Vec3d youre_in_grave_danger$getLastGroundPos() {
        return this.youre_in_grave_danger$lastGroundPos;
    }
}
