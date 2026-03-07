// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.composition;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;

import java.util.Collections;
import java.util.Map;

/**
 * Placeholder composition that auto-manages itself.
 * Will be expanded as more of the original API is ported.
 */
public class AutoParticleComposition extends ParticleComposition {

    @Override
    public Map<CompositionData, RelativeLocation> getParticles() {
        return Collections.emptyMap();
    }

    @Override
    public void onDisplay() {
        // No-op placeholder
    }
}
