package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.mim1q.minecells.entity.boss.ConjunctiviusEntity")
public class MinecellsBossMixin {
    @Redirect(method = "lambda$tick$4(Lnet/minecraft/util/math/BlockPos;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;breakBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    private boolean breakBlock(World world, BlockPos pos, boolean b) {
        if (b && world.getBlockState(pos).isOf(Yigd.GRAVE_BLOCK)) {
            return false;
        }
        return b;
    }
}
