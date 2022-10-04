package com.b1n_ry.yigd.mixin;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.block.entity.GraveBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import twilightforest.util.EntityUtil;

@Pseudo
@Mixin(EntityUtil.class)
public class TwilightEntityUtilMixin {
    @Redirect(method = "canDestroyBlock(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/entity/Entity;)Z", at = @At(value = "INVOKE", target = "Ltwilightforest/util/EntityUtil;canEntityDestroyBlock(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)Z"))
    private static boolean destroy(BlockState state, BlockView level, BlockPos pos, Entity entity) {
        if (state.isOf(Yigd.GRAVE_BLOCK)) {
            if (level.getBlockEntity(pos) instanceof GraveBlockEntity grave && grave.getGraveOwner() != null && !grave.isClaimed()) return false;
        }
        return EntityUtil.canEntityDestroyBlock(state, level, pos, entity);
    }
}
