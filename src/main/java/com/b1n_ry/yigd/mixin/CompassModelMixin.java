package com.b1n_ry.yigd.mixin;

import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ModelPredicateProviderRegistry.class)
public class CompassModelMixin {
    @Inject(method = "method_43220", at = @At("HEAD"), cancellable = true)
    private static void changeCompassDirection(ClientWorld world, ItemStack stack, Entity entity, CallbackInfoReturnable<GlobalPos> cir) {
        NbtCompound itemNbt = stack.getNbt();
        final String graveDimensionKey = "grave_dimension";
        final String gravePosKey = "grave_pos";

        if (itemNbt != null && itemNbt.contains(graveDimensionKey) && itemNbt.contains(gravePosKey)) {
            Optional<RegistryKey<World>> optional = World.CODEC.parse(NbtOps.INSTANCE, itemNbt.get(graveDimensionKey)).result();
            BlockPos blockPos = NbtHelper.toBlockPos(itemNbt.getCompound(gravePosKey));

            if (optional.isPresent()) {
                cir.setReturnValue(GlobalPos.create(optional.get(), blockPos));
            } else {
                cir.setReturnValue(null);
            }
        }
    }
}
