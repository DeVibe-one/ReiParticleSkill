// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.data;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A range of float values with random sampling support.
 */
public final class FloatRangeData {
    private final float min;
    private final float max;

    public FloatRangeData(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public float getMin() {
        return min;
    }

    public float getMax() {
        return max;
    }

    public float random() {
        return ThreadLocalRandom.current().nextFloat() * (max - min) + min;
    }

    public boolean isIn(float value) {
        return value >= min && value <= max;
    }
}
