package com.b1n_ry.yigd.compat;

import baritone.api.fakeplayer.FakeServerPlayerEntity;
import com.mojang.authlib.GameProfile;
import ladysnake.requiem.api.v1.possession.Possessable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public class RequiemCompat {
    public static boolean isPlayerShellEntity(PlayerEntity player) {
        return player instanceof FakeServerPlayerEntity;
    }

    public static GameProfile getDisplayProfile(PlayerEntity player) {
        return ((FakeServerPlayerEntity) player).getDisplayProfile();
    }

    public static boolean isPossessedByPlayer(LivingEntity entity) {
        return entity instanceof Possessable possessable && possessable.isBeingPossessed();
    }

    public static PlayerEntity getPossessor(LivingEntity entity) {
        return entity instanceof Possessable possessable ? possessable.getPossessor() : null;
    }
}
