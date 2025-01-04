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
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.ScheduledTick;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GraveBlock extends BaseEntityBlock implements EntityBlock, SimpleWaterloggedBlock {
    private static VoxelShape SHAPE_EAST;
    private static VoxelShape SHAPE_WEST;
    private static VoxelShape SHAPE_SOUTH;
    private static VoxelShape SHAPE_NORTH;

    public static final MapCodec<GraveBlock> CODEC = simpleCodec(GraveBlock::new);

    public GraveBlock(Properties settings) {
        super(settings);
            this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.WATERLOGGED);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.getBlockEntity(pos) instanceof GraveBlockEntity grave && itemStack.has(DataComponents.CUSTOM_NAME)) {
            GraveComponent graveComponent = grave.getComponent();
            if (graveComponent == null) {
                grave.setGraveText(itemStack.getDisplayName());
                grave.setChanged();
            }
        }
        super.setPlacedBy(world, pos, state, placer, itemStack);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction dir = ctx.getHorizontalDirection().getOpposite();  // Have the grave facing you, not away from you
        BlockState state = this.defaultBlockState();
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, dir).setValue(BlockStateProperties.WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            world.getFluidTicks().schedule(ScheduledTick.probe(Fluids.WATER, pos));
        }
        return direction.getAxis().isHorizontal() ? state : super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        YigdConfig.GraveRendering config = YigdConfig.getConfig().graveRendering;
        return config.useCustomFeatureRenderer ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GraveBlockEntity(pos, state);
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

        return switch (direction) {
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            case SOUTH -> SHAPE_SOUTH;
            default -> SHAPE_NORTH;
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
            return createTickerHelper(type, Yigd.GRAVE_BLOCK_ENTITY, GraveBlockEntity::tick);
    }

    @Override
    public @NotNull InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        YigdConfig config = YigdConfig.getConfig();
        InteractionHand hand = player.getUsedItemHand();  // Maybe?
        if (!(player instanceof ServerPlayer) || player instanceof FakePlayer) return InteractionResult.PASS;
        if (!world.isClientSide && world.getBlockEntity(pos) instanceof GraveBlockEntity grave) {
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

            if (config.graveConfig.persistentGraves.enabled && graveComponent.getStatus() == GraveStatus.CLAIMED && hand == InteractionHand.MAIN_HAND) {
                MutableComponent message = graveComponent.getDeathMessage().copy();

                TimePoint creationTime = graveComponent.getCreationTime();
                if (config.graveConfig.persistentGraves.showDeathDay)
                    message.append(Component.translatable("text.yigd.message.on_day", creationTime.getDay()));
                if (config.graveConfig.persistentGraves.showDeathIrlTime)
                    message.append(Component.translatable("text.yigd.message.irl_time",
                            creationTime.getMonthName(),
                            creationTime.getDate(),
                            creationTime.getYear(),
                            creationTime.getHour(config.graveConfig.persistentGraves.useAmPm),
                            creationTime.getMinute(),
                            creationTime.getTimePostfix(config.graveConfig.persistentGraves.useAmPm)
                    ));

                player.sendSystemMessage(message);
                return InteractionResult.SUCCESS;
            }

            // If it's not on the client side, player and world should safely be able to be cast into their serverside counterpart classes
            if (config.graveConfig.retrieveMethods.onClick)
                return graveComponent.claim((ServerPlayer) player, (ServerLevel) world, grave.getPreviousState(), pos, player.getItemInHand(hand));
        }
        return InteractionResult.FAIL;
    }
    private InteractionResult interactWithNonPlayerGrave(GraveBlockEntity grave, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ignoredHit) {
        if (player.isShiftKeyDown()) return InteractionResult.FAIL;

        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.PLAYER_HEAD) && stack.has(DataComponents.PROFILE)) {
            ResolvableProfile profile = stack.get(DataComponents.PROFILE);

            if (profile == null) return InteractionResult.PASS;
            grave.setGraveSkull(profile);  // Works since profile is nullable
            grave.setChanged();
            world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);

            if (!player.isCreative())
                stack.shrink(1);

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClientSide && entity instanceof ServerPlayer player) {
            YigdConfig.GraveConfig graveConfig = YigdConfig.getConfig().graveConfig;
            if (graveConfig.retrieveMethods.onStand || (graveConfig.retrieveMethods.onSneak && player.isShiftKeyDown())) {
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
                            graveComponent.claim(player, (ServerLevel) world, grave.getPreviousState(), pos, player.getMainHandItem());
                        }
                }
            }
        }

        super.stepOn(world, pos, state, entity);
    }

    @Override
    public void playerDestroy(Level world, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        YigdConfig config = YigdConfig.getConfig();
        if (!world.isClientSide && blockEntity instanceof GraveBlockEntity grave && grave.getComponent() != null && grave.getComponent().getStatus() != GraveStatus.CLAIMED) {
            if (config.graveConfig.retrieveMethods.onBreak) {
                InteractionResult claimResult = grave.getComponent().claim((ServerPlayer) player, (ServerLevel) world, grave.getPreviousState(), pos, tool);
                if (claimResult != InteractionResult.FAIL)
                    return;
            }
            world.setBlockAndUpdate(pos, state);
            Optional<GraveBlockEntity> be = world.getBlockEntity(pos, Yigd.GRAVE_BLOCK_ENTITY);
            if (be.isPresent()) {
                GraveBlockEntity graveBlockEntity = be.get();

                graveBlockEntity.setPreviousState(grave.getPreviousState());
                Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(grave.getGraveId());
                component.ifPresent(graveBlockEntity::setComponent);

                Yigd.END_OF_TICK.add(() -> {  // Required because it might take a tick for the game to realize the block is replaced
                    graveBlockEntity.setChanged();
                    world.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
                });

                return;
            }
        }
        super.playerDestroy(world, player, pos, state, blockEntity, tool);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof GraveBlockEntity grave) || !grave.isUnclaimed()
                || YigdConfig.getConfig().graveConfig.retrieveMethods.onBreak) {
            // Same calculations as done for "normal" blocks, except with the overwritten destroy speed of 0.8
            float f = 0.8f;
            int i = player.hasCorrectToolForDrops(state) ? 30 : 100;
            return player.getDestroySpeed(state) / f / (float)i;
        }
        return super.getDestroyProgress(state, player, world, pos);
    }

    public static void reloadShapeFromJson(JsonObject json) throws IllegalStateException {
        VoxelShape voxelShapeNorth = Shapes.empty();
        VoxelShape voxelShapeSouth = Shapes.empty();
        VoxelShape voxelShapeEast = Shapes.empty();
        VoxelShape voxelShapeWest = Shapes.empty();

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

            voxelShapeNorth = Shapes.or(voxelShapeNorth, Shapes.create(x1, y1, z1, x2, y2, z2));
            voxelShapeEast = Shapes.or(voxelShapeEast, Shapes.create(1 - z2, y1, x1, 1 - z1, y2, x2));
            voxelShapeSouth = Shapes.or(voxelShapeSouth, Shapes.create(1 - x2, y1, 1 - z2, 1 - x1, y2, 1 - z1));
            voxelShapeWest = Shapes.or(voxelShapeWest, Shapes.create(z1, y1, 1 - x2, z2, y2, 1 - x1));
        }

        SHAPE_NORTH = voxelShapeNorth;
        SHAPE_EAST = voxelShapeEast;
        SHAPE_SOUTH = voxelShapeSouth;
        SHAPE_WEST = voxelShapeWest;
    }

    static {
        VoxelShape bottom = Shapes.create(0, 0, 0, 1, 1D / 16D, 1);
        VoxelShape supportEast = Shapes.create(1D / 16D, 1D / 16D, 2D / 16D, 6D / 16D, 3D / 16D, 14D / 16D);
        VoxelShape bustEast = Shapes.create(2D / 16D, 3D / 16D, 3D / 16D, 5D / 16D, 15D / 16D, 13D / 16D);
        VoxelShape topEast = Shapes.create(2D / 16D, 15D / 16D, 4D / 16D, 5D / 16D, 1, 12D / 16D);

        VoxelShape supportWest = Shapes.create(10D / 16D, 1D / 16D, 2D / 16D, 15D / 16D, 3D / 16D, 14D / 16D);
        VoxelShape bustWest = Shapes.create(11D / 16D, 3D / 16D, 3D / 16D, 14D / 16D, 15D / 16D, 13D / 16D);
        VoxelShape topWest = Shapes.create(11D / 16D, 15D / 16D, 4D / 16D, 14D / 16D, 1, 12D / 16D);

        VoxelShape supportSouth = Shapes.create(2D / 16D, 1D / 16D, 1D / 16D, 14D / 16D, 3D / 16D, 6D / 16D);
        VoxelShape bustSouth = Shapes.create(3D / 16D, 3D / 16D, 2D / 16D, 13D / 16D, 15D / 16D, 5D / 16D);
        VoxelShape topSouth = Shapes.create(4D / 16D, 15D / 16D, 2D / 16D, 12D / 16D, 1, 5D / 16D);

        VoxelShape supportNorth = Shapes.create(2D / 16D, 1D / 16D, 10D / 16D, 14D / 16D, 3D / 16D, 15D / 16D);
        VoxelShape bustNorth = Shapes.create(3D / 16D, 3D / 16D, 11D / 16D, 13D / 16D, 15D / 16D, 14D / 16D);
        VoxelShape topNorth = Shapes.create(4D / 16D, 15D / 16D, 11D / 16D, 12D / 16D, 1, 14D / 16D);

        SHAPE_EAST = Shapes.or(bottom, supportEast, bustEast, topEast);
        SHAPE_WEST = Shapes.or(bottom, supportWest, bustWest, topWest);
        SHAPE_SOUTH = Shapes.or(bottom, supportSouth, bustSouth, topSouth);
        SHAPE_NORTH = Shapes.or(bottom, supportNorth, bustNorth, topNorth);
    }
}
