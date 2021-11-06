package com.b1n4ry.yigd.block;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.block.entity.GraveBlockEntity;
import com.b1n4ry.yigd.config.RetrievalType;
import com.b1n4ry.yigd.config.YigdConfig;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import java.util.Arrays;
import java.util.List;

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
        RetrievalType retrievalType = YigdConfig.getConfig().graveSettings.retrievalType;
        if (retrievalType == RetrievalType.ON_USE || retrievalType == null) {
            RetrieveItems(player, world, pos);
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }
    @Override
    public void onSteppedOn(World world, BlockPos pos, Entity entity) {
        if (YigdConfig.getConfig().graveSettings.retrievalType == RetrievalType.ON_STAND && entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            RetrieveItems(player, world, pos);
        } else if (YigdConfig.getConfig().graveSettings.retrievalType == RetrievalType.ON_SNEAK && entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            if (player.isInSneakingPose()) {
                RetrieveItems(player, world, pos);
            }
        }

        super.onSteppedOn(world, pos, entity);
    }
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (YigdConfig.getConfig().graveSettings.retrievalType == RetrievalType.ON_BREAK) {
            if (RetrieveItems(player, world, pos)) return;
        }

        super.onBreak(world, pos, state, player);
    }
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof GraveBlockEntity) {
            GraveBlockEntity graveEntity = (GraveBlockEntity) blockEntity;

            if (YigdConfig.getConfig().graveSettings.retrievalType == RetrievalType.ON_BREAK && player.getGameProfile().equals(graveEntity.getGraveOwner())) {
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
            if (blockEntity != null && blockEntity instanceof GraveBlockEntity) {
                GraveBlockEntity graveBlockEntity = (GraveBlockEntity) blockEntity;

                graveBlockEntity.setCustomName(customName);
            }
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext ct) {
        Direction dir = state.get(FACING);
        switch(dir) {
            case NORTH:
                return SHAPE_NORTH;
            case SOUTH:
                return SHAPE_SOUTH;
            case EAST:
                return SHAPE_EAST;
            case WEST:
                return SHAPE_WEST;
            default:
                return VoxelShapes.fullCube();
        }
    }
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(Properties.HORIZONTAL_FACING, context.getPlayerFacing().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new GraveBlockEntity(customName);
    }

    private boolean RetrieveItems(PlayerEntity player, World world, BlockPos pos) {
        if (world.isClient) return false;

        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (!(blockEntity instanceof GraveBlockEntity)) return false;
        GraveBlockEntity graveEntity = (GraveBlockEntity)blockEntity;
        graveEntity.sync();


        GameProfile graveOwner = graveEntity.getGraveOwner();

        DefaultedList<ItemStack> inventory = graveEntity.getStoredInventory();

        if (graveOwner == null) return false;
        if (inventory == null) return false;

        if (!player.getGameProfile().getId().equals(graveOwner.getId())) return false;

        DefaultedList<ItemStack> items = graveEntity.getStoredInventory();
        DefaultedList<ItemStack> retrievalInventory = DefaultedList.of();

        retrievalInventory.addAll(player.inventory.main);
        retrievalInventory.addAll(player.inventory.armor);
        retrievalInventory.addAll(player.inventory.offHand);

        for (YigdApi yigdApi : Yigd.apiMods) {
            retrievalInventory.addAll(yigdApi.getInventory(player));

            yigdApi.dropAll(player);
        }


        player.inventory.clear(); // Delete all items

        List<ItemStack> armorInventory = items.subList(36, 40);
        List<ItemStack> mainInventory = items.subList(0, 36);


        List<String> bindingCurse = Arrays.asList(new String[] {"minecraft:binding_curse"});

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
            player.equip(i, mainInventory.get(i));
        }


        DefaultedList<ItemStack> extraItems = DefaultedList.of();

        List<Integer> openArmorSlots = getInventoryOpenSlots(player.inventory.armor); // Armor slots that does not have armor selected

        for(int i = 0; i < 4; i++) {
            if (openArmorSlots.contains(i)) {
                player.equipStack(EquipmentSlot.fromTypeIndex(EquipmentSlot.Type.ARMOR, i), retrievalInventory.subList(36, 40).get(i)); // Put player armor back
                extraItems.add(armorInventory.get(i));
            } else extraItems.add(retrievalInventory.subList(36, 40).get(i));
        }

        ItemStack offHandItem = player.inventory.offHand.get(0);
        if(offHandItem == ItemStack.EMPTY || offHandItem.getItem() == Items.AIR) player.equipStack(EquipmentSlot.OFFHAND, retrievalInventory.get(40));
        else extraItems.add(retrievalInventory.get(40));


        extraItems.addAll(retrievalInventory.subList(0, 36));
        if (retrievalInventory.size() > 41) extraItems.addAll(retrievalInventory.subList(41, retrievalInventory.size()));


        List<Integer> openSlots = getInventoryOpenSlots(player.inventory.main);

        int loopIterations = Math.min(openSlots.size(), extraItems.size());
        for(int i = 0; i < loopIterations; i++) {
            player.equip(openSlots.get(i), extraItems.get(i));
        }

        DefaultedList<ItemStack> dropItems = DefaultedList.of();
        dropItems.addAll(extraItems.subList(openSlots.size(), extraItems.size()));

        int inventoryOffset = 41;
        for (YigdApi yigdApi : Yigd.apiMods) {
            int inventorySize = yigdApi.getInventorySize(player);

            yigdApi.setInventory(items.subList(inventoryOffset, inventoryOffset + inventorySize), player);
            inventoryOffset += inventorySize;
        }

        BlockPos playerPos = player.getBlockPos();

        ItemScatterer.spawn(world, playerPos, dropItems);


        player.addExperience(graveEntity.getStoredXp());
        world.removeBlock(pos, false);

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
