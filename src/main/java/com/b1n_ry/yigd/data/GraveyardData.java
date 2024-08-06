package com.b1n_ry.yigd.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GraveyardData {
    @SerializedName("point2point")
    public boolean point2point = false;
    @SerializedName("dimension")
    public Identifier dimensionId = Identifier.of("overworld");
    @SerializedName("use_closest")
    public boolean useClosest = false;
    @SerializedName("coordinates")
    public List<GraveLocation> graveLocations = new ArrayList<>();


    public static class GraveLocation {
        @SerializedName("x")
        public int x;
        @SerializedName("y")
        public int y;
        @SerializedName("z")
        public int z;

        @Nullable
        @SerializedName("for_player")
        public String forPlayer = null;
        @Nullable
        @SerializedName("direction")
        public Direction direction = null;

        public GraveLocation(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public void handlePoint2Point() {
        if (!this.point2point || this.graveLocations.size() != 2) return;  // Can't handle point2point. Disabled or format not followed

        GraveLocation first = this.graveLocations.get(0);
        GraveLocation second = this.graveLocations.get(1);
        int minX = Math.min(first.x, second.x);
        int minY = Math.min(first.y, second.y);
        int minZ = Math.min(first.z, second.z);
        int maxX = Math.max(first.x, second.x);
        int maxY = Math.max(first.y, second.y);
        int maxZ = Math.max(first.z, second.z);

        this.graveLocations.clear();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    this.graveLocations.add(new GraveLocation(x, y, z));
                }
            }
        }
    }
}
