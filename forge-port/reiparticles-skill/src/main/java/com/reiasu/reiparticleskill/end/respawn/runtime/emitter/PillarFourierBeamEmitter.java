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
 * Fourier-curve light beam that erupts upward from an obsidian pillar
 * during the dragon respawn finale. The beam cross-section traces a
 * multi-frequency rose curve that rotates and expands as it rises.
 * <p>
 * Cross-section: r(θ) = A·cos(3θ) + B·cos(5θ) + C·sin(2θ)
 * The beam sweeps upward over time, leaving a fading trail.
 *
 * @author Reiasu
 */
public final class PillarFourierBeamEmitter extends TimedRespawnEmitter {
    private static final Vector3f BEAM_COLOR = new Vector3f(210f / 255f, 80f / 255f, 1.0f);
    private static final Vector3f BRIGHT_COLOR = new Vector3f(240f / 255f, 160f / 255f, 1.0f);
    private static final double TAU = Math.PI * 2.0;
    private static final int CROSS_SECTION_POINTS = 40;
    private static final double BEAM_HEIGHT = 180.0;
    private static final double RISE_SPEED = 6.0;

    private final Vec3 pillarBase;

    public PillarFourierBeamEmitter(Vec3 pillarBase, int maxTicks) {
        super(maxTicks);
        this.pillarBase = pillarBase;
    }

    @Override
    protected int emit(ServerLevel level, Vec3 center, int tick) {
        int emitted = 0;
        double progress = tick / 60.0;
        double beamTop = Math.min(BEAM_HEIGHT, tick * RISE_SPEED);

        // Phase 1: Rising beam front (first 30 ticks)
        if (tick < 30) {
            emitted += emitBeamFront(level, tick, beamTop);
        }

        // Phase 2: Sustained beam body with rotating cross-section
        emitted += emitBeamBody(level, tick, beamTop);

        // Phase 3: Base flash burst
        if (tick < 10) {
            emitted += emitBaseFlash(level, tick);
        }

        return emitted;
    }

    private int emitBeamFront(ServerLevel level, int tick, double beamTop) {
        int emitted = 0;
        double frontY = beamTop;
        // Expanding ring at the beam front
        double frontRadius = 2.0 + tick * 0.15;
        DustParticleOptions brightDust = new DustParticleOptions(BRIGHT_COLOR, 4.0f);

        for (int i = 0; i < 28; i++) {
            double angle = TAU * i / 28.0 + tick * 0.2;
            double px = Math.cos(angle) * frontRadius;
            double pz = Math.sin(angle) * frontRadius;
            ServerParticleUtil.sendForce(level, brightDust,
                    pillarBase.x + px, pillarBase.y + frontY, pillarBase.z + pz,
                    3, 0.15, 0.4, 0.15, 0.01);
            emitted += 3;
        }

        // Central bright core at front
        ServerParticleUtil.sendForce(level, ParticleTypes.FLASH,
                pillarBase.x, pillarBase.y + frontY, pillarBase.z,
                0, 0, 0, 0, 1.0);
        emitted++;

        return emitted;
    }

    private int emitBeamBody(ServerLevel level, int tick, double beamTop) {
        int emitted = 0;
        // Sample the beam at multiple heights
        int slices = Math.min(40, (int) (beamTop / 4.5));
        double rotation = tick * 0.08;

        for (int s = 0; s < slices; s++) {
            double y = (s / (double) Math.max(1, slices)) * beamTop;
            double heightFraction = y / BEAM_HEIGHT;

            // Fade: bright at base, dimming toward top
            float alpha = Mth.clamp(1.0f - (float) heightFraction * 0.7f, 0.2f, 1.0f);
            // Beam narrows then widens: hourglass shape
            double radiusScale = 1.0 + 0.6 * Math.sin(heightFraction * Math.PI);
            // Rotation accelerates with height
            double localRot = rotation + heightFraction * TAU * 0.5;

            // Fourier cross-section: r(θ) = A·cos(3θ) + B·cos(5θ) + C·sin(2θ)
            float size = Mth.clamp(3.5f * alpha, 1.0f, 4.0f);
            DustParticleOptions dust = new DustParticleOptions(BEAM_COLOR, size);

            for (int i = 0; i < CROSS_SECTION_POINTS; i++) {
                double theta = TAU * i / (double) CROSS_SECTION_POINTS;
                double r = (2.5 * Math.cos(3.0 * theta)
                        + 1.5 * Math.cos(5.0 * theta)
                        + 1.0 * Math.sin(2.0 * theta)) * radiusScale;

                double px = r * Math.cos(theta + localRot);
                double pz = r * Math.sin(theta + localRot);

                ServerParticleUtil.sendForce(level, dust,
                        pillarBase.x + px, pillarBase.y + y, pillarBase.z + pz,
                        3, 0.08, 0.08, 0.08, 0.01);
                emitted += 3;
            }

            // Central spine: bright core line
            if (s % 1 == 0) {
                DustParticleOptions coreDust = new DustParticleOptions(BRIGHT_COLOR,
                        Mth.clamp(4.0f * alpha, 1.5f, 4.0f));
                ServerParticleUtil.sendForce(level, coreDust,
                        pillarBase.x, pillarBase.y + y, pillarBase.z,
                        3, 0.15, 0.15, 0.15, 0.0);
                emitted += 3;
            }
        }
        return emitted;
    }

    private int emitBaseFlash(ServerLevel level, int tick) {
        int emitted = 0;
        // Ground-level shockwave ring expanding outward
        double ringRadius = 1.0 + tick * 1.5;
        DustParticleOptions flashDust = new DustParticleOptions(BRIGHT_COLOR, 4.0f);

        for (int i = 0; i < 32; i++) {
            double angle = TAU * i / 32.0;
            double px = Math.cos(angle) * ringRadius;
            double pz = Math.sin(angle) * ringRadius;
            ServerParticleUtil.sendForce(level, flashDust,
                    pillarBase.x + px, pillarBase.y + 0.5, pillarBase.z + pz,
                    3, 0.15, 0.08, 0.15, 0.01);
            emitted += 3;
        }

        // Upward particle burst
        ServerParticleUtil.sendForce(level, ParticleTypes.FLASH,
                pillarBase.x, pillarBase.y + 1.0, pillarBase.z,
                0, 0, 0.5, 0, 1.0);
        emitted++;

        return emitted;
    }
}


