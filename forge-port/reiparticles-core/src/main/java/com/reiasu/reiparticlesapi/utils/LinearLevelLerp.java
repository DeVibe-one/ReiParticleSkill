// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-level linear interpolation utility.
 * Supports chained interpolation levels for smooth multi-stage transitions.
 */
public final class LinearLevelLerp {
    private List<Double> levels = new ArrayList<>();
    private List<Double> prevLevels = new ArrayList<>();

    public LinearLevelLerp addLevel() {
        levels.add(0.0);
        prevLevels.add(0.0);
        return this;
    }

    public LinearLevelLerp setLevelProgress(int level, double progress) {
        if (level >= levels.size()) {
            throw new ArrayIndexOutOfBoundsException("Not enough interpolation levels");
        }
        prevLevels.set(level, levels.get(level));
        levels.set(level, progress);
        return this;
    }

    public LinearLevelLerp setLevelProgress(int level, float progress) {
        return setLevelProgress(level, (double) progress);
    }

    public double lerp(double min, double max) {
        if (levels.isEmpty()) {
            return min;
        }
        double value = min + (max - min) * levels.get(0);
        for (int i = 1; i < levels.size(); i++) {
            double prev = min + (max - min) * prevLevels.get(i - 1);
            double now = min + (max - min) * levels.get(i - 1);
            value = prev + (now - prev) * levels.get(i);
        }
        return value;
    }

    public float lerp(float min, float max) {
        return (float) lerp((double) min, (double) max);
    }

    public Vec3 lerp(Vec3 min, Vec3 max) {
        return new Vec3(
                lerp(min.x, max.x),
                lerp(min.y, max.y),
                lerp(min.z, max.z)
        );
    }

    public Vector3f lerp(Vector3f min, Vector3f max) {
        return new Vector3f(
                lerp(min.x, max.x),
                lerp(min.y, max.y),
                lerp(min.z, max.z)
        );
    }
}
