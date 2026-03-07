// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;

/**
 * Moves a {@link RelativeLocation} reference by a direction vector each tick,
 * up to a maximum number of ticks.
 */
public final class MovementDirectionHelper {
    private final RelativeLocation reference;
    private RelativeLocation direction;
    private final int maxTick;
    private int tick;
    private final RelativeLocation defaultPos;

    public MovementDirectionHelper(RelativeLocation reference, RelativeLocation direction, int maxTick) {
        this.reference = reference;
        this.direction = direction;
        this.maxTick = maxTick;
        this.defaultPos = reference.clone();
    }

    public RelativeLocation getReference() {
        return reference;
    }

    public RelativeLocation getDirection() {
        return direction;
    }

    public void setDirection(RelativeLocation direction) {
        this.direction = direction;
    }

    public int getMaxTick() {
        return maxTick;
    }

    public int getTick() {
        return tick;
    }

    public void setTick(int tick) {
        this.tick = tick;
    }

    public RelativeLocation getDefaultPos() {
        return defaultPos;
    }

    /**
     * Advance one tick: move the reference by the direction vector.
     */
    public void next() {
        if (tick++ > maxTick) {
            return;
        }
        reference.add(direction);
    }

    /**
     * Reset the reference to its original position and tick counter to 0.
     */
    public void reset() {
        reference.setX(defaultPos.getX());
        reference.setY(defaultPos.getY());
        reference.setZ(defaultPos.getZ());
        tick = 0;
    }
}
