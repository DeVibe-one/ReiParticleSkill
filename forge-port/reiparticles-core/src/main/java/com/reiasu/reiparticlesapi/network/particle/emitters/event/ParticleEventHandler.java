// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.event;

/**
 * A handler that reacts to a specific {@link ParticleEvent} type.
 * Handlers are ordered by priority (lower priority value = executed first).
 */
public interface ParticleEventHandler extends Comparable<ParticleEventHandler> {

    /**
     * Process the given event. May mutate event state or cancel it.
     */
    void handle(ParticleEvent event);

    /**
     * The event ID this handler targets (must match {@link ParticleEvent#getEventID()}).
     */
    String getTargetEventID();

    /**
     * Unique identifier for this handler instance.
     */
    String getHandlerID();

    /**
     * Execution priority. Lower values run first.
     */
    int getPriority();

    @Override
    default int compareTo(ParticleEventHandler other) {
        return this.getPriority() - other.getPriority();
    }
}
