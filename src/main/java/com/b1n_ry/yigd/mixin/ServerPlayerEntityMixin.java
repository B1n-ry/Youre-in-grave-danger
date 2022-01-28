package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.YigdApi;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.b1n_ry.yigd.core.PlayerEntityExt;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin {
    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void onRespawn(ServerPlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        if (oldPlayer.getServerWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY) || alive) return;

        ServerPlayerEntity newPlayer = (ServerPlayerEntity) (Object) this;

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

    @Redirect(method = "createEndSpawnPlatform", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    private boolean removeBlock(ServerWorld world, BlockPos blockPos, BlockState blockState) {
        BlockEntity be = world.getBlockEntity(blockPos);
        if (be instanceof GraveBlockEntity) {
            GraveBlockEntity grave = (GraveBlockEntity) be;
            if (grave.getGraveOwner() != null) return false;
        }
        return world.setBlockState(blockPos, blockState);
    }
}
