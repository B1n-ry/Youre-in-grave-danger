package com.b1n_ry.yigd.compat;

import com.b1n_ry.yigd.config.YigdConfig;
import com.b1n_ry.yigd.core.ModTags;
import com.finallion.graveyard.init.TGBlocks;
import com.finallion.graveyard.world.structures.TGJigsawStructure;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TheGraveyardCompat {
    public static Pair<BlockPos, Direction> getGraveyardGrave(ServerWorld world, BlockPos pos, int radius) {
        YigdConfig.GraveCompatConfig compatConfig = YigdConfig.getConfig().graveSettings.graveCompatConfig;
        Pair<BlockPos, Direction> posDir = new Pair<>(pos, null);
        if (!compatConfig.prioritiseTheGraveyardGraves) {
            return posDir;
        }

        BlockPos structurePos = world.locateStructure(ModTags.GRAVEYARD_STRUCTURES, pos, radius, false);
        if (structurePos == null) return posDir;

        Map<Structure, LongSet> map = world.getStructureAccessor().getStructureReferences(structurePos);

        List<BlockPos> graveCoords = new ArrayList<>();
        StructureAccessor accessor = world.getStructureAccessor();

        for (Structure feature : map.keySet()) {
            if (!(feature instanceof TGJigsawStructure)) continue;
            // Since there's no way of knowing which y-level the structure is at, which is required to know, we loop every 10 blocks (shortest graveyard is 10 blocks high) to find a graveyard structure
            StructureStart start;
            int height = 50;
            do {
                start = accessor.getStructureAt(new BlockPos(structurePos.getX(), height, structurePos.getZ()), feature);
                height += 10;
            } while (height < 150 && !start.hasChildren());
            List<StructurePiece> pieces = start.getChildren();

            for (StructurePiece sPiece : pieces) {
                if (!(sPiece instanceof PoolStructurePiece piece)) continue;

                StructurePoolElement poolElement = piece.getPoolElement();

                if (!(poolElement instanceof SinglePoolElement singlePoolElement)) continue;

                String asString = singlePoolElement.toString();
                asString = asString.replaceAll(".*\\[graveyard:", "");
                asString = asString.replaceAll("]", "");

                BlockRotation rotation = piece.getRotation();
                BlockMirror mirror = piece.getMirror();
                BlockBox box = piece.getBoundingBox();
                switch (asString) {
                    case "small_graveyard/small_graveyard_01" -> graveCoords.add(fromStructurePos(6, 2, 11, box, rotation, mirror));
                    case "small_graveyard/small_graveyard_02" -> {
                        graveCoords.add(fromStructurePos(5, 1, 10, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(11, 1, 5, box, rotation, mirror));
                    }
                    case "small_graveyard/small_graveyard_03", "small_graveyard/small_graveyard_04", "small_graveyard/small_graveyard_06", "small_graveyard/small_graveyard_08" -> {
                        graveCoords.add(fromStructurePos(8, 1, 5, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(13, 1, 11, box, rotation, mirror));
                    }
                    case "small_graveyard/small_graveyard_05" -> {
                        graveCoords.add(fromStructurePos(8, 2, 16, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(12, 2, 16, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(14, 2, 16, box, rotation, mirror));
                    }
                    case "small_graveyard/small_graveyard_07" -> {
                        graveCoords.add(fromStructurePos(6, 1, 5, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(8, 1, 5, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(13, 1, 9, box, rotation, mirror));
                    }
                    case "small_graveyard/small_graveyard_09" -> {
                        graveCoords.add(fromStructurePos(8, 1, 5, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(13, 1, 9, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(13, 1, 11, box, rotation, mirror));
                    }
                    case "small_desert_graveyard/small_desert_graveyard_01" -> {
                        graveCoords.add(fromStructurePos(3, 4, 8, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(11, 3, 7, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(11, 3, 11, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(23, 3, 6, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(23, 3, 10, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(23, 3, 12, box, rotation, mirror));
                    }
                    case "medium_graveyard/medium_graveyard_01" -> {
                        graveCoords.add(fromStructurePos(16, 1, 4, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(24, 1, 4, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(20, 1, 11, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(20, 1, 21, box, rotation, mirror));
                    }
                    case "medium_graveyard/medium_graveyard_02" -> {
                        graveCoords.add(fromStructurePos(5, 2, 30, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(9, 2, 5, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(9, 1, 15, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(9, 1, 24, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(11, 2, 5, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(11, 1, 15, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(11, 2, 34, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(13, 1, 15, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(13, 1, 24, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(13, 2, 34, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(15, 1, 15, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(15, 1, 24, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(17, 2, 5, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(19, 2, 34, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(21, 2, 5, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(21, 2, 34, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(25, 2, 12, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(25, 2, 14, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(25, 2, 25, box, rotation, mirror));
                    }
                    case "large_graveyard/branch_pool/branch_graves_01" -> {
                        graveCoords.add(fromStructurePos(1, 1, 4, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(6, 1, 4, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(8, 1, 4, box, rotation, mirror));
                    }
                    case "large_graveyard/branch_pool/branch_graves_03" -> {
                        graveCoords.add(fromStructurePos(4, 1, 6, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(6, 1, 6, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(12, 1, 6, box, rotation, mirror));
                    }
                    case "large_graveyard/branch_pool/branch_graves_04" -> graveCoords.add(fromStructurePos(12, 1, 3, box, rotation, mirror));
                    case "large_graveyard/branch_pool/branch_graves_05" -> {
                        graveCoords.add(fromStructurePos(7, 1, 1, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(2, 1, 17, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(5, 1, 21, box, rotation, mirror));
                    }
                    case "large_graveyard/branch_pool/branch_large_grave_01" -> {
                        graveCoords.add(fromStructurePos(4, 2, 7, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(11, 2, 7, box, rotation, mirror));
                    }
                    case "large_graveyard/branch_pool/branch_looted_graves_01" -> graveCoords.add(fromStructurePos(1, 1, 1, box, rotation, mirror));
                    case "large_graveyard/feature_pool/walled_graves_with_crypt" -> {
                        graveCoords.add(fromStructurePos(13, 3, 6, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(13, 3, 24, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(13, 3, 31, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(21, 2, 15, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(21, 2, 22, box, rotation, mirror));
                    }
                    case "large_graveyard/feature_pool/walled_street_01" -> {
                        graveCoords.add(fromStructurePos(10, 1, 4, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(14, 2, 4, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(4, 1, 22, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(12, 1, 22, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(14, 1, 22, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(18, 2, 22, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(22, 2, 22, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(27, 2, 22, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(10, 1, 27, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(12, 1, 27, box, rotation, mirror));
                    }
                    case "large_graveyard/feature_pool/walled_street_02" -> {
                        graveCoords.add(fromStructurePos(4, 2, 5, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(4, 1, 22, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(10, 1, 11, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(13, 1, 27, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(14, 1, 11, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(16, 1, 11, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(16, 1, 20, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(18, 1, 5, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(21, 1, 20, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(21, 1, 27, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(23, 1, 20, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(27, 1, 11, box, rotation, mirror));
                    }
                    case "large_graveyard/feature_pool/walled_street_03" -> {
                        graveCoords.add(fromStructurePos(4, 3, 4, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(4, 3, 6, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(4, 3, 25, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(6, 1, 21, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(14, 1, 20, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(18, 1, 20, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(20, 1, 10, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(20, 1, 20, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(25, 3, 4, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(27, 3, 4, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(27, 1, 10, box, rotation, mirror));
                    }
                    case "large_graveyard/feature_pool/wither_skeleton_mill" -> {
                        graveCoords.add(fromStructurePos(5, 3, 42, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(5, 3, 44, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(12, 3, 44, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(15, 3, 42, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(27, 3, 19, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(27, 3, 21, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(28, 3, 32, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(36, 3, 15, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(38, 3, 15, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(42, 3, 20, box, rotation, mirror));
                    }
                    case "large_graveyard/street_pool/corner_01" -> {
                        graveCoords.add(fromStructurePos(13, 1, 7, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(13, 1, 9, box, rotation, mirror));
                    }
                    case "large_graveyard/street_pool/corner_02" -> {
                        graveCoords.add(fromStructurePos(11, 1, 5, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(11, 1, 8, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(17, 1, 8, box, rotation, mirror));
                    }
                    case "large_graveyard/street_pool/crossroad_01" -> {
                        graveCoords.add(fromStructurePos(4, 1, 4, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(4, 1, 10, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(4, 1, 21, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(4, 1, 23, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(4, 1, 25, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(11, 2, 9, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(12, 2, 4, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(20, 1, 9, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(20, 1, 13, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(27, 2, 18, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(27, 2, 20, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(27, 2, 22, box, rotation, mirror));
                    }
                    case "large_graveyard/street_pool/crossroad_02" -> {
                        graveCoords.add(fromStructurePos(1, 1, 4, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(1, 1, 8, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(1, 1, 10, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(6, 1, 1, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(10, 1, 1, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(12, 1, 1, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(29, 1, 22, box, rotation, mirror));
                    }
                    case "large_graveyard/street_pool/street_01" -> {
                        graveCoords.add(fromStructurePos(1, 1, 2, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(4, 2, 13, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(8, 2, 13, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(9, 2, 2, box, rotation, mirror));
                    }
                    case "large_graveyard/street_pool/street_02" -> {
                        graveCoords.add(fromStructurePos(1, 1, 2, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(5, 2, 2, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(14, 1, 2, box, rotation, mirror));
                    }
                    case "large_graveyard/street_pool/street_03", "large_graveyard/street_pool/street_04" -> graveCoords.add(fromStructurePos(7, 1, 2, box, rotation, mirror));
                    case "large_graveyard/street_pool/street_end_01" -> {
                        graveCoords.add(fromStructurePos(3, 1, 9, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(6, 1, 0, box, rotation, mirror));
                    }
                    case "large_graveyard/street_pool/street_end_02" -> graveCoords.add(fromStructurePos(7, 1, 1, box, rotation, mirror));
                    case "large_graveyard/street_pool/street_end_03" -> graveCoords.add(fromStructurePos(8, 1, 0, box, rotation, mirror));
                    case "large_graveyard/graveyard_entrance" -> {
                        graveCoords.add(fromStructurePos(20, 1, 20, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(20, 1, 24, box, rotation, mirror));
                        graveCoords.add(fromStructurePos(20, 1, 28, box, rotation, mirror));
                    }
                }
            }
            break;
        }

        for (BlockPos coord : graveCoords) {
            BlockState coordState = world.getBlockState(coord);
            if (coordState.isOf(TGBlocks.GRAVESTONE) || coordState.isOf(TGBlocks.DEEPSLATE_GRAVESTONE)) {
                posDir.setLeft(coord);
                Direction dir = coordState.get(Properties.HORIZONTAL_FACING);
                if (dir != null) posDir.setRight(dir);
                return posDir;
            }
        }
        return posDir;
    }

    private static BlockPos fromStructurePos(int x, int y, int z, BlockBox box, BlockRotation rotation, BlockMirror mirror) {
        return mirror(rotate(new Vec3i(x, y, z), rotation, box), mirror, box);
    }
    private static BlockPos rotate(Vec3i relativePos, BlockRotation rotation, BlockBox box) {
        int minX = box.getMinX();
        int maxX = box.getMaxX();
        int minY = box.getMinY();
        int minZ = box.getMinZ();
        int maxZ = box.getMaxZ();

        if (rotation == null) rotation = BlockRotation.NONE;

        switch (rotation) {
            case CLOCKWISE_90 -> {
                return new BlockPos(maxX - relativePos.getZ(), minY + relativePos.getY(), minZ + relativePos.getX());
            }
            case CLOCKWISE_180 -> {
                return new BlockPos(maxX - relativePos.getX(), minY + relativePos.getY(), maxZ - relativePos.getZ());
            }
            case COUNTERCLOCKWISE_90 -> {
                return new BlockPos(minX + relativePos.getZ(), minY + relativePos.getY(), maxZ - relativePos.getX());
            }
            default -> {
                return new BlockPos(minX + relativePos.getX(), minY + relativePos.getY(), minZ + relativePos.getZ());
            }
        }
    }
    private static BlockPos mirror(BlockPos pos, BlockMirror mirror, BlockBox box) {
        int minX = box.getMinX();
        int maxX = box.getMaxX();
        int minZ = box.getMinZ();
        int maxZ = box.getMaxZ();

        int xDiff = pos.getX() - minX;
        int zDiff = pos.getZ() - minZ;

        if (mirror == null) mirror = BlockMirror.NONE;

        switch (mirror) {
            case FRONT_BACK -> {
                return new BlockPos(maxX - xDiff, pos.getY(), pos.getZ());
            }
            case LEFT_RIGHT -> {
                return new BlockPos(pos.getX(), pos.getY(), maxZ - zDiff);
            }
            default -> {
                return pos;
            }
        }
    }
}