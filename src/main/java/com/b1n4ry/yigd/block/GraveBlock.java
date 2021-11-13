package com.b1n4ry.yigd.block;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.block.entity.GraveBlockEntity;
import com.b1n4ry.yigd.config.DropTypeConfig;
import com.b1n4ry.yigd.config.RetrievalTypeConfig;
import com.b1n4ry.yigd.config.YigdConfig;
import com.b1n4ry.yigd.core.DeadPlayerData;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GraveBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    public static final DirectionProperty FACING;
    protected static final VoxelShape SHAPE_NORTH;
    protected static final VoxelShape SHAPE_BASE_NORTH;
    protected static final VoxelShape SHAPE_FOOT_NORTH;
    protected static final VoxelShape SHAPE_CORE_NORTH;
    protected static final VoxelShape SHAPE_TOP_NORTH;
    protected static final VoxelShape SHAPE_WEST;
    protected static final VoxelShape SHAPE_BASE_WEST;
    protected static final VoxelShape SHAPE_FOOT_WEST;
    protected static final VoxelShape SHAPE_CORE_WEST;
    protected static final VoxelShape SHAPE_TOP_WEST;
    protected static final VoxelShape SHAPE_EAST;
    protected static final VoxelShape SHAPE_BASE_EAST;
    protected static final VoxelShape SHAPE_FOOT_EAST;
    protected static final VoxelShape SHAPE_CORE_EAST;
    protected static final VoxelShape SHAPE_TOP_EAST;
    protected static final VoxelShape SHAPE_SOUTH;
    protected static final VoxelShape SHAPE_BASE_SOUTH;
    protected static final VoxelShape SHAPE_FOOT_SOUTH;
    protected static final VoxelShape SHAPE_CORE_SOUTH;
    protected static final VoxelShape SHAPE_TOP_SOUTH;

    private String customName = null;


    public GraveBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        RetrievalTypeConfig retrievalType = YigdConfig.getConfig().graveSettings.retrievalType;
        if (retrievalType == RetrievalTypeConfig.ON_USE || retrievalType == null) {
            RetrieveItems(player, world, pos);
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }
    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (YigdConfig.getConfig().graveSettings.retrievalType == RetrievalTypeConfig.ON_STAND && entity instanceof PlayerEntity player) {
            RetrieveItems(player, world, pos);
        } else if (YigdConfig.getConfig().graveSettings.retrievalType == RetrievalTypeConfig.ON_SNEAK && entity instanceof PlayerEntity player) {
            if (player.isInSneakingPose()) {
                RetrieveItems(player, world, pos);
            }
        }

        super.onSteppedOn(world, pos, state, entity);
    }
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (YigdConfig.getConfig().graveSettings.retrievalType == RetrievalTypeConfig.ON_BREAK) {
            if (RetrieveItems(player, world, pos)) return;
        }

        super.onBreak(world, pos, state, player);
    }
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof GraveBlockEntity graveEntity) {
            if (YigdConfig.getConfig().graveSettings.retrievalType == RetrievalTypeConfig.ON_BREAK && player.getGameProfile().equals(graveEntity.getGraveOwner())) {
                return super.calcBlockBreakingDelta(state, player, world, pos);
            }
        }
        return 0f;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (itemStack.hasCustomName()) {
            customName = itemStack.getName().asString();

            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof GraveBlockEntity graveBlockEntity) {
                graveBlockEntity.setCustomName(customName);
            }
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext ct) {
        Direction dir = state.get(FACING);
        return switch (dir) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> VoxelShapes.fullCube();
        };
    }
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(Properties.HORIZONTAL_FACING, context.getPlayerFacing().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GraveBlockEntity(customName, pos, state);
    }

    private boolean RetrieveItems(PlayerEntity playerEntity, World world, BlockPos pos) {
        if (world.isClient) return false;

        if (!(playerEntity instanceof ServerPlayerEntity player)) return false;

        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (!(blockEntity instanceof GraveBlockEntity graveEntity)) return false;
        graveEntity.sync();

        GameProfile graveOwner = graveEntity.getGraveOwner();

        DefaultedList<ItemStack> items = graveEntity.getStoredInventory();

        if (graveOwner == null) return false;
        if (items == null) return false;

        if (!player.getGameProfile().getId().equals(graveOwner.getId())) return false;


        if (YigdConfig.getConfig().graveSettings.dropType == DropTypeConfig.ON_GROUND) {
            List<List<ItemStack>> graveModItems = graveEntity.getModdedInventories();
            for (List<ItemStack> graveModItem : graveModItems) {
                items.addAll(graveModItem);
            }

            ItemScatterer.spawn(world, pos, items);
            world.removeBlock(pos, false);
            return true;
        }

        PlayerInventory inventory = player.getInventory();

        DefaultedList<ItemStack> retrievalInventory = DefaultedList.of();

        retrievalInventory.addAll(inventory.main);
        retrievalInventory.addAll(inventory.armor);
        retrievalInventory.addAll(inventory.offHand);

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

            boolean currentHasCurse = Yigd.hasEnchantments(bindingCurse, rArmorItem); // If retrieval armor has "curse of binding" this will return true

            // If retrieval trip armor has "curse of binding" it should stay on, and if grave armor had "curse of binding" it should end up in the inventory
            // Grave armor only gets equipped if neither last nor current armor has "curse of binding"
            if (!currentHasCurse && !Yigd.hasEnchantments(bindingCurse, gArmorItem)) {
                player.equipStack(equipmentSlot, gArmorItem); // Both armor parts are free from curse of binding and armor can be replaced
            }
        }

        player.equipStack(EquipmentSlot.OFFHAND, items.get(40)); // Replace offhand from grave

        for (int i = 0; i < mainInventory.size(); i++) { // Replace main inventory from grave
            inventory.setStack(i, mainInventory.get(i));
        }


        DefaultedList<ItemStack> extraItems = DefaultedList.of();

        UUID userId = player.getUuid();
        List<Object> modInventories = DeadPlayerData.getModdedInventories(userId);
        for (int i = 0; i < Yigd.apiMods.size(); i++) {
            YigdApi yigdApi = Yigd.apiMods.get(i);
            if (modInventories.size() >= i) {
                yigdApi.setInventory(modInventories.get(i), player);
            } else {
                extraItems.addAll(graveEntity.getModdedInventories().get(i));
            }
        }
        DeadPlayerData.dropModdedInventory(userId);

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

        if (retrievalInventory.size() > 41) extraItems.addAll(retrievalInventory.subList(41, retrievalInventory.size()));

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

        ItemScatterer.spawn(world, playerPos, dropItems);


        player.addExperience(graveEntity.getStoredXp());
        world.removeBlock(pos, false);

        DeadPlayerData.dropDeathInventory(player.getUuid());

        return true;
    }


    private List<Integer> getInventoryOpenSlots(DefaultedList<ItemStack> inventory) {
        List<Integer> openSlots = new ArrayList<>();

        for (int i = 0; i < inventory.size(); i++) {
            if(inventory.get(i) == ItemStack.EMPTY)
                openSlots.add(i);
        }

        return openSlots;
    }

    static {
        FACING = HorizontalFacingBlock.FACING;
        SHAPE_BASE_NORTH = Block.createCuboidShape(0.0f, 0.0f, 0.0f, 16.0f, 1.0f, 16.0f);
        SHAPE_FOOT_NORTH = Block.createCuboidShape(2.0f, 1.0f, 10.0f, 14.0f, 3.0f, 15.0f);
        SHAPE_CORE_NORTH = Block.createCuboidShape(3.0f, 3.0f, 11.0f, 13.0f, 14.0f, 14.0f);
        SHAPE_TOP_NORTH = Block.createCuboidShape(4.0f, 14.0f, 11.0f, 12.0f, 15.0f, 14.0f);

        SHAPE_BASE_EAST = Block.createCuboidShape(0.0f, 0.0f, 0.0f, 16.0f, 1.0f, 16.0f);
        SHAPE_FOOT_EAST = Block.createCuboidShape(1.0f, 1.0f, 2.0f, 6.0f, 3.0f, 14.0f);
        SHAPE_CORE_EAST = Block.createCuboidShape(2.0f, 3.0f, 3.0f, 5.0f, 14.0f, 13.0f);
        SHAPE_TOP_EAST = Block.createCuboidShape(2.0f, 14.0f, 4.0f, 5.0f, 15.0f, 12.0f);

        SHAPE_BASE_WEST = Block.createCuboidShape(0.0f, 0.0f, 0.0f, 16.0f, 1.0f, 16.0f);
        SHAPE_FOOT_WEST = Block.createCuboidShape(10.0f, 1.0f, 2.0f, 15.0f, 3.0f, 14.0f);
        SHAPE_CORE_WEST = Block.createCuboidShape(11.0f, 3.0f, 3.0f, 14.0f, 14.0f, 13.0f);
        SHAPE_TOP_WEST = Block.createCuboidShape(11.0f, 14.0f, 4.0f, 14.0f, 15.0f, 12.0f);

        SHAPE_BASE_SOUTH = Block.createCuboidShape(0.0f, 0.0f, 0.0f, 16.0f, 1.0f, 16.0f);
        SHAPE_FOOT_SOUTH = Block.createCuboidShape(2.0f, 1.0f, 1.0f, 14.0f, 3.0f, 6.0f);
        SHAPE_CORE_SOUTH = Block.createCuboidShape(3.0f, 3.0f, 2.0f, 13.0f, 14.0f, 5.0f);
        SHAPE_TOP_SOUTH = Block.createCuboidShape(4.0f, 14.0f, 2.0f, 12.0f, 15.0f, 5.0f);

        SHAPE_NORTH = VoxelShapes.union(SHAPE_BASE_NORTH, SHAPE_FOOT_NORTH, SHAPE_CORE_NORTH, SHAPE_TOP_NORTH);
        SHAPE_WEST = VoxelShapes.union(SHAPE_BASE_WEST, SHAPE_FOOT_WEST, SHAPE_CORE_WEST, SHAPE_TOP_WEST);
        SHAPE_EAST = VoxelShapes.union(SHAPE_BASE_EAST, SHAPE_FOOT_EAST, SHAPE_CORE_EAST, SHAPE_TOP_EAST);
        SHAPE_SOUTH = VoxelShapes.union(SHAPE_BASE_SOUTH, SHAPE_FOOT_SOUTH, SHAPE_CORE_SOUTH, SHAPE_TOP_SOUTH);
    }
}
