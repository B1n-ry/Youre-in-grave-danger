package com.b1n_ry.yigd;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.components.ExpComponent;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
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
import net.minecraft.world.GameRules;

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
        InventoryComponent.clearPlayer(player);  // No use for actual inventory when inventory component is created

        ExpComponent expComponent = new ExpComponent(player);  // Will keep track of XP
        ExpComponent.clearXp(player);  // No use for actual exp when exp component is created

        // TODO: Replace this with an event for other things to possibly check as well
        if (world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
            respawnComponent.setSoulboundInventory(inventoryComponent);
            respawnComponent.setSoulboundExp(expComponent);
        } else {
            inventoryComponent.onDeath(respawnComponent, context);
            expComponent.onDeath(respawnComponent, context);
        }


        if (config.inventoryConfig.itemLoss.enabled) {
            inventoryComponent.applyLoss(context);
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
            BlockPos gravePos = graveComponent.findGravePos();

            Direction direction = player.getHorizontalFacing();
            boolean waterlogged = world.getFluidState(gravePos).equals(Fluids.WATER.getDefaultState());  // Grave generated in full water block (submerged)
            BlockState graveBlock = Yigd.GRAVE_BLOCK.getDefaultState()
                    .with(Properties.HORIZONTAL_FACING, direction)
                    .with(Properties.WATERLOGGED, waterlogged);


            // At this point is where the END_OF_TICK would be implemented, unless it wasn't already so
            BlockState previousState = world.getBlockState(gravePos);

            boolean placed = graveComponent.tryPlaceGraveAt(gravePos, graveBlock);

            if (!placed) {
                Yigd.LOGGER.error("Failed to generate grave at X: %d, Y: %d, Z: %d, %s".formatted(gravePos.getX(), gravePos.getY(), gravePos.getZ(), world.getRegistryKey().getValue()));
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
