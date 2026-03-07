// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.interpolator.data;

import com.reiasu.reiparticlesapi.utils.GraphMathHelper;
import net.minecraft.world.phys.Vec3;

/**
 * Interpolated Vec3 value with component-wise lerp.
 */
public final class InterpolatorVec3d implements InterpolatorData<Vec3> {
    private Vec3 value;
    private Vec3 last;

    public InterpolatorVec3d(Vec3 value) {
        this.value = value;
        this.last = value;
    }

    public Vec3 getLast() {
        return last;
    }

    public void setLast(Vec3 last) {
        this.last = last;
    }

    @Override
    public InterpolatorVec3d update(Vec3 current) {
        this.last = this.value;
        this.value = current;
        return this;
    }

    @Override
    public Vec3 getWithInterpolator(Number progress) {
        double p = progress.doubleValue();
        return new Vec3(
                GraphMathHelper.lerp(p, last.x, value.x),
                GraphMathHelper.lerp(p, last.y, value.y),
                GraphMathHelper.lerp(p, last.z, value.z)
        );
    }

    @Override
    public Vec3 getCurrent() {
        return value;
    }
}
