package com.b1n_ry.yigd.block;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.DropTypeConfig;
import com.b1n_ry.yigd.config.RetrievalTypeConfig;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.DeadPlayerData;
import com.b1n_ry.yigd.core.DeathInfoManager;
import com.b1n_ry.yigd.core.GraveHelper;
import com.b1n_ry.yigd.item.KeyItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.message.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.tick.OrderedTick;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("deprecation")
public class GraveBlock extends BlockWithEntity implements BlockEntityProvider, Waterloggable {
    public static JsonObject customModel;

    public static final DirectionProperty FACING;
    protected static VoxelShape SHAPE_NORTH;
    protected static final VoxelShape SHAPE_BASE_NORTH;
    protected static final VoxelShape SHAPE_FOOT_NORTH;
    protected static final VoxelShape SHAPE_CORE_NORTH;
    protected static final VoxelShape SHAPE_TOP_NORTH;
    protected static VoxelShape SHAPE_WEST;
    protected static final VoxelShape SHAPE_BASE_WEST;
    protected static final VoxelShape SHAPE_FOOT_WEST;
    protected static final VoxelShape SHAPE_CORE_WEST;
    protected static final VoxelShape SHAPE_TOP_WEST;
    protected static VoxelShape SHAPE_EAST;
    protected static final VoxelShape SHAPE_BASE_EAST;
    protected static final VoxelShape SHAPE_FOOT_EAST;
    protected static final VoxelShape SHAPE_CORE_EAST;
    protected static final VoxelShape SHAPE_TOP_EAST;
    protected static VoxelShape SHAPE_SOUTH;
    protected static final VoxelShape SHAPE_BASE_SOUTH;
    protected static final VoxelShape SHAPE_FOOT_SOUTH;
    protected static final VoxelShape SHAPE_CORE_SOUTH;
    protected static final VoxelShape SHAPE_TOP_SOUTH;

    protected static final BooleanProperty WATERLOGGED;

    private String customName = null;


