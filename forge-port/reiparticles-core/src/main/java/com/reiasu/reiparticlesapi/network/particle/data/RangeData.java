// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.data;

/**
 * Abstract base class for a min-max range of comparable values.
 * Enforces the invariant that {@code min <= max} at construction time.
 *
 * @param <T> the value type, must be self-comparable
 */
public abstract class RangeData<T extends Comparable<? super T>> {

    private T min;
    private T max;

    protected RangeData(T min, T max) {
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("min must be <= " + max);
        }
        this.min = min;
        this.max = max;
    }

    public T getMin() {
        return min;
    }

    public void setMin(T min) {
        this.min = min;
    }

    public T getMax() {
        return max;
    }

    public void setMax(T max) {
        this.max = max;
    }
}
