package com.b1n4ry.yigd.block;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.block.entity.GraveBlockEntity;
import com.b1n4ry.yigd.config.DropTypeConfig;
import com.b1n4ry.yigd.config.RetrievalTypeConfig;
import com.b1n4ry.yigd.config.YigdConfig;
import com.b1n4ry.yigd.core.GraveHelper;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
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
import net.minecraft.world.WorldAccess;
import net.minecraft.world.tick.OrderedTick;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
public class GraveBlock extends BlockWithEntity implements BlockEntityProvider, Waterloggable {
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

    protected static final BooleanProperty WATERLOGGED;

    private String customName = null;


    public GraveBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH).with(Properties.WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING, WATERLOGGED);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        RetrievalTypeConfig retrievalType = YigdConfig.getConfig().graveSettings.retrievalType;
        if (retrievalType == RetrievalTypeConfig.ON_USE || retrievalType == null) {
            RetrieveItems(player, world, pos);
            return ActionResult.SUCCESS;
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
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity be, ItemStack stack) {
        if (YigdConfig.getConfig().graveSettings.retrievalType == RetrievalTypeConfig.ON_BREAK) {
            if (RetrieveItems(player, world, pos, be)) return;
        }

        boolean bs = world.setBlockState(pos, state);
        if (bs) {
            world.addBlockEntity(be);
        } else {
            System.out.println("[YIGD] Did not manage to safely replace grave at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ". Items were deleted ;(");
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        System.out.println("[YIGD] Grave at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " was replaced with " + newState.getBlock());
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof GraveBlockEntity graveEntity) {
            YigdConfig.GraveRobbing graveRobbing = YigdConfig.getConfig().graveSettings.graveRobbing;
            boolean canRobGrave = graveRobbing.enableRobbing && (!graveRobbing.onlyMurderer || graveEntity.getKiller() == player.getUuid());
            boolean timePassed = graveEntity.age > graveRobbing.afterTime * graveRobbing.timeType.tickFactor();
            if ((YigdConfig.getConfig().graveSettings.retrievalType == RetrievalTypeConfig.ON_BREAK && (player.getGameProfile().equals(graveEntity.getGraveOwner()) || (canRobGrave && timePassed))) || graveEntity.getGraveOwner() == null) {
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

    private VoxelShape getShape(Direction dir) {
        return switch (dir) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> VoxelShapes.fullCube();
        };
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext ct) {
        Direction dir = state.get(FACING);
        return getShape(dir);
    }
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockPos blockPos = context.getBlockPos();
        FluidState fluidState = context.getWorld().getFluidState(blockPos);
        return this.getDefaultState().with(Properties.HORIZONTAL_FACING, context.getPlayerFacing().getOpposite()).with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.getFluidTickScheduler().scheduleTick(OrderedTick.create(Fluids.WATER, pos));
        }

        return direction.getAxis().isHorizontal() ? state : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, Yigd.GRAVE_BLOCK_ENTITY, GraveBlockEntity::tick);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GraveBlockEntity(customName, pos, state);
    }
    private void RetrieveItems(PlayerEntity player, World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        RetrieveItems(player, world, pos, blockEntity);
    }
    private boolean RetrieveItems(PlayerEntity player, World world, BlockPos pos, BlockEntity blockEntity) {
        if (world.isClient) return false;

        if (!(blockEntity instanceof GraveBlockEntity graveEntity)) return false;
        if (graveEntity.getGraveOwner() == null) return false;

        GameProfile graveOwner = graveEntity.getGraveOwner();

        DefaultedList<ItemStack> items = graveEntity.getStoredInventory();
        int xp = graveEntity.getStoredXp();

        if (graveOwner == null) return false;
        if (items == null) return false;

        Map<String, Object> graveModItems = graveEntity.getModdedInventories();

        if (YigdConfig.getConfig().graveSettings.dropType == DropTypeConfig.ON_GROUND || player == null) {
            for (YigdApi yigdApi : Yigd.apiMods) {
                Object o = graveModItems.get(yigdApi.getModName());
                items.addAll(yigdApi.toStackList(o));
            }

            ItemScatterer.spawn(world, pos, items);
            world.removeBlock(pos, false);
            if (YigdConfig.getConfig().graveSettings.dropGraveBlock) {
                ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), Yigd.GRAVE_BLOCK.asItem().getDefaultStack());
            }
            return true;
        }


        YigdConfig.GraveRobbing graveRobbing = YigdConfig.getConfig().graveSettings.graveRobbing;
        boolean canRobGrave = graveRobbing.enableRobbing && (!graveRobbing.onlyMurderer || graveEntity.getKiller() == player.getUuid());
        int age = graveEntity.age;
        int requiredAge = graveRobbing.afterTime * graveRobbing.timeType.tickFactor();

        boolean isRobbing = false;
        boolean timePassed = age > requiredAge;
        if (!player.getGameProfile().getId().equals(graveOwner.getId())) {
            if (!(canRobGrave && timePassed)) {
                if (canRobGrave) {
                    List<String> timeStrings = new ArrayList<>();
                    double timeRemaining = ((double) requiredAge - age) / 20;
                    if (timeRemaining >= 3600d) {
                        timeStrings.add((int) (timeRemaining / 36000) + " hours");
                        timeRemaining %= 3600d;
                    }
                    if (timeRemaining >= 60) {
                        timeStrings.add((int) (timeRemaining / 60) + " minutes");
                        timeRemaining %= 60;
                    }
                    timeStrings.add((int) timeRemaining + " seconds");
                    player.sendMessage(Text.of("You can retrieve the items from this grave in: " + String.join(", ", timeStrings)), true);
                } else {
                    player.sendMessage(Text.of("You are not allowed to retrieve these items"), true);
                }
                return false;
            } else {
                isRobbing = true;
            }
        }

        if (YigdConfig.getConfig().graveSettings.dropGraveBlock) {
            ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), Yigd.GRAVE_BLOCK.asItem().getDefaultStack());
        }
        world.removeBlock(pos, false);

        System.out.println("[YIGD] " + player.getDisplayName().asString() + " is retrieving their grave at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
        GraveHelper.RetrieveItems(player, items, graveModItems, xp, isRobbing);
        return true;
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

        WATERLOGGED = Properties.WATERLOGGED;
    }
}
