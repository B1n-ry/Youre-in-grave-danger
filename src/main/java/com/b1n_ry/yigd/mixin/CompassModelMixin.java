package com.b1n_ry.yigd.mixin;

import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
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
    @Inject(method = "method_43220(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/GlobalPos;", at = @At("HEAD"), cancellable = true)
    private static void setCompassPos(ClientWorld world, ItemStack stack, Entity entity, CallbackInfoReturnable<GlobalPos> cir) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !(entity instanceof PlayerEntity)) return;
        if (nbt.contains("pointTowards")) {
            NbtCompound pos = nbt.getCompound("pointTowards");
            NbtElement dimNbt = pos.get("dimension");
            NbtCompound blockPosNbt = pos.getCompound("pos");
            if (dimNbt != null && blockPosNbt != null) {
                Optional<RegistryKey<World>> key = World.CODEC.parse(NbtOps.INSTANCE, dimNbt).result();
                BlockPos blockPos = NbtHelper.toBlockPos(blockPosNbt);

                key.ifPresent(registryKey -> cir.setReturnValue(GlobalPos.create(registryKey, blockPos)));
            }
        }
    }
}
