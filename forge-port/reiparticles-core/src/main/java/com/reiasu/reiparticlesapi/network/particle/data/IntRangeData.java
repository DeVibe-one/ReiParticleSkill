// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.data;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A range of integer values with random sampling support.
 * <p>
 * The {@link #random()} method returns a value in {@code [min, max)} following
 * the original Kotlin Random.nextInt(min, max) semantics.
 */
public final class IntRangeData extends RangeData<Integer> {

    public IntRangeData(int min, int max) {
        super(min, max);
    }

    /**
     * Returns a random integer in {@code [min, max)}.
     */
    public int random() {
        return ThreadLocalRandom.current().nextInt(getMin(), getMax());
    }
}
