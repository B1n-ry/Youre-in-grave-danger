package com.b1n4ry.yigd.mixin;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.core.DeadPlayerData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;
import java.util.UUID;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    @ModifyArg(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;isSpaceEmpty(Lnet/minecraft/entity/Entity;)Z"))
    private Entity equipSoulboundItems(Entity entity) {
        ServerPlayerEntity player = (ServerPlayerEntity) entity;

        UUID userId = player.getUuid();

        if (player.getServerWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return player;

        DefaultedList<ItemStack> soulboundItems = DeadPlayerData.getSoulboundInventory(userId);

        if (soulboundItems == null) return player;
        if (soulboundItems.size() == 0) return player;

        List<ItemStack> armorInventory = soulboundItems.subList(36, 40);
        List<ItemStack> mainInventory = soulboundItems.subList(0, 36);

        for (ItemStack itemStack : armorInventory) {
            EquipmentSlot equipmentSlot = MobEntity.getPreferredEquipmentSlot(itemStack); // return EquipmentSlot

            if (itemStack.isEmpty()) continue;

            player.equipStack(equipmentSlot, itemStack);
        }

        player.equipStack(EquipmentSlot.OFFHAND, soulboundItems.get(40));

        for (int i = 0; i < mainInventory.size(); i++) { // Replace main inventory from grave
            player.getInventory().setStack(i, mainInventory.get(i));
        }

        if (soulboundItems.size() > 41) {
            int inventoryOffset = 41;
            for (YigdApi yigdApi : Yigd.apiMods) {
                int inventorySize = yigdApi.getInventorySize(player);

                yigdApi.setInventory(soulboundItems.subList(inventoryOffset, inventoryOffset + inventorySize), player);
                inventoryOffset += inventorySize;
            }
        }

        DeadPlayerData.dropSoulbound(userId);
        return player;
    }
}
