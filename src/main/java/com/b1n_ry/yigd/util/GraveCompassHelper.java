package com.b1n_ry.yigd.util;

import com.b1n_ry.yigd.Yigd;
import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.config.YigdConfig.ExtraFeatures.GraveCompassConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GraveCompassHelper {
    private static final Map<RegistryKey<World>, KDNode> GRAVE_POSITIONS = new HashMap<>();
    public static void giveCompass(ServerPlayerEntity player, UUID graveId, BlockPos gravePos, RegistryKey<World> worldKey) {
        ItemStack compass = Items.COMPASS.getDefaultStack();
        NbtCompound compassNbt = new NbtCompound();

        BlockPos closestPos = null;
        RegistryKey<World> playerWorld = player.getServerWorld().getRegistryKey();
        if (YigdConfig.getConfig().extraFeatures.graveCompass.pointToClosest != GraveCompassConfig.CompassGraveTarget.DISABLED) {
            closestPos = findClosest(player.getUuid(), playerWorld, player.getBlockPos());
        }
        if (closestPos != null) {  // Point to closest not disabled and a grave was found
            compassNbt.put("grave_pos", NbtHelper.fromBlockPos(closestPos));
            World.CODEC.encodeStart(NbtOps.INSTANCE, playerWorld).resultOrPartial(Yigd.LOGGER::error).ifPresent(worldNbt -> compassNbt.put("grave_dimension", worldNbt));
        } else {
            // Standard grave compass
            compassNbt.putUuid("linked_grave", graveId);  // Speed up the process of identifying the grave server side

            // Make clients read the grave position
            compassNbt.put("grave_pos", NbtHelper.fromBlockPos(gravePos));
            World.CODEC.encodeStart(NbtOps.INSTANCE, worldKey).resultOrPartial(Yigd.LOGGER::error).ifPresent(worldNbt -> compassNbt.put("grave_dimension", worldNbt));
        }

        compass.setNbt(compassNbt);
        compass.setCustomName(Text.translatable("item.yigd.grave_compass").styled(style -> style.withItalic(false)));
        player.giveItemStack(compass);
    }

    public static void updateClosestNbt(RegistryKey<World> worldKey, BlockPos pos, UUID holderId, ItemStack compass) {
        if (YigdConfig.getConfig().extraFeatures.graveCompass.pointToClosest == GraveCompassConfig.CompassGraveTarget.DISABLED) return;

        NbtCompound compassNbt = compass.getNbt();
        if (compassNbt == null) return;

        if (!compassNbt.contains("grave_pos")) return;  // Not a grave compass

        BlockPos closestPos = findClosest(holderId, worldKey, pos);
        if (closestPos != null) {
            compassNbt.put("grave_pos", NbtHelper.fromBlockPos(closestPos));
            World.CODEC.encodeStart(NbtOps.INSTANCE, worldKey).resultOrPartial(Yigd.LOGGER::error).ifPresent(worldNbt -> compassNbt.put("grave_dimension", worldNbt));
        }
    }
    public static void addGravePosition(RegistryKey<World> worldKey, BlockPos gravePos, UUID ownerId) {
        KDNode root = GRAVE_POSITIONS.get(worldKey);
        int[] pos = new int[] { gravePos.getX(), gravePos.getY(), gravePos.getZ() };
        if (root == null) {
            GRAVE_POSITIONS.put(worldKey, new KDNode(ownerId, pos, null, null));
            return;
        }

        addGravePosition(root, ownerId, pos, 0);
    }
    private static void addGravePosition(KDNode parent, UUID ownerId, int[] pos, int depth) {
        if (depth > 100)
            return;

        int cmp = depth % 3;
        if (pos[cmp] < parent.pos[cmp]) {
            if (parent.left == null) {
                parent.left = new KDNode(ownerId, pos, null, null);
            } else {
                addGravePosition(parent.left, ownerId, pos, depth + 1);
            }
        } else {
            if (parent.right == null) {
                parent.right = new KDNode(ownerId, pos, null, null);
            } else {
                addGravePosition(parent.right, ownerId, pos, depth + 1);
            }
        }
    }

    public static void setClaimed(RegistryKey<World> worldKey, BlockPos gravePos) {
        KDNode root = GRAVE_POSITIONS.get(worldKey);
        if (root == null) {
            return;
        }

        int[] pos = new int[] { gravePos.getX(), gravePos.getY(), gravePos.getZ() };

        setClaimed(root, pos, 0);
    }
    private static void setClaimed(KDNode node, int[] pos, int depth) {
        if (node == null) {
            return;
        }

        if (node.pos[0] == pos[0] && node.pos[1] == pos[1] && node.pos[2] == pos[2]) {
            node.isUnclaimed = false;
            return;
        }

        int cmp = depth % 3;
        if (node.pos[cmp] < pos[cmp]) {
            setClaimed(node.right, pos, depth + 1);
        } else {
            setClaimed(node.left, pos, depth + 1);
        }
    }

    public static @Nullable BlockPos findClosest(UUID ownerId, RegistryKey<World> worldKey, BlockPos pos) {
        GraveCompassConfig config = YigdConfig.getConfig().extraFeatures.graveCompass;
        if (config.pointToClosest == GraveCompassConfig.CompassGraveTarget.DISABLED) return null;  // What how are we here?

        KDNode root = GRAVE_POSITIONS.get(worldKey);
        if (root == null) {
            return null;
        }

        int[] searchPos = new int[] { pos.getX(), pos.getY(), pos.getZ() };

        int[] closest = findClosest(root, ownerId, searchPos, 0, config);

        return closest == null ? null : new BlockPos(closest[0], closest[1], closest[2]);
    }
    private static int[] findClosest(KDNode node, UUID ownerId, int[] pos, int depth, GraveCompassConfig config) {
        if (node == null || depth > 100) {
            return null;
        }

        int cmp = depth % 3;
        int[] closest = null;
        // If not all, it's player specific. If not that, it's disabled, and won't reach this
        if ((config.pointToClosest == GraveCompassConfig.CompassGraveTarget.ALL || node.ownerId.equals(ownerId)) && node.isUnclaimed) {
            closest = node.pos;
        }

        KDNode nextNode, otherNode;
        if (pos[cmp] < node.pos[cmp]) {
            nextNode = node.left;
            otherNode = node.right;
        } else {
            nextNode = node.right;
            otherNode = node.left;
        }

        int[] closestChild = findClosest(nextNode, ownerId, pos, depth + 1, config);

        if (closestChild != null && (closest == null || distanceSquared(closestChild, pos) < distanceSquared(closest, pos)))
            closest = closestChild;

        if (closestChild == null || Math.pow(node.pos[cmp] - pos[cmp], 2) < distanceSquared(closest, pos)) {
            closestChild = findClosest(otherNode, ownerId, pos, depth + 1, config);
            if (closestChild != null && (closest == null || distanceSquared(closestChild, pos) < distanceSquared(closest, pos)))
                closest = closestChild;
        }

        return closest;
    }
    private static double distanceSquared(int[] pos1, int[] pos2) {
        return Math.pow(pos1[0] - pos2[0], 2) + Math.pow(pos1[1] - pos2[1], 2) + Math.pow(pos1[2] - pos2[2], 2);
    }

    private static class KDNode {
        @NotNull
        private final UUID ownerId;
        private boolean isUnclaimed = true;
        private final int @NotNull [] pos;
        @Nullable
        private KDNode left;
        @Nullable
        private KDNode right;

        public KDNode(@NotNull UUID ownerId, int @NotNull [] pos, @Nullable KDNode left, @Nullable KDNode right) {
            this.ownerId = ownerId;
            this.pos = pos;
            this.left = left;
            this.right = right;
        }
    }
}
