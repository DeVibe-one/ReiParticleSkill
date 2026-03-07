// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.data;

/**
 * Utility methods for {@link IntRangeData}, ported from Kotlin extension functions.
 */
public final class IntRangeDataKt {

    private IntRangeDataKt() {
    }

    /**
     * Checks whether the given integer falls within the range (inclusive on both ends).
     */
    public static boolean isIn(int value, IntRangeData range) {
        return value >= range.getMin() && value <= range.getMax();
    }

    /**
     * Creates an {@link IntRangeData} where the receiver is the min.
     */
    public static IntRangeData minRangeTo(int min, int max) {
        return new IntRangeData(min, max);
    }

    /**
     * Creates an {@link IntRangeData} where the receiver is the max.
     */
    public static IntRangeData maxRangeTo(int max, int min) {
        return new IntRangeData(min, max);
    }
}
