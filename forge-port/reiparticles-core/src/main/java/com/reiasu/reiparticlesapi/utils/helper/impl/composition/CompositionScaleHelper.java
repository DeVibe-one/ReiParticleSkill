// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper.impl.composition;

import com.reiasu.reiparticlesapi.network.particle.composition.ParticleComposition;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.utils.helper.ScaleHelper;

/**
 * {@link ScaleHelper} implementation that delegates scale operations to a
 * {@link ParticleComposition}.
 */
public final class CompositionScaleHelper extends ScaleHelper {
    private ParticleComposition composition;

    public CompositionScaleHelper(double minScale, double maxScale, int scaleTick) {
        super(minScale, maxScale, scaleTick);
    }

    public ParticleComposition getComposition() {
        return composition;
    }

    public void setComposition(ParticleComposition composition) {
        this.composition = composition;
    }

    @Override
    public Controllable<?> getLoadedGroup() {
        return composition; // ParticleComposition now implements Controllable
    }

    @Override
    public double getGroupScale() {
        return composition != null ? composition.getScale() : 1.0;
    }

    @Override
    public void scale(double scale) {
        if (composition != null) {
            composition.scale(scale);
        }
    }

    @Override
    public void loadController(Controllable<?> controller) {
        if (controller instanceof ParticleComposition pc) {
            loadComposition(pc);
        }
    }

    /**
     * Directly load a composition reference (for use when ParticleComposition
     * doesn't implement Controllable yet).
     */
    public void loadComposition(ParticleComposition composition) {
        this.composition = composition;
        if (composition != null) {
            composition.scale(getMinScale());
        }
    }
}
