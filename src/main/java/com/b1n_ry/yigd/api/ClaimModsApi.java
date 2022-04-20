package com.b1n_ry.yigd.api;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public interface ClaimModsApi {
    boolean isInClaim(BlockPos pos, ServerWorld world);
}
