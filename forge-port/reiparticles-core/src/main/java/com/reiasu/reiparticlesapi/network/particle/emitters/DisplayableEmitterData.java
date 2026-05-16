// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

import com.reiasu.reiparticlesapi.network.particle.data.SerializableData;
import com.reiasu.reiparticlesapi.particles.ParticleDisplayer;

import javax.annotation.Nullable;

/**
 * Stateless {@link SerializableData} marker for emitter display data.
 * <p>
 * Emitter packets are displayed by {@link ParticleEmittersManager}; this type
 * only preserves the legacy data-factory contract for callers that expect an
 * emitter display data object.
 */
public final class DisplayableEmitterData implements SerializableData {

    @Override
    public SerializableData clone() {
        return this;
    }

    @Override
    @Nullable
    public ParticleDisplayer createDisplayer() {
        return null;
    }
}
