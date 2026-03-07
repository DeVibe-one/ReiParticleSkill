// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper;

import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.utils.GraphMathHelper;

public abstract class ScaleHelper implements ParticleHelper {
    private double minScale;
    private double maxScale;
    private int scaleTick;
    private int current;
    private double step;

    protected ScaleHelper(double minScale, double maxScale, int scaleTick) {
        this.minScale = minScale;
        this.maxScale = maxScale;
        this.scaleTick = Math.max(1, scaleTick);
        double temp = Math.min(this.minScale, this.maxScale);
        this.maxScale = Math.max(this.minScale, this.maxScale);
        this.minScale = temp;
        this.step = Math.abs(this.maxScale - this.minScale) / (double) this.scaleTick;
    }

    public final double getMinScale() {
        return minScale;
    }

    public final void setMinScale(double minScale) {
        this.minScale = minScale;
    }

    public final double getMaxScale() {
        return maxScale;
    }

    public final void setMaxScale(double maxScale) {
        this.maxScale = maxScale;
    }

    public final int getScaleTick() {
        return scaleTick;
    }

    public final void setScaleTick(int scaleTick) {
        this.scaleTick = Math.max(1, scaleTick);
    }

    public final int getCurrent() {
        return current;
    }

    protected final void setCurrent(int current) {
        this.current = Math.max(0, current);
    }

    protected final double getStep() {
        return step;
    }

    protected final void setStep(double step) {
        this.step = step;
    }

    public ScaleHelper recalculateStep() {
        double temp = Math.min(minScale, maxScale);
        maxScale = Math.max(minScale, maxScale);
        minScale = temp;
        step = Math.abs(maxScale - minScale) / (double) Math.max(1, scaleTick);
        return this;
    }

    public void toggleScale(double scale) {
        if (scale <= minScale) {
            resetScaleMin();
            return;
        }
        if (scale >= maxScale) {
            current = Math.max(0, scaleTick - 1);
            resetScaleMax();
            return;
        }
        double point = scale - minScale;
        int tick = (int) Math.round(point / Math.max(1.0E-9, step));
        doScaleTo(tick);
    }

    public final void resetScaleMin() {
        if (getLoadedGroup() == null) {
            return;
        }
        current = 0;
        scale(minScale);
    }

    public final void resetScaleMax() {
        if (getLoadedGroup() == null) {
            return;
        }
        current = Math.max(0, scaleTick - 1);
        scale(maxScale);
    }

    public void doScaleTo(int current) {
        if (getLoadedGroup() == null) {
            return;
        }
        this.current = Math.max(0, current);
        if (current >= scaleTick - 1) {
            resetScaleMax();
            return;
        }
        if (current <= 0) {
            resetScaleMin();
            return;
        }
        double lerp = GraphMathHelper.lerp((double) current / (double) scaleTick, minScale, maxScale);
        scale(lerp);
    }

    public void doScale() {
        if (getLoadedGroup() == null || over()) {
            return;
        }
        current++;
        double lerp = GraphMathHelper.lerp((double) current / (double) scaleTick, minScale, maxScale);
        scale(lerp);
    }

    public void doScaleReversed() {
        if (getLoadedGroup() == null || isZero()) {
            return;
        }
        current--;
        double lerp = GraphMathHelper.lerp((double) current / (double) scaleTick, minScale, maxScale);
        scale(lerp);
    }

    public boolean over() {
        return scaleTick <= current;
    }

    public boolean isZero() {
        return current <= 0;
    }

    public abstract Controllable<?> getLoadedGroup();

    public abstract double getGroupScale();

    public abstract void scale(double scale);
}
