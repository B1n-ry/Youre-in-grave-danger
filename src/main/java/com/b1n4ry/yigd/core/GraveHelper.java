package com.b1n4ry.yigd.core;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.block.entity.GraveBlockEntity;
import com.b1n4ry.yigd.config.LastResortConfig;
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
import net.minecraft.item.Items;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
            blockPos = new BlockPos(blockPos.getX(), bottomY + YigdConfig.getConfig().graveSettings.graveSpawnHeight, blockPos.getZ());
        } else if (blockPos.getY() > topY - 1) {
            blockPos = new BlockPos(blockPos.getX(), topY - 2, blockPos.getZ());
        }

        boolean foundViableGrave = false;

        for (BlockPos gravePos : BlockPos.iterateOutwards(blockPos.add(new Vec3i(0, 1, 0)), 5, 5, 5)) {
            if (gravePlaceableAt(world, gravePos)) {
                placeGraveBlock(player, world, gravePos, invItems, modInventories, xpPoints, killerId);
                foundViableGrave = true;
                break;
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

    private static boolean gravePlaceableAt(World world, BlockPos blockPos) {
        BlockEntity blockEntity = world.getBlockEntity(blockPos);

        if (blockEntity != null) return false;

        Block block = world.getBlockState(blockPos).getBlock();

        List<String> blacklistBlockId = YigdConfig.getConfig().graveSettings.blacklistBlocks;
        String id = Registry.BLOCK.getId(block).toString();

        if (blacklistBlockId.contains(id)) return false;

        int yPos = blockPos.getY();
        return yPos > world.getBottomY() + 1 && yPos < world.getTopY() - 1; // Return false if block exists outside the map (y-axis) and true if the block exists within the confined space worldBottom < y < worldTop
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
            String blockId = Registry.BLOCK.getId(blockUnder).toString();

            if (blockUnderConfig.whiteListBlocks.contains(blockId)) {
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

            List<List<ItemStack>> moddedInvStacks = new ArrayList<>();
            for (int i = 0; i < Yigd.apiMods.size(); i++) {
                YigdApi yigdApi = Yigd.apiMods.get(i);
                moddedInvStacks.add(new ArrayList<>());
                moddedInvStacks.get(i).addAll(yigdApi.toStackList(modInventories.get(i)));
            }

            placedGraveEntity.setInventory(invItems);
            placedGraveEntity.setGraveOwner(playerProfile);
            placedGraveEntity.setCustomName(playerProfile.getName());
            placedGraveEntity.setStoredXp(xpPoints);
            placedGraveEntity.setModdedInventories(moddedInvStacks);
            placedGraveEntity.setKiller(killerId);

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

    public static void RetrieveItems(PlayerEntity player, DefaultedList<ItemStack> items, int xp) {
        PlayerInventory inventory = player.getInventory();

        DefaultedList<ItemStack> retrievalInventory = DefaultedList.of();

        retrievalInventory.addAll(inventory.main);
        retrievalInventory.addAll(inventory.armor);
        retrievalInventory.addAll(inventory.offHand);

        if (inventory.size() > 41) {
            for (int i = 41; i < inventory.size(); i++) {
                retrievalInventory.add(inventory.getStack(i));
            }
        }

        List<ItemStack> asIStack = new ArrayList<>();
        for (YigdApi yigdApi : Yigd.apiMods) {
            Object modInv = yigdApi.getInventory(player);
            asIStack.addAll(yigdApi.toStackList(modInv));

            yigdApi.dropAll(player);
        }


        inventory.clear(); // Delete all items

        List<ItemStack> armorInventory = items.subList(36, 40);
        List<ItemStack> mainInventory = items.subList(0, 36);


        List<String> bindingCurse = Collections.singletonList("minecraft:binding_curse");

        for (int i = 0; i < armorInventory.size(); i++) { // Replace armor from grave
            EquipmentSlot equipmentSlot = MobEntity.getPreferredEquipmentSlot(armorInventory.get(i)); // return EquipmentSlot

            ItemStack rArmorItem = retrievalInventory.subList(36, 40).get(i);
            ItemStack gArmorItem = armorInventory.get(i);

            boolean currentHasCurse = hasEnchantments(bindingCurse, rArmorItem); // If retrieval armor has "curse of binding" this will return true

            // If retrieval trip armor has "curse of binding" it should stay on, and if grave armor had "curse of binding" it should end up in the inventory
            // Grave armor only gets equipped if neither last nor current armor has "curse of binding"
            if (!currentHasCurse && !hasEnchantments(bindingCurse, gArmorItem)) {
                player.equipStack(equipmentSlot, gArmorItem); // Both armor parts are free from curse of binding and armor can be replaced
            }
        }

        player.equipStack(EquipmentSlot.OFFHAND, items.get(40)); // Replace offhand from grave

        if (items.size() > 41) { // Replaced possible extra slots from mods
            for (int i = 41; i < items.size(); i++) {
                inventory.setStack(i, items.get(i));
            }
        }

        for (int i = 0; i < mainInventory.size(); i++) { // Replace main inventory from grave
            inventory.setStack(i, mainInventory.get(i));
        }


        DefaultedList<ItemStack> extraItems = DefaultedList.of();

        UUID userId = player.getUuid();
        List<Object> modInventories = Yigd.deadPlayerData.getModdedInventories(userId);
        if (modInventories == null) modInventories = new ArrayList<>(0);
        for (int i = 0; i < modInventories.size(); i++) {
            YigdApi yigdApi = Yigd.apiMods.get(i);

            yigdApi.setInventory(modInventories.get(i), player);
        }

        extraItems.addAll(retrievalInventory.subList(0, 36));
        extraItems.addAll(asIStack);

        List<Integer> openArmorSlots = getInventoryOpenSlots(inventory.armor); // Armor slots that does not have armor selected

        for(int i = 0; i < 4; i++) {
            ItemStack armorPiece = retrievalInventory.subList(36, 40).get(i);
            if (openArmorSlots.contains(i)) {
                player.equipStack(EquipmentSlot.fromTypeIndex(EquipmentSlot.Type.ARMOR, i), armorPiece); // Put player armor back
                extraItems.add(armorInventory.get(i));
            } else {
                extraItems.add(armorPiece);
            }
        }

        ItemStack offHandItem = inventory.offHand.get(0);
        if(offHandItem == ItemStack.EMPTY || offHandItem.getItem() == Items.AIR) player.equipStack(EquipmentSlot.OFFHAND, retrievalInventory.get(40));
        else extraItems.add(retrievalInventory.get(40));

        if (retrievalInventory.size() > 41) {
            for (int i = 41; i < retrievalInventory.size(); i++) {
                if (inventory.getStack(i).isEmpty()) {
                    inventory.setStack(i, retrievalInventory.get(i));
                } else {
                    extraItems.add(retrievalInventory.get(i));
                }
            }
        }

        List<Integer> openSlots = getInventoryOpenSlots(inventory.main);
        List<Integer> stillOpen = new ArrayList<>();

        int loopIterations = Math.min(openSlots.size(), extraItems.size());
        for(int i = 0; i < loopIterations; i++) {
            int currentSlot = openSlots.get(i);
            ItemStack currentExtra = extraItems.get(i);
            inventory.setStack(currentSlot, currentExtra);

            if (currentExtra.isEmpty()) {
                stillOpen.add(currentSlot);
            }
        }

        List<ItemStack> overflow = extraItems.subList(loopIterations, extraItems.size());
        overflow.removeIf(ItemStack::isEmpty);

        for (int i = 0; i < Math.min(overflow.size(), stillOpen.size()); i++) {
            inventory.setStack(stillOpen.get(i), overflow.get(i));
        }

        DefaultedList<ItemStack> dropItems = DefaultedList.of();
        if (stillOpen.size() < overflow.size()) dropItems.addAll(overflow.subList(stillOpen.size(), overflow.size()));


        BlockPos playerPos = player.getBlockPos();

        ItemScatterer.spawn(player.world, playerPos, dropItems);
        player.addExperience(xp);

        Yigd.deadPlayerData.dropDeathXp(userId);
        Yigd.deadPlayerData.dropDeathInventory(userId);
        Yigd.deadPlayerData.dropModdedInventory(userId);
        Yigd.deadPlayerData.dropDeathPos(userId);
    }
}
