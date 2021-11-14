package com.b1n4ry.yigd.core;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import java.security.Permission;
import java.security.Permissions;
import java.util.UUID;

public class YigdCommand {
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(CommandManager.literal("retrieve_grave")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(context -> {
                        PlayerEntity player = EntityArgumentType.getPlayer(context, "player");

                        return retrieveGrave(player);
                    })
                )
                .executes(context -> {
                    PlayerEntity player = context.getSource().getPlayer();

                    return retrieveGrave(player);
                })
            );
        });
    }

    private static int retrieveGrave(PlayerEntity player) {
        UUID userId = player.getUuid();

        if (!DeadPlayerData.hasStoredInventory(userId)) {
            player.sendMessage(Text.of("Could not find grave to fetch"), true);
            return -1;
        }

        BlockPos gravePos = DeadPlayerData.getDeathPos(userId);
        DefaultedList<ItemStack> items = DeadPlayerData.getDeathPlayerInventory(userId);
        int xp = DeadPlayerData.getDeathXp(userId);

        GraveHelper.RetrieveItems(player, items, xp);

        player.world.removeBlock(gravePos, false);
        player.sendMessage(Text.of("Retrieved grave remotely successfully"), true);

        return 1;
    }
}
