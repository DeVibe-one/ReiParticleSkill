// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.barrages;

import com.reiasu.reiparticlesapi.utils.Math3DUtil;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

/**
 * Axis-aligned hit box defined by two corner points.
 * Corners are automatically sorted so (x1,y1,z1) &le; (x2,y2,z2).
 */
public final class HitBox {

    private double x1, y1, z1;
    private double x2, y2, z2;

    public HitBox(double x1, double y1, double z1, double x2, double y2, double z2) {
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        replacePoint();
    }

    /**
     * Creates a centered HitBox with dimensions {@code dx * dy * dz}.
     */
    public static HitBox of(double dx, double dy, double dz) {
        return new HitBox(-dx / 2.0, -dy / 2.0, -dz / 2.0, dx / 2.0, dy / 2.0, dz / 2.0);
    }

    // ---- Getters / Setters ----

    public double getX1() { return x1; }
    public void setX1(double v) { this.x1 = v; }
    public double getY1() { return y1; }
    public void setY1(double v) { this.y1 = v; }
    public double getZ1() { return z1; }
    public void setZ1(double v) { this.z1 = v; }
    public double getX2() { return x2; }
    public void setX2(double v) { this.x2 = v; }
    public double getY2() { return y2; }
    public void setY2(double v) { this.y2 = v; }
    public double getZ2() { return z2; }
    public void setZ2(double v) { this.z2 = v; }

    /**
     * Returns a Minecraft {@link AABB} offset by the given center position.
     */
    public AABB ofBox(Vec3 center) {
        return new AABB(
                x1 + center.x, y1 + center.y, z1 + center.z,
                x2 + center.x, y2 + center.y, z2 + center.z
        );
    }

    /**
     * Rotates the two corner points from {@code axis} direction to {@code to} direction,
     * then re-normalizes so corners stay sorted.
     */
    public void rotateTo(RelativeLocation axis, RelativeLocation to) {
        RelativeLocation p1 = new RelativeLocation(x1, y1, z1);
        RelativeLocation p2 = new RelativeLocation(x2, y2, z2);
        List<RelativeLocation> points = Arrays.asList(p1, p2);
        Math3DUtil.rotatePointsToPoint(points, to, axis);
        x1 = p1.getX(); y1 = p1.getY(); z1 = p1.getZ();
        x2 = p2.getX(); y2 = p2.getY(); z2 = p2.getZ();
        replacePoint();
    }

    /**
     * Returns a copy of this HitBox with the same dimensions.
     */
    public HitBox copy() {
        return new HitBox(x1, y1, z1, x2, y2, z2);
    }

    // ---- Internal ----

    private void replacePoint() {
        double tx = x1; x1 = Math.min(x1, x2); x2 = Math.max(tx, x2);
        double ty = y1; y1 = Math.min(y1, y2); y2 = Math.max(ty, y2);
        double tz = z1; z1 = Math.min(z1, z2); z2 = Math.max(tz, z2);
    }

    @Override
    public String toString() {
        return "HitBox(x1=" + x1 + ", y1=" + y1 + ", z1=" + z1
                + ", x2=" + x2 + ", y2=" + y2 + ", z2=" + z2 + ")";
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(x1);
        result = 31 * result + Double.hashCode(y1);
        result = 31 * result + Double.hashCode(z1);
        result = 31 * result + Double.hashCode(x2);
        result = 31 * result + Double.hashCode(y2);
        result = 31 * result + Double.hashCode(z2);
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof HitBox h)) return false;
        return Double.compare(x1, h.x1) == 0
                && Double.compare(y1, h.y1) == 0
                && Double.compare(z1, h.z1) == 0
                && Double.compare(x2, h.x2) == 0
                && Double.compare(y2, h.y2) == 0
                && Double.compare(z2, h.z2) == 0;
    }
}
