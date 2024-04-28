package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.DeathHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, priority = 500)
public class LivingEntityMixin {
    @Inject(method = "drop", at = @At("HEAD"))
    private void drop(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity e = (LivingEntity) (Object) this;

        if (!(e instanceof ServerPlayerEntity player)) return;

        ServerWorld world = player.getServerWorld();

        if (!player.isDead()) return;  // If some weird shit happens, this is a failsafe
        if (player.isSpectator()) return;  // Spectators don't generate graves

        if (world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return;  // KeepInv should be handled by vanilla. No need to complicate things

        DeathHandler deathHandler = new DeathHandler();
        deathHandler.onPlayerDeath(player, world, player.getPos(), damageSource);
    }
}
