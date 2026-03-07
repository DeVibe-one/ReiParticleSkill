// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles.control.group;

import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;

import java.util.Map;
import java.util.UUID;

/**
 * Provider (factory) for creating and modifying {@link ControllableParticleGroup} instances.
 * <p>
 * Registered with {@link ClientParticleGroupManager} for client-side group creation
 * when receiving server-side packets.
 *
 * @deprecated Use ParticleGroupStyle instead.
 */
@Deprecated
public interface ControllableParticleGroupProvider {

    /**
     * Create a new particle group with the given UUID and initialization args.
     */
    ControllableParticleGroup createGroup(UUID uuid, Map<String, ? extends ParticleControllerDataBuffer<?>> args);

    /**
     * Apply changes to an existing group with the given args.
     */
    void changeGroup(ControllableParticleGroup group, Map<String, ? extends ParticleControllerDataBuffer<?>> args);
}
