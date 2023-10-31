package com.b1n_ry.yigd.util;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.server.command.ServerCommandSource;

public class ListModeArgumentType extends EnumArgumentType<ListMode> {
    protected ListModeArgumentType() {
        super(ListMode.CODEC, ListMode::values);
    }

    public static ListModeArgumentType listMode() {
        return new ListModeArgumentType();
    }
    public static ListMode getListMode(CommandContext<ServerCommandSource> context, String id) {
        return context.getArgument(id, ListMode.class);
    }
}
