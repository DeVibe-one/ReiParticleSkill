// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ParticleEmitterVisibilityTrackerTest {
    @Test
    void computeLodIntervalScalesWithViewerDistance() {
        assertEquals(1, ParticleEmitterVisibilityTracker.computeLodInterval(5.0, 100.0));
        assertEquals(3, ParticleEmitterVisibilityTracker.computeLodInterval(30.0, 100.0));
        assertEquals(6, ParticleEmitterVisibilityTracker.computeLodInterval(60.0, 100.0));
        assertEquals(12, ParticleEmitterVisibilityTracker.computeLodInterval(90.0, 100.0));
    }
}
