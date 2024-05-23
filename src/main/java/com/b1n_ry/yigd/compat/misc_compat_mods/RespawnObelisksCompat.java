package com.b1n_ry.yigd.compat.misc_compat_mods;


import com.b1n_ry.yigd.components.ExpComponent;
import com.b1n_ry.yigd.components.GraveComponent;
import com.b1n_ry.yigd.components.InventoryComponent;
import com.b1n_ry.yigd.components.RespawnComponent;
import com.b1n_ry.yigd.data.DeathContext;
import com.b1n_ry.yigd.events.DelayGraveGenerationEvent;
import com.b1n_ry.yigd.util.DropRule;
import com.redpxnda.respawnobelisks.config.RespawnObelisksConfig;
import com.redpxnda.respawnobelisks.data.listener.ObeliskInteraction;
import com.redpxnda.respawnobelisks.registry.block.entity.RespawnObeliskBlockEntity;
import com.redpxnda.respawnobelisks.util.CoreUtils;
import com.redpxnda.respawnobelisks.util.ObeliskUtils;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RespawnObelisksCompat {
    private static final StatusEffect IMMORTALITY_CURSE = Registries.STATUS_EFFECT.get(new Identifier("respawnobelisks", "immortality_curse"));
    private static final Map<UUID, GraveGenerationData> GRAVE_GENERATION_DATA = new HashMap<>();

    public static void init() {
        DelayGraveGenerationEvent.EVENT.register((grave, direction, context, respawnComponent, caller) -> {
            ServerPlayerEntity player = context.player();
            boolean noDrop = player.getCommandTags().contains("respawnobelisks:no_drops_entity");
            if (noDrop) {
                return true;
            }

            if (
                    player.getSpawnPointPosition() != null &&
                    player.getServerWorld().getBlockEntity(player.getSpawnPointPosition()) instanceof RespawnObeliskBlockEntity be &&
                    CoreUtils.hasInteraction(be.getCoreInstance(), ObeliskInteraction.SAVE_INV) &&
                    be.getCharge(player) >= RespawnObelisksConfig.INSTANCE.respawnPerks.minKeepItemRadiance &&
                    (RespawnObelisksConfig.INSTANCE.respawnPerks.allowCursedItemKeeping || !player.hasStatusEffect(IMMORTALITY_CURSE))
            ) {
                InventoryComponent inventoryComponent = grave.getInventoryComponent();
                int mainSize = inventoryComponent.mainSize;
                int armorSize = inventoryComponent.armorSize;
                int offHandSize = inventoryComponent.offHandSize;

                // Save appropriate hotbar items
                inventoryComponent.handleItemPairs(mod -> mod.equals("vanilla"), (item, i, pair) -> {
                    if (item.isEmpty()) return;

                    boolean keep;
                    double chance;
                    if (i < 9) {
                        keep = RespawnObelisksConfig.INSTANCE.respawnPerks.hotbar.keepHotbar;
                        chance = RespawnObelisksConfig.INSTANCE.respawnPerks.hotbar.keepHotbarChance;
                    } else if (i < mainSize) {
                        keep = RespawnObelisksConfig.INSTANCE.respawnPerks.inventory.keepInventory;
                        chance = RespawnObelisksConfig.INSTANCE.respawnPerks.inventory.keepInventoryChance;
                    } else if (i < mainSize + armorSize) {
                        keep = RespawnObelisksConfig.INSTANCE.respawnPerks.armor.keepArmor;
                        chance = RespawnObelisksConfig.INSTANCE.respawnPerks.armor.keepArmorChance;
                    } else if (i < mainSize + armorSize + offHandSize) {
                        keep = RespawnObelisksConfig.INSTANCE.respawnPerks.offhand.keepOffhand;
                        chance = RespawnObelisksConfig.INSTANCE.respawnPerks.offhand.keepOffhandChance;
                    } else {
                        keep = RespawnObelisksConfig.INSTANCE.respawnPerks.inventory.keepInventory;
                        chance = RespawnObelisksConfig.INSTANCE.respawnPerks.inventory.keepInventoryChance;
                    }

                    if (ObeliskUtils.shouldSaveItem(keep, chance, item)) {
                        pair.setRight(DropRule.KEEP);
                    }
                });

                // Save trinkets
                inventoryComponent.handleItemPairs(mod -> mod.equals("trinkets"), (item, i, pair) -> {
                    if (ObeliskUtils.shouldSaveItem(RespawnObelisksConfig.INSTANCE.respawnPerks.armor.keepArmor, RespawnObelisksConfig.INSTANCE.respawnPerks.armor.keepArmorChance, item)) {
                        pair.setRight(DropRule.KEEP);
                    }
                });

                // Save experience
                if (RespawnObelisksConfig.INSTANCE.respawnPerks.experience.keepExperience) changeExp:{
                    ExpComponent graveXp = grave.getExpComponent();
                    double xp = graveXp.getOriginalXp();
                    ExpComponent expComponent = respawnComponent.getSoulboundExp();
                    if (expComponent == null)
                        break changeExp;  // Break out of if statement, if no exp component is found

                    int newStoredXp = (int) (xp * (RespawnObelisksConfig.INSTANCE.respawnPerks.experience.keepExperiencePercent / 100D));
                    expComponent.setStoredXp(newStoredXp);

                    // Make sure total XP doesn't increase
                    if (xp - newStoredXp < graveXp.getStoredXp()) {
                        graveXp.setStoredXp((int) xp - newStoredXp);
                    }
                }

                // Reset soulbound inventory to also contain items that Respawn Obelisks would keep
                respawnComponent.setSoulboundInventory(inventoryComponent.filteredInv(dropRule -> dropRule == DropRule.KEEP));
            }

            GRAVE_GENERATION_DATA.put(player.getUuid(), new GraveGenerationData(grave, direction, context, respawnComponent));

            return true;
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) return;

            GraveGenerationData data = GRAVE_GENERATION_DATA.remove(oldPlayer.getUuid());
            if (data == null) return;

            GraveComponent grave = data.grave();
            grave.generateOrDrop(data.direction(), data.context(), data.respawnComponent());
        });
    }

    private record GraveGenerationData(GraveComponent grave, Direction direction, DeathContext context,
                                       RespawnComponent respawnComponent) {}
}
