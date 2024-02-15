package com.b1n_ry.yigd;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.ExpComponent;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.data.DirectionalPos;
import com.b1n_ry.yigd.data.TranslatableDeathMessage;
import com.b1n_ry.yigd.events.AllowGraveGenerationEvent;
import com.b1n_ry.yigd.impl.ServerPlayerEntityImpl;
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

import java.util.UUID;

public class DeathHandler {
    public void onPlayerDeath(ServerPlayerEntity player, ServerWorld world, Vec3d pos, DamageSource deathSource) {
        YigdConfig config = YigdConfig.getConfig();

        UUID killerId;
        if (deathSource.getAttacker() instanceof ServerPlayerEntity killer) {
            killerId = killer.getUuid();
        } else {
            killerId = null;
        }

        DeathContext context = new DeathContext(player, world, pos, deathSource);

        RespawnComponent respawnComponent = new RespawnComponent(player);  // Will keep track of data used on respawn

        InventoryComponent inventoryComponent = new InventoryComponent(player);  // Will keep track of all items
        ExpComponent expComponent = new ExpComponent(player);  // Will keep track of XP

        InventoryComponent.clearPlayer(player);  // No use for actual inventory when inventory component is created
        ExpComponent.clearXp(player);  // No use for actual exp when exp component is created

        // Here would be an if statement for keepInventory if vanilla didn't handle keepInventory
        // There once was code here, but is no more since removing it was the easiest fix a duplication bug

        inventoryComponent.onDeath(respawnComponent, context);
        expComponent.onDeath(respawnComponent, context);


        if (config.inventoryConfig.itemLoss.enabled) {
            inventoryComponent.applyLoss();
        }

        Vec3d graveGenerationPos = !config.graveConfig.generateOnLastGroundPos ? pos : ((ServerPlayerEntityImpl) player).youre_in_grave_danger$getLastGroundPos();
        GraveComponent graveComponent = new GraveComponent(player.getGameProfile(), inventoryComponent, expComponent,
                world, graveGenerationPos.add(0D, .5D, 0D), new TranslatableDeathMessage(deathSource, player), killerId);  // Will keep track of player grave (if enabled)

        GameProfile profile = player.getGameProfile();
        graveComponent.backUp();
        respawnComponent.primeForRespawn(profile);

        // Check storage options first, in case that will lead to empty graves
        if (!config.graveConfig.storeItems) {
            inventoryComponent.dropAll(world, pos);
            graveComponent.getInventoryComponent().clear();
        }
        if (!config.graveConfig.storeXp) {
            expComponent.dropAll(world, pos);
            graveComponent.getExpComponent().clear();
        }

        if (!AllowGraveGenerationEvent.EVENT.invoker().allowGeneration(context, graveComponent)) {
            inventoryComponent.dropAll(world, pos);
            expComponent.dropAll(world, pos);
        } else {
            DirectionalPos dirGravePos = graveComponent.findGravePos(player.getHorizontalFacing());
            BlockPos gravePos = dirGravePos.pos();

            ServerWorld graveWorld = graveComponent.getWorld();
            assert graveWorld != null;  // Should never be able to be null on server

            Direction direction = dirGravePos.dir();
            boolean waterlogged = graveWorld.getFluidState(gravePos).isOf(Fluids.WATER);  // Grave generated in full water block (submerged)
            BlockState graveBlock = Yigd.GRAVE_BLOCK.getDefaultState()
                    .with(Properties.HORIZONTAL_FACING, direction)
                    .with(Properties.WATERLOGGED, waterlogged);


            // At this point is where the END_OF_TICK would be implemented, unless it wasn't already so
            Yigd.END_OF_TICK.add(() -> {
                BlockState previousState = graveWorld.getBlockState(gravePos);

                boolean placed = graveComponent.tryPlaceGraveAt(gravePos, graveBlock);
                BlockPos placedPos = graveComponent.getPos();

                if (!placed) {
                    Yigd.LOGGER.error("Failed to generate grave at X: %d, Y: %d, Z: %d, %s".formatted(placedPos.getX(), placedPos.getY(), placedPos.getZ(), graveWorld.getRegistryKey().getValue()));
                    Yigd.LOGGER.info("Dropping items on ground instead of in grave");
                    graveComponent.getInventoryComponent().dropAll(graveWorld, Vec3d.of(placedPos));
                    graveComponent.getExpComponent().dropAll(graveWorld, Vec3d.of(placedPos));
                    return;
                }

                GraveBlockEntity be = (GraveBlockEntity) graveWorld.getBlockEntity(placedPos);
                if (be == null) return;
                be.setPreviousState(previousState);
                be.setComponent(graveComponent);
            });
        }
    }
}
