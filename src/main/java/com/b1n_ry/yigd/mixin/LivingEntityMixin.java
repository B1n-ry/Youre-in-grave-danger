package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.DeathHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Redirect(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropInventory()V"))
    private void onDeath(LivingEntity entity, DamageSource damageSource) {
        if (!entity.isDead()) return;  // In case some mod made you alive again

        if (entity instanceof ServerPlayerEntity player) {
            DeathHandler deathHandler = new DeathHandler();
            deathHandler.onPlayerDeath(player, player.getServerWorld(), player.getPos(), damageSource);
        }
    }
}
