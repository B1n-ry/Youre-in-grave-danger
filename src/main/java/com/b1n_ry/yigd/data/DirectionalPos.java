package com.b1n_ry.yigd.data;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

public record DirectionalPos(BlockPos pos, Direction dir) {
    public DirectionalPos(int x, int y, int z, Direction dir) {
        this(new BlockPos(x, y, z), dir);
    }

    public double getSquaredDistance(Vec3i pos) {
        return this.pos.getSquaredDistance(pos);
    }
}
