package com.b1n4ry.yigd.core;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.config.YigdConfig;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class YigdCommand {
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            if (YigdConfig.getConfig().commandToggles.retrieveGrave) {
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
            }
        });
    }

    private static int retrieveGrave(PlayerEntity player) {
        UUID userId = player.getUuid();

        BlockPos gravePos = Yigd.deadPlayerData.getDeathPos(userId);
        if (player.world.getBlockState(gravePos).getBlock() != Yigd.GRAVE_BLOCK) {
            player.sendMessage(Text.of("Please go to the dimension you died in to retrieve your items"), true);
            return -1;
        }

        if (!Yigd.deadPlayerData.hasStoredInventory(userId)) {
            player.sendMessage(Text.of("Could not find grave to fetch"), true);
            return -1;
        }

        DefaultedList<ItemStack> items = Yigd.deadPlayerData.getDeathPlayerInventory(userId);

        int xp = Yigd.deadPlayerData.getDeathXp(userId);
        List<Object> modInventories = Yigd.deadPlayerData.getModdedInventories(userId);
        Map<String, Object> modInv = new HashMap<>();
        for (int i = 0; i < Yigd.apiMods.size(); i++) {
            YigdApi yigdApi = Yigd.apiMods.get(i);
            modInv.put(yigdApi.getModName(), modInventories.get(i));
        }

        GraveHelper.RetrieveItems(player, items, modInv, xp, false);

        player.world.removeBlock(gravePos, false);
        if (!player.world.getBlockState(gravePos).isAir() && gravePos != null) {
            player.world.removeBlock(gravePos, false);
            if (YigdConfig.getConfig().graveSettings.dropGraveBlock) {
                ItemScatterer.spawn(player.world, gravePos.getX(), gravePos.getY(), gravePos.getZ(), Yigd.GRAVE_BLOCK.asItem().getDefaultStack());
            }
        }
        player.sendMessage(Text.of("Retrieved grave remotely successfully"), true);

        return 1;
    }
}
