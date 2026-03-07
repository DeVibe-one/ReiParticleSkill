// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.animation.api;

import net.minecraft.world.phys.Vec3;

/**
 * Abstract base for path motions. Implements the tick-incrementing {@link #next()}
 * method; subclasses need only provide {@link #pathFunction()}, {@link #apply(Vec3)},
 * and {@link #checkValid()}.
 */
public abstract class AbstractPathMotion implements PathMotion {
    private Vec3 origin;
    private int currentTick;

    protected AbstractPathMotion(Vec3 origin) {
        this.origin = origin;
    }

    @Override
    public Vec3 getOrigin() {
        return origin;
    }

    @Override
    public void setOrigin(Vec3 origin) {
        this.origin = origin;
    }

    @Override
    public int getCurrentTick() {
        return currentTick;
    }

    @Override
    public void setCurrentTick(int tick) {
        this.currentTick = tick;
    }

    @Override
    public Vec3 next() {
        Vec3 value = pathFunction();
        currentTick++;
        return value;
    }

    /**
     * Compute the path offset for the current tick.
     * Called by {@link #next()} before the tick counter is incremented.
     */
    public abstract Vec3 pathFunction();
}
