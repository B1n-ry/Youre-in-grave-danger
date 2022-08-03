package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.GraveHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LivingEntity.class, priority = 9001)
public abstract class LivingEntityMixin {
    @Shadow protected abstract void dropInventory();

    @Shadow protected abstract void dropXp();

    @Redirect(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropInventory()V"))
    private void generateGrave(LivingEntity livingEntity, DamageSource source) {
        if (!(livingEntity instanceof PlayerEntity player) || livingEntity.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
            this.dropInventory();
            return;
        }

        Vec3d pos = player.getPos();
        World playerWorld = player.world;
        Yigd.NEXT_TICK.add(() -> {
            if (!YigdConfig.getConfig().debugConfig.createGraveBeforeDeathMessage) {
                GraveHelper.onDeath(player, playerWorld, pos, source);
            }

            this.dropInventory();
        });
    }

    @Redirect(method = "drop", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;dropXp()V"))
    private void overwriteXp(LivingEntity instance) {
        if (!(instance instanceof PlayerEntity)) this.dropXp();
    }
}
