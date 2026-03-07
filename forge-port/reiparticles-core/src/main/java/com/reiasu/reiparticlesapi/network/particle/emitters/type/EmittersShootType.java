// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.type;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Defines how particles are spatially distributed when an emitter fires.
 * <p>
 * Each implementation produces a list of spawn positions relative to the emitter origin
 * and computes a default direction vector for each particle.
 * <p>
 * Forge port note: {@code getCodec()} from Fabric StreamCodec is omitted;
 * Forge networking uses FriendlyByteBuf directly where needed.
 */
public interface EmittersShootType {

    /**
     * Returns the unique string identifier for this shoot type (e.g. "point", "line").
     */
    String getID();

    /**
     * Computes the list of spawn positions for a batch of particles.
     *
     * @param origin the emitter world position
     * @param tick   the current emitter tick
     * @param count  the number of particles to produce
     * @return list of world-space spawn positions (size == count)
     */
    List<Vec3> getPositions(Vec3 origin, int tick, int count);

    /**
     * Computes the default movement direction for a particle.
     *
     * @param enter  the emitter's base direction vector (may be zero)
     * @param tick   the current emitter tick
     * @param pos    the individual particle spawn position
     * @param origin the emitter world position
     * @return the direction vector for the particle
     */
    Vec3 getDefaultDirection(Vec3 enter, int tick, Vec3 pos, Vec3 origin);
}
