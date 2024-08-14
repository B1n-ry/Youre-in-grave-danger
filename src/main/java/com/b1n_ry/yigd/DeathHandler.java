package com.b1n_ry.yigd;


import com.b1n_ry.yigd.components.ExpComponent;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.YigdEvents;
import com.b1n_ry.yigd.util.DropRule;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import java.util.UUID;

public class DeathHandler {
    public void onPlayerDeath(ServerPlayer player, ServerLevel world, Vec3 pos, DamageSource deathSource) {
        YigdConfig config = YigdConfig.getConfig();

        UUID killerId;
        if (deathSource.getEntity() instanceof ServerPlayer killer) {
            killerId = killer.getUUID();
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

        ResolvableProfile profile = new ResolvableProfile(player.getGameProfile());
        Vec3 graveGenerationPos = !config.graveConfig.generateOnLastGroundPos ? pos : player.getData(Yigd.LAST_GROUND_POS);
        GraveComponent graveComponent = new GraveComponent(profile, inventoryComponent, expComponent,
                world, graveGenerationPos.add(0D, .5D, 0D), deathSource.getLocalizedDeathMessage(player), killerId);  // Will keep track of player grave (if enabled)

        if (graveComponent.isEmpty()) return;  // There is literally no information worth saving

        graveComponent.backUp();
        respawnComponent.primeForRespawn(profile);

        Direction playerDirection = player.getDirection();

        YigdEvents.DelayGraveGenerationEvent event = NeoForge.EVENT_BUS.post(
                new YigdEvents.DelayGraveGenerationEvent(graveComponent, playerDirection, context, respawnComponent, "vanilla"));
        if (!event.generationIsDelayed()) {
            graveComponent.generateOrDrop(playerDirection, context, respawnComponent);
        }
    }
}
