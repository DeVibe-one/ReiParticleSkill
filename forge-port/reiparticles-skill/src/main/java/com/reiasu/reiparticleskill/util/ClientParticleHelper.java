// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * Client-side equivalent of {@link ParticleHelper}.
 * Spawns particles locally via {@link Level#addAlwaysVisibleParticle} —
 * zero network packets, zero server cost.
 * <p>
 * Replicates the vanilla {@code ClientboundLevelParticlesPacket} behavior:
 * <ul>
 *   <li>{@code count > 0}: spawn {@code count} particles with Gaussian offsets</li>
 *   <li>{@code count == 0}: spawn 1 particle with exact velocity from dist params</li>
 * </ul>
 */
public final class ClientParticleHelper {

    private static final RandomSource RANDOM = RandomSource.create();

    private ClientParticleHelper() {
    }

    public static <T extends ParticleOptions> void addForce(
            Level level, T type,
            double x, double y, double z,
            int count,
            double xDist, double yDist, double zDist,
            double speed
    ) {
        if (level == null || !level.isClientSide()) return;

        if (count == 0) {
            // Exact velocity mode: 1 particle, dist params = velocity
            level.addAlwaysVisibleParticle(type, true,
                    x, y, z, xDist, yDist, zDist);
            return;
        }

        for (int i = 0; i < count; i++) {
            double ox = RANDOM.nextGaussian() * xDist;
            double oy = RANDOM.nextGaussian() * yDist;
            double oz = RANDOM.nextGaussian() * zDist;
            double sx = RANDOM.nextGaussian() * speed;
            double sy = RANDOM.nextGaussian() * speed;
            double sz = RANDOM.nextGaussian() * speed;
            level.addAlwaysVisibleParticle(type, true,
                    x + ox, y + oy, z + oz, sx, sy, sz);
        }
    }
}
