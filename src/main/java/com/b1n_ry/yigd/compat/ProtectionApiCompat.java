package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.api.ClaimModsApi;
import eu.pb4.common.protection.impl.ProtectionImpl;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class ProtectionApiCompat implements ClaimModsApi {
    @Override
    public boolean isInClaim(BlockPos pos, ServerWorld world) {
        return ProtectionImpl.isProtected(world, pos);
    }
}
