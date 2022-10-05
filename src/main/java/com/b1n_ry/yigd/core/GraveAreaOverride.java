package com.b1n_ry.yigd.core;

import com.b1n_ry.yigd.config.DeathEffectConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class GraveAreaOverride {
    private static final List<GraveArea> areaOverrides = new ArrayList<>();

    public static void reloadGraveAreas(JsonObject graveAreas) {
        areaOverrides.clear();

        JsonArray values = graveAreas.getAsJsonArray("values");
        if (values == null) return;
        for (JsonElement e : values) {
            if (!e.isJsonObject()) continue;
            JsonObject area = e.getAsJsonObject();
            JsonArray fromCords = area.getAsJsonArray("from");
            JsonArray toCords = area.getAsJsonArray("to");
            BlockPos fromPos = new BlockPos(fromCords.get(0).getAsInt(), fromCords.get(1).getAsInt(), fromCords.get(2).getAsInt());
            BlockPos toPos = new BlockPos(toCords.get(0).getAsInt(), toCords.get(1).getAsInt(), toCords.get(2).getAsInt());

            DeathEffectConfig dropType = Enum.valueOf(DeathEffectConfig.class, area.get("drop_rule").getAsString());
            boolean yDependant = area.get("y_dependant").getAsBoolean();
            String worldId = area.get("world_id").getAsString();

            areaOverrides.add(new GraveArea(dropType, yDependant, fromPos, toPos, new Identifier(worldId)));
        }
    }

    public static DeathEffectConfig canGenerateOnPos(BlockPos pos, Identifier worldId, boolean defaultGraves) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        for (GraveArea area : areaOverrides) {
            if (!area.worldId.equals(worldId)) continue;
            if (x < area.fromCorner.getX() || x > area.toCorner.getX() || z < area.fromCorner.getZ() || z > area.toCorner.getZ()) continue;
            if (area.yDependant && (y < area.fromCorner.getY() || y > area.toCorner.getY())) continue;
            return area.dropType;
        }

        return defaultGraves ? DeathEffectConfig.CREATE_GRAVE : DeathEffectConfig.DROP_ITEMS;
    }

    public record GraveArea(DeathEffectConfig dropType, boolean yDependant, BlockPos fromCorner, BlockPos toCorner, Identifier worldId) {}
}
