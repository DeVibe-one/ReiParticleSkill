// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;

/**
 * Moves a {@link RelativeLocation} reference toward a target over a fixed number of ticks.
 * Each call to {@link #next()} advances the reference by (target - default) / maxTick.
 */
public final class MovementTargetHelper {
    private final RelativeLocation reference;
    private RelativeLocation target;
    private final int maxTick;
    private final RelativeLocation defaultPos;

    public MovementTargetHelper(RelativeLocation reference, RelativeLocation target, int maxTick) {
        this.reference = reference;
        this.target = target;
        this.maxTick = Math.max(1, maxTick);
        this.defaultPos = reference.clone();
    }

    public RelativeLocation getReference() {
        return reference;
    }

    public RelativeLocation getTarget() {
        return target;
    }

    public void setTarget(RelativeLocation target) {
        this.target = target;
    }

    public int getMaxTick() {
        return maxTick;
    }

    public RelativeLocation getDefaultPos() {
        return defaultPos;
    }

    /**
     * Advance one tick toward the target.
     */
    public void next() {
        RelativeLocation direction = target.minus(defaultPos).scale(1.0 / (double) maxTick);
        reference.add(direction);
    }

    /**
     * Reset the reference to its original position.
     */
    public void reset() {
        reference.setX(defaultPos.getX());
        reference.setY(defaultPos.getY());
        reference.setZ(defaultPos.getZ());
    }
}
