package com.ultraop.mocap.playback;

import org.bukkit.Location;
import org.bukkit.util.Vector;

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

    /** Transforms a block-space position and applies the configured rounding policy. */
    public Vector transformBlockPosition(Vector point) {
        Vector transformed = transformPos(point.clone(), null);
        if (modifiers.transformationConfig().roundBlockPos()) {
            transformed.setX(Math.round(transformed.getX()));
            transformed.setY(Math.round(transformed.getY()));
            transformed.setZ(Math.round(transformed.getZ()));
        }
        return transformed;
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
        double dx = point.getX() - pivot.getX();
        double dz = point.getZ() - pivot.getZ();
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
        return modifiers.rotationDegrees() == 0.0
                && modifiers.mirror() == PlaybackModifiers.Mirror.NONE
                && modifiers.sceneScale() == 1.0
                && modifiers.offsetX() == 0.0
                && modifiers.offsetY() == 0.0
                && modifiers.offsetZ() == 0.0;
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
