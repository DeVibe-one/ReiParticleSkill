// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper.impl;

import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.particles.control.ParticleController;
import com.reiasu.reiparticlesapi.utils.helper.AlphaHelper;

/**
 * {@link AlphaHelper} implementation for a single particle controller.
 * <p>
 * Delegates alpha operations through {@link ParticleController} to the
 * underlying {@link com.reiasu.reiparticlesapi.particles.ControllableParticle}.
 */
public final class ParticleAlphaHelper extends AlphaHelper {
    private Controllable<?> controller;
    private float currentAlpha = 1.0f;

    public ParticleAlphaHelper(double minAlpha, double maxAlpha, int alphaTick) {
        super(minAlpha, maxAlpha, alphaTick);
    }

    public Controllable<?> getController() {
        return controller;
    }

    public void setController(Controllable<?> controller) {
        this.controller = controller;
    }

    @Override
    public Controllable<?> getLoadedGroup() {
        return controller;
    }

    @Override
    public double getCurrentAlpha() {
        if (controller instanceof ParticleController pc) {
            try {
                return pc.getParticle().getParticleAlpha();
            } catch (IllegalStateException e) {
                // Particle not loaded yet
            }
        }
        return currentAlpha;
    }

    @Override
    public void setAlpha(double alpha) {
        this.currentAlpha = (float) alpha;
        if (controller instanceof ParticleController pc) {
            try {
                pc.getParticle().setParticleAlpha((float) alpha);
            } catch (IllegalStateException e) {
                // Particle not loaded yet
            }
        }
    }

    @Override
    public void loadController(Controllable<?> controller) {
        if (!(controller instanceof ParticleController)) return;
        this.controller = controller;
    }
}
