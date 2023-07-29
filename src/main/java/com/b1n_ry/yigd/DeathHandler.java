package com.b1n_ry.yigd;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.ExpComponent;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.data.TranslatableDeathMessage;
import com.mojang.authlib.GameProfile;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class DeathHandler {
    public void onPlayerDeath(ServerPlayerEntity player, ServerWorld world, Vec3d pos, DamageSource deathSource) {
        YigdConfig config = YigdConfig.getConfig();

        DeathContext context = new DeathContext(player, world, pos, deathSource);

        RespawnComponent respawnComponent = new RespawnComponent();  // Will keep track of data used on respawn

        InventoryComponent inventoryComponent = new InventoryComponent(player);  // Will keep track of all items
        InventoryComponent.clearPlayer(player);  // No use for actual inventory when inventory component is created
        inventoryComponent.onDeath(respawnComponent, context);

        ExpComponent expComponent = new ExpComponent(player);  // Will keep track of XP
        ExpComponent.clearXp(player);  // No use for actual exp when exp component is created
        expComponent.onDeath(respawnComponent, context);

        if (config.inventoryConfig.itemLoss.enabled) {
            inventoryComponent.applyLoss(context);
        }

        GraveComponent graveComponent = new GraveComponent(player.getGameProfile(), inventoryComponent, expComponent,
                world, pos, new TranslatableDeathMessage(deathSource, player));  // Will keep track of player grave (if enabled)

        GameProfile profile = player.getGameProfile();
        graveComponent.backUp();
        respawnComponent.primeForRespawn(profile);


        if (!graveComponent.shouldGenerate(config, deathSource)) {
            inventoryComponent.dropAll(world, pos);
            expComponent.dropAll(world, pos);
        } else {
            BlockPos gravePos = graveComponent.findGravePos();

            Direction direction = player.getHorizontalFacing();
            boolean waterlogged = world.getFluidState(gravePos).equals(Fluids.WATER.getDefaultState());  // Grave generated in full water block (submerged)
            BlockState graveBlock = Yigd.GRAVE_BLOCK.getDefaultState()
                    .with(Properties.HORIZONTAL_FACING, direction)
                    .with(Properties.WATERLOGGED, waterlogged);

            BlockState previousState = world.getBlockState(gravePos);

            boolean placed = graveComponent.tryPlaceGraveAt(gravePos, graveBlock);

            if (!placed) {
                Yigd.LOGGER.error("Failed to generate grave at X: %s, Y: %s, Z: %s, %s".formatted(gravePos.getX(), gravePos.getY(), gravePos.getZ(), world.getRegistryKey().getValue()));
                Yigd.LOGGER.info("Dropping items on ground instead of in grave");
                graveComponent.getInventoryComponent().dropAll(world, Vec3d.of(gravePos));
                graveComponent.getExpComponent().dropAll(world, Vec3d.of(gravePos));
                return;
            }

            GraveBlockEntity be = (GraveBlockEntity) world.getBlockEntity(gravePos);
            if (be == null) return;
            be.setPreviousState(previousState);
            be.setComponent(graveComponent);
        }
    }
}
