package com.ultraop.mocap.playback;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Center-based transformation chain matching MoCap's PositionTransformer model. */
public final class PositionTransformer {
    private final PlaybackModifiers modifiers;
    private final PositionTransformer parent;
    private final Vector center;

    public PositionTransformer(PlaybackModifiers modifiers, PositionTransformer parent, Vector center) {
        this.modifiers = modifiers == null ? PlaybackModifiers.DEFAULT : modifiers;
        this.parent = parent;
        this.center = center == null ? null : center.clone();
    }

    public Vector transform(Vector point) {
        return transformPos(point.clone(), null);
    }

    /** Transforms a recorded block into every block occupied by its transformed unit cube. */
    public List<Vector> transformBlockPositions(Vector blockPos, boolean allowScaled) {
        return transformBlockPositions(List.of(blockPos.clone()), null, allowScaled);
    }

    /** Compatibility helper for callers that only need one block position. */
    public Vector transformBlockPosition(Vector point) {
        List<Vector> positions = transformBlockPositions(point, true);
        return positions.isEmpty() ? point.clone() : positions.get(0);
    }

    private List<Vector> transformBlockPositions(List<Vector> input, Vector childCenter, boolean allowScaled) {
        if (childCenter == null && center == null) throw new IllegalStateException("Both childCenter and center are null");
        Vector centerToUse = center != null ? center.clone() : childCenter.clone();
        List<Vector> transformed = new ArrayList<>();
        for (Vector block : input) transformed.addAll(applyToBlockPosition(block, centerToUse));
        if (transformed.size() > 1 && !allowScaled) return List.of();
        return parent == null ? transformed : parent.transformBlockPositions(transformed, centerToUse, allowScaled);
    }

    private List<Vector> applyToBlockPosition(Vector blockPos, Vector pivot) {
        if (!modifiers.transformationConfig().roundBlockPos()
                && (!isIntVec(pivot.clone().multiply(2.0))
                || !isIntegerRotation(modifiers.rotationDegrees())
                || !canScaleInt(pivot)
                || !isIntegerOffset())) {
            return List.of();
        }
        Vector point1 = apply(blockPos.clone(), pivot);
        Vector point2 = apply(blockPos.clone().add(new Vector(1, 1, 1)), pivot);
        return voxelizeCube(point1, point2);
    }

    private boolean canScaleInt(Vector startPos) {
        double scale = modifiers.sceneScale();
        if (scale == 1.0) return true;
        if (scale != (int) scale) return false;
        Vector vec = ((int) scale % 2 == 0) ? startPos.clone() : startPos.clone().add(new Vector(0.5, 0, 0.5));
        return isIntVec(vec);
    }

    private List<Vector> voxelizeCube(Vector pos1, Vector pos2) {
        if (isIntVec(pos1)
                && pos1.getX() + 1.0 == pos2.getX()
                && pos1.getY() + 1.0 == pos2.getY()
                && pos1.getZ() + 1.0 == pos2.getZ()) {
            return List.of(pos1.clone());
        }
        int startY = (int) Math.round(pos1.getY());
        int stopY = (int) Math.round(pos2.getY());
        if (Math.abs(pos1.getX() - pos2.getX()) == Math.abs(pos1.getZ() - pos2.getZ())) {
            int startX = (int) Math.round(Math.min(pos1.getX(), pos2.getX()));
            int stopX = (int) Math.round(Math.max(pos1.getX(), pos2.getX()));
            int startZ = (int) Math.round(Math.min(pos1.getZ(), pos2.getZ()));
            int stopZ = (int) Math.round(Math.max(pos1.getZ(), pos2.getZ()));
            List<Vector> result = new ArrayList<>(Math.max(0, (stopX - startX) * (stopZ - startZ) * (stopY - startY)));
            for (int y = startY; y < stopY; y++) for (int z = startZ; z < stopZ; z++) for (int x = startX; x < stopX; x++) result.add(new Vector(x, y, z));
            return result;
        }
        double bottomY = pos1.getY();
        double centerX = (pos1.getX() + pos2.getX()) / 2.0;
        double centerZ = (pos1.getZ() + pos2.getZ()) / 2.0;
        Vector pos3 = new Vector(centerX + (pos1.getZ() - centerZ), bottomY, centerZ - (pos1.getX() - centerX));
        Vector pos4 = new Vector(centerX + (pos2.getZ() - centerZ), bottomY, centerZ - (pos2.getX() - centerX));
        int minZ = (int) Math.round(Math.min(Math.min(pos1.getZ(), pos2.getZ()), Math.min(pos3.getZ(), pos4.getZ())));
        int maxZ = (int) Math.round(Math.max(Math.max(pos1.getZ(), pos2.getZ()), Math.max(pos3.getZ(), pos4.getZ())));
        Vector[] vertices = {pos1, pos3, pos2, pos4};
        List<Vector> result = new ArrayList<>();
        List<Integer> nodesX = new ArrayList<>(4);
        for (int z = minZ; z <= maxZ; z++) {
            nodesX.clear();
            int j = vertices.length - 1;
            for (int i = 0; i < vertices.length; i++) {
                Vector v1 = vertices[i], v2 = vertices[j];
                if ((z > v1.getZ() && z < v2.getZ()) || (z > v2.getZ() && z < v1.getZ())) {
                    double x = v1.getX() + ((v2.getX() - v1.getX()) * ((z - v1.getZ()) / (v2.getZ() - v1.getZ())));
                    nodesX.add((int) Math.round(x));
                }
                j = i;
            }
            if (nodesX.isEmpty()) continue;
            int startX = Collections.min(nodesX), stopX = Collections.max(nodesX);
            for (int x = startX; x < stopX; x++) for (int y = startY; y < stopY; y++) result.add(new Vector(x, y, z));
        }
        return result;
    }

