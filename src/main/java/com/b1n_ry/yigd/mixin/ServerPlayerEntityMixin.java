package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.impl.ServerPlayerEntityImpl;
import net.minecraft.block.BlockState;
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

    @Override
    public Vec3d youre_in_grave_danger$getLastGroundPos() {
        return this.youre_in_grave_danger$lastGroundPos;
    }
}
