// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.data;

import com.reiasu.reiparticlesapi.particles.ParticleDisplayer;

/**
 * Marker interface for data objects that can be network-serialized and
 * used to create a {@link ParticleDisplayer} on the client side.
 * <p>
 * Forge port note: the original Fabric {@code getCodec()} returned a
 * StreamCodec for network serialization. In the Forge port, serialization
 * is handled at a higher level using FriendlyByteBuf directly, so the
 * codec method is omitted. Implementations should still support
 * clone and createDisplayer.
 */
public interface SerializableData {

    /**
     * Returns a deep copy of this data.
     */
    SerializableData clone();

    /**
     * Creates a {@link ParticleDisplayer} that renders the particle
     * described by this data.
     */
    ParticleDisplayer createDisplayer();
}
