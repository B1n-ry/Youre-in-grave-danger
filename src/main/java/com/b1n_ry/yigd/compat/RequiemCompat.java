package com.b1n_ry.yigd.compat;

import com.mojang.authlib.GameProfile;
import ladysnake.requiem.api.v1.possession.Possessable;
import ladysnake.requiem.common.entity.PlayerShellEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

public class RequiemCompat {
    public static boolean isPlayerShellEntity(PlayerEntity player) {
        return player instanceof PlayerShellEntity;
    }

    public static UUID getDisplayId(PlayerEntity player) {
        GameProfile shellProfile = ((PlayerShellEntity) player).getDisplayProfile();
        if (shellProfile == null) return player.getUuid();
        return shellProfile.getId();
    }

    public static GameProfile getDisplayProfile(PlayerEntity player) {
        return ((PlayerShellEntity) player).getDisplayProfile();
    }

    public static boolean isPossessedByPlayer(LivingEntity entity) {
        return entity instanceof Possessable possessable && possessable.isBeingPossessed();
    }

    public static PlayerEntity getPossessor(LivingEntity entity) {
        return entity instanceof Possessable possessable ? possessable.getPossessor() : null;
    }
}
