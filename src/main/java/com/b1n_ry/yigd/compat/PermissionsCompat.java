package com.b1n_ry.yigd.compat;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.command.ServerCommandSource;

public class PermissionsCompat {
    public static boolean moderatePermission(PlayerEntity player) {
        return Permissions.check(player, "yigd.command.moderate", 4);
    }
    public static boolean moderatePermission(ServerCommandSource source) {
        return Permissions.check(source, "yigd.command.moderate", 4);
    }

    public static boolean robPermission(PlayerEntity player) {
        return Permissions.check(player, "yigd.command.rob", 4);
    }
    public static boolean robPermission(ServerCommandSource source) {
        return Permissions.check(source, "yigd.command.rob", 4);
    }

    public static boolean restorePermission(PlayerEntity player) {
        return Permissions.check(player, "yigd.command.restore", 4);
    }
    public static boolean restorePermission(ServerCommandSource source) {
        return Permissions.check(source, "yigd.command.restore", 4);
    }

    public static boolean viewPermission(PlayerEntity player) {
        return Permissions.check(player, "yigd.command.view", 4);
    }
    public static boolean viewPermission(ServerCommandSource source) {
        return Permissions.check(source, "yigd.command.view", 4);
    }

    public static boolean clearPermission(PlayerEntity player) {
        return Permissions.check(player, "yigd.command.clear", 4);
    }
    public static boolean clearPermission(ServerCommandSource source) {
        return Permissions.check(source, "yigd.command.clear", 4);
    }

    public static boolean whitelistPermission(PlayerEntity player) {
        return Permissions.check(player, "yigd.command.whitelist", 4);
    }
    public static boolean whitelistPermission(ServerCommandSource source) {
        return Permissions.check(source, "yigd.command.whitelist", 4);
    }

    public static boolean deletePermission(PlayerEntity player) {
        return Permissions.check(player, "yigd.command.delete", 4);
    }
}
