package com.b1n4ry.yigd.mixin;

import com.b1n4ry.yigd.core.PlayerEntityExt;
import net.minecraft.item.ItemStack;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {
    // Will make this work in the future with a custom soulbound enchantment
    @Inject(method = "respawnPlayer", at = @At("RETURN"))
    private void equipSoulboundItems(ServerPlayerEntity player, boolean alive, CallbackInfoReturnable<ServerPlayerEntity> cir) {
        if (player.getServerWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return;

        DefaultedList<ItemStack> soulboundItems = ((PlayerEntityExt)player).getSoulboundInventory();

        if (soulboundItems == null) return;
        if (soulboundItems.size() == 0) return;

        for (int i = 0; i < soulboundItems.size(); i++) {
            ItemStack item = soulboundItems.get(i);

            if (item == ItemStack.EMPTY) continue;
            player.equip(i, item);
        }
    }
}
