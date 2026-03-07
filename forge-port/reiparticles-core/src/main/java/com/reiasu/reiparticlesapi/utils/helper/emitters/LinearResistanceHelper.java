// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper.emitters;

import net.minecraft.world.phys.Vec3;

/**
 * Utility for applying linear resistance (velocity scaling) to emitter motion.
 */
public final class LinearResistanceHelper {
    public static final LinearResistanceHelper INSTANCE = new LinearResistanceHelper();

    private LinearResistanceHelper() {
    }

    /**
     * Scale the velocity vector by a percentage factor.
     *
     * @param enter   the current velocity
     * @param percent the scaling factor (e.g. 0.95 for 5% deceleration per tick)
     * @return the scaled velocity
     */
    public Vec3 setPercentageVelocity(Vec3 enter, double percent) {
        return enter.scale(percent);
    }
}
