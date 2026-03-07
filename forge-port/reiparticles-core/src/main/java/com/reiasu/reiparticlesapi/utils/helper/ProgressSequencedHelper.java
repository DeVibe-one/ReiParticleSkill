// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper;

import com.reiasu.reiparticlesapi.particles.Controllable;

/**
 * Abstract helper that drives sequenced progress (show/hide particles in sequence)
 * over a tick-based timeline.
 */
public abstract class ProgressSequencedHelper implements ParticleHelper {
    private int maxCount;
    private int progressMaxTick;
    private int current;
    private double step;
    private double remainder;

    protected ProgressSequencedHelper(int maxCount, int progressMaxTick) {
        if (maxCount <= 0) {
            throw new IllegalArgumentException("maxCount must be > 0");
        }
        if (progressMaxTick <= 0) {
            throw new IllegalArgumentException("progressMaxTick must be > 0");
        }
        this.maxCount = maxCount;
        this.progressMaxTick = progressMaxTick;
        this.step = (double) maxCount / (double) progressMaxTick;
        this.remainder = 0.0;
    }

    public final int getMaxCount() {
        return maxCount;
    }

    public final void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public final int getProgressMaxTick() {
        return progressMaxTick;
    }

    public final void setProgressMaxTick(int progressMaxTick) {
        this.progressMaxTick = progressMaxTick;
    }

    public final int getCurrent() {
        return current;
    }

    protected final void setCurrent(int current) {
        this.current = current;
    }

    protected final double getStep() {
        return step;
    }

    protected final void setStep(double step) {
        this.step = step;
    }

    /**
     * Recalculate step size after changing maxCount or progressMaxTick.
     */
    public ProgressSequencedHelper recalculateStep() {
        this.step = (double) maxCount / (double) progressMaxTick;
        this.remainder = 0.0;
        return this;
    }

    /**
     * Set progress by a normalized percentage [0.0, 1.0].
     */
    public void setProgress(double percent) {
        double clamped = Math.max(0.0, Math.min(1.0, percent));
        int targetTick = (int) Math.round(clamped * (double) progressMaxTick);
        doProgressTo(targetTick);
    }

    /**
     * Advance progress by one tick (add particles).
     */
    public void increaseProgress() {
        if (over() || getLoadedStyle() == null) {
            return;
        }
        int actualStep = calculateActualStep(true);
        if (actualStep > 0) {
            addMultiple(actualStep);
            current++;
        }
    }

    /**
     * Reverse progress by one tick (remove particles).
     */
    public void decreaseProgress() {
        if (isZero() || getLoadedStyle() == null) {
            return;
        }
        int actualStep = calculateActualStep(false);
        if (actualStep > 0) {
            removeMultiple(actualStep);
            current--;
        }
    }

    /**
     * Jump to a specific tick, adding or removing particles as needed.
     */
    public void doProgressTo(int targetTick) {
        if (getLoadedStyle() == null) {
            return;
        }
        int target = Math.max(0, Math.min(progressMaxTick, targetTick));
        if (target > current) {
            int addCount = calculateTotalStep(current, target);
            addMultiple(addCount);
        } else if (target < current) {
            int removeCount = calculateTotalStep(target, current);
            removeMultiple(removeCount);
        }
        current = target;
    }

    public boolean over() {
        return current >= progressMaxTick;
    }

    public boolean isZero() {
        return current <= 0;
    }

    private int calculateActualStep(boolean isAdding) {
        double raw = step + remainder;
        int integerPart = (int) raw;
        remainder = raw - integerPart;
        return Math.max(1, integerPart);
    }

    private int calculateTotalStep(int from, int to) {
        int delta = to - from;
        return (int) Math.round((double) delta * step);
    }

    public abstract void addMultiple(int count);

    public abstract void removeMultiple(int count);

    public abstract Controllable<?> getLoadedStyle();

    protected abstract void changeStatusBatch(int[] indexes, boolean status);
}
