package com.b1n4ry.yigd.mixin;

import com.b1n4ry.yigd.Yigd;
import com.b1n4ry.yigd.api.YigdApi;
import com.b1n4ry.yigd.core.PlayerEntityExt;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    // Will make this work in the future with a custom soulbound enchantment
    @ModifyArgs(method = "respawnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerManager;setGameMode(Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/server/world/ServerWorld;)V"))
    private void equipSoulboundItems(Args args) {
        ServerPlayerEntity newPlayer = args.get(0);
        ServerPlayerEntity oldPlayer = args.get(1);

        if (oldPlayer.getServerWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return;

        DefaultedList<ItemStack> soulboundItems = ((PlayerEntityExt)oldPlayer).getSoulboundInventory();

        if (soulboundItems == null) return;
        if (soulboundItems.size() == 0) return;

        List<ItemStack> armorInventory = soulboundItems.subList(36, 40);
        List<ItemStack> mainInventory = soulboundItems.subList(0, 36);

        for (ItemStack itemStack : armorInventory) {
            EquipmentSlot equipmentSlot = MobEntity.getPreferredEquipmentSlot(itemStack); // return EquipmentSlot

            if (itemStack.isEmpty()) continue;

            newPlayer.equipStack(equipmentSlot, itemStack);
        }

        newPlayer.equipStack(EquipmentSlot.OFFHAND, soulboundItems.get(40));

        for (int i = 0; i < mainInventory.size(); i++) { // Replace main inventory from grave
            newPlayer.equip(i, mainInventory.get(i));
        }

        if (soulboundItems.size() > 41) {
            int inventoryOffset = 41;
            for (YigdApi yigdApi : Yigd.apiMods) {
                int inventorySize = yigdApi.getInventorySize(newPlayer);

                yigdApi.setInventory(soulboundItems.subList(inventoryOffset, inventoryOffset + inventorySize), newPlayer);
                inventoryOffset += inventorySize;
            }
        }

        soulboundItems.clear();
    }
}
