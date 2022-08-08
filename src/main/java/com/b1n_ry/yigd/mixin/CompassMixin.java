package com.b1n_ry.yigd.mixin;

import net.minecraft.item.CompassItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(targets = "net/minecraft/client/item/ModelPredicateProviderRegistry$2")
public class CompassMixin {
    private static boolean pointsTowardsPos(NbtCompound nbt) {
        return nbt.contains("pointTowards");
    }

    @Inject(method = "getLodestonePos", at = @At("HEAD"), cancellable = true)
    private void getCompassPos(World world, NbtCompound nbt, CallbackInfoReturnable<BlockPos> cir) {
        if (!pointsTowardsPos(nbt)) return;

        NbtCompound sourceNbt = nbt.getCompound("pointTowards");

        NbtCompound posNbt = sourceNbt.getCompound("pos");
        NbtElement dimNbt = sourceNbt.get("dimension");
        if (dimNbt == null || posNbt == null) return;

        Optional<RegistryKey<World>> key = World.CODEC.parse(NbtOps.INSTANCE, dimNbt).result();

        if (key.isPresent() && world.getRegistryKey() == key.get()) {
            cir.setReturnValue(NbtHelper.toBlockPos(posNbt));
        } else {
            cir.setReturnValue(null);
        }
    }
    @Redirect(method = "unclampedCall", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/CompassItem;hasLodestone(Lnet/minecraft/item/ItemStack;)Z"))
    private boolean pointsSomewhere(ItemStack stack) {
        return pointsTowardsPos(stack.getOrCreateNbt()) || CompassItem.hasLodestone(stack);
    }
}