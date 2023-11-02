package com.b1n_ry.yigd.util;

import com.google.gson.annotations.SerializedName;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.List;

public class GraveOverrideAreas {
    public static GraveOverrideAreas INSTANCE = new GraveOverrideAreas();

    @SerializedName("default_drop_rule")
    public DropRule defaultDropRule = DropRule.PUT_IN_GRAVE;
    @SerializedName("values")
    public List<Area> values = new ArrayList<>();

    public DropRule getDropRuleFromArea(BlockPos pos, ServerWorld world) {
        Identifier worldId = world.getRegistryKey().getValue();
        for (Area area : this.values) {
            if (!worldId.equals(area.worldId))
                continue;

            int minX = Math.min(area.from.getX(), area.to.getX());
            int minY = Math.min(area.from.getY(), area.to.getY());
            int minZ = Math.min(area.from.getZ(), area.to.getZ());
            int maxX = Math.max(area.from.getX(), area.to.getX());
            int maxY = Math.max(area.from.getY(), area.to.getY());
            int maxZ = Math.max(area.from.getZ(), area.to.getZ());

            int blockX = pos.getX();
            int blockY = pos.getY();
            int blockZ = pos.getZ();
            if (blockX < minX || maxX < blockX) continue;
            if ((blockY < minY || maxY < blockY) && !area.yDependent) continue;
            if (blockZ < minZ || maxZ < blockZ) continue;

            return area.areaDropRule;
        }
        return this.defaultDropRule;
    }

    public static class Area {
        @SerializedName("from")
        public Vec3i from;
        @SerializedName("to")
        public Vec3i to;
        @SerializedName("area_drop_rule")
        public DropRule areaDropRule;
        @SerializedName("y_dependent")
        public boolean yDependent = false;
        @SerializedName("world_id")
        public Identifier worldId;
    }
}
