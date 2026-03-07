// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn.runtime.emitter;

import com.reiasu.reiparticlesapi.network.particle.util.ServerParticleUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class EndCrystalStyleEmitter extends TimedRespawnEmitter {
    private static final Vector3f MAIN_COLOR = new Vector3f(210f / 255f, 80f / 255f, 1.0f);
    private static final int SCALE_IN_TICKS = 20;

    private final Vec3 source;
    private final Vec3 target;

    private double rotateSpeed = 0.02454369260617026;

    public EndCrystalStyleEmitter(Vec3 source, Vec3 target, int maxTicks) {
        super(maxTicks);
        this.source = source;
        this.target = target;
    }

    public EndCrystalStyleEmitter setRotateSpeed(double rotateSpeed) {
        this.rotateSpeed = rotateSpeed;
        return this;
    }

    @Override
    protected int emit(ServerLevel level, Vec3 center, int tick) {
        int emitted = 0;

        Vec3 direction = target.subtract(source);
        if (direction.lengthSqr() < 1.0E-6) {
            direction = new Vec3(0.0, 1.0, 0.0);
        } else {
            direction = direction.normalize();
        }

        Vec3 basisA = direction.cross(new Vec3(0.0, 1.0, 0.0));
        if (basisA.lengthSqr() < 1.0E-6) {
            basisA = direction.cross(new Vec3(1.0, 0.0, 0.0));
        }
        basisA = basisA.normalize();
        Vec3 basisB = direction.cross(basisA).normalize();

        double rotate = tick * rotateSpeed;
        double scale = styleScaleAtTick(tick);
        double ringRadius = 2.0 * scale;
        DustParticleOptions dustSmall = new DustParticleOptions(MAIN_COLOR, Math.max(0.2f, (float) (1.2 * scale)));
        DustParticleOptions dustMid = new DustParticleOptions(MAIN_COLOR, Math.max(0.3f, (float) (1.8 * scale)));

        // Base ring (matching EndCrystalStyle's circle component).
        for (int i = 0; i < 120; i++) {
            double a = rotate + (Math.PI * 2.0 * i) / 120.0;
            Vec3 p = source
                    .add(direction.scale(1.0))
                    .add(basisA.scale(Math.cos(a) * ringRadius))
                    .add(basisB.scale(Math.sin(a) * ringRadius));
            ServerParticleUtil.sendForce(level,
                    ParticleTypes.PORTAL,
                    p.x,
                    p.y,
                    p.z,
                    0,
                    0.0,
                    0.0,
                    0.0,
                    1.0
            );
            emitted++;
            ServerParticleUtil.sendForce(level, dustMid, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            emitted++;
        }

        // Harmonic glyph follows EndCrystalStyle's Fourier count (314).
        for (int i = 0; i < 314; i++) {
            double t = rotate + (Math.PI * 2.0 * i) / 314.0;
            double h = 0.2857142857142857;
            double r = h * (2.0 * Math.cos(t) - 5.0 * Math.cos(2.0 * t)) * scale;
            double s = h * (4.0 * Math.sin(t) - 3.0 * Math.sin(2.0 * t)) * scale;
            Vec3 p = source
                    .add(direction.scale(1.0))
                    .add(basisA.scale(r))
                    .add(basisB.scale(s));
            ServerParticleUtil.sendForce(level,
                    ParticleTypes.PORTAL,
                    p.x,
                    p.y,
                    p.z,
                    0,
                    0.0,
                    0.0,
                    0.0,
                    1.0
            );
            emitted++;
            ServerParticleUtil.sendForce(level, dustSmall, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            emitted++;
        }
        return emitted;
    }

    private double styleScaleAtTick(int tick) {
        if (tick <= 0) {
            return 0.01;
        }
        if (tick < SCALE_IN_TICKS) {
            double t = tick / (double) SCALE_IN_TICKS;
            return 0.01 + (0.99 * t);
        }
        return 1.0;
    }
}


