package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.feature.EndPlatformFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EndPlatformFeature.class)
public class EndPlatformFeatureMixin {
    @Redirect(method = "generate(Lnet/minecraft/world/ServerWorldAccess;Lnet/minecraft/util/math/BlockPos;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;isOf(Lnet/minecraft/block/Block;)Z"))
    private static boolean generateEndPlatform(BlockState instance, Block block) {
        return instance.isOf(Yigd.GRAVE_BLOCK) || instance.isOf(block);
    }
}
