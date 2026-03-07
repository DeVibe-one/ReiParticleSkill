// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

/**
 * Factory interface for creating {@link ControllableParticleData} instances.
 * <p>
 * Used by emitter implementations to produce the initial per-particle data
 * when spawning new particles each tick.
 */
@FunctionalInterface
public interface ParticleDataFactory {

    /**
     * Creates a new {@link ControllableParticleData} with default values.
     */
    ControllableParticleData create();
}
