// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

public final class Math3DUtil {
    private Math3DUtil() {
    }

    public static double length(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double lerp(double from, double to, double alpha) {
        return from + (to - from) * alpha;
    }

    public static Vector3f colorOf(int r, int g, int b) {
        int rr = (int) clamp(r, 0, 255);
        int gg = (int) clamp(g, 0, 255);
        int bb = (int) clamp(b, 0, 255);
        return new Vector3f((float) rr / 255f, (float) gg / 255f, (float) bb / 255f);
    }

    /**
     * Compute Euler angles (pitch, yaw, roll) to orient towards the given target direction.
     *
     * @param target the target direction vector
     * @return float array [pitch, yaw, roll] in radians
     */
    public static float[] calculateEulerAnglesToPointArray(Vector3f target) {
        if (target.x == 0.0f && target.y == 0.0f && target.z == 0.0f) {
            return new float[]{0.0f, 0.0f, 0.0f};
        }
        float pitch = (float) Math.atan2(target.y, Math.sqrt(target.x * target.x + target.z * target.z));
        float yaw = -(float) Math.atan2(target.z, target.x);
        float roll = 0.0f;
        return new float[]{pitch, yaw, roll};
    }

    public static double getYawFromLocation(RelativeLocation location) {
        return Math.atan2(-location.getX(), location.getZ());
    }

    public static double getPitchFromLocation(RelativeLocation location) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        return Math.atan2(-y, Math.sqrt(x * x + z * z));
    }

    /**
     * Generate points along a line from start to end with the given step distance.
     */
    public static List<RelativeLocation> fillLine(RelativeLocation start, RelativeLocation end, double step) {
        List<RelativeLocation> result = new ArrayList<>();
        double dist = start.distance(end);
        if (dist <= 0.0 || step <= 0.0) {
            result.add(start.copy());
            return result;
        }
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double dz = end.getZ() - start.getZ();
        int count = Math.max(1, (int) Math.ceil(dist / step));
        for (int i = 0; i <= count; i++) {
            double t = (double) i / (double) count;
            result.add(new RelativeLocation(
                    start.getX() + dx * t,
                    start.getY() + dy * t,
                    start.getZ() + dz * t
            ));
        }
        return result;
    }

    /**
     * Generate branching lightning bolt points from origin to target.
     *
     * @param target    end point relative to origin
     * @param segments  number of line segments
     * @param branches  number of side branches
     * @return list of points along the bolt path
     */
    public static List<RelativeLocation> getLightningEffectPoints(RelativeLocation target, int segments, int branches) {
        List<RelativeLocation> result = new ArrayList<>();
        if (target == null || segments <= 0) return result;

        java.util.Random rand = new java.util.Random();
        double dx = target.getX() / segments;
        double dy = target.getY() / segments;
        double dz = target.getZ() / segments;

        RelativeLocation prev = new RelativeLocation(0, 0, 0);
        for (int i = 1; i <= segments; i++) {
            double jitterX = (rand.nextDouble() - 0.5) * 2.0;
            double jitterY = (rand.nextDouble() - 0.5) * 2.0;
            double jitterZ = (rand.nextDouble() - 0.5) * 2.0;
            RelativeLocation next = new RelativeLocation(
                    dx * i + jitterX, dy * i + jitterY, dz * i + jitterZ);
            // Fill line between prev and next
            List<RelativeLocation> seg = fillLine(prev, next, 0.5);
            result.addAll(seg);

            // Side branches
            if (branches > 0 && rand.nextInt(3) == 0) {
                RelativeLocation branchEnd = new RelativeLocation(
                        next.getX() + (rand.nextDouble() - 0.5) * 4.0,
                        next.getY() + (rand.nextDouble() - 0.5) * 4.0,
                        next.getZ() + (rand.nextDouble() - 0.5) * 4.0);
                List<RelativeLocation> branchSeg = fillLine(next, branchEnd, 0.5);
                result.addAll(branchSeg);
            }
            prev = next;
        }
        return result;
    }

