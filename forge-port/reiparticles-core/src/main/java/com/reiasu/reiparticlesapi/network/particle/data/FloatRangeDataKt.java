// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.data;

/**
 * Kotlin-style extension utilities for FloatRangeData.
 * In pure Java, these serve as static helper methods.
 */
public final class FloatRangeDataKt {
    private FloatRangeDataKt() {
    }

    public static boolean isIn(float value, FloatRangeData range) {
        return value >= range.getMin() && value <= range.getMax();
    }

    public static FloatRangeData minRangeTo(float min, float max) {
        return new FloatRangeData(min, max);
    }

    public static FloatRangeData maxRangeTo(float max, float min) {
        return new FloatRangeData(min, max);
    }
}
