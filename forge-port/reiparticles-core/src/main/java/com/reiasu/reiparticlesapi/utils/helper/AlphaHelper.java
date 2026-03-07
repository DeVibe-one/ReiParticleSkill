// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper;

import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.utils.GraphMathHelper;

/**
 * Abstract helper that drives alpha (opacity) transitions over a tick-based timeline.
 * Mirrors the pattern of {@link ScaleHelper} but for alpha values.
 */
public abstract class AlphaHelper implements ParticleHelper {
    private double minAlpha;
    private double maxAlpha;
    private int alphaTick;
    private int current;
    private double step;

    protected AlphaHelper(double minAlpha, double maxAlpha, int alphaTick) {
        this.minAlpha = minAlpha;
        this.maxAlpha = maxAlpha;
        this.alphaTick = Math.max(1, alphaTick);
        double temp = Math.min(this.minAlpha, this.maxAlpha);
        this.maxAlpha = Math.max(this.minAlpha, this.maxAlpha);
        this.minAlpha = temp;
        this.step = Math.abs(this.maxAlpha - this.minAlpha) / (double) this.alphaTick;
    }

    public final double getMinAlpha() {
        return minAlpha;
    }

    public final void setMinAlpha(double minAlpha) {
        this.minAlpha = minAlpha;
    }

    public final double getMaxAlpha() {
        return maxAlpha;
    }

    public final void setMaxAlpha(double maxAlpha) {
        this.maxAlpha = maxAlpha;
    }

    public final int getAlphaTick() {
        return alphaTick;
    }

    public final void setAlphaTick(int alphaTick) {
        this.alphaTick = Math.max(1, alphaTick);
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
     * Recalculate step size after changing min/max/tick parameters.
     */
    public AlphaHelper recalculateStep() {
        double temp = Math.min(minAlpha, maxAlpha);
        maxAlpha = Math.max(minAlpha, maxAlpha);
        minAlpha = temp;
        step = Math.abs(maxAlpha - minAlpha) / (double) alphaTick;
        return this;
    }

    /**
     * Jump to the alpha value closest to the given value.
     */
    public void toggleAlpha(double alpha) {
        if (alpha <= minAlpha) {
            resetAlphaMin();
        } else if (alpha >= maxAlpha) {
            current = alphaTick;
            resetAlphaMax();
        } else {
            double point = alpha - minAlpha;
            int tick = (int) Math.round(point / Math.max(1.0E-9, step));
            current = tick;
            doAlphaTo(current);
        }
    }

    public void resetAlphaMin() {
        if (getLoadedGroup() == null) {
            return;
        }
        current = 0;
        setAlpha(minAlpha);
    }

    public void resetAlphaMax() {
        if (getLoadedGroup() == null) {
            return;
        }
        current = alphaTick;
        setAlpha(maxAlpha);
    }

    public void doAlphaTo(int current) {
        if (getLoadedGroup() == null) {
            return;
        }
        this.current = Math.max(0, current);
        if (current >= alphaTick) {
            resetAlphaMax();
        } else if (current <= 0) {
            resetAlphaMin();
        } else {
            double progress = (double) current / (double) alphaTick;
            double alpha = GraphMathHelper.lerp(progress, minAlpha, maxAlpha);
            setAlpha(alpha);
        }
    }

    public void increaseAlpha() {
        if (getLoadedGroup() == null || over()) {
            return;
        }
        current++;
        double progress = (double) current / (double) alphaTick;
        double alpha = GraphMathHelper.lerp(progress, minAlpha, maxAlpha);
        setAlpha(alpha);
    }

    public void decreaseAlpha() {
        if (getLoadedGroup() == null || isZero()) {
            return;
        }
        current--;
        double progress = (double) current / (double) alphaTick;
        double alpha = GraphMathHelper.lerp(progress, minAlpha, maxAlpha);
        setAlpha(alpha);
    }

    public boolean over() {
        return alphaTick <= current;
    }

    public boolean isZero() {
        return current <= 0;
    }

    public abstract Controllable<?> getLoadedGroup();

    public abstract double getCurrentAlpha();

    public abstract void setAlpha(double alpha);
}