    public static List<RelativeLocation> getPolygonInCircleVertices(int sides, double radius) {
        List<RelativeLocation> vertices = new ArrayList<>();
        if (sides < 3) {
            return vertices;
        }
        for (int i = 0; i < sides; i++) {
            double t = Math.PI * 2.0 * i / sides;
            vertices.add(new RelativeLocation(Math.cos(t) * radius, 0.0, Math.sin(t) * radius));
        }
        return vertices;
    }

    /**
     * Rotates all points in-place around the given axis by the specified angle
     * using Rodrigues' rotation formula.
     */
    public static void rotateAsAxis(List<RelativeLocation> points, RelativeLocation axis, double radians) {
        if (points == null || points.isEmpty() || axis == null) return;
        double len = axis.length();
        if (len < 1.0E-10) return;
        double ux = axis.getX() / len;
        double uy = axis.getY() / len;
        double uz = axis.getZ() / len;
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        for (RelativeLocation p : points) {
            double x = p.getX();
            double y = p.getY();
            double z = p.getZ();
            double dot = ux * x + uy * y + uz * z;
            double rx = x * cos + (uy * z - uz * y) * sin + ux * dot * (1.0 - cos);
            double ry = y * cos + (uz * x - ux * z) * sin + uy * dot * (1.0 - cos);
            double rz = z * cos + (ux * y - uy * x) * sin + uz * dot * (1.0 - cos);
            p.setX(rx);
            p.setY(ry);
            p.setZ(rz);
        }
    }

    /**
     * Rotates all points in-place so that the current {@code fromAxis} direction
     * is aligned to the {@code toAxis} direction.
     */
    public static void rotatePointsToPoint(List<RelativeLocation> points, RelativeLocation toAxis, RelativeLocation fromAxis) {
        if (points == null || points.isEmpty() || toAxis == null || fromAxis == null) return;
        double fromLen = fromAxis.length();
        double toLen = toAxis.length();
        if (fromLen < 1.0E-10 || toLen < 1.0E-10) return;

        double fx = fromAxis.getX() / fromLen;
        double fy = fromAxis.getY() / fromLen;
        double fz = fromAxis.getZ() / fromLen;
        double tx = toAxis.getX() / toLen;
        double ty = toAxis.getY() / toLen;
        double tz = toAxis.getZ() / toLen;

        // Cross product: rotation axis
        double cx = fy * tz - fz * ty;
        double cy = fz * tx - fx * tz;
        double cz = fx * ty - fy * tx;
        double crossLen = Math.sqrt(cx * cx + cy * cy + cz * cz);

        if (crossLen < 1.0E-10) {
            // Vectors are parallel or anti-parallel
            double dot = fx * tx + fy * ty + fz * tz;
            if (dot < 0) {
                // 180-degree rotation around any perpendicular axis
                RelativeLocation perp = perpendicular(fx, fy, fz);
                rotateAsAxis(points, perp, Math.PI);
            }
            // else: already aligned, nothing to do
            return;
        }

        double dot = fx * tx + fy * ty + fz * tz;
        double angle = Math.acos(clamp(dot, -1.0, 1.0));
        RelativeLocation rotAxis = new RelativeLocation(cx / crossLen, cy / crossLen, cz / crossLen);
        rotateAsAxis(points, rotAxis, angle);
    }

    /**
     * Returns a unit vector perpendicular to (x, y, z).
     */
    private static RelativeLocation perpendicular(double x, double y, double z) {
        if (Math.abs(x) < 0.9) {
            // cross with (1, 0, 0)
            double cy = z;
            double cz = -y;
            double len = Math.sqrt(cy * cy + cz * cz);
            return new RelativeLocation(0.0, cy / len, cz / len);
        } else {
            // cross with (0, 1, 0)
            double cx = -z;
            double cz = x;
            double len = Math.sqrt(cx * cx + cz * cz);
            return new RelativeLocation(cx / len, 0.0, cz / len);
        }
    }
}
