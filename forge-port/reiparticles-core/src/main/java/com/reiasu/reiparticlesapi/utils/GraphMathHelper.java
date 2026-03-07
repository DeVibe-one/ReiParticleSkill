// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils;

import net.minecraft.world.phys.Vec3;

public final class GraphMathHelper {
    private GraphMathHelper() {
    }

    public static float lerp(float alpha, float from, float to) {
        return from + (to - from) * alpha;
    }

    public static double lerp(double alpha, double from, double to) {
        return from + (to - from) * alpha;
    }

    /**
     * Linearly interpolate between two {@link Vec3} positions.
     *
     * @param alpha interpolation factor (0 = from, 1 = to)
     * @param from  start position
     * @param to    end position
     * @return interpolated position
     */
    public static Vec3 lerp(float alpha, Vec3 from, Vec3 to) {
        return new Vec3(
                lerp((double) alpha, from.x, to.x),
                lerp((double) alpha, from.y, to.y),
                lerp((double) alpha, from.z, to.z)
        );
    }

    public static double inverseLerp(double value, double min, double max) {
        if (max == min) {
            return 0.0;
        }
        return (value - min) / (max - min);
    }

    public static double smoothstep(double edge0, double edge1, double x) {
        double t = Math3DUtil.clamp(inverseLerp(x, edge0, edge1), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * Inverse-power distance falloff. Returns a value in [0, 1] that decreases
     * with distance according to {@code 1 / (1 + (dist/range)^power)}.
     * <p>
     * When {@code dist == 0}, returns 1.0. When {@code dist == range}, returns 0.5
     * for {@code power == 1}.
     *
     * @param dist    the distance from the source
     * @param range   the characteristic range (half-strength distance at power=1)
     * @param power   the falloff exponent (higher = steeper)
     * @return falloff factor in [0, 1]
     */
    public static double inversePowerFalloff(double dist, double range, double power) {
        if (range <= 0.0) {
            return 0.0;
        }
        double ratio = dist / range;
        return 1.0 / (1.0 + Math.pow(ratio, power));
    }

    /**
     * Exponential damping factor for velocity drag.
     * Returns {@code exp(-damping * dt)}, clamped to [0, 1].
     *
     * @param damping the damping coefficient (higher = more drag)
     * @param dt      the time step
     * @return multiplicative factor in [0, 1]
     */
    public static double expDampFactor(double damping, double dt) {
        double factor = Math.exp(-damping * dt);
        return Math.max(0.0, Math.min(1.0, factor));
    }
}
