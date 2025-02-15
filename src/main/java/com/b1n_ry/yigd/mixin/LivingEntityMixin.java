package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.DeathHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, priority = 500)
public class LivingEntityMixin {
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void drop(ServerLevel world, DamageSource damageSource, CallbackInfo ci) {
        LivingEntity e = (LivingEntity) (Object) this;

        if (!(e instanceof ServerPlayer player)) return;

        if (player.isSpectator()) return;  // Spectators don't generate graves

        if (world.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;  // KeepInv should be handled by vanilla. No need to complicate things

        DeathHandler deathHandler = new DeathHandler();
        deathHandler.onPlayerDeath(player, world, player.position(), damageSource);
    }
}
