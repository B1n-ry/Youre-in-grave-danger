package com.b1n_ry.yigd.data;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class DeathContext {
    private final ServerPlayerEntity player;
    @NotNull
    private final ServerWorld world;
    private final Vec3d deathPos;
    private final DamageSource deathSource;

    public DeathContext(ServerPlayerEntity player, @NotNull ServerWorld world, Vec3d deathPos, DamageSource deathSource) {
        this.player = player;
        this.deathSource = deathSource;
        this.world = world;
        this.deathPos = deathPos;
    }

    public ServerPlayerEntity getPlayer() {
        return this.player;
    }
    public @NotNull ServerWorld getWorld() {
        return this.world;
    }
    public Vec3d getDeathPos() {
        return this.deathPos;
    }
    public DamageSource getDeathSource() {
        return this.deathSource;
    }
}
