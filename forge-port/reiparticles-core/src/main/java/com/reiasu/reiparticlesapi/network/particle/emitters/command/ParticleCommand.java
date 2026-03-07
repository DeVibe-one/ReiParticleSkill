// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.command;

import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;

/**
 * A command that mutates particle state during the emitter tick loop.
 */
@FunctionalInterface
public interface ParticleCommand {

    /**
     * Execute this command against the given particle data and particle instance.
     *
     * @param data     the mutable particle data
     * @param particle the particle instance
     */
    void execute(ControllableParticleData data, ControllableParticle particle);
}
