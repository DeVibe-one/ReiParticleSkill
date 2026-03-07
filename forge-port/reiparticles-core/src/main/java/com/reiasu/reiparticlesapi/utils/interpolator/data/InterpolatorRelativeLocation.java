// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.interpolator.data;

import com.reiasu.reiparticlesapi.utils.GraphMathHelper;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;

/**
 * Interpolated RelativeLocation value with component-wise lerp.
 */
public final class InterpolatorRelativeLocation implements InterpolatorData<RelativeLocation> {
    private RelativeLocation value;
    private RelativeLocation last;

    public InterpolatorRelativeLocation(RelativeLocation value) {
        this.value = value;
        this.last = value;
    }

    public RelativeLocation getLast() {
        return last;
    }

    public void setLast(RelativeLocation last) {
        this.last = last;
    }

    @Override
    public InterpolatorRelativeLocation update(RelativeLocation current) {
        this.last = this.value;
        this.value = current;
        return this;
    }

    @Override
    public RelativeLocation getWithInterpolator(Number progress) {
        double p = progress.doubleValue();
        return new RelativeLocation(
                GraphMathHelper.lerp(p, last.getX(), value.getX()),
                GraphMathHelper.lerp(p, last.getY(), value.getY()),
                GraphMathHelper.lerp(p, last.getZ(), value.getZ())
        );
    }

    @Override
    public RelativeLocation getCurrent() {
        return value;
    }
}
