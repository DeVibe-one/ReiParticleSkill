// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn.runtime.emitter;

import com.reiasu.reiparticlesapi.network.particle.util.ServerParticleUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Expanding shockwave wall that erupts outward from the center during
 * the dragon respawn finale. The wall is a tall cylinder of particles
 * that expands rapidly, creating a dramatic visual impact.
 * <p>
 * The wall has two layers:
 * <ul>
 *   <li>Primary ring: dense dust particles forming the wall face</li>
 *   <li>Trailing wisps: enchant particles left behind as the wall passes</li>
 * </ul>
 *
 * @author Reiasu
 */
public final class ShockwaveWallEmitter extends TimedRespawnEmitter {
    private static final Vector3f WALL_COLOR = new Vector3f(180f / 255f, 60f / 255f, 1.0f);
    private static final Vector3f BRIGHT_COLOR = new Vector3f(230f / 255f, 140f / 255f, 1.0f);
    private static final double TAU = Math.PI * 2.0;

    private static final double EXPAND_SPEED = 4.5;
    private static final double MAX_RADIUS = 200.0;
    private static final double WALL_HEIGHT = 30.0;
    private static final int RING_POINTS = 128;
    private static final int HEIGHT_SLICES = 14;

    public ShockwaveWallEmitter(int maxTicks) {
        super(maxTicks);
    }

    @Override
    protected int emit(ServerLevel level, Vec3 center, int tick) {
        int emitted = 0;
        double radius = tick * EXPAND_SPEED;
        if (radius > MAX_RADIUS) return 0;

        // Wall intensity fades as it expands
        float intensity = Mth.clamp(1.0f - (float) (radius / MAX_RADIUS), 0.1f, 1.0f);
        double wallThickness = 1.5 + radius * 0.02;

        // ---- Primary wall ring at multiple heights ----
        for (int h = 0; h < HEIGHT_SLICES; h++) {
            double y = (h / (double) HEIGHT_SLICES) * WALL_HEIGHT;
            // Sinusoidal height envelope: thicker in the middle
            double heightFactor = Math.sin(Math.PI * h / (double) HEIGHT_SLICES);
            float size = Mth.clamp(3.5f * intensity * (float) heightFactor, 0.8f, 4.0f);

            DustParticleOptions dust = new DustParticleOptions(
                    (h % 2 == 0) ? WALL_COLOR : BRIGHT_COLOR, size);

            int pointsThisSlice = (int) (RING_POINTS * (0.5 + 0.5 * heightFactor));
            for (int i = 0; i < pointsThisSlice; i++) {
                double angle = TAU * i / (double) pointsThisSlice + tick * 0.05 * (h % 2 == 0 ? 1 : -1);
                double px = Math.cos(angle) * radius;
                double pz = Math.sin(angle) * radius;

                ServerParticleUtil.sendForce(level, dust,
                        center.x + px, center.y + y, center.z + pz,
                        3, wallThickness * 0.3, 0.25, wallThickness * 0.3, 0.01);
                emitted += 3;
            }
        }

        // ---- Ground-level leading edge: bright ring ----
        DustParticleOptions edgeDust = new DustParticleOptions(BRIGHT_COLOR, 4.0f);
        for (int i = 0; i < RING_POINTS; i++) {
            double angle = TAU * i / (double) RING_POINTS;
            double px = Math.cos(angle) * radius;
            double pz = Math.sin(angle) * radius;
            ServerParticleUtil.sendForce(level, edgeDust,
                    center.x + px, center.y + 0.5, center.z + pz,
                    3, 0.2, 0.12, 0.2, 0.01);
            emitted += 3;
        }

        // ---- Trailing enchant wisps (only first 60% of expansion) ----
        if (radius < MAX_RADIUS * 0.6) {
            double trailRadius = radius * 0.85;
            int trailPoints = RING_POINTS / 2;
            for (int i = 0; i < trailPoints; i++) {
                double angle = TAU * i / (double) trailPoints + tick * 0.1;
                double trailY = Math.random() * WALL_HEIGHT * 0.6;
                ServerParticleUtil.sendForce(level, ParticleTypes.ENCHANT,
                        center.x + Math.cos(angle) * trailRadius,
                        center.y + trailY,
                        center.z + Math.sin(angle) * trailRadius,
                        0, Math.cos(angle) * 0.05, 0.02, Math.sin(angle) * 0.05, 1.0);
                emitted++;
            }
        }

        return emitted;
    }
}


