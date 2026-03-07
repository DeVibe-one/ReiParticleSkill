// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles;

import net.minecraft.core.particles.ParticleOptions;

import java.util.UUID;

/**
 * Marker interface for controllable particle effects.
 * <p>
 * Extends {@link ParticleOptions} so implementations can be used with
 * {@link net.minecraft.client.particle.ParticleProvider} factories.
 * The actual serialization is handled by the ReiParticles custom network,
 * so {@link ParticleOptions} methods are implemented with defaults in
 * each concrete class.
 */
public interface ControllableParticleEffect extends ParticleOptions {
    UUID getControlUUID();
    void setControlUUID(UUID uuid);
    boolean getFaceToPlayer();
    ControllableParticleEffect clone();
}

