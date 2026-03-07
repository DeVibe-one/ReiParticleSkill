// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles;

import net.minecraft.world.phys.Vec3;

/**
 * Interpolator for particle position lerping between ticks.
 */
public interface ParticleLerpInterpolator {
    /**
     * Compute an interpolated position between previous and current tick positions.
     *
     * @param prev    the position at the previous tick
     * @param current the position at the current tick
     * @param delta   the partial-tick fraction (0..1)
     * @return the interpolated position
     */
    Vec3 consume(Vec3 prev, Vec3 current, float delta);
}
