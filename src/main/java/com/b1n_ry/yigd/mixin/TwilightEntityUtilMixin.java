package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "twilightforest.util.EntityUtil")
public class TwilightEntityUtilMixin {
    @Inject(method = "canDestroyBlock(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/Entity;)Z", at = @At(value = "HEAD", target = "Ltwilightforest/util/EntityUtil;canEntityDestroyBlock(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Z"), cancellable = true)
    private static void destroy(World world, BlockPos pos, BlockState state, Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (state.isOf(Yigd.GRAVE_BLOCK)) cir.setReturnValue(false);
    }
}
