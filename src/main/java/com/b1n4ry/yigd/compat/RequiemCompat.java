package com.b1n4ry.yigd.compat;

import com.mojang.authlib.GameProfile;
import ladysnake.requiem.common.entity.PlayerShellEntity;
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
}
