// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.core.emitters.p1;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SummonExplosionEmitterTest {
    @Test
    void shouldBeDiscoverableByRuntimeAutoRegistrar() {
        assertTrue(SummonExplosionEmitter.class.isAnnotationPresent(ReiAutoRegister.class));
    }

    @Test
    void collectEnderPowerShouldBeDiscoverableByRuntimeAutoRegistrar() {
        assertTrue(CollectEnderPowerEmitter.class.isAnnotationPresent(ReiAutoRegister.class));
    }
}
