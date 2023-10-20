package com.b1n_ry.yigd.block;


import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class GraveBlock extends BlockWithEntity implements BlockEntityProvider, Waterloggable {
    private static final VoxelShape SHAPE_EAST;
    private static final VoxelShape SHAPE_WEST;
    private static final VoxelShape SHAPE_SOUTH;
    private static final VoxelShape SHAPE_NORTH;

    public GraveBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING, Properties.WATERLOGGED);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.getBlockEntity(pos) instanceof GraveBlockEntity grave && itemStack.hasCustomName()) {
            if (grave.getGraveOwner() == null)
                grave.setGraveText(itemStack.getName());
        }
        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction dir = ctx.getHorizontalPlayerFacing().getOpposite();  // Have the grave facing you, not away from you
        BlockState state = this.getDefaultState();
        return state.with(Properties.HORIZONTAL_FACING, dir);
    }

//    @Override
//    public BlockRenderType getRenderType(BlockState state) {
//        return BlockRenderType.MODEL;
//    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GraveBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction direction = state.get(Properties.HORIZONTAL_FACING);

        return switch (direction) {
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case SOUTH -> SHAPE_SOUTH;
            default -> SHAPE_NORTH;
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, Yigd.GRAVE_BLOCK_ENTITY, GraveBlockEntity::tick);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        YigdConfig config = YigdConfig.getConfig();
        if (!world.isClient && world.getBlockEntity(pos) instanceof GraveBlockEntity grave && config.graveConfig.retrieveMethods.onClick) {
            GraveComponent graveComponent = grave.getComponent();

            if (graveComponent == null) {
                // Check if it actually *is* not a personal grave, or if the component value is just missing
                UUID graveId = grave.getGraveId();
                if (graveId != null) {
                    Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(graveId);
                    if (component.isPresent())
                        graveComponent = component.get();
                }

                if (graveComponent == null)
                    // It was indeed just a normal grave, belonging to no one whatsoever
                    return ActionResult.PASS;
            }

            if (config.graveConfig.persistentGraves && graveComponent.getStatus() == GraveStatus.CLAIMED && hand == Hand.MAIN_HAND) {
                player.sendMessage(graveComponent.getDeathMessage().getDeathMessage().copy().append(" : Day " + graveComponent.getCreationTime() / 24000));
                return ActionResult.SUCCESS;
            }

            // If it's not on the client side, player and world should safely be able to be cast into their serverside counterpart classes
            return graveComponent.claim((ServerPlayerEntity) player, (ServerWorld) world, grave.getPreviousState(), pos, player.getStackInHand(hand));
        }
        return ActionResult.FAIL;
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        YigdConfig config = YigdConfig.getConfig();
        if (!world.isClient && blockEntity instanceof GraveBlockEntity grave && grave.getComponent() != null && grave.getComponent().getStatus() != GraveStatus.CLAIMED) {
            if (config.graveConfig.retrieveMethods.onBreak) {
                grave.getComponent().claim((ServerPlayerEntity) player, (ServerWorld) world, grave.getPreviousState(), pos, tool);
                return;
            } else {
                world.setBlockState(pos, state);
                Optional<GraveBlockEntity> be = world.getBlockEntity(pos, Yigd.GRAVE_BLOCK_ENTITY);
                if (be.isPresent()) {
                    GraveBlockEntity graveBlockEntity = be.get();

                    graveBlockEntity.setPreviousState(grave.getPreviousState());
                    Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(grave.getGraveId());
                    component.ifPresent(graveBlockEntity::setComponent);

                    return;
                }
            }
        }
        super.afterBreak(world, player, pos, state, blockEntity, tool);
    }

    static {
        VoxelShape bottom = VoxelShapes.cuboid(0, 0, 0, 1, 1D / 16D, 1);
        VoxelShape supportEast = VoxelShapes.cuboid(1D / 16D, 1D / 16D, 2D / 16D, 6D / 16D, 3D / 16D, 14D / 16D);
        VoxelShape bustEast = VoxelShapes.cuboid(2D / 16D, 3D / 16D, 3D / 16D, 5D / 16D, 15D / 16D, 13D / 16D);
        VoxelShape topEast = VoxelShapes.cuboid(2D / 16D, 15D / 16D, 4D / 16D, 5D / 16D, 1, 12D / 16D);

        VoxelShape supportWest = VoxelShapes.cuboid(10D / 16D, 1D / 16D, 2D / 16D, 15D / 16D, 3D / 16D, 14D / 16D);
        VoxelShape bustWest = VoxelShapes.cuboid(11D / 16D, 3D / 16D, 3D / 16D, 14D / 16D, 15D / 16D, 13D / 16D);
        VoxelShape topWest = VoxelShapes.cuboid(11D / 16D, 15D / 16D, 4D / 16D, 14D / 16D, 1, 12D / 16D);

        VoxelShape supportSouth = VoxelShapes.cuboid(2D / 16D, 1D / 16D, 1D / 16D, 14D / 16D, 3D / 16D, 6D / 16D);
        VoxelShape bustSouth = VoxelShapes.cuboid(3D / 16D, 3D / 16D, 2D / 16D, 13D / 16D, 15D / 16D, 5D / 16D);
        VoxelShape topSouth = VoxelShapes.cuboid(4D / 16D, 15D / 16D, 2D / 16D, 12D / 16D, 1, 5D / 16D);

        VoxelShape supportNorth = VoxelShapes.cuboid(2D / 16D, 1D / 16D, 10D / 16D, 14D / 16D, 3D / 16D, 15D / 16D);
        VoxelShape bustNorth = VoxelShapes.cuboid(3D / 16D, 3D / 16D, 11D / 16D, 13D / 16D, 15D / 16D, 14D / 16D);
        VoxelShape topNorth = VoxelShapes.cuboid(4D / 16D, 15D / 16D, 11D / 16D, 12D / 16D, 1, 14D / 16D);

        SHAPE_EAST = VoxelShapes.union(bottom, supportEast, bustEast, topEast);
        SHAPE_WEST = VoxelShapes.union(bottom, supportWest, bustWest, topWest);
        SHAPE_SOUTH = VoxelShapes.union(bottom, supportSouth, bustSouth, topSouth);
        SHAPE_NORTH = VoxelShapes.union(bottom, supportNorth, bustNorth, topNorth);
    }
}
