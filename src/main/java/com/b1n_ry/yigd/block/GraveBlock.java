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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
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

import java.util.Optional;
import java.util.UUID;

public class GraveBlock extends BaseEntityBlock implements EntityBlock {
    private static VoxelShape SHAPE_EAST;
    private static VoxelShape SHAPE_WEST;
    private static VoxelShape SHAPE_SOUTH;
    private static VoxelShape SHAPE_NORTH;

    public GraveBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return Yigd.GRAVE_BLOCK_CODEC.value();
    }

    @Nullable
    @Override
    public GraveBlockEntity newBlockEntity(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        return new GraveBlockEntity(blockPos, blockState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING, BlockStateProperties.WATERLOGGED);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof GraveBlockEntity grave && stack.has(DataComponents.CUSTOM_NAME)) {
            GraveComponent graveComponent = grave.getComponent();
            if (graveComponent == null) {
                grave.setGraveText(stack.getDisplayName());
                grave.setChanged();
            }
        }
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext ctx) {
        Direction dir = ctx.getHorizontalDirection().getOpposite();  // Have the grave facing you, not away from you
        BlockState state = this.defaultBlockState();
        FluidState fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, dir).setValue(BlockStateProperties.WATERLOGGED, fluidState.is(Fluids.WATER));
    }

    @Override
    protected @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.getFluidTicks().schedule(ScheduledTick.probe(Fluids.WATER, pos));
        }
        return direction.getAxis().isHorizontal() ? state : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        YigdConfig.GraveRendering config = YigdConfig.getConfig().graveRendering;
        return config.useCustomFeatureRenderer ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, Yigd.GRAVE_BLOCK_ENTITY.get(), GraveBlockEntity::tick);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        YigdConfig config = YigdConfig.getConfig();
        InteractionHand hand = player.getUsedItemHand();  // Maybe?
        if (!(player instanceof ServerPlayer)) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GraveBlockEntity grave) {
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
                    return this.interactWithNonPlayerGrave(grave, state, level, pos, player, hand, hit);
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
                return graveComponent.claim((ServerPlayer) player, (ServerLevel) level, grave.getPreviousState(), pos, player.getItemInHand(hand));
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
    public void stepOn(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            YigdConfig.GraveConfig graveConfig = YigdConfig.getConfig().graveConfig;
            if (graveConfig.retrieveMethods.onStand || (graveConfig.retrieveMethods.onSneak && player.isShiftKeyDown())) {
                if (level.getBlockEntity(pos) instanceof GraveBlockEntity grave) {
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
                            graveComponent.claim(player, (ServerLevel) level, grave.getPreviousState(), pos, player.getMainHandItem());
                        }
                }
            }
        }

        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void playerDestroy(@NotNull Level level, @NotNull Player player, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable BlockEntity blockEntity, @NotNull ItemStack tool) {
        YigdConfig config = YigdConfig.getConfig();
        if (!level.isClientSide && blockEntity instanceof GraveBlockEntity grave && grave.getComponent() != null && grave.getComponent().getStatus() != GraveStatus.CLAIMED) {
            if (config.graveConfig.retrieveMethods.onBreak) {
                InteractionResult claimResult = grave.getComponent().claim((ServerPlayer) player, (ServerLevel) level, grave.getPreviousState(), pos, tool);
                if (claimResult != InteractionResult.FAIL)
                    return;
            }
            level.setBlockAndUpdate(pos, state);
            Optional<GraveBlockEntity> be = level.getBlockEntity(pos, Yigd.GRAVE_BLOCK_ENTITY.get());
            if (be.isPresent()) {
                GraveBlockEntity graveBlockEntity = be.get();

                graveBlockEntity.setPreviousState(grave.getPreviousState());
                Optional<GraveComponent> component = DeathInfoManager.INSTANCE.getGrave(grave.getGraveId());
                component.ifPresent(graveBlockEntity::setComponent);

                Yigd.END_OF_TICK.add(() -> {  // Required because it might take a tick for the game to realize the block is replaced
                    graveBlockEntity.setChanged();
                    level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
                });

                return;
            }
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    protected float getDestroyProgress(@NotNull BlockState state, @NotNull Player player, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof GraveBlockEntity grave && grave.isUnclaimed()
                && (!YigdConfig.getConfig().graveConfig.retrieveMethods.onBreak
                || !(new ResolvableProfile(player.getGameProfile())).equals(grave.getGraveSkull()))) {
            return 0;
        }
        return super.getDestroyProgress(state, player, level, pos);
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
