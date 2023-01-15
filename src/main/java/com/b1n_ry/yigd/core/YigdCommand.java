package com.b1n_ry.yigd.core;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.compat.PermissionsCompat;
import com.b1n_ry.yigd.config.YigdConfig;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class YigdCommand {
    public static void registerCommands() {
        YigdConfig.CommandToggles config = YigdConfig.getConfig().commandToggles;

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(literal(config.coreCommandName)
                .executes(ctx -> viewGrave(ctx.getSource().getPlayer(), ctx.getSource().getPlayer()))
                .then(literal("restore")
                        .requires(source -> hasPermission(source, "yigd.command.restore") && config.retrieveGrave)
                        .then(argument("player", EntityArgumentType.player())
                                .then(argument("x", IntegerArgumentType.integer())
                                        .then(argument("y", IntegerArgumentType.integer())
                                                .then(argument("z", IntegerArgumentType.integer())
                                                        .then(argument("dim", DimensionArgumentType.dimension())
                                                                .executes(ctx -> {
                                                                    ServerCommandSource src = ctx.getSource();
                                                                    Entity entity = src.getEntity();

                                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");

                                                                    int posX = IntegerArgumentType.getInteger(ctx, "x");
                                                                    int posY = IntegerArgumentType.getInteger(ctx, "y");
                                                                    int posZ = IntegerArgumentType.getInteger(ctx, "z");
                                                                    Identifier worldId = DimensionArgumentType.getDimensionArgument(ctx, "dim").getRegistryKey().getValue();
                                                                    BlockPos pos = new BlockPos(posX, posY, posZ);

                                                                    UUID graveId = posToId(pos, worldId, player.getUuid());

                                                                    if (entity instanceof ServerPlayerEntity commandUser) {
                                                                        return restoreGrave(player, commandUser, graveId);
                                                                    } else {
                                                                        return restoreGrave(player, null, graveId);
                                                                    }
                                                                })
                                                        )
                                                        .executes(ctx -> {
                                                            ServerCommandSource src = ctx.getSource();
                                                            Entity entity = src.getEntity();

                                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");

                                                            int posX = IntegerArgumentType.getInteger(ctx, "x");
                                                            int posY = IntegerArgumentType.getInteger(ctx, "y");
                                                            int posZ = IntegerArgumentType.getInteger(ctx, "z");
                                                            Identifier worldId = player.world.getRegistryKey().getValue();
                                                            BlockPos pos = new BlockPos(posX, posY, posZ);

                                                            UUID graveId = posToId(pos, worldId, player.getUuid());

                                                            if (entity instanceof ServerPlayerEntity commandUser) {
                                                                return restoreGrave(player, commandUser, graveId);
                                                            } else {
                                                                return restoreGrave(player, null, graveId);
                                                            }
                                                        })
                                                )
                                        )
                                )
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();
                                    Entity entity = src.getEntity();
                                    if (entity instanceof PlayerEntity commandUser) {
                                        return restoreGrave(EntityArgumentType.getPlayer(ctx, "player"), commandUser, null);
                                    } else {
                                        return restoreGrave(EntityArgumentType.getPlayer(ctx, "player"), null, null);
                                    }
                                })
                        )
                        .executes(ctx -> restoreGrave(ctx.getSource().getPlayer(), ctx.getSource().getPlayer(), null))
                )
                .then(literal("rob")
                        .requires(source -> hasPermission(source, "yigd.command.rob") && config.robGrave)
                        .then(argument("victim", EntityArgumentType.player())
                                .then(argument("x", IntegerArgumentType.integer())
                                        .then(argument("y", IntegerArgumentType.integer())
                                                .then(argument("z", IntegerArgumentType.integer())
                                                        .then(argument("dim", DimensionArgumentType.dimension())
                                                                .executes(ctx -> {
                                                                    ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "victim");

                                                                    int posX = IntegerArgumentType.getInteger(ctx, "x");
                                                                    int posY = IntegerArgumentType.getInteger(ctx, "y");
                                                                    int posZ = IntegerArgumentType.getInteger(ctx, "z");
                                                                    Identifier worldId = DimensionArgumentType.getDimensionArgument(ctx, "dim").getRegistryKey().getValue();
                                                                    BlockPos pos = new BlockPos(posX, posY, posZ);

                                                                    UUID graveId = posToId(pos, worldId, player.getUuid());

                                                                    return robGrave(player.getGameProfile(), ctx.getSource().getPlayer(), graveId);
                                                                })
                                                        )
                                                        .executes(ctx -> {
                                                            ServerPlayerEntity commandUser = ctx.getSource().getPlayer();
                                                            if (commandUser == null) return -1;

                                                            ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "victim");

                                                            int posX = IntegerArgumentType.getInteger(ctx, "x");
                                                            int posY = IntegerArgumentType.getInteger(ctx, "y");
                                                            int posZ = IntegerArgumentType.getInteger(ctx, "z");
                                                            Identifier worldId = commandUser.world.getRegistryKey().getValue();
                                                            BlockPos pos = new BlockPos(posX, posY, posZ);

                                                            UUID graveId = posToId(pos, worldId, player.getUuid());

                                                            return robGrave(player.getGameProfile(), commandUser, graveId);
                                                        })
                                                )
                                        )
                                )
                                .executes(ctx -> robGrave(EntityArgumentType.getPlayer(ctx, "victim").getGameProfile(), ctx.getSource().getPlayer(), null))
                        )
                )
                .then(literal("grave")
                        .requires(source -> config.selfView || hasPermission(source, "yigd.command.view"))
                        .executes(ctx -> viewGrave(ctx.getSource().getPlayer(), ctx.getSource().getPlayer()))
                        .then(argument("player", EntityArgumentType.player())
                                .requires(source -> hasPermission(source, "yigd.command.view") && config.adminView)
                                .executes(ctx -> viewGrave(EntityArgumentType.getPlayer(ctx, "player"), ctx.getSource().getPlayer()))
                        )
                )
                .then(literal("moderate")
                        .requires(source -> hasPermission(source, "yigd.command.moderate") && config.moderateGraves)
                        .executes(ctx -> moderateGraves(ctx.getSource().getPlayer()))
                )
                .then(literal("clear")
                        .requires(source -> hasPermission(source, "yigd.command.clear") && config.clearGraveBackups)
                        .then(argument("victim", EntityArgumentType.players())
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();
                                    Entity entity = src.getEntity();
                                    if (entity instanceof PlayerEntity commandUser) {
                                        return clearBackup(EntityArgumentType.getPlayers(ctx, "victim"), commandUser);
                                    } else {
                                        return clearBackup(EntityArgumentType.getPlayers(ctx, "victim"), null);
                                    }
                                })
                        )
                )
                .then(literal("whitelist")
                        .requires(source -> hasPermission(source, "yigd.command.whitelist") && config.whitelist)
                        .then(literal("add")
                                .requires(source -> config.whitelistAdd)
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerCommandSource src = ctx.getSource();
                                            Entity entity = src.getEntity();
                                            if (entity instanceof PlayerEntity commandUser) {
                                                return addWhitelist(commandUser, EntityArgumentType.getPlayer(ctx, "player"));
                                            } else {
                                                return addWhitelist(null, EntityArgumentType.getPlayer(ctx, "player"));
                                            }
                                        })
                                )
                        )
                        .then(literal("remove")
                                .requires(source -> config.whitelistRemove)
                                .then(argument("player", EntityArgumentType.player())
                                        .executes(ctx -> {
                                            ServerCommandSource src = ctx.getSource();
                                            Entity entity = src.getEntity();
                                            if (entity instanceof PlayerEntity commandUser) {
                                                return removeWhitelist(commandUser, EntityArgumentType.getPlayer(ctx, "player"));
                                            } else {
                                                return removeWhitelist(null, EntityArgumentType.getPlayer(ctx, "player"));
                                            }
                                        })
                                )
                        )
                        .then(literal("toggle")
                                .requires(source -> config.whitelistToggle)
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();
                                    Entity entity = src.getEntity();
                                    if (entity instanceof PlayerEntity commandUser) {
                                        return toggleWhitelist(commandUser);
                                    } else {
                                        return toggleWhitelist(null);
                                    }
                                })
                        )
                )
                .then(literal("coordinates")
                        .requires(source -> config.coordinateToggle)
                        .executes(ctx -> showGraveCoordinates(ctx.getSource().getPlayer()))
                )
        ));
    }

    private static int showGraveCoordinates(PlayerEntity commandUser) {
        if (!YigdConfig.getConfig().commandToggles.coordinateToggle) {
            commandUser.sendMessage(new TranslatableText("text.yigd.message.missing_permission"), false);
            return -1;
        }
        List<DeadPlayerData> data = DeathInfoManager.INSTANCE.data.get(commandUser.getUuid());
        if (data != null && data.size() > 0) {
            DeadPlayerData graveData = data.get(data.size() - 1);
            commandUser.sendMessage(new TranslatableText("text.yigd.message.grave_location_info", graveData.gravePos.getX(), graveData.gravePos.getY(), graveData.gravePos.getZ(), graveData.dimensionName), false);
            return 1;
        }
        return 0;
    }

    private static int addWhitelist(@Nullable PlayerEntity commandUser, PlayerEntity addedPlayer) {
        if (commandUser != null && !hasPermission(commandUser, "yigd.command.whitelist") || !YigdConfig.getConfig().commandToggles.whitelistAdd) {
            if (commandUser != null) commandUser.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }

        DeathInfoManager.INSTANCE.addToWhiteList(addedPlayer.getUuid());
        if (commandUser != null) commandUser.sendMessage(new TranslatableText("text.yigd.message.whitelist.added_player", addedPlayer.getDisplayName().asString()), false);
        return 1;
    }
    private static int removeWhitelist(@Nullable PlayerEntity commandUser, PlayerEntity removedPlayer) {
        if (commandUser != null && !hasPermission(commandUser, "yigd.command.whitelist") || !YigdConfig.getConfig().commandToggles.whitelistRemove) {
            if (commandUser != null) commandUser.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }

        DeathInfoManager.INSTANCE.removeFromWhiteList(removedPlayer.getUuid());
        if (commandUser != null) commandUser.sendMessage(new TranslatableText("text.yigd.message.whitelist.removed_player", removedPlayer.getDisplayName().asString()), false);
        return 1;
    }
    private static int toggleWhitelist(@Nullable PlayerEntity commandUser) {
        if (commandUser != null && !hasPermission(commandUser, "yigd.command.whitelist") || !YigdConfig.getConfig().commandToggles.whitelistToggle) {
            if (commandUser != null) commandUser.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }

        boolean toggledTo = DeathInfoManager.INSTANCE.toggleListMode();
        if (commandUser != null) commandUser.sendMessage(new TranslatableText(toggledTo ? "text.yigd.message.whitelist.to_whitelist" : "text.yigd.message.whitelist.to_blacklist"), false);
        return 1;
    }

    private static int moderateGraves(ServerPlayerEntity player) {
        if (!hasPermission(player, "yigd.command.moderate") || !YigdConfig.getConfig().commandToggles.moderateGraves) {
            player.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }
        boolean existsGraves = false;
        for (List<DeadPlayerData> data : DeathInfoManager.INSTANCE.data.values()) {
            if (data.size() > 0) {
                existsGraves = true;
                break;
            }
        }

        if (existsGraves) {
            PacketByteBuf buf = PacketByteBufs.create();
            Map<UUID, List<DeadPlayerData>> graveData = DeathInfoManager.INSTANCE.data;

            buf.writeInt(graveData.size());
            for (Map.Entry<UUID, List<DeadPlayerData>> entry : graveData.entrySet()) {
                GameProfile profile;
                List<DeadPlayerData> graveList = entry.getValue();
                if (graveList.size() <= 0) {
                    profile = new GameProfile(entry.getKey(), null);
                } else {
                    profile = graveList.get(0).graveOwner;
                }

                buf.writeNbt(NbtHelper.writeGameProfile(new NbtCompound(), profile));
                buf.writeInt(graveList.size());
                for (DeadPlayerData grave : graveList) {
                    buf.writeByte(grave.availability);
                }
            }

            ServerPlayNetworking.send(player, PacketIdentifiers.ALL_PLAYER_GRAVES, buf);
        } else {
            player.sendMessage(new TranslatableText("text.yigd.message.grave_not_found"), false);
            return 0;
        }
        return 1;
    }

    private static int viewGrave(PlayerEntity player, PlayerEntity commandUser) {
        UUID userId = player.getUuid();
        YigdConfig config = YigdConfig.getConfig();
        if (!((hasPermission(commandUser, "yigd.command.view") && config.commandToggles.adminView) || (config.commandToggles.selfView && userId.equals(commandUser.getUuid())))) {
            commandUser.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }

        if (commandUser instanceof ServerPlayerEntity spe && DeathInfoManager.INSTANCE.data.containsKey(userId) && DeathInfoManager.INSTANCE.data.get(userId).size() > 0) {
            List<DeadPlayerData> deadPlayerData = DeathInfoManager.INSTANCE.data.get(userId);

            Yigd.LOGGER.info("Sending packets to " + spe.getDisplayName().asString() + " with grave info...");

            GameProfile profile = deadPlayerData.get(0).graveOwner;

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeNbt(NbtHelper.writeGameProfile(new NbtCompound(), profile));
            buf.writeInt(deadPlayerData.size());
            for (DeadPlayerData data : deadPlayerData) {
                int itemCount = 0;
                for (ItemStack stack : data.inventory) {
                    if (!stack.isEmpty()) itemCount++;
                }
                for (int i = 0; i < Yigd.apiMods.size(); i++) {
                    YigdApi yigdApi = Yigd.apiMods.get(i);

                    itemCount += yigdApi.getInventorySize(data.modInventories.get(i));
                }

                int points = data.xp;
                int i;
                for (i = 0; points >= 0; i++) {
                    if (i < 16) points -= (2 * i) + 7;
                    else if (i < 31) points -= (5 * i) - 38;
                    else points -= (9 * i) - 158;
                }

                buf.writeUuid(data.id);
                buf.writeBlockPos(data.gravePos);
                buf.writeString(data.dimensionName);
                buf.writeInt(itemCount);
                buf.writeInt(i - 1);
                buf.writeByte(data.availability);
            }

            ServerPlayNetworking.send((ServerPlayerEntity) commandUser, PacketIdentifiers.PLAYER_GRAVES_GUI, buf);
        } else {
            commandUser.sendMessage(new TranslatableText("text.yigd.message.view_command.fail", player.getDisplayName().asString()).styled(style -> style.withColor(0xFF0000)), false);
            return 0;
        }
        return 1;
    }

    // Get world reference variable from registry key identifier
    private static ServerWorld worldFromId (@Nullable MinecraftServer server, Identifier worldId) {
        ServerWorld world = null;
        if (server != null) {
            for (ServerWorld serverWorld : server.getWorlds()) {
                world = serverWorld;
                if (world.getRegistryKey().getValue() == worldId) break;
            }
        }

        return world;
    }

    public static int robGrave(GameProfile victim, PlayerEntity stealer, @Nullable UUID graveId) {
        if (!hasPermission(stealer, "yigd.command.rob") || !YigdConfig.getConfig().commandToggles.robGrave) {
            stealer.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }
        UUID victimId = victim.getId();

        if (!DeathInfoManager.INSTANCE.data.containsKey(victimId)) {
            stealer.sendMessage(new TranslatableText("text.yigd.message.rob_command.fail"), true);
            return 0;
        }
        List<DeadPlayerData> deadPlayerData = DeathInfoManager.INSTANCE.data.get(victimId);

        if (deadPlayerData.size() <= 0) {
            stealer.sendMessage(new TranslatableText("text.yigd.message.unclaimed_grave_missing", victim.getName()).styled(style -> style.withColor(0xFF0000)), true);
            return 0;
        }
        DeadPlayerData foundDeath = deadPlayerData.get(deadPlayerData.size() - 1);
        if (graveId != null) {
            for (DeadPlayerData data : deadPlayerData) {
                if (data.id != graveId) continue;
                foundDeath = data;
                break;
            }
        }
        DeathInfoManager.INSTANCE.markDirty();

        Map<String, Object> modInv = new HashMap<>();
        for (int i = 0; i < Yigd.apiMods.size(); i++) {
            YigdApi yigdApi = Yigd.apiMods.get(i);
            modInv.put(yigdApi.getModName(), foundDeath.modInventories.get(i));
        }

        ServerWorld world = worldFromId(stealer.getServer(), foundDeath.worldId);

        if (world != null && foundDeath.gravePos != null && world.getBlockState(foundDeath.gravePos).isOf(Yigd.GRAVE_BLOCK)) {
            world.removeBlock(foundDeath.gravePos, false);
            if (YigdConfig.getConfig().graveSettings.dropGraveBlock) {
                ItemScatterer.spawn(world, foundDeath.gravePos.getX(), foundDeath.gravePos.getY(), foundDeath.gravePos.getZ(), Yigd.GRAVE_BLOCK.asItem().getDefaultStack());
            }
        }

        GraveHelper.RetrieveItems(stealer, foundDeath.inventory, modInv, foundDeath.xp, true);

        stealer.sendMessage(new TranslatableText("text.yigd.message.rob_command.success"), true);

        if (stealer instanceof ServerPlayerEntity spe) {
            PlayerEntity victimPlayer = spe.server.getPlayerManager().getPlayer(victimId);
            if (victimPlayer != null) {
                victimPlayer.sendMessage(new TranslatableText("text.yigd.message.rob_command.victim"), false);
            } else {
                Yigd.notNotifiedRobberies.put(victimId, stealer.getGameProfile().getName());
            }
        }
        return 1;
    }

    private static UUID posToId(BlockPos pos, Identifier worldId, UUID userId) {
        List<DeadPlayerData> data = DeathInfoManager.INSTANCE.data.get(userId);
        if (data == null) return null;

        for (DeadPlayerData deadData : data) {
            if (deadData.gravePos.equals(pos) && deadData.worldId.equals(worldId)) return deadData.id;
        }
        return null;
    }
    public static int restoreGrave(PlayerEntity player, @Nullable PlayerEntity commandUser, @Nullable UUID graveId) {
        if (commandUser != null && !hasPermission(commandUser, "yigd.command.restore") || !YigdConfig.getConfig().commandToggles.retrieveGrave) {
            if (commandUser != null) commandUser.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }
        UUID userId = player.getUuid();

        if (!DeathInfoManager.INSTANCE.data.containsKey(userId)) {
            if (commandUser != null) commandUser.sendMessage(new TranslatableText("text.yigd.message.restore_command.fail"), true);
            return -1;
        }
        List<DeadPlayerData> deadPlayerData = DeathInfoManager.INSTANCE.data.get(userId);

        if (deadPlayerData.size() <= 0) {
            if (commandUser != null) commandUser.sendMessage(new TranslatableText("text.yigd.message.unclaimed_grave_missing", player.getDisplayName().asString()).styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }
        DeadPlayerData foundDeath = null;
        if (graveId == null) {
            foundDeath = deadPlayerData.get(deadPlayerData.size() - 1);
        } else {
            for (DeadPlayerData data : deadPlayerData) {
                if (!data.id.equals(graveId)) continue;
                foundDeath = data;
                break;
            }
        }

        if (foundDeath == null) return -1;

        Map<String, Object> modInv = new HashMap<>();
        for (int i = 0; i < Yigd.apiMods.size(); i++) {
            YigdApi yigdApi = Yigd.apiMods.get(i);
            modInv.put(yigdApi.getModName(), foundDeath.modInventories.get(i));
        }

        ServerWorld world = worldFromId(player.getServer(), foundDeath.worldId);

        if (world != null && foundDeath.gravePos != null && world.getBlockState(foundDeath.gravePos).isOf(Yigd.GRAVE_BLOCK)) {
            world.removeBlock(foundDeath.gravePos, false);

            if (YigdConfig.getConfig().graveSettings.dropGraveBlock) {
                ItemScatterer.spawn(world, foundDeath.gravePos.getX(), foundDeath.gravePos.getY(), foundDeath.gravePos.getZ(), Yigd.GRAVE_BLOCK.asItem().getDefaultStack());
            }
        }
        foundDeath.availability = 0;
        DeathInfoManager.INSTANCE.markDirty();

        GraveHelper.RetrieveItems(player, foundDeath.inventory, modInv, foundDeath.xp, false);

        if (commandUser != null) commandUser.sendMessage(new TranslatableText("text.yigd.message.restore_command.success"), true);
        return 1;
    }

    private static int clearBackup(Collection<ServerPlayerEntity> victims, @Nullable PlayerEntity commandUser) {
        if (commandUser != null && !hasPermission(commandUser, "yigd.command.clear") || !YigdConfig.getConfig().commandToggles.clearGraveBackups) {
            if (commandUser != null) commandUser.sendMessage(new TranslatableText("text.yigd.message.missing_permission").styled(style -> style.withColor(0xFF0000)), false);
            return -1;
        }
        int i = 0;
        for (PlayerEntity victim : victims) {
            UUID victimId = victim.getUuid();

            if (!DeathInfoManager.INSTANCE.data.containsKey(victimId)) continue;
            i++;
            DeathInfoManager.INSTANCE.data.get(victimId).clear();
        }
        DeathInfoManager.INSTANCE.markDirty();
        if (commandUser != null) commandUser.sendMessage(new TranslatableText("text.yigd.message.backup.delete_player", i), false);
        return 1;
    }

    public static boolean hasPermission(PlayerEntity player, String permission) {
        if (Yigd.miscCompatMods.contains("permissions")) {
            return switch (permission) {
                case "yigd.command.moderate" -> PermissionsCompat.moderatePermission(player);
                case "yigd.command.rob" -> PermissionsCompat.robPermission(player);
                case "yigd.command.restore" -> PermissionsCompat.restorePermission(player);
                case "yigd.command.view" -> PermissionsCompat.viewPermission(player);
                case "yigd.command.clear" -> PermissionsCompat.clearPermission(player);
                case "yigd.command.whitelist" -> PermissionsCompat.whitelistPermission(player);
                case "yigd.command.delete" -> PermissionsCompat.deletePermission(player);
                default -> false;
            };
        }
        return player.hasPermissionLevel(2);
    }
    private static boolean hasPermission(ServerCommandSource source, String permission) {
        if (Yigd.miscCompatMods.contains("permissions")) {
            return switch (permission) {
                case "yigd.command.moderate" -> PermissionsCompat.moderatePermission(source);
                case "yigd.command.rob" -> PermissionsCompat.robPermission(source);
                case "yigd.command.restore" -> PermissionsCompat.restorePermission(source);
                case "yigd.command.view" -> PermissionsCompat.viewPermission(source);
                case "yigd.command.clear" -> PermissionsCompat.clearPermission(source);
                case "yigd.command.whitelist" -> PermissionsCompat.whitelistPermission(source);
                default -> false;
            };
        }
        return source.hasPermissionLevel(2);
    }
}
