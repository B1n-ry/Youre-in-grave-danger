package com.b1n_ry.yigd;

import com.b1n_ry.yigd.components.ExpComponent;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.data.TranslatableDeathMessage;
import com.b1n_ry.yigd.events.DelayGraveGenerationEvent;
import com.b1n_ry.yigd.impl.ServerPlayerEntityImpl;
import com.b1n_ry.yigd.util.DropRule;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

        // Here would be an if statement for keepInventory, if the mod didn't let vanilla handle keepInventory
        // There once was code here, but is no more since removing it was the easiest fix a duplication bug

        // Handle drop rules
        inventoryComponent.onDeath(context);

        // Set kept items as soulbound in respawn component
        InventoryComponent soulboundInventory = inventoryComponent.filteredInv(dropRule -> dropRule == DropRule.KEEP);
        respawnComponent.setSoulboundInventory(soulboundInventory);
        // Keep XP
        ExpComponent keepExp = expComponent.getSoulboundExp();
        respawnComponent.setSoulboundExp(keepExp);


        if (config.inventoryConfig.itemLoss.enabled) {
            inventoryComponent.applyLoss();
        }

        Vec3d graveGenerationPos = !config.graveConfig.generateOnLastGroundPos ? pos : ((ServerPlayerEntityImpl) player).youre_in_grave_danger$getLastGroundPos();
        GraveComponent graveComponent = new GraveComponent(player.getGameProfile(), inventoryComponent, expComponent,
                world, graveGenerationPos.add(0D, .5D, 0D), new TranslatableDeathMessage(deathSource, player), killerId);  // Will keep track of player grave (if enabled)

        GameProfile profile = player.getGameProfile();
        if (!graveComponent.isEmpty()) {
            graveComponent.backUp();
        } else {
            Yigd.LOGGER.info("Did not backup data (grave data empty)");  // There is literally no information worth saving
        }

        respawnComponent.primeForRespawn(profile);

        Direction playerDirection = player.getHorizontalFacing();

        if (!DelayGraveGenerationEvent.EVENT.invoker()
                .skipGenerationCall(graveComponent, playerDirection, context, respawnComponent, "vanilla")) {
            graveComponent.generateOrDrop(playerDirection, context, respawnComponent);
        }
    }
}
