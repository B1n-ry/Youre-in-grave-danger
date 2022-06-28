package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.ClaimModsApi;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.config.DeathEffectConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.*;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.*;

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
