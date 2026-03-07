// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.interpolator.data;

/**
 * Generic interpolation data container.
 * Stores a current value and provides interpolated results based on a progress factor.
 *
 * @param <T> the value type
 */
public interface InterpolatorData<T> {
    InterpolatorData<T> update(T current);

    T getWithInterpolator(Number progress);

    T getCurrent();
}
