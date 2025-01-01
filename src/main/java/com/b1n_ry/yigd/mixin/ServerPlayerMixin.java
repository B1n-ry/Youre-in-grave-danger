package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.impl.ServerPlayerEntityImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements ServerPlayerEntityImpl {
    @Unique
    private Vec3 youre_in_grave_danger$lastGroundPos = Vec3.ZERO;  // Initial value. WILL change as soon as game ticks once

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void updateGroundPos(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!player.onGround()) return;

        this.youre_in_grave_danger$lastGroundPos = player.position();
    }

    @Override
    public Vec3 youre_in_grave_danger$getLastGroundPos() {
        return this.youre_in_grave_danger$lastGroundPos;
    }
}
