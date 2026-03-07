// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.command;

/**
 * Defines the orbit calculation mode for {@code ParticleOrbitCommand}.
 */
public enum OrbitMode {
    /** Full Newtonian force-based orbit (gravitational pull + velocity) */
    PHYSICAL,
    /** Spring-like elasticity toward the orbit center */
    SPRING,
    /** Instantly snap to the computed orbit position */
    SNAP
}
