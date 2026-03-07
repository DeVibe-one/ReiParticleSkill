// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper.impl;

import com.reiasu.reiparticlesapi.network.particle.style.ParticleGroupStyle;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.particles.control.ParticleController;
import com.reiasu.reiparticlesapi.utils.helper.AlphaHelper;

/**
 * {@link AlphaHelper} implementation for {@link ParticleGroupStyle}.
 * Propagates alpha changes recursively to all particles in the style's hierarchy.
 * <p>
 * Propagates alpha changes to all particles in the style via
 * {@link ParticleController} instances found in the style's particle map.
 */
public final class StyleAlphaHelper extends AlphaHelper {
    private ParticleGroupStyle style;
    private float currentAlpha = 1.0f;

    public StyleAlphaHelper(double minAlpha, double maxAlpha, int alphaTick) {
        super(minAlpha, maxAlpha, alphaTick);
    }

    public ParticleGroupStyle getStyle() {
        return style;
    }

    public void setStyle(ParticleGroupStyle style) {
        this.style = style;
    }

    public float getCurrentAlphaFloat() {
        return currentAlpha;
    }

    public void setCurrentAlpha(float currentAlpha) {
        this.currentAlpha = currentAlpha;
    }

    @Override
    public Controllable<?> getLoadedGroup() {
        return style;
    }

    @Override
    public double getCurrentAlpha() {
        return currentAlpha;
    }

    @Override
    public void setAlpha(double alpha) {
        this.currentAlpha = (float) alpha;
        if (style == null) return;
        for (Controllable<?> c : style.getParticles().values()) {
            if (c instanceof ParticleController pc) {
                try {
                    pc.getParticle().setParticleAlpha(currentAlpha);
                } catch (IllegalStateException e) {
                    // Particle not loaded yet
                }
            }
        }
    }

    @Override
    public void loadController(Controllable<?> controller) {
        if (!(controller instanceof ParticleGroupStyle pgs)) {
            return;
        }
        this.style = pgs;
        setAlpha(getMinAlpha());
    }
}
