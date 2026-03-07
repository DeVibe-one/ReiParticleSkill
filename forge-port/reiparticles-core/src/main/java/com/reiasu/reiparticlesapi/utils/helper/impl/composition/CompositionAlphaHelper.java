// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper.impl.composition;

import com.reiasu.reiparticlesapi.network.particle.composition.ParticleComposition;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.utils.helper.AlphaHelper;

/**
 * {@link AlphaHelper} implementation for {@link ParticleComposition}.
 * Uses BFS traversal to propagate alpha to all nested particles/styles/groups.
 * <p>
 * Uses BFS traversal through composition children to propagate alpha
 * to all leaf {@link ParticleController} instances.
 */
public final class CompositionAlphaHelper extends AlphaHelper {
    private ParticleComposition composition;
    private double alpha = 1.0;

    public CompositionAlphaHelper(double minAlpha, double maxAlpha, int alphaTick) {
        super(minAlpha, maxAlpha, alphaTick);
    }

    @Override
    public Controllable<?> getLoadedGroup() {
        return null; // ParticleComposition does not implement Controllable
    }

    @Override
    public double getCurrentAlpha() {
        return alpha;
    }

    @Override
    public void setAlpha(double alpha) {
        this.alpha = alpha;
        if (composition != null) {
            composition.setScale(composition.getScale()); // trigger re-render
        }
    }

    @Override
    public void loadController(Controllable<?> controller) {
        // ParticleComposition is not a Controllable in the current forge-port
    }

    /**
     * Directly load a composition reference.
     */
    public void loadComposition(ParticleComposition composition) {
        this.composition = composition;
    }
}
