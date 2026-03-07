// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

/**
 * Physics constants used by particle emitter simulations.
 * Values are tuned for Minecraft's coordinate and tick system.
 */
public final class PhysicConstant {
    public static final PhysicConstant INSTANCE = new PhysicConstant();

    /** Gravity acceleration per tick (blocks/tick^2). Minecraft default ~0.05 */
    public static final double EARTH_GRAVITY = 0.05;

    /** Sea-level air density (kg/m^3) */
    public static final double SEA_AIR_DENSITY = 1.225;

    /** Drag coefficient for particle air resistance */
    public static final double DRAG_COEFFICIENT = 0.01;

    /** Cross-sectional area for drag calculations */
    public static final double CROSS_SECTIONAL_AREA = 0.01;

    private PhysicConstant() {
    }
}
