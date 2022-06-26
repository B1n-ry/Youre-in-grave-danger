package com.b1n_ry.yigd.mixin;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.TFEventListener;

@Pseudo
@Mixin(TFEventListener.class)
public class TFEventListenerMixin {
    @Redirect(method = "returnStoredItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;readNbt(Lnet/minecraft/nbt/NbtList;)V"))
    private static void giveDeathItems(PlayerInventory instance, NbtList nbtList) {
        for (int i = 0; i < nbtList.size(); ++i) {
            NbtCompound nbtCompound = nbtList.getCompound(i);
            int j = nbtCompound.getByte("Slot") & 0xFF;
            ItemStack itemStack = ItemStack.fromNbt(nbtCompound);
            if (itemStack.isEmpty()) continue;
            if (j < instance.main.size()) {
                instance.main.set(j, itemStack);
                continue;
            }
            if (j >= 100 && j < instance.armor.size() + 100) {
                instance.armor.set(j - 100, itemStack);
                continue;
            }
            if (j < 150 || j >= instance.offHand.size() + 150) continue;
            instance.offHand.set(j - 150, itemStack);
        }
    }
}
