// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.event;

import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;

/**
 * Base interface for particle-level events fired during emitter simulation.
 */
public interface ParticleEvent {

    /**
     * Returns the unique string identifier for this event type.
     */
    String getEventID();

    /**
     * Gets the particle instance.
     */
    ControllableParticle getParticle();

    /**
     * Sets the particle instance.
     */
    void setParticle(ControllableParticle particle);

    /**
     * Gets the particle's mutable data.
     */
    ControllableParticleData getParticleData();

    /**
     * Sets the particle's mutable data.
     */
    void setParticleData(ControllableParticleData data);

    /**
     * Whether this event has been canceled by a handler.
     */
    boolean getCanceled();

    /**
     * Cancel or un-cancel this event.
     */
    void setCanceled(boolean canceled);
}
