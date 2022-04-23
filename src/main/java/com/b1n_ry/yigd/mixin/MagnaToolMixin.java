package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import draylar.magna.api.MagnaTool;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(MagnaTool.class)
public interface MagnaToolMixin {
    // This mixin will make hammer things always unable to break graves, no matter what graves and what mode the tool is in, since this was the easiest to do
    @Redirect(method = "isBlockValidForBreaking", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getHardness(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F"))
    private float getHardness(BlockState state, BlockView blockView, BlockPos blockPos) {
        if (state.isOf(Yigd.GRAVE_BLOCK)) return -1.0f; // Make hammers and such find graves unbreakable, while they can be broken if configured
        return state.getHardness(blockView, blockPos);
    }
}