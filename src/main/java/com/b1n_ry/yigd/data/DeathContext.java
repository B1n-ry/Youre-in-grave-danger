package com.b1n_ry.yigd.data;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public record DeathContext(ServerPlayerEntity player, @NotNull ServerWorld world, Vec3d deathPos,
                           DamageSource deathSource) {
}
