package com.b1n4ry.yigd.mixin;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.api.YigdApi;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
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

        // In case some items are by mistake placed in the inventory that should not be there
        player.getInventory().clear();
        for (YigdApi yigdApi : Yigd.apiMods) {
            yigdApi.dropAll(player);
        }

        DefaultedList<ItemStack> soulboundItems = Yigd.deadPlayerData.getSoulboundInventory(userId);

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

        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < mainInventory.size(); i++) { // Replace main inventory from grave
            inventory.setStack(i, mainInventory.get(i));
        }

        List<Object> modSoulbounds = Yigd.deadPlayerData.getModdedSoulbound(userId);
        for (int i = 0; i < Yigd.apiMods.size(); i++) {
            YigdApi yigdApi = Yigd.apiMods.get(i);
            Object modSoulbound = modSoulbounds.get(i);

            yigdApi.setInventory(modSoulbound, player);
        }

        if (soulboundItems.size() > 41) {
            for (int i = 41; i < soulboundItems.size(); i++) {
                inventory.setStack(i, soulboundItems.get(i));
            }
        }

        Yigd.deadPlayerData.dropModdedSoulbound(userId);
        Yigd.deadPlayerData.dropSoulbound(userId);

        BlockPos deathPos = Yigd.deadPlayerData.getDeathPos(userId);
        if (deathPos != null) {
            player.sendMessage(Text.of("Your grave has been generated at\nX: " + deathPos.getX() + " / Y: " + deathPos.getY() + " / Z: " + deathPos.getZ()), false);
        }

        return player;
    }
}
