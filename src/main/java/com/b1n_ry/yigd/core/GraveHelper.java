package com.b1n_ry.yigd.core;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.LastResortConfig;
import com.b1n_ry.yigd.config.PriorityInventoryConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.*;

public class GraveHelper {
    public static List<Integer> getInventoryOpenSlots(DefaultedList<ItemStack> inventory) {
        List<Integer> openSlots = new ArrayList<>();

        for (int i = 0; i < inventory.size(); i++) {
            if(inventory.get(i) == ItemStack.EMPTY)
                openSlots.add(i);
        }

        return openSlots;
    }

    public static void placeDeathGrave(World world, Vec3d pos, PlayerEntity player, DefaultedList<ItemStack> invItems, List<Object> modInventories, int xpPoints, DamageSource source) {
        if (world.isClient()) return;
        int bottomY = world.getBottomY();
        int topY = world.getTopY();
        if (!YigdConfig.getConfig().graveSettings.graveInVoid && pos.y < bottomY + 1) return;

        double yPos = pos.y - 1D;
        if ((int) yPos != (int) (yPos + 0.5D) && player.isOnGround()) yPos++; // If player is standing on a slab or taller block, function should operate from the block above

        BlockPos blockPos = new BlockPos(pos.x, (int) yPos, pos.z);

        if (blockPos.getY() <= bottomY) {
            blockPos = new BlockPos(blockPos.getX(), bottomY + YigdConfig.getConfig().graveSettings.graveSpawnHeight, blockPos.getZ());
        } else if (blockPos.getY() >= topY) {
            blockPos = new BlockPos(blockPos.getX(), topY - 2, blockPos.getZ());
        }

        double boundEast = world.getWorldBorder().getBoundEast();
        double boundWest = world.getWorldBorder().getBoundWest();
        double boundSouth = world.getWorldBorder().getBoundSouth();
        double boundNorth = world.getWorldBorder().getBoundNorth();
        if (blockPos.getX() >= boundEast) {
            blockPos = new BlockPos(boundEast - 1, blockPos.getY(), blockPos.getZ());
        } else if (blockPos.getX() <= boundWest) {
            blockPos = new BlockPos(boundWest + 1, blockPos.getY(), blockPos.getZ());
        }
        if (blockPos.getZ() >= boundSouth) {
            blockPos = new BlockPos(blockPos.getX(), blockPos.getY(), boundSouth - 1);
        } else if (blockPos.getZ() <= boundNorth) {
            blockPos = new BlockPos(blockPos.getX(), blockPos.getY(), boundNorth + 1);
        }

        boolean foundViableGrave = false;
        MinecraftServer server = world.getServer();
        if (server != null) {
            JsonElement json = Yigd.graveyard.get("coordinates");
            boolean point2point = Yigd.graveyard.get("point2point").getAsBoolean();
            ServerWorld overworld = world.getServer().getOverworld();
            if (json instanceof JsonArray coordinates) {
                if (!point2point) {
                    for (JsonElement blockPosition : coordinates) {
                        if (blockPosition instanceof JsonObject xyz) {
                            int x = xyz.get("x").getAsInt();
                            int y = xyz.get("y").getAsInt();
                            int z = xyz.get("z").getAsInt();

                            Direction direction;
                            String dir = xyz.get("direction") != null ? xyz.get("direction").getAsString() : "none";
                            switch (dir) {
                                case "NORTH" -> direction = Direction.NORTH;
                                case "SOUTH" -> direction = Direction.SOUTH;
                                case "WEST" -> direction = Direction.WEST;
                                case "EAST" -> direction = Direction.EAST;
                                default -> direction = player.getHorizontalFacing();
                            }

                            BlockPos gravePos = new BlockPos(x, y, z);

                            if (gravePlaceableAt(overworld, gravePos, false)) {
                                boolean isPlaced = placeGraveBlock(player, overworld, gravePos, invItems, modInventories, xpPoints, source, direction);
                                if (!isPlaced) continue;
                                foundViableGrave = true;
                                break;
                            }
                        }
                    }
                } else if(coordinates.size() >= 2) {
                    if (coordinates.get(0) instanceof JsonObject pos1 && coordinates.get(1) instanceof JsonObject pos2) {
                        int x1 = pos1.get("x").getAsInt();
                        int x2 = pos2.get("x").getAsInt();
                        int y1 = pos1.get("y").getAsInt();
                        int y2 = pos2.get("y").getAsInt();
                        int z1 = pos1.get("z").getAsInt();
                        int z2 = pos2.get("z").getAsInt();
                        int changeX = (x2 - x1) / Math.abs(x2 - x1 != 0 ? x2 - x1 : 1);
                        int changeY = (y2 - y1) / Math.abs(y2 - y1 != 0 ? y2 - y1 : 1);
                        int changeZ = (z2 - z1) / Math.abs(z2 - z1 != 0 ? z2 - z1 : 1);

                        for (int y = y1; y != y2; y += changeY) {
                            if (foundViableGrave) break;
                            for (int z = z1; z != z2; z += changeZ) {
                                if (foundViableGrave) break;
                                for (int x = x1; x != x2; x += changeX) {
                                    BlockPos gravePos = new BlockPos(x, y, z);

                                    if (gravePlaceableAt(overworld, gravePos, false)) {
                                        boolean isPlaced = placeGraveBlock(player, overworld, gravePos, invItems, modInventories, xpPoints, source);
                                        if (!isPlaced) continue;
                                        foundViableGrave = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!foundViableGrave && YigdConfig.getConfig().graveSettings.trySoft) { // Trying soft
            for (BlockPos gravePos : BlockPos.iterateOutwards(blockPos.add(new Vec3i(0, 1, 0)), 5, 5, 5)) {
                if (gravePlaceableAt(world, gravePos, false)) {
                    boolean isPlaced = placeGraveBlock(player, world, gravePos, invItems, modInventories, xpPoints, source);
                    if (!isPlaced) continue;

                    foundViableGrave = true;
                    break;
                }
            }
        }
        if (!foundViableGrave && YigdConfig.getConfig().graveSettings.tryStrict) { // Trying strict
            for (BlockPos gravePos : BlockPos.iterateOutwards(blockPos.add(new Vec3i(0, 1, 0)), 5, 5, 5)) {
                if (gravePlaceableAt(world, gravePos, true)) {
                    boolean isPlaced = placeGraveBlock(player, world, gravePos, invItems, modInventories, xpPoints, source);
                    if (!isPlaced) continue;
                    foundViableGrave = true;
                    break;
                }
            }
        }

        // If there is nowhere to place the grave for some reason the items should not disappear
        if (!foundViableGrave) { // No grave was placed
            boolean isPlaced = false;
            if (YigdConfig.getConfig().graveSettings.lastResort == LastResortConfig.SET_GRAVE) {
                isPlaced = placeGraveBlock(player, world, blockPos, invItems, modInventories, xpPoints, source);
                if (!isPlaced) {
                    Yigd.LOGGER.warn("Failed to set grave as a last resort");
                }
            }
            if (!isPlaced) {
                for (YigdApi yigdApi : Yigd.apiMods) {
                    invItems.addAll(yigdApi.toStackList(player));
                }
                ItemScatterer.spawn(world, blockPos, invItems); // Scatter items at death pos
                ExperienceOrbEntity.spawn((ServerWorld) world, new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()), xpPoints);
                Yigd.LOGGER.info("Dropped items as a last resort");
            }
        }
    }

    private static boolean gravePlaceableAt(World world, BlockPos blockPos, boolean strict) {
        BlockEntity blockEntity = world.getBlockEntity(blockPos);

        if (blockEntity != null) return false;

        double boundEast = world.getWorldBorder().getBoundEast();
        double boundWest = world.getWorldBorder().getBoundWest();
        double boundSouth = world.getWorldBorder().getBoundSouth();
        double boundNorth = world.getWorldBorder().getBoundNorth();

        String path;
        if (strict) {
            path = "replace_blacklist";
        } else {
            path = "soft_whitelist";
        }

        boolean hasTag = false;

        Block block = world.getBlockState(blockPos).getBlock();
        Collection<Identifier> tagIds = world.getTagManager().getOrCreateTagGroup(Registry.BLOCK_KEY).getTagsFor(block);
        for (Identifier tagId : tagIds) {
            if (!tagId.getNamespace().equals("yigd")) continue;
            if (tagId.getPath().equals(path)) {
                hasTag = true;
                break;
            }
        }

        if ((hasTag && strict) || (!hasTag && !strict)) return false;

        int xPos = blockPos.getX();
        int yPos = blockPos.getY();
        int zPos = blockPos.getZ();
        // Return false if block exists outside the map and true if the block exists within the world border and build height
        return !(xPos >= boundEast && xPos <= boundWest && yPos <= world.getBottomY() && yPos >= world.getTopY() && zPos <= boundNorth && zPos >= boundSouth);
    }

    private static boolean placeGraveBlock(PlayerEntity player, World world, BlockPos gravePos, DefaultedList<ItemStack> invItems, List<Object> modInventories, int xpPoints, DamageSource source) {
        Direction direction = player.getHorizontalFacing();
        return placeGraveBlock(player, world, gravePos, invItems, modInventories, xpPoints, source, direction);
    }

    private static boolean placeGraveBlock(PlayerEntity player, World world, BlockPos gravePos, DefaultedList<ItemStack> invItems, List<Object> modInventories, int xpPoints, DamageSource source, Direction direction) {
        boolean waterlogged = world.getFluidState(gravePos) == Fluids.WATER.getDefaultState();
        BlockState graveBlock = Yigd.GRAVE_BLOCK.getDefaultState().with(Properties.HORIZONTAL_FACING, direction).with(Properties.WATERLOGGED, waterlogged);
        boolean isPlaced = world.setBlockState(gravePos, graveBlock);
        if (!isPlaced) {
            return false;
        }

        BlockPos blockPosUnder = gravePos.down();

        YigdConfig.BlockUnderGrave blockUnderConfig = YigdConfig.getConfig().graveSettings.blockUnderGrave;
        String replaceUnderBlock;

        if (blockUnderConfig.generateBlockUnder && blockPosUnder.getY() >= world.getBottomY() + 1) { // If block should generate under, and if there is a "block" under that can be replaced
            Block blockUnder = world.getBlockState(blockPosUnder).getBlock();

            boolean canPlaceUnder = false;
            Collection<Identifier> tagIds = world.getTagManager().getOrCreateTagGroup(Registry.BLOCK_KEY).getTagsFor(blockUnder);
            for (Identifier tagId : tagIds) {
                if (!tagId.getNamespace().equals("yigd")) continue;
                if (tagId.getPath().equals("support_replace_whitelist")) {
                    canPlaceUnder = true;
                    break;
                }
            }

            if (canPlaceUnder) {
                if (world.getRegistryKey() == World.OVERWORLD) {
                    replaceUnderBlock = blockUnderConfig.inOverWorld;
                } else if (world.getRegistryKey() == World.NETHER) {
                    replaceUnderBlock = blockUnderConfig.inNether;
                } else if (world.getRegistryKey() == World.END) {
                    replaceUnderBlock = blockUnderConfig.inTheEnd;
                } else {
                    replaceUnderBlock = blockUnderConfig.inCustom;
                }

                Identifier blockIdentifier = Identifier.tryParse(replaceUnderBlock);
                BlockState blockStateUnder;
                if (blockIdentifier == null) {
                    blockStateUnder = Blocks.DIRT.getDefaultState();
                } else {
                    blockStateUnder = Registry.BLOCK.get(blockIdentifier).getDefaultState();
                }

                world.setBlockState(blockPosUnder, blockStateUnder); // Place support block under grave
            }
        }

        BlockEntity placed = world.getBlockEntity(gravePos);
        if (placed instanceof GraveBlockEntity placedGraveEntity) {

            GameProfile playerProfile = player.getGameProfile();

            Map<String, Object> moddedInvStacks = new HashMap<>();
            for (int i = 0; i < Yigd.apiMods.size(); i++) {
                YigdApi yigdApi = Yigd.apiMods.get(i);
                moddedInvStacks.put(yigdApi.getModName(), modInventories.get(i));
            }

            UUID killerId;
            Entity e = source.getSource();
            if (e instanceof PlayerEntity) {
                killerId = e.getUuid();
            } else {
                killerId = null;
            }

            placedGraveEntity.setInventory(invItems);
            placedGraveEntity.setGraveOwner(playerProfile);
            placedGraveEntity.setCustomName(playerProfile.getName());
            placedGraveEntity.setStoredXp(xpPoints);
            placedGraveEntity.setModdedInventories(moddedInvStacks);
            placedGraveEntity.setKiller(killerId);

            DeadPlayerData deadData = DeadPlayerData.create(invItems, modInventories, gravePos, player.getGameProfile(), xpPoints, world, source);

            UUID userId = player.getUuid();
            if (!DeathInfoManager.INSTANCE.data.containsKey(userId)) {
                List<DeadPlayerData> deadPlayerData = new ArrayList<>();
                deadPlayerData.add(deadData);
                DeathInfoManager.INSTANCE.data.put(userId, deadPlayerData);
            } else {
                DeathInfoManager.INSTANCE.data.get(userId).add(deadData);
            }
            DeathInfoManager.INSTANCE.markDirty();

            Yigd.LOGGER.info("[Yigd] Grave spawned at: " + gravePos.getX() + ", " +  gravePos.getY() + ", " + gravePos.getZ() + " | " + deadData.dimensionName);
        }
        return true;
    }

    public static void removeFromList(DefaultedList<ItemStack> list, DefaultedList<ItemStack> remove) {
        for (ItemStack item : remove) {
            int match = list.indexOf(item);
            if (match < 0) continue;

            list.set(match, ItemStack.EMPTY);
        }
    }

    public static DefaultedList<ItemStack> getEnchantedItems(DefaultedList<ItemStack> items, List<String> enchantStrings) {
        DefaultedList<ItemStack> included = DefaultedList.ofSize(items.size(), ItemStack.EMPTY);

        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            if (hasEnchantments(enchantStrings, item)) included.set(i, item);
        }
        return included;
    }

    public static boolean hasEnchantments(List<String> enchants, ItemStack item) {
        if (!item.hasEnchantments()) return false;

        for (NbtElement enchantment : item.getEnchantments()) {
            String enchantId = ((NbtCompound) enchantment).getString("id");

            if (enchants.stream().anyMatch(enchantId::equals)) {
                return true;
            }
        }

        return false;
    }

    public static void RetrieveItems(PlayerEntity player, DefaultedList<ItemStack> graveInv, Map<String, Object> modInventories, int xp, boolean robbing) {
        PlayerInventory inventory = player.getInventory();

        DefaultedList<ItemStack> invInventory = DefaultedList.of();

        invInventory.addAll(inventory.main);
        invInventory.addAll(inventory.armor);
        invInventory.addAll(inventory.offHand);

        if (inventory.size() > 41) {
            for (int i = 41; i < inventory.size(); i++) {
                invInventory.add(inventory.getStack(i));
            }
        }

        Map<String, Object> currentModInv = new HashMap<>();
        for (YigdApi yigdApi : Yigd.apiMods) {
            Object modInv = yigdApi.getInventory(player);
            currentModInv.put(yigdApi.getModName(), modInv);

            yigdApi.dropAll(player);
        }


        inventory.clear(); // Delete all items

        UUID playerId = player.getUuid();

        PriorityInventoryConfig priorityInventory;
        if (robbing) {
            if (Yigd.clientRobPriorities.containsKey(playerId)) {
                priorityInventory = Yigd.clientRobPriorities.get(playerId);
            } else {
                priorityInventory = YigdConfig.getConfig().graveSettings.graveRobbing.robPriority;
            }
        } else {
            if (Yigd.clientPriorities.containsKey(playerId)) {
                priorityInventory = Yigd.clientPriorities.get(playerId);
            } else {
                priorityInventory = YigdConfig.getConfig().graveSettings.priority;
            }
        }

        DefaultedList<ItemStack> extraItems;
        if (priorityInventory == PriorityInventoryConfig.GRAVE) {
            extraItems = fillInventory(player, graveInv, modInventories, true);
            extraItems.addAll(fillInventory(player, invInventory, currentModInv, false));
        } else {
            extraItems = fillInventory(player, invInventory, currentModInv, false);
            extraItems.addAll(fillInventory(player, graveInv, modInventories, true));
        }

        List<Integer> openSlots = getInventoryOpenSlots(inventory.main);
        extraItems.removeIf(ItemStack::isEmpty);

        for (int i : openSlots) {
            if (extraItems.size() <= 0) break;
            inventory.setStack(i, extraItems.get(0));
            extraItems.remove(0);
        }

        ItemScatterer.spawn(player.world, player.getBlockPos(), extraItems);
        player.addExperience(xp);

        Yigd.LOGGER.info(player.getDisplayName().asString() + " retrieved the items from the grave");
    }

    private static DefaultedList<ItemStack> fillInventory(PlayerEntity player, DefaultedList<ItemStack> inv, Map<String, Object> modInv, boolean fromGrave) {
        List<ItemStack> armorInventory = inv.subList(36, 40);
        List<ItemStack> mainInventory = inv.subList(0, 36);

        PlayerInventory inventory = player.getInventory();

        List<String> bindingCurse = Collections.singletonList("minecraft:binding_curse");
        DefaultedList<ItemStack> extraItems = DefaultedList.of();

        for (int i = 0; i < armorInventory.size(); i++) {
            ItemStack armorItem = armorInventory.get(i);

            Collection<Identifier> tags = player.world.getTagManager().getOrCreateTagGroup(Registry.ITEM_KEY).getTagsFor(armorItem.getItem());
            if (tags.contains(new Identifier("yigd", "force_item_slot"))) {
                ItemStack equipped = inventory.getArmorStack(i);
                if (!equipped.isEmpty()) {
                    extraItems.add(equipped);
                }
                inventory.setStack(mainInventory.size() + i, armorItem);
            } else if (hasEnchantments(bindingCurse, armorItem) && YigdConfig.getConfig().graveSettings.applyBindingCurse) {
                if (!fromGrave) {
                    ItemStack equipped = inventory.getArmorStack(i);
                    if (!equipped.isEmpty()) {
                        extraItems.add(equipped);
                    }
                    inventory.setStack(mainInventory.size() + i, armorItem);
                } else {
                    extraItems.add(armorItem);
                }
            } else {
                ItemStack equipped = inventory.getArmorStack(i);
                if (equipped.isEmpty()) {
                    inventory.setStack(mainInventory.size() + i, armorItem);
                } else {
                    extraItems.add(armorItem);
                }
            }
        }

        for (int i = 40; i < inv.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (inventory.size() <= i) {
                extraItems.add(inv.get(i));
            } else if (stack.isEmpty()) {
                inventory.setStack(i, inv.get(i));
            } else {
                extraItems.add(inv.get(i));
            }
        }

        for (int i = 0; i < mainInventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                inventory.setStack(i, inv.get(i));
            } else {
                extraItems.add(inv.get(i));
            }
        }

        if (modInv != null) {
            for (int i = 0; i < modInv.size(); i++) {
                YigdApi yigdApi = Yigd.apiMods.get(i);
                extraItems.addAll(yigdApi.setInventory(modInv.get(yigdApi.getModName()), player));
            }
        }

        return extraItems;
    }
}