    private static boolean isIntVec(Vector v) {
        return v.getX() == (int) v.getX() && v.getY() == (int) v.getY() && v.getZ() == (int) v.getZ();
    }

    private static boolean isIntegerRotation(double degrees) {
        double normalized = Math.abs(degrees % 90.0);
        return normalized < 1.0E-9;
    }

    private boolean isIntegerOffset() {
        return isInteger(modifiers.offsetX()) && isInteger(modifiers.offsetY()) && isInteger(modifiers.offsetZ());
    }

    private static boolean isInteger(double value) { return value == (int) value; }

    public BlockData transformBlockState(BlockData data) {
        if (data == null || isTransformationDefault()) return data;
        BlockData result = data.clone();
        int quarterTurns = (int) Math.round(modifiers.rotationDegrees() / 90.0);
        switch (Math.floorMod(quarterTurns, 4)) {
            case 1 -> result.rotate(StructureRotation.CLOCKWISE_90);
            case 2 -> result.rotate(StructureRotation.CLOCKWISE_180);
            case 3 -> result.rotate(StructureRotation.COUNTERCLOCKWISE_90);
            default -> { }
        }
        switch (modifiers.mirror()) {
            case X -> result.mirror(Mirror.FRONT_BACK);
            case Z -> result.mirror(Mirror.LEFT_RIGHT);
            case XZ -> {
                result.mirror(Mirror.FRONT_BACK);
                result.mirror(Mirror.LEFT_RIGHT);
            }
            case NONE -> { }
        }
        return parent == null ? result : parent.transformBlockState(result);
    }

    private Vector transformPos(Vector point, Vector childCenter) {
        if (childCenter == null && center == null) throw new IllegalStateException("Both childCenter and center are null");
        Vector centerToUse = center != null ? center.clone() : childCenter.clone();
        Vector transformed = apply(point, centerToUse);
        return parent == null ? transformed : parent.transformPos(transformed, centerToUse);
    }

    public float transformRotation(float rotation) {
        float result = (float) applyRotation(rotation);
        return parent == null ? result : parent.transformRotation(result);
    }

    private Vector apply(Vector point, Vector pivot) {
        if (isTransformationDefault()) return point;
        double radians = Math.toRadians(modifiers.rotationDegrees());
        double dx = point.getX() - pivot.getX(), dz = point.getZ() - pivot.getZ();
        double rx = dx * Math.cos(radians) - dz * Math.sin(radians) + pivot.getX();
        double rz = dx * Math.sin(radians) + dz * Math.cos(radians) + pivot.getZ();
        switch (modifiers.mirror()) {
            case X -> rx = pivot.getX() - (rx - pivot.getX());
            case Z -> rz = pivot.getZ() - (rz - pivot.getZ());
            case XZ -> { rx = pivot.getX() - (rx - pivot.getX()); rz = pivot.getZ() - (rz - pivot.getZ()); }
            case NONE -> { }
        }
        double scale = modifiers.sceneScale();
        rx = pivot.getX() + (rx - pivot.getX()) * scale;
        double ry = pivot.getY() + (point.getY() - pivot.getY()) * scale;
        rz = pivot.getZ() + (rz - pivot.getZ()) * scale;
        return new Vector(rx + modifiers.offsetX(), ry + modifiers.offsetY(), rz + modifiers.offsetZ());
    }

    private boolean isTransformationDefault() {
        return modifiers.rotationDegrees() == 0.0 && modifiers.mirror() == PlaybackModifiers.Mirror.NONE
                && modifiers.sceneScale() == 1.0 && modifiers.offsetX() == 0.0
                && modifiers.offsetY() == 0.0 && modifiers.offsetZ() == 0.0;
    }

    private double applyRotation(double rotation) {
        double result = rotation + modifiers.rotationDegrees();
        if (modifiers.mirror() == PlaybackModifiers.Mirror.X || modifiers.mirror() == PlaybackModifiers.Mirror.XZ) result = -result;
        if (modifiers.mirror() == PlaybackModifiers.Mirror.Z || modifiers.mirror() == PlaybackModifiers.Mirror.XZ) result = -(result + 90.0) - 90.0;
        return clampRotation(result);
    }

    private static double clampRotation(double rotation) {
        rotation %= 360.0;
        if (rotation >= 180.0) rotation -= 360.0;
        if (rotation < -180.0) rotation += 360.0;
        return rotation;
    }

    public Location transform(Location location) {
        Vector result = transform(location.toVector());
        return new Location(location.getWorld(), result.getX(), result.getY(), result.getZ(),
                transformRotation(location.getYaw()), location.getPitch());
    }
}
