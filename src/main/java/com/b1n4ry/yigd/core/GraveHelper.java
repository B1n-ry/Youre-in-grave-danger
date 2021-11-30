package com.b1n4ry.yigd.core;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.block.entity.GraveBlockEntity;
import com.b1n4ry.yigd.config.LastResortConfig;
import com.b1n4ry.yigd.config.PriorityInventoryConfig;
import com.b1n4ry.yigd.config.YigdConfig;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
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

    public static void placeDeathGrave(World world, Vec3d pos, PlayerEntity player, DefaultedList<ItemStack> invItems, List<Object> modInventories, int xpPoints, UUID killerId) {
        if (world.isClient()) return;
        int bottomY = world.getBottomY();
        int topY = world.getTopY();
        if (!YigdConfig.getConfig().graveSettings.graveInVoid && pos.y < bottomY + 1) return;

        BlockPos blockPos = new BlockPos(pos.x, pos.y - 1, pos.z);

        if (blockPos.getY() < bottomY + 1) {
            blockPos = new BlockPos(blockPos.getX(), YigdConfig.getConfig().graveSettings.graveSpawnHeight, blockPos.getZ());
        } else if (blockPos.getY() > topY - 1) {
            blockPos = new BlockPos(blockPos.getX(), topY - 2, blockPos.getZ());
        }

        boolean foundViableGrave = false;


        for (YigdConfig.BlockPosition blockPosition : YigdConfig.getConfig().graveSettings.graveyard) {
            BlockPos gravePos = new BlockPos(blockPosition.x, blockPosition.y, blockPosition.z);

            if (gravePlaceableAt(world, gravePos, false)) {
                placeGraveBlock(player, world, gravePos, invItems, modInventories, xpPoints, killerId);
            }
        }

        if (YigdConfig.getConfig().graveSettings.trySoft) { // Trying soft
            for (BlockPos gravePos : BlockPos.iterateOutwards(blockPos.add(new Vec3i(0, 1, 0)), 5, 5, 5)) {
                if (gravePlaceableAt(world, gravePos, false)) {
                    placeGraveBlock(player, world, gravePos, invItems, modInventories, xpPoints, killerId);
                    foundViableGrave = true;
                    break;
                }
            }
        }
        if (!foundViableGrave && YigdConfig.getConfig().graveSettings.tryStrict) { // Trying strict
            for (BlockPos gravePos : BlockPos.iterateOutwards(blockPos.add(new Vec3i(0, 1, 0)), 5, 5, 5)) {
                if (gravePlaceableAt(world, gravePos, true)) {
                    placeGraveBlock(player, world, gravePos, invItems, modInventories, xpPoints, killerId);
                    foundViableGrave = true;
                    break;
                }
            }
        }

        // If there is nowhere to place the grave for some reason the items should not disappear
        if (!foundViableGrave) { // No grave was placed
            if (YigdConfig.getConfig().graveSettings.lastResort == LastResortConfig.SET_GRAVE) {
                placeGraveBlock(player, world, blockPos, invItems, modInventories, xpPoints, killerId);
            } else {
                for (YigdApi yigdApi : Yigd.apiMods) {
                    invItems.addAll(yigdApi.toStackList(player));
                }
                ItemScatterer.spawn(world, blockPos, invItems); // Scatter items at death pos
                ExperienceOrbEntity.spawn((ServerWorld) world, new Vec3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()), xpPoints);
            }
        }
    }

    private static boolean gravePlaceableAt(World world, BlockPos blockPos, boolean strict) {
        BlockEntity blockEntity = world.getBlockEntity(blockPos);

        if (blockEntity != null) return false;

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

        int yPos = blockPos.getY();
        return yPos > world.getBottomY() && yPos < world.getTopY(); // Return false if block exists outside the map (y-axis) and true if the block exists within the confined space of y = 0-255
    }

    private static void placeGraveBlock(PlayerEntity player, World world, BlockPos gravePos, DefaultedList<ItemStack> invItems, List<Object> modInventories, int xpPoints, UUID killerId) {
        boolean waterlogged = world.getFluidState(gravePos) == Fluids.WATER.getDefaultState();
        BlockState graveBlock = Yigd.GRAVE_BLOCK.getDefaultState().with(Properties.HORIZONTAL_FACING, player.getHorizontalFacing()).with(Properties.WATERLOGGED, waterlogged);
        world.setBlockState(gravePos, graveBlock);

        BlockPos blockPosUnder = new BlockPos(gravePos.getX(), gravePos.getY() - 1, gravePos.getZ());

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

            DefaultedList<ItemStack> moddedInvStacks = DefaultedList.of();
            for (int i = 0; i < Yigd.apiMods.size(); i++) {
                YigdApi yigdApi = Yigd.apiMods.get(i);
                moddedInvStacks.addAll(yigdApi.toStackList(modInventories.get(i)));
            }

            placedGraveEntity.setInventory(invItems);
            placedGraveEntity.setGraveOwner(playerProfile);
            placedGraveEntity.setCustomName(playerProfile.getName());
            placedGraveEntity.setStoredXp(xpPoints);
            placedGraveEntity.setModdedInventories(moddedInvStacks);
            placedGraveEntity.setKiller(killerId);

            placedGraveEntity.sync();

            System.out.println("[Yigd] Grave spawned at: " + gravePos.getX() + ", " +  gravePos.getY() + ", " + gravePos.getZ());
        }
        if (YigdConfig.getConfig().graveSettings.tellDeathPos) Yigd.deadPlayerData.setDeathPos(player.getUuid(), gravePos); // Backup of the coordinates where you died
    }

    public static DefaultedList<ItemStack> removeFromList(DefaultedList<ItemStack> list, DefaultedList<ItemStack> remove) {
        for (ItemStack item : remove) {
            int match = list.indexOf(item);
            if (match < 0) continue;

            list.set(match, ItemStack.EMPTY);
        }

        return list;
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

    public static void RetrieveItems(PlayerEntity player, DefaultedList<ItemStack> graveInv, List<ItemStack> graveModInv, int xp, boolean robbing) {
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

        List<Object> currentModInv = new ArrayList<>();
        for (YigdApi yigdApi : Yigd.apiMods) {
            Object modInv = yigdApi.getInventory(player);
            currentModInv.add(modInv);

            yigdApi.dropAll(player);
        }
        UUID userId = player.getUuid();
        List<Object> modInventories = Yigd.deadPlayerData.getModdedInventories(userId);


        inventory.clear(); // Delete all items

        PriorityInventoryConfig priorityInventory;
        if (robbing) {
            priorityInventory = YigdConfig.getConfig().graveSettings.graveRobbing.robPriority;
        } else {
            priorityInventory = YigdConfig.getConfig().graveSettings.priority;
        }

        DefaultedList<ItemStack> extraItems;
        if (priorityInventory == PriorityInventoryConfig.GRAVE) {
            extraItems = fillInventory(player, graveInv, modInventories, true);
            extraItems.addAll(fillInventory(player, invInventory, currentModInv, false));
        } else {
            extraItems = fillInventory(player, invInventory, currentModInv, false);
            extraItems.addAll(fillInventory(player, graveInv, modInventories, true));
        }

        if (modInventories == null && graveModInv != null) {
            extraItems.addAll(graveModInv);
        }

        List<Integer> openSlots = getInventoryOpenSlots(inventory.main);

        for (int i : openSlots) {
            if (extraItems.size() <= 0) break;
            inventory.setStack(i, extraItems.get(0));
            extraItems.remove(0);
        }

        ItemScatterer.spawn(player.world, player.getBlockPos(), extraItems);
        player.addExperience(xp);

        Yigd.deadPlayerData.dropDeathXp(userId);
        Yigd.deadPlayerData.dropDeathInventory(userId);
        Yigd.deadPlayerData.dropModdedInventory(userId);
        Yigd.deadPlayerData.dropDeathPos(userId);
    }

    private static DefaultedList<ItemStack> fillInventory(PlayerEntity player, DefaultedList<ItemStack> inv, List<Object> modInv, boolean fromGrave) {
        List<ItemStack> armorInventory = inv.subList(36, 40);
        List<ItemStack> mainInventory = inv.subList(0, 36);

        PlayerInventory inventory = player.getInventory();

        List<String> bindingCurse = Collections.singletonList("minecraft:binding_curse");
        DefaultedList<ItemStack> extraItems = DefaultedList.of();

        for (int i = 0; i < armorInventory.size(); i++) {
            ItemStack armorItem = armorInventory.get(i);
            EquipmentSlot equipmentSlot = MobEntity.getPreferredEquipmentSlot(armorItem);

            if (hasEnchantments(bindingCurse, armorItem)) {
                if (!fromGrave) {
                    ItemStack equipped = inventory.getArmorStack(i);
                    if (!equipped.isEmpty()) {
                        extraItems.add(equipped);
                    }
                    player.equipStack(equipmentSlot, armorItem);
                } else {
                    extraItems.add(armorItem);
                }
            } else {
                ItemStack equipped = inventory.getArmorStack(i);
                if (equipped.isEmpty()) {
                    player.equipStack(equipmentSlot, armorItem);
                } else {
                    extraItems.add(armorItem);
                }
            }
        }

        for (int i = 40; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
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
                extraItems.addAll(yigdApi.setInventory(modInv.get(i), player));
            }
        }

        return extraItems;
    }
}
