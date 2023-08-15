package com.b1n_ry.yigd.data;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class DeathContext {
    private final PlayerEntity player;
    @NotNull
    private final World world;
    private final Vec3d deathPos;
    private final DamageSource deathSource;

    public DeathContext(PlayerEntity player, @NotNull World world, Vec3d deathPos, DamageSource deathSource) {
        this.player = player;
        this.deathSource = deathSource;
        this.world = world;
        this.deathPos = deathPos;
    }

    public PlayerEntity getPlayer() {
        return this.player;
    }
    public @NotNull World getWorld() {
        return this.world;
    }
    public Vec3d getDeathPos() {
        return this.deathPos;
    }
    public DamageSource getDeathSource() {
        return this.deathSource;
    }
}
