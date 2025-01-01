package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public class LevelChunkMixin {
    @Inject(method = "removeBlockEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;setRemoved()V"))
    private void blockEntityBroken(BlockPos pos, CallbackInfo ci, @Local @Nullable BlockEntity removed) {
        if (removed instanceof GraveBlockEntity grave)
            grave.onBroken();
    }
}
