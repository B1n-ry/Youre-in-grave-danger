package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.api.ClaimModsApi;
import io.github.flemmli97.flan.claim.Claim;
import io.github.flemmli97.flan.claim.IClaimStorage;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class FlanCompat implements ClaimModsApi {
    @Override
    public boolean isInClaim(BlockPos pos, ServerWorld world) {
        Claim claim = ((IClaimStorage) world).get().getClaimAt(pos);
        return claim != null;
    }
}
