package com.b1n4ry.yigd.mixin;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.config.YigdConfig;
import com.b1n4ry.yigd.core.DeadPlayerData;
import com.b1n4ry.yigd.core.DeathInfoManager;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    @Inject(method = "copyFrom", at = @At(value = "TAIL"))
    private void onRespawn(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        if (alive) return;

        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

        UUID userId = player.getUuid();

        if (player.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return;

        // In case some items are by mistake placed in the inventory that should not be there
        player.getInventory().clear();
        for (YigdApi yigdApi : Yigd.apiMods) {
            yigdApi.dropAll(player);
        }

        List<Object> modSoulbounds = DeadPlayerData.Soulbound.getModdedSoulbound(userId);
        DefaultedList<ItemStack> soulboundItems = DeadPlayerData.Soulbound.getSoulboundInventory(userId);

        if (soulboundItems == null && modSoulbounds == null) return;

        if (soulboundItems != null && soulboundItems.size() > 0) {
            List<ItemStack> armorInventory = soulboundItems.subList(36, 40);
            List<ItemStack> mainInventory = soulboundItems.subList(0, 36);

            for (ItemStack itemStack : armorInventory) {
                EquipmentSlot equipmentSlot = MobEntity.getPreferredEquipmentSlot(itemStack); // return EquipmentSlot

                if (itemStack.isEmpty()) continue;

                player.equipStack(equipmentSlot, itemStack);
            }

            player.equipStack(EquipmentSlot.OFFHAND, soulboundItems.get(40));

            PlayerInventory inventory = player.getInventory();
            for (int i = 0; i < Math.min(inventory.size(), mainInventory.size()); i++) { // Replace main inventory from grave
                inventory.setStack(i, mainInventory.get(i));
            }

            if (soulboundItems.size() > 41) {
                for (int i = 41; i < Math.min(soulboundItems.size(), inventory.size()); i++) {
                    inventory.setStack(i, soulboundItems.get(i));
                }
            }
        }

        // Modded soulbound doesn't work without this because of cardinal components
        Yigd.NEXT_TICK.add(() -> {
            if (modSoulbounds.size() > 0) {
                for (int i = 0; i < Math.min(Yigd.apiMods.size(), modSoulbounds.size()); i++) {
                    YigdApi yigdApi = Yigd.apiMods.get(i);
                    Object modSoulbound = modSoulbounds.get(i);

                    yigdApi.setInventory(modSoulbound, player);
                }
            }
        });

        DeadPlayerData.Soulbound.dropModdedSoulbound(userId);
        DeadPlayerData.Soulbound.dropSoulbound(userId);

        try {
            List<DeadPlayerData> deadPlayerData = DeathInfoManager.INSTANCE.data.get(userId);
            if (deadPlayerData != null && deadPlayerData.size() > 0) {
                DeadPlayerData latestDeath = deadPlayerData.get(deadPlayerData.size() - 1);
                BlockPos deathPos = latestDeath.gravePos;
                if (deathPos != null && YigdConfig.getConfig().graveSettings.tellDeathPos) {
                    player.sendMessage(Text.of("Your grave has been generated at\nX: " + deathPos.getX() + " / Y: " + deathPos.getY() + " / Z: " + deathPos.getZ() + " / " + latestDeath.dimensionName), false);
                }
            }
        }
        catch (Exception e) {
            System.out.println("[YIGD] Death data has not been generated\n" + e);
        }
    }
}