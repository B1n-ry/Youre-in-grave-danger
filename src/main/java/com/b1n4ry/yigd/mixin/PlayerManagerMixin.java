package com.b1n4ry.yigd.mixin;

import com.b1n4ry.yigd.core.PlayerEntityExt;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Optional;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

    // Will make this work in the future with a custom soulbound enchantment
    @Inject(method = "respawnPlayer", at = @At(value = "RETURN"))
    private void equipSoulboundItems(ServerPlayerEntity oldPlayer, boolean dimensionChange, CallbackInfoReturnable<ServerPlayerEntity> callback, BlockPos blockPos, boolean forcedSpawn, ServerWorld oldWorld, Optional optional, ServerPlayerInteractionManager interactionManager, ServerWorld newWorld, ServerPlayerEntity newPlayer) {
        if (oldPlayer.getServerWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return;

        DefaultedList<ItemStack> soulboundItems = ((PlayerEntityExt)oldPlayer).getSoulboundInventory();

        if (soulboundItems == null) return;
        if (soulboundItems.size() == 0) return;

        for (int i = 0; i < soulboundItems.size(); i++) {
            ItemStack item = soulboundItems.get(i);
            System.out.println(item.getItem());

//            if (item == ItemStack.EMPTY || item.getItem() == Items.AIR) continue;
//            newPlayer.equip(i, item);
        }

        soulboundItems.clear();
    }
}
