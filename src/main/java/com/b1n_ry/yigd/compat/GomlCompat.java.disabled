package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.api.ClaimModsApi;
import com.jamieswhiteshirt.rtree3i.Entry;
import com.jamieswhiteshirt.rtree3i.Selection;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimBox;
import draylar.goml.api.ClaimUtils;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class GomlCompat implements ClaimModsApi {
    @Override
    public boolean isInClaim(BlockPos pos, ServerWorld world) {
        Selection<Entry<ClaimBox, Claim>> claims = ClaimUtils.getClaimsAt(world, pos);
        Yigd.LOGGER.debug("" + claims.count());
        return !claims.isEmpty();
    }
}
