// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.interpolator.particle;

import com.reiasu.reiparticlesapi.utils.CircularQueue;
import com.reiasu.reiparticlesapi.utils.GraphMathHelper;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.interpolator.Interpolator;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Arc-based particle interpolator. Generates intermediate points along a circular arc
 * between two positions, relative to a moving origin.
 */
public final class CircleParticleInterpolator implements Interpolator {
    private final Supplier<RelativeLocation> originProvider;
    private double refinerCount;
    private double limit;
    private final CircularQueue<RelativeLocation> queue;
    private BiConsumer<CircleParticleInterpolator, List<RelativeLocation>> rotater;
    private BiConsumer<CircleParticleInterpolator, List<RelativeLocation>> rotaterBack;

    public CircleParticleInterpolator(Supplier<RelativeLocation> originProvider) {
        this.originProvider = originProvider;
        this.refinerCount = 2.0;
        this.limit = 256.0;
        this.queue = new CircularQueue<>(2);
        this.rotater = (interp, list) -> {};
        this.rotaterBack = (interp, list) -> {};
    }

    public Supplier<RelativeLocation> getOriginProvider() {
        return originProvider;
    }

    @Override
    public double getRefinerCount() {
        return refinerCount;
    }

    public void setRefinerCount(double value) {
        this.refinerCount = value;
    }

    @Override
    public CircleParticleInterpolator insertPoint(Vec3 vec) {
        insertPoint(RelativeLocation.of(vec));
        return this;
    }

    @Override
    public CircleParticleInterpolator insertPoint(Vector3f vec) {
        insertPoint(new RelativeLocation(vec.x, vec.y, vec.z));
        return this;
    }

    @Override
    public CircleParticleInterpolator insertPoint(RelativeLocation vec) {
        RelativeLocation origin = originProvider.get();
        if (vec.minus(origin).length() <= 1.0E-6) {
            return this;
        }
        queue.addFirst(vec);
        return this;
    }

    @Override
    public CircleParticleInterpolator setLimit(double limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public CircleParticleInterpolator setRefiner(double refiner) {
        this.refinerCount = refiner;
        return this;
    }

    /**
     * Set the rotation transform applied before arc interpolation.
     */
    public CircleParticleInterpolator setRotater(BiConsumer<CircleParticleInterpolator, List<RelativeLocation>> rotater) {
        this.rotater = rotater;
        return this;
    }

    /**
     * Set the inverse rotation transform applied after arc interpolation.
     */
    public CircleParticleInterpolator setRotaterBack(BiConsumer<CircleParticleInterpolator, List<RelativeLocation>> rotaterBack) {
        this.rotaterBack = rotaterBack;
        return this;
    }

    @Override
    public List<RelativeLocation> getRefinedResult() {
        if (queue.empty()) {
            return new ArrayList<>();
        }
        if (queue.notNullSize() == 1) {
            return new ArrayList<>(Collections.singletonList(queue.get(0)));
        }

        RelativeLocation origin = originProvider.get();
        RelativeLocation p0 = queue.get(0);
        RelativeLocation p1 = queue.get(1);

        RelativeLocation rel0 = p0.minus(origin);
        RelativeLocation rel1 = p1.minus(origin);
        double dist0 = rel0.length();
        double dist1 = rel1.length();

        if (dist0 <= 1.0E-6 || dist1 <= 1.0E-6) {
            return new ArrayList<>(Collections.singletonList(p1));
        }
        if (p0.distance(p1) > limit) {
            return new ArrayList<>(Collections.singletonList(p1));
        }

        // Compute the angle between the two relative vectors
        double dot = (rel0.getX() * rel1.getX() + rel0.getY() * rel1.getY() + rel0.getZ() * rel1.getZ())
                / (dist0 * dist1);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        double angle = Math.acos(dot);

        int count = Math.max(1, (int) Math.round(refinerCount));
        List<RelativeLocation> result = new ArrayList<>(count + 1);

        for (int i = 0; i <= count; i++) {
            double t = (double) i / (double) count;
            double r = GraphMathHelper.lerp(t, dist0, dist1);
            double a = angle * t;

            // Slerp-like interpolation
            RelativeLocation dir = slerp(rel0, rel1, t, dist0, dist1);
            double dirLen = dir.length();
            if (dirLen > 1.0E-6) {
                dir = new RelativeLocation(
                        dir.getX() / dirLen * r,
                        dir.getY() / dirLen * r,
                        dir.getZ() / dirLen * r
                );
            }
            result.add(origin.plus(dir));
        }

        rotater.accept(this, result);
        rotaterBack.accept(this, result);
        return result;
    }

    private static RelativeLocation slerp(RelativeLocation a, RelativeLocation b, double t,
                                           double lenA, double lenB) {
        if (lenA <= 1.0E-6 || lenB <= 1.0E-6) {
            return new RelativeLocation(
                    a.getX() + (b.getX() - a.getX()) * t,
                    a.getY() + (b.getY() - a.getY()) * t,
                    a.getZ() + (b.getZ() - a.getZ()) * t
            );
        }
        double dot = (a.getX() * b.getX() + a.getY() * b.getY() + a.getZ() * b.getZ()) / (lenA * lenB);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        double omega = Math.acos(dot);
        if (Math.abs(omega) < 1.0E-6) {
            return new RelativeLocation(
                    a.getX() + (b.getX() - a.getX()) * t,
                    a.getY() + (b.getY() - a.getY()) * t,
                    a.getZ() + (b.getZ() - a.getZ()) * t
            );
        }
        double sinOmega = Math.sin(omega);
        double factorA = Math.sin((1.0 - t) * omega) / sinOmega;
        double factorB = Math.sin(t * omega) / sinOmega;
        return new RelativeLocation(
                a.getX() * factorA + b.getX() * factorB,
                a.getY() * factorA + b.getY() * factorB,
                a.getZ() * factorA + b.getZ() * factorB
        );
    }
}
