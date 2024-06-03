package com.b1n_ry.yigd.block;


import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathInfoManager;
import com.b1n_ry.yigd.data.GraveStatus;
import com.b1n_ry.yigd.data.TimePoint;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
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

import java.util.*;

public class GraveBlock extends BlockWithEntity implements BlockEntityProvider, Waterloggable {
    private static VoxelShape SHAPE_EAST;
    private static VoxelShape SHAPE_WEST;
    private static VoxelShape SHAPE_SOUTH;
    private static VoxelShape SHAPE_NORTH;

    public GraveBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH).with(Properties.WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING, Properties.WATERLOGGED);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.getBlockEntity(pos) instanceof GraveBlockEntity grave && itemStack.hasCustomName()) {
            if (grave.getComponent() == null) {
                grave.setGraveText(itemStack.getName());
                grave.markDirty();
            }
        }
        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction dir = ctx.getHorizontalPlayerFacing().getOpposite();  // Have the grave facing you, not away from you
        BlockState state = this.getDefaultState();
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        return state.with(Properties.HORIZONTAL_FACING, dir).with(Properties.WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(Properties.WATERLOGGED)) {
            world.getFluidTickScheduler().scheduleTick(OrderedTick.create(Fluids.WATER, pos));
        }
        return direction.getAxis().isHorizontal() ? state : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(Properties.WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        YigdConfig.GraveRendering config = YigdConfig.getConfig().graveRendering;
        return config.useCustomFeatureRenderer ? BlockRenderType.INVISIBLE : BlockRenderType.MODEL;
    }

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
        return validateTicker(type, Yigd.GRAVE_BLOCK_ENTITY, GraveBlockEntity::tick);
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
                    return this.interactWithNonPlayerGrave(grave, state, world, pos, player, hand, hit);
            }

            if (config.graveConfig.persistentGraves.enabled && graveComponent.getStatus() == GraveStatus.CLAIMED && hand == Hand.MAIN_HAND) {
                MutableText message = graveComponent.getDeathMessage().getDeathMessage();

                TimePoint creationTime = graveComponent.getCreationTime();
                if (config.graveConfig.persistentGraves.showDeathDay)
                    message.append(Text.translatable("text.yigd.message.on_day", creationTime.getDay()));
                if (config.graveConfig.persistentGraves.showDeathIrlTime)
                    message.append(Text.translatable("text.yigd.message.irl_time",
                            creationTime.getMonthName(),
                            creationTime.getDate(),
                            creationTime.getYear(),
                            creationTime.getHour(config.graveConfig.persistentGraves.useAmPm),
                            creationTime.getMinute(),
                            creationTime.getTimePostfix(config.graveConfig.persistentGraves.useAmPm)
                    ));

                player.sendMessage(message);
                return ActionResult.SUCCESS;
            }

            // If it's not on the client side, player and world should safely be able to be cast into their serverside counterpart classes
            return graveComponent.claim((ServerPlayerEntity) player, (ServerWorld) world, grave.getPreviousState(), pos, player.getStackInHand(hand));
        }
        return ActionResult.FAIL;
    }
    private ActionResult interactWithNonPlayerGrave(GraveBlockEntity grave, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult ignoredHit) {
        if (player.isSneaking()) return ActionResult.FAIL;

        ItemStack stack = player.getStackInHand(hand);
        NbtCompound nbt = stack.getNbt();
        if (stack.isOf(Items.PLAYER_HEAD) && nbt != null) {
            GameProfile profile = SkullBlockEntity.getProfile(nbt);

            grave.setGraveSkull(profile);  // Works since profile is nullable
            grave.markDirty();
            world.updateListeners(pos, state, state, Block.NOTIFY_ALL);

            if (!player.isCreative())
                stack.decrement(1);

            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClient && entity instanceof ServerPlayerEntity player) {
            YigdConfig.GraveConfig graveConfig = YigdConfig.getConfig().graveConfig;
            if (graveConfig.retrieveMethods.onStand || (graveConfig.retrieveMethods.onSneak && player.isSneaking())) {
                if (world.getBlockEntity(pos) instanceof GraveBlockEntity grave) {
                    GraveComponent graveComponent = grave.getComponent();

                    if (graveComponent == null) {
                        // Check if it actually *is* not a personal grave, or if the component value is just missing
                        UUID graveId = grave.getGraveId();
                        if (graveId != null) {
                            Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(graveId);
                            if (component.isPresent())
                                graveComponent = component.get();
                        }
                    }

                    if (graveComponent != null)  // Check needed again
                        if (graveComponent.getStatus() != GraveStatus.CLAIMED) {
                            graveComponent.claim(player, (ServerWorld) world, grave.getPreviousState(), pos, player.getMainHandStack());
                        }
                }
            }
        }

        super.onSteppedOn(world, pos, state, entity);
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

                    Yigd.END_OF_TICK.add(() -> {  // Required because it might take a tick for the game to realize the block is replaced
                        graveBlockEntity.markDirty();
                        world.updateListeners(pos, state, state, Block.NOTIFY_ALL);
                    });

                    return;
                }
            }
        }
        super.afterBreak(world, player, pos, state, blockEntity, tool);
    }

    @Override
    @SuppressWarnings("deprecation")
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof GraveBlockEntity grave && grave.isUnclaimed()
                && !YigdConfig.getConfig().graveConfig.retrieveMethods.onBreak) {
            return 0;
        }
        return super.calcBlockBreakingDelta(state, player, world, pos);
    }

    public static void reloadShapeFromJson(JsonObject json) throws IllegalStateException {
        List<VoxelShape> voxelShapesNorth = new ArrayList<>();
        List<VoxelShape> voxelShapesSouth = new ArrayList<>();
        List<VoxelShape> voxelShapesEast = new ArrayList<>();
        List<VoxelShape> voxelShapesWest = new ArrayList<>();

        JsonArray elements = json.getAsJsonArray("elements");
        for (JsonElement element : elements) {
            JsonObject o = element.getAsJsonObject();
            JsonArray from = o.getAsJsonArray("from");
            JsonArray to = o.getAsJsonArray("to");

            double x1 = from.get(0).getAsDouble() / 16D;
            double y1 = from.get(1).getAsDouble() / 16D;
            double z1 = from.get(2).getAsDouble() / 16D;
            double x2 = to.get(0).getAsDouble() / 16D;
            double y2 = to.get(1).getAsDouble() / 16D;
            double z2 = to.get(2).getAsDouble() / 16D;

            voxelShapesNorth.add(VoxelShapes.cuboid(x1, y1, z1, x2, y2, z2));
            voxelShapesEast.add(VoxelShapes.cuboid(1 - z2, y1, x1, 1 - z1, y2, x2));
            voxelShapesSouth.add(VoxelShapes.cuboid(1 - x2, y1, 1 - z2, 1 - x1, y2, 1 - z1));
            voxelShapesWest.add(VoxelShapes.cuboid(z1, y1, 1 - x2, z2, y2, 1 - x1));
        }

        if (voxelShapesNorth.isEmpty()) return;  // This should never happen. If it does, we have a problem. Although here we just make the problem not happen
        SHAPE_NORTH = voxelShapesNorth.remove(0);
        SHAPE_EAST = voxelShapesEast.remove(0);
        SHAPE_SOUTH = voxelShapesSouth.remove(0);
        SHAPE_WEST = voxelShapesWest.remove(0);
        voxelShapesNorth.forEach(shape -> SHAPE_NORTH = VoxelShapes.union(SHAPE_NORTH, shape));
        voxelShapesEast.forEach(shape -> SHAPE_EAST = VoxelShapes.union(SHAPE_EAST, shape));
        voxelShapesSouth.forEach(shape -> SHAPE_SOUTH = VoxelShapes.union(SHAPE_SOUTH, shape));
        voxelShapesWest.forEach(shape -> SHAPE_WEST = VoxelShapes.union(SHAPE_WEST, shape));
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
