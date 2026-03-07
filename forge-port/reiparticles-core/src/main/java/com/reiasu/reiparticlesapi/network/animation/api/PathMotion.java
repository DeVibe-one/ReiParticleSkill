// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.animation.api;

import net.minecraft.world.phys.Vec3;

/**
 * Interface defining path-based motion for particle entities.
 * Implementations compute per-tick offsets from an origin, and apply
 * the resulting world position to their target.
 */
public interface PathMotion {
    int getCurrentTick();

    void setCurrentTick(int tick);

    Vec3 getOrigin();

    void setOrigin(Vec3 origin);

    /**
     * Apply the computed world position to the motion target
     * (e.g. teleport a style/emitter to this position).
     */
    void apply(Vec3 actualPos);

    /**
     * Compute and return the next path offset from origin.
     * This also advances the internal tick counter.
     */
    Vec3 next();

    /**
     * Check whether the motion target is still alive/valid.
     * Returns false if the target has been removed or cancelled.
     */
    boolean checkValid();
}
