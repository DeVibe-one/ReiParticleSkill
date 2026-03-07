// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.interpolator.data;

import com.reiasu.reiparticlesapi.utils.GraphMathHelper;

/**
 * Interpolated float value with lerp support.
 */
public final class InterpolatorFloat implements InterpolatorData<Float> {
    private float value;
    private float last;

    public InterpolatorFloat(float value) {
        this.value = value;
    }

    public float getLast() {
        return last;
    }

    public void setLast(float last) {
        this.last = last;
    }

    public InterpolatorFloat update(float current) {
        this.last = this.value;
        this.value = current;
        return this;
    }

    @Override
    public InterpolatorFloat update(Float current) {
        return update(current.floatValue());
    }

    @Override
    public Float getWithInterpolator(Number progress) {
        return (float) GraphMathHelper.lerp(progress.doubleValue(), last, value);
    }

    @Override
    public Float getCurrent() {
        return value;
    }

    // Operator-style methods

    public InterpolatorFloat plus(float f) {
        update(value + f);
        return this;
    }

    public InterpolatorFloat minus(float f) {
        update(value - f);
        return this;
    }

    public InterpolatorFloat times(float f) {
        update(value * f);
        return this;
    }

    public InterpolatorFloat div(float f) {
        if (f == 0.0f) {
            throw new IllegalArgumentException("Division by zero");
        }
        update(value / f);
        return this;
    }

    public InterpolatorFloat unaryMinus() {
        update(-value);
        return this;
    }

    public InterpolatorFloat plus(InterpolatorFloat other) {
        update(value + other.value);
        return this;
    }

    public InterpolatorFloat minus(InterpolatorFloat other) {
        update(value - other.value);
        return this;
    }

    public InterpolatorFloat times(InterpolatorFloat other) {
        update(value * other.value);
        return this;
    }

    public InterpolatorFloat div(InterpolatorFloat other) {
        if (other.value == 0.0f) {
            throw new IllegalArgumentException("Division by zero");
        }
        update(value / other.value);
        return this;
    }
}
