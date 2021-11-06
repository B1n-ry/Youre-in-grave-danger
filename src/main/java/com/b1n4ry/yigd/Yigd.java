package com.b1n4ry.yigd;

import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.block.GraveBlock;
import com.b1n4ry.yigd.block.entity.GraveBlockEntity;
import com.b1n4ry.yigd.compat.CuriosCompat;
import com.b1n4ry.yigd.compat.CuriosCosmeticCompat;
import com.b1n4ry.yigd.compat.TrinketsCompat;
import com.b1n4ry.yigd.config.YigdConfig;
import com.mojang.authlib.GameProfile;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
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
import java.util.List;

public class Yigd implements ModInitializer {

    public static final GraveBlock GRAVE_BLOCK = new GraveBlock(FabricBlockSettings.of(Material.STONE).strength(0.8f, 3000.0f));
    public static BlockEntityType<GraveBlockEntity> GRAVE_BLOCK_ENTITY;

    public static final ArrayList<YigdApi> apiMods = new ArrayList<>();

    @Override
    public void onInitialize() {
        AutoConfig.register(YigdConfig.class, Toml4jConfigSerializer::new);

        Registry.register(Registry.BLOCK, new Identifier("yigd", "grave"), GRAVE_BLOCK);
        Registry.register(Registry.ITEM, new Identifier("yigd", "grave"), new BlockItem(GRAVE_BLOCK, new FabricItemSettings().group(ItemGroup.DECORATIONS)));

        GRAVE_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, "yigd:grave_block_entity", BlockEntityType.Builder.create(GraveBlockEntity::new, GRAVE_BLOCK).build(null));


        if (FabricLoader.getInstance().isModLoaded("trinkets")) {
            apiMods.add(new TrinketsCompat());
        }
        if (FabricLoader.getInstance().isModLoaded("curios")) {
            apiMods.add(new CuriosCompat());
            apiMods.add(new CuriosCosmeticCompat());
        }
        apiMods.addAll(FabricLoader.getInstance().getEntrypoints("yigd", YigdApi.class));
    }

    public static void placeDeathGrave(World world, Vec3d pos, PlayerEntity player, DefaultedList<ItemStack> invItems) {
        if (world.isClient()) return;

        BlockPos blockPos = new BlockPos(pos.x, pos.y - 1, pos.z);

        if (blockPos.getY() < 0) {
            blockPos = new BlockPos(blockPos.getX(), 10, blockPos.getZ());
        } else if (blockPos.getY() > 255) {
            blockPos = new BlockPos(blockPos.getX(), 254, blockPos.getZ());
        }

        for (YigdApi yigdApi : Yigd.apiMods) {
            invItems.addAll(yigdApi.getInventory(player));

            yigdApi.dropAll(player);
        }


        boolean foundViableGrave = false;

        for (BlockPos gravePos : BlockPos.iterateOutwards(blockPos.add(new Vec3i(0, 1, 0)), 5, 5, 5)) {
            if (gravePlaceableAt(world, gravePos)) {
                BlockState graveBlock = Yigd.GRAVE_BLOCK.getDefaultState().with(Properties.HORIZONTAL_FACING, player.getHorizontalFacing());
                world.setBlockState(gravePos, graveBlock);

                BlockPos blockPosUnder = new BlockPos(gravePos.getX(), gravePos.getY() - 1, gravePos.getZ());
                Block blockUnder = world.getBlockState(blockPosUnder).getBlock();
                String blockId = Registry.BLOCK.getId(blockUnder).toString();

                YigdConfig.BlockUnderGrave blockUnderConfig = YigdConfig.getConfig().graveSettings.blockUnderGrave;
                String replaceUnderBlock;

                if (blockUnderConfig.generateBlockUnder) {
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
                if (placed instanceof GraveBlockEntity) {
                    GraveBlockEntity placedGraveEntity = (GraveBlockEntity)placed;

                    GameProfile playerProfile = player.getGameProfile();

                    int xpPoints;
                    if (YigdConfig.getConfig().graveSettings.defaultXpDrop) {
                        xpPoints = Math.min(7 * player.experienceLevel, 100);
                    } else {
                        xpPoints = (YigdConfig.getConfig().graveSettings.xpDropPercent / 100) * player.totalExperience;
                    }

                    placedGraveEntity.setInventory(invItems);
                    placedGraveEntity.setGraveOwner(playerProfile);
                    placedGraveEntity.setStoredXp(xpPoints);
                    placedGraveEntity.setCustomName(playerProfile.getName());

                    placedGraveEntity.sync();


                    System.out.println("[Yigd] Grave spawned at: " + gravePos.getX() + ", " +  gravePos.getY() + ", " + gravePos.getZ());
                }
                foundViableGrave = true;
                break;
            }
        }

        // If there is nowhere to place the grave for some reason the items should not disappear
        if (!foundViableGrave) { // No grave was placed
            ItemScatterer.spawn(world, blockPos, invItems); // Scatter items at death pos
        }
    }

    private static boolean gravePlaceableAt(World world, BlockPos blockPos) {
        BlockEntity blockEntity = world.getBlockEntity(blockPos);

        if (blockEntity != null) return false;

        Block block = world.getBlockState(blockPos).getBlock();

        List<String> blacklistBlockId = YigdConfig.getConfig().graveSettings.blacklistBlocks;
        String id = Registry.BLOCK.getId(block).toString();

        if (blacklistBlockId.contains(id)) return false;

        return blockPos.getY() >= 0 && blockPos.getY() <= 255; // Return false if block exists outside the map (y-axis) and true if the block exists within the confined space of y = 0-255
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
}