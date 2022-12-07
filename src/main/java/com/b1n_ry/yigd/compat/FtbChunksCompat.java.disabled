package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.api.ClaimModsApi;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class FtbChunksCompat implements ClaimModsApi {
    @Override
    public boolean isInClaim(BlockPos pos, ServerWorld world) {
        ChunkDimPos chunkDimPos = new ChunkDimPos(world, pos);
        ClaimedChunk claimedChunk = FTBChunksAPI.getManager().getChunk(chunkDimPos);
        if (claimedChunk == null) return false;
        return claimedChunk.isSuccess();
    }
}
