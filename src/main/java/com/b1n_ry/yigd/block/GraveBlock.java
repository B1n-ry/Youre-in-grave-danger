package com.b1n_ry.yigd.block;


import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.config.RetrieveMethod;
import com.b1n_ry.yigd.config.YigdConfig;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GraveBlock extends BlockWithEntity implements BlockEntityProvider, Waterloggable {
    private static final BooleanProperty WATERLOGGED;

    public GraveBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(Properties.HORIZONTAL_FACING, WATERLOGGED);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new GraveBlockEntity(pos, state);
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
        if (!world.isClient && world.getBlockEntity(pos) instanceof GraveBlockEntity grave && config.graveConfig.retrieveMethods.contains(RetrieveMethod.ON_CLICK)) {
            // If it's not on the client side, player and world should safely be able to be cast into their serverside counterpart classes
            return grave.getComponent().claim((ServerPlayerEntity) player, (ServerWorld) world, grave.getPreviousState(), pos, player.getStackInHand(hand));
        }
        return ActionResult.FAIL;
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        YigdConfig config = YigdConfig.getConfig();
        if (!world.isClient && blockEntity instanceof GraveBlockEntity grave && config.graveConfig.retrieveMethods.contains(RetrieveMethod.ON_BREAK)) {
            grave.getComponent().claim((ServerPlayerEntity) player, (ServerWorld) world, grave.getPreviousState(), pos, tool);
            return;
        }
        super.afterBreak(world, player, pos, state, blockEntity, tool);
    }

    static {
        WATERLOGGED = Properties.WATERLOGGED;
    }
}
