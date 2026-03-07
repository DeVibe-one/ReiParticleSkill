// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.animation.timeline;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;

/**
 * Collection of standard easing functions and factory methods.
 */
public final class Eases {
    public static final Eases INSTANCE = new Eases();

    public static final Ease linear = t -> t;

    public static final Ease outCubic = x -> {
        double t = clamp01(x);
        return 1.0 - (1.0 - t) * (1.0 - t) * (1.0 - t);
    };

    public static final Ease inOutSine = x -> {
        double t = clamp01(x);
        return (1.0 - Math.cos(Math.PI * t)) / 2.0;
    };

    public static final Ease outExpo = x -> {
        double t = clamp01(x);
        return t >= 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * t);
    };

    public static final Ease inCubic = x -> {
        double t = clamp01(x);
        return t * t * t;
    };

    public static final Ease inOutCubic = x -> {
        double t = clamp01(x);
        return t < 0.5 ? 4.0 * t * t * t : 1.0 - Math.pow(-2.0 * t + 2.0, 3) / 2.0;
    };

    public static final Ease outQuad = x -> {
        double t = clamp01(x);
        return 1.0 - (1.0 - t) * (1.0 - t);
    };

    public static final Ease outBack = outBack(1.70158);
    public static final Ease outElastic = outElastic(2.0943951023931953, 10.0, 0.75);
    public static final Ease outBounce = outBounce(7.5625, 2.75);

    private Eases() {
    }

    public static Ease outBack(double overshoot) {
        return x -> {
            double t = clamp01(x);
            double c3 = overshoot + 1.0;
            return 1.0 + c3 * Math.pow(t - 1.0, 3) + overshoot * Math.pow(t - 1.0, 2);
        };
    }

    public static Ease outElastic(double period, double decay, double shift) {
        return x -> {
            double t = clamp01(x);
            if (t == 0.0 || t == 1.0) return t;
            return Math.pow(2.0, -decay * t) * Math.sin((t * 10.0 - shift) * period) + 1.0;
        };
    }

    public static Ease outElastic(double period, double decay) {
        return outElastic(period, decay, 0.75);
    }

    public static Ease outElastic(double period) {
        return outElastic(period, 10.0, 0.75);
    }

    public static Ease outElastic() {
        return outElastic(2.0943951023931953, 10.0, 0.75);
    }

    public static Ease outBounce(double n1, double d1) {
        return x -> {
            double t = clamp01(x);
            if (t < 1.0 / d1) {
                return n1 * t * t;
            } else if (t < 2.0 / d1) {
                double x2 = t - 1.5 / d1;
                return n1 * x2 * x2 + 0.75;
            } else if (t < 2.5 / d1) {
                double x3 = t - 2.25 / d1;
                return n1 * x3 * x3 + 0.9375;
            } else {
                double x4 = t - 2.625 / d1;
                return n1 * x4 * x4 + 0.984375;
            }
        };
    }

    public static Ease outBounce(double n1) {
        return outBounce(n1, 2.75);
    }

    public static Ease outBounce() {
        return outBounce(7.5625, 2.75);
    }

    /**
     * Create a cubic bezier easing function using RelativeLocation handles.
     * Start at (0,0), end at (1,1). startHandle and endHandle define the two control points.
     */
    public Ease bezierEase(RelativeLocation startHandle, RelativeLocation endHandle) {
        RelativeLocation target = new RelativeLocation(1, 1, 0);
        RelativeLocation end = new RelativeLocation(
                target.getX() + endHandle.getX(),
                target.getY() + endHandle.getY(),
                target.getZ() + endHandle.getZ()
        );
        return x -> {
            double t = clamp01(x);
            double u = 1.0 - t;
            double u2 = u * u;
            double t2 = t * t;
            double y = u2 * u * 0.0
                    + 3.0 * u2 * t * startHandle.getY()
                    + 3.0 * u * t2 * end.getY()
                    + t2 * t * target.getY();
            return target.getY() == 0.0 ? 0.0 : clamp01(y / target.getY());
        };
    }

    /**
     * Create a cubic bezier easing function using Vec3 handles.
     */
    public Ease bezierEase(Vec3 startHandle, Vec3 endHandle) {
        Vec3 target = new Vec3(1.0, 1.0, 0.0);
        Vec3 end = target.add(endHandle);
        return x -> {
            double t = clamp01(x);
            double u = 1.0 - t;
            double u2 = u * u;
            double t2 = t * t;
            double y = u2 * u * 0.0
                    + 3.0 * u2 * t * startHandle.y
                    + 3.0 * u * t2 * end.y
                    + t2 * t * target.y;
            return target.y == 0.0 ? 0.0 : clamp01(y / target.y);
        };
    }

    /**
     * Create a cubic bezier easing function using Vec2 handles.
     */
    public Ease bezierEase(Vec2 startHandle, Vec2 endHandle) {
        Vec2 target = new Vec2(1.0f, 1.0f);
        Vec2 end = new Vec2(target.x + endHandle.x, target.y + endHandle.y);
        return x -> {
            double t = clamp01(x);
            double u = 1.0 - t;
            double u2 = u * u;
            double t2 = t * t;
            double y = u2 * u * 0.0
                    + 3.0 * u2 * t * (double) startHandle.y
                    + 3.0 * u * t2 * (double) end.y
                    + t2 * t * (double) target.y;
            return target.y == 0.0f ? 0.0 : clamp01(y / (double) target.y);
        };
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
