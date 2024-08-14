package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EndPlatformFeature.class)
public class EndPlatformFeatureMixin {
    @Redirect(method = "createEndPlatform", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private static boolean generateEndPlatform(BlockState instance, Block block) {
        return instance.is(Yigd.GRAVE.get()) || instance.is(block);
    }
}
