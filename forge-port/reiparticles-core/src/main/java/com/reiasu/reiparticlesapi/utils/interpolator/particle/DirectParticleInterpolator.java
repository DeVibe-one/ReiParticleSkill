// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.interpolator.particle;

import com.reiasu.reiparticlesapi.utils.CircularQueue;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.interpolator.Interpolator;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple direct (straight-line) particle interpolator between two points.
 * Generates intermediate points along a line from the old position to the new position.
 */
public final class DirectParticleInterpolator implements Interpolator {
    private double refinerCount = 2.0;
    private double limit = 256.0;
    private final CircularQueue<RelativeLocation> queue = new CircularQueue<>(2);

    @Override
    public double getRefinerCount() {
        return refinerCount;
    }

    public void setRefinerCount(double value) {
        this.refinerCount = value;
    }

    @Override
    public DirectParticleInterpolator insertPoint(Vec3 vec) {
        queue.addFirst(RelativeLocation.of(vec));
        return this;
    }

    @Override
    public DirectParticleInterpolator insertPoint(Vector3f vec) {
        queue.addFirst(new RelativeLocation(vec.x, vec.y, vec.z));
        return this;
    }

    @Override
    public DirectParticleInterpolator insertPoint(RelativeLocation vec) {
        queue.addFirst(vec.copy());
        return this;
    }

    @Override
    public DirectParticleInterpolator setLimit(double limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public DirectParticleInterpolator setRefiner(double refiner) {
        this.refinerCount = refiner;
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
        RelativeLocation p0 = queue.get(0);
        RelativeLocation p1 = queue.get(1);
        if (p0.distance(p1) > limit) {
            return new ArrayList<>(Collections.singletonList(p1));
        }
        int count = Math.max(1, (int) Math.round(refinerCount));
        List<RelativeLocation> result = new ArrayList<>(count + 1);
        for (int i = 0; i <= count; i++) {
            double t = (double) i / (double) count;
            result.add(new RelativeLocation(
                    p0.getX() + (p1.getX() - p0.getX()) * t,
                    p0.getY() + (p1.getY() - p0.getY()) * t,
                    p0.getZ() + (p1.getZ() - p0.getZ()) * t
            ));
        }
        return result;
    }
}