    public GraveBlock(Settings settings) {
        super(settings.nonOpaque());
        this.setDefaultState(this.stateManager.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH).with(Properties.WATERLOGGED, false));
    }

    public static void reloadVoxelShapes(JsonObject graveModel) {
        if (graveModel == null) return;

        JsonElement shapesArray = graveModel.get("elements");
        if (shapesArray == null || !shapesArray.isJsonArray()) return;

        VoxelShape groundShapeNorth = Block.createCuboidShape(0, 0, 0, 0, 0, 0);
        VoxelShape groundShapeWest = Block.createCuboidShape(0, 0, 0, 0, 0, 0);
        VoxelShape groundShapeSouth = Block.createCuboidShape(0, 0, 0, 0, 0, 0);
        VoxelShape groundShapeEast = Block.createCuboidShape(0, 0, 0, 0, 0, 0);
        List<VoxelShape> shapesNorth = new ArrayList<>();
        List<VoxelShape> shapesWest = new ArrayList<>();
        List<VoxelShape> shapesSouth = new ArrayList<>();
        List<VoxelShape> shapesEast = new ArrayList<>();
        for (JsonElement e : (JsonArray) shapesArray) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();

            if (o.get("name") != null && o.get("name").getAsString().equals("Base_Layer")) {
                Map<String, VoxelShape> groundShapes = getShapeFromJson(o);
                groundShapeNorth = groundShapes.get("NORTH");
                groundShapeWest = groundShapes.get("WEST");
                groundShapeSouth = groundShapes.get("SOUTH");
                groundShapeEast = groundShapes.get("EAST");
            } else {
                Map<String, VoxelShape> directionShapes = getShapeFromJson(o);
                shapesNorth.add(directionShapes.get("NORTH"));
                shapesWest.add(directionShapes.get("WEST"));
                shapesSouth.add(directionShapes.get("SOUTH"));
                shapesEast.add(directionShapes.get("EAST"));
            }
        }

        SHAPE_NORTH = VoxelShapes.union(groundShapeNorth, shapesNorth.toArray(new VoxelShape[0]));
        SHAPE_WEST = VoxelShapes.union(groundShapeWest, shapesWest.toArray(new VoxelShape[0]));
        SHAPE_SOUTH = VoxelShapes.union(groundShapeSouth, shapesSouth.toArray(new VoxelShape[0]));
        SHAPE_EAST = VoxelShapes.union(groundShapeEast, shapesEast.toArray(new VoxelShape[0]));
    }

    private static Map<String, VoxelShape> getShapeFromJson(JsonObject object) {
        Map<String, VoxelShape> shapes = new HashMap<>();

        JsonArray from = object.get("from").getAsJsonArray();
        float x1 = from.get(0).getAsFloat();
        float y1 = from.get(1).getAsFloat();
        float z1 = from.get(2).getAsFloat();

        JsonArray to = object.getAsJsonArray("to").getAsJsonArray();
        float x2 = to.get(0).getAsFloat();
        float y2 = to.get(1).getAsFloat();
        float z2 = to.get(2).getAsFloat();

        shapes.put("NORTH", Block.createCuboidShape(x1, y1, z1, x2, y2, z2));
        shapes.put("EAST", Block.createCuboidShape(16 - z2, y1, x1, 16 - z1, y2, x2));
        shapes.put("SOUTH", Block.createCuboidShape(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1));
        shapes.put("WEST", Block.createCuboidShape(z1, y1, 16 - x2, z2, y2, 16 - x1));

        return shapes;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        if (YigdConfig.getConfig().graveSettings.graveRenderSettings.useRenderFeatures) return BlockRenderType.INVISIBLE;
        return BlockRenderType.MODEL;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING, WATERLOGGED);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        RetrievalTypeConfig retrievalType = YigdConfig.getConfig().graveSettings.retrievalType;
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof GraveBlockEntity grave)) return super.onUse(state, world, pos, player, hand, hit);

        if ((retrievalType == RetrievalTypeConfig.ON_USE || retrievalType == null) && grave.getGraveOwner() != null) {
            RetrieveItems(player, world, pos);
            return ActionResult.SUCCESS;
        }

        ItemStack heldItem = player.getStackInHand(hand);
        if (heldItem.getItem() == Items.PLAYER_HEAD) {
            NbtCompound nbt = heldItem.getNbt();
            if (nbt != null) {
                GameProfile gameProfile = null;
                if (nbt.contains("SkullOwner", NbtElement.COMPOUND_TYPE)) {
                    gameProfile = NbtHelper.toGameProfile(nbt.getCompound("SkullOwner"));
                } else if (nbt.contains("SkullOwner", NbtElement.STRING_TYPE) && !StringUtils.isBlank(nbt.getString("SkullOwner"))) {
                    gameProfile = new GameProfile(null, nbt.getString("SkullOwner"));
                }

                // Set skull and decrease count of items
                if (gameProfile != null) {
                    if (grave.getGraveSkull() != null) {
                        grave.dropCosmeticSkull();
                    }

                    grave.setGraveSkull(gameProfile);
                    heldItem.decrement(1);

                    return ActionResult.SUCCESS;
                }
            }
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
        if (!(be instanceof GraveBlockEntity graveBlockEntity) || graveBlockEntity.getGraveOwner() == null) {
            super.afterBreak(world, player, pos, state, be, stack);
            return;
        }

        if (YigdConfig.getConfig().graveSettings.retrievalType == RetrievalTypeConfig.ON_BREAK) {
            if (RetrieveItems(player, world, pos, be)) return;
        }

        boolean bs = world.setBlockState(pos, state);
        if (bs) {
            world.addBlockEntity(be);
        } else {
            Yigd.LOGGER.warn("Did not manage to safely replace grave data at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ". If grave contained items they've been deleted as no data was found");
        }
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        Yigd.LOGGER.info("Grave at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " was replaced with " + newState.getBlock());
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof GraveBlockEntity graveEntity) {
            YigdConfig.GraveSettings config = YigdConfig.getConfig().graveSettings;
            YigdConfig.GraveRobbing graveRobbing = config.graveRobbing;
            boolean canRobGrave = graveRobbing.enableRobbing && (!graveRobbing.onlyMurderer || graveEntity.getKiller() == player.getUuid());
            if ((config.retrievalType == RetrievalTypeConfig.ON_BREAK && (player.getGameProfile().equals(graveEntity.getGraveOwner()) || canRobGrave)) || graveEntity.getGraveOwner() == null) {
                return super.calcBlockBreakingDelta(state, player, world, pos);
            }
        }
        return 0f;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if (itemStack.hasCustomName()) {
            customName = itemStack.getName().getString();

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
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        return false;
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
        if (player == null || player.isDead()) return false;

        if (!(blockEntity instanceof GraveBlockEntity graveEntity)) return false;

        GameProfile graveOwner = graveEntity.getGraveOwner();

        if (graveOwner == null) return false;
        if (graveEntity.getGraveOwner() != null && graveEntity.age < 20) {
            ((ServerPlayerEntity) player).sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.too_fast")), MessageType.SYSTEM);
            return false;
        }

        int xp = graveEntity.getStoredXp();

        DefaultedList<ItemStack> items = graveEntity.getStoredInventory();

        if (items == null) return false;

        YigdConfig config = YigdConfig.getConfig();
        YigdConfig.GraveRobbing graveRobbing = config.graveSettings.graveRobbing;
        boolean canRobGrave = graveRobbing.enableRobbing && (!graveRobbing.onlyMurderer || graveEntity.getKiller() == player.getUuid());
        canRobGrave = canRobGrave || (config.graveSettings.unlockableGraves && DeathInfoManager.INSTANCE.unlockedGraves.contains(graveEntity.getGraveId()));
        int age = graveEntity.age;
        int requiredAge = graveRobbing.afterTime * graveRobbing.timeType.tickFactor();

        boolean isRobbing = false;
        boolean timePassed = age > requiredAge;
        boolean isGraveOwner = player.getGameProfile().getId().equals(graveOwner.getId());
        if (!isGraveOwner) {
            if (!(canRobGrave && timePassed)) {
                if (config.utilitySettings.graveKeySettings.enableKeys) {
                    ItemStack heldStack = player.getMainHandStack();
                    isRobbing = KeyItem.isKeyForGrave(heldStack, graveEntity);
                }
                if (canRobGrave && !isRobbing) {
                    double timeRemaining = ((double) requiredAge - age) / 20;

                    int hours = (int) (timeRemaining / 3600);
                    timeRemaining %= 3600;

                    int minutes = (int) (timeRemaining / 60);
                    timeRemaining %= 60;

                    int seconds = (int) timeRemaining;

                    player.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.retrieve.rob_cooldown", hours, minutes, seconds)), true);
                } else {
                    player.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.retrieve.missing_permission")), true);
                }
                if (!isRobbing) return false;
            } else {
                isRobbing = true;
            }
        } else if(config.utilitySettings.graveKeySettings.alwaysRequire) {
            ItemStack stack = player.getMainHandStack();
            if (!KeyItem.isKeyForGrave(stack, graveEntity)) {
                ((ServerPlayerEntity) player).sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.retrieve.missing_key")), MessageType.SYSTEM);
                return false;
            }
        }

        Map<String, Object> graveModItems = graveEntity.getModdedInventories();

        DeadPlayerData data = DeathInfoManager.findUserGrave(graveOwner.getId(), graveEntity.getGraveId());
        if (data != null) {
            data.availability = 0;
            DeathInfoManager.INSTANCE.markDirty();
        } else {
            Yigd.LOGGER.warn("Tried to change status of grave for %s (%s) at %s, but grave was not found".formatted(graveOwner.getName(), graveOwner.getId(), pos));
        }

        if (config.graveSettings.dropType == DropTypeConfig.ON_GROUND) {
            for (YigdApi yigdApi : Yigd.apiMods) {
                Object o = graveModItems.get(yigdApi.getModName());
                items.addAll(yigdApi.toStackList(o));
            }

            if (world instanceof ServerWorld sWorld) ExperienceOrbEntity.spawn(sWorld, Vec3d.of(pos), graveEntity.getStoredXp());
            ItemScatterer.spawn(world, pos, items);
            if (config.graveSettings.dropGraveBlock) {
                ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), Yigd.GRAVE_BLOCK.asItem().getDefaultStack());
            }
            if (config.graveSettings.replaceWhenClaimed) {
                world.setBlockState(pos, graveEntity.getPreviousState());
            } else {
                world.removeBlock(pos, false);
            }
            return true;
        }

        if (config.graveSettings.dropGraveBlock) {
            ItemScatterer.spawn(world, pos.getX(), pos.getY(), pos.getZ(), Yigd.GRAVE_BLOCK.asItem().getDefaultStack());
        }
        if (config.graveSettings.replaceWhenClaimed) {
            world.setBlockState(pos, graveEntity.getPreviousState());
        } else {
            world.removeBlock(pos, false);
        }

        Yigd.LOGGER.info(player.getDisplayName().getString() + " is retrieving " + (isRobbing ? "someone else's" : "their") + " grave at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
        MinecraftServer server = world.getServer();
        if (isRobbing && server != null) {
            UUID playerId = graveOwner.getId();
            ServerPlayerEntity robbedPlayer = server.getPlayerManager().getPlayer(playerId);
            if (robbedPlayer != null) {
                robbedPlayer.sendMessage(MutableText.of(new TranslatableTextContent("text.yigd.message.robbed")), false);
            } else {
                Yigd.notNotifiedRobberies.add(playerId);
            }
        }
        GraveHelper.RetrieveItems(player, items, graveModItems, xp, isRobbing);

        return true;
    }

    static {
        FACING = HorizontalFacingBlock.FACING;

        SHAPE_BASE_NORTH = Block.createCuboidShape(0.0f, 0.0f, 0.0f, 16.0f, 1.0f, 16.0f);
        SHAPE_FOOT_NORTH = Block.createCuboidShape(2.0f, 1.0f, 10.0f, 14.0f, 3.0f, 15.0f);
        SHAPE_CORE_NORTH = Block.createCuboidShape(3.0f, 3.0f, 11.0f, 13.0f, 15.0f, 14.0f);
        SHAPE_TOP_NORTH = Block.createCuboidShape(4.0f, 15.0f, 11.0f, 12.0f, 16.0f, 14.0f);

        SHAPE_BASE_EAST = Block.createCuboidShape(0.0f, 0.0f, 0.0f, 16.0f, 1.0f, 16.0f);
        SHAPE_FOOT_EAST = Block.createCuboidShape(1.0f, 1.0f, 2.0f, 6.0f, 3.0f, 14.0f);
        SHAPE_CORE_EAST = Block.createCuboidShape(2.0f, 3.0f, 3.0f, 5.0f, 15.0f, 13.0f);
        SHAPE_TOP_EAST = Block.createCuboidShape(2.0f, 15.0f, 4.0f, 5.0f, 16.0f, 12.0f);

        SHAPE_BASE_WEST = Block.createCuboidShape(0.0f, 0.0f, 0.0f, 16.0f, 1.0f, 16.0f);
        SHAPE_FOOT_WEST = Block.createCuboidShape(10.0f, 1.0f, 2.0f, 15.0f, 3.0f, 14.0f);
        SHAPE_CORE_WEST = Block.createCuboidShape(11.0f, 3.0f, 3.0f, 14.0f, 15.0f, 13.0f);
        SHAPE_TOP_WEST = Block.createCuboidShape(11.0f, 15.0f, 4.0f, 14.0f, 16.0f, 12.0f);

        SHAPE_BASE_SOUTH = Block.createCuboidShape(0.0f, 0.0f, 0.0f, 16.0f, 1.0f, 16.0f);
        SHAPE_FOOT_SOUTH = Block.createCuboidShape(2.0f, 1.0f, 1.0f, 14.0f, 3.0f, 6.0f);
        SHAPE_CORE_SOUTH = Block.createCuboidShape(3.0f, 3.0f, 2.0f, 13.0f, 15.0f, 5.0f);
        SHAPE_TOP_SOUTH = Block.createCuboidShape(4.0f, 15.0f, 2.0f, 12.0f, 16.0f, 5.0f);

        SHAPE_NORTH = VoxelShapes.union(SHAPE_BASE_NORTH, SHAPE_FOOT_NORTH, SHAPE_CORE_NORTH, SHAPE_TOP_NORTH);
        SHAPE_WEST = VoxelShapes.union(SHAPE_BASE_WEST, SHAPE_FOOT_WEST, SHAPE_CORE_WEST, SHAPE_TOP_WEST);
        SHAPE_EAST = VoxelShapes.union(SHAPE_BASE_EAST, SHAPE_FOOT_EAST, SHAPE_CORE_EAST, SHAPE_TOP_EAST);
        SHAPE_SOUTH = VoxelShapes.union(SHAPE_BASE_SOUTH, SHAPE_FOOT_SOUTH, SHAPE_CORE_SOUTH, SHAPE_TOP_SOUTH);

        WATERLOGGED = Properties.WATERLOGGED;
    }
}
