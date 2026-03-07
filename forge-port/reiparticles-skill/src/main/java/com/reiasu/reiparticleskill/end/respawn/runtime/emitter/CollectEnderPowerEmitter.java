// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn.runtime.emitter;

import com.reiasu.reiparticlesapi.network.particle.util.ServerParticleUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;

public final class CollectEnderPowerEmitter extends TimedRespawnEmitter {
    private static final Vector3f MAIN_COLOR = new Vector3f(210f / 255f, 80f / 255f, 1.0f);
    private static final int MAX_ACTIVE_MAIN = 1500;
    private static final int MAX_ACTIVE_FLASH = 128;

    private final RandomSource random = RandomSource.create();
    private final ArrayList<SpiralState> mainParticles = new ArrayList<>();
    private final ArrayList<SpiralState> flashParticles = new ArrayList<>();

    private double r = 128.0;
    private double radiusOffset = 12.0;
    private int countMin = 40;
    private int countMax = 80;
    private double speed = 1.9;
    private Vec3 originOffset = Vec3.ZERO;
    private Vec3 targetOffset = Vec3.ZERO;

    public CollectEnderPowerEmitter(int maxTicks) {
        super(maxTicks);
    }

    public CollectEnderPowerEmitter setR(double r) {
        this.r = r;
        return this;
    }

    public CollectEnderPowerEmitter setRadiusOffset(double radiusOffset) {
        this.radiusOffset = Math.max(0.0, radiusOffset);
        return this;
    }

    public CollectEnderPowerEmitter setCountMin(int countMin) {
        this.countMin = Math.max(1, countMin);
        return this;
    }

    public CollectEnderPowerEmitter setCountMax(int countMax) {
        this.countMax = Math.max(1, countMax);
        return this;
    }

    public CollectEnderPowerEmitter setSpeed(double speed) {
        this.speed = Math.max(0.01, speed);
        return this;
    }

    public CollectEnderPowerEmitter setOriginOffset(Vec3 originOffset) {
        this.originOffset = originOffset;
        return this;
    }

    public CollectEnderPowerEmitter setTargetOffset(Vec3 targetOffset) {
        this.targetOffset = targetOffset;
        return this;
    }

    /**
     * Logarithmic spiral inward trajectory with angular momentum conservation.
     * Each particle orbits the target on a tightening spiral path:
     *   r(t) = r0 * e^(-lambda * t)
     *   theta(t) = theta0 + omega0 * r0 / r(t) * t   (angular momentum r * omega ~= const)
     *
     * @author Reiasu
     */
    @Override
    protected int emit(ServerLevel level, Vec3 center, int tick) {
        Vec3 origin = center.add(originOffset);
        Vec3 target = center.add(targetOffset);
        int emitted = 0;

        emitted += tickSpiral(level, target);
        emitted += tickFlash(level);
        emitted += spawnSpiral(origin, target);
        if (tick > 80) {
            emitted += spawnFlash(target);
        }

        return emitted;
    }

    private int tickSpiral(ServerLevel level, Vec3 target) {
        int emitted = 0;
        Iterator<SpiralState> it = mainParticles.iterator();
        while (it.hasNext()) {
            SpiralState p = it.next();
            p.age++;
            if (p.age > p.maxAge) {
                it.remove();
                continue;
            }

            // Logarithmic spiral decay: r shrinks exponentially
            double decay = Math.exp(-p.lambda * p.age);
            double currentR = p.initialRadius * decay;

                        // Angular momentum conservation keeps the angle accelerating as radius shrinks
            double angularProgress = p.omega0 * p.initialRadius * p.initialRadius
                    * (1.0 / Math.max(0.5, currentR * currentR));
            double theta = p.initialTheta + angularProgress * p.age * 0.05;

            // Vertical lerp: smoothly converge Y toward target
            double verticalT = smoothstep(Mth.clamp((double) p.age / p.maxAge, 0.0, 1.0));
            double py = Mth.lerp(verticalT, p.initialY, target.y);

            // Compose position on the spiral
            p.pos = new Vec3(
                    target.x + Math.cos(theta) * currentR,
                    py,
                    target.z + Math.sin(theta) * currentR
            );

            // Converge to exact target when very close
            if (currentR < 0.6) {
                p.pos = target;
                if (!p.converged) {
                    p.maxAge += 15;
                    p.converged = true;
                }
            }

            float vis = fadeInOut(p.age, p.maxAge, 16);
            float size = Mth.clamp(p.baseSize * vis * 5.0f, 0.8f, 4.0f);
            ServerParticleUtil.sendForce(level,
                    new DustParticleOptions(MAIN_COLOR, size),
                    p.pos.x, p.pos.y, p.pos.z,
                    1, 0.0, 0.0, 0.0, 0.0);
            emitted++;

            if (p.age % 4 == 0) {
                double tangentX = -Math.sin(theta) * 0.02;
                double tangentZ = Math.cos(theta) * 0.02;
                ServerParticleUtil.sendForce(level,
                        ParticleTypes.PORTAL,
                        p.pos.x, p.pos.y, p.pos.z,
                        0, tangentX, random.nextGaussian() * 0.005, tangentZ, 0.05);
                emitted++;
            }
        }
        return emitted;
    }

    private int tickFlash(ServerLevel level) {
        int emitted = 0;
        Iterator<SpiralState> it = flashParticles.iterator();
        while (it.hasNext()) {
            SpiralState p = it.next();
            p.age++;
            if (p.age > p.maxAge) {
                it.remove();
                continue;
            }
            float vis = fadeInOut(p.age, p.maxAge, 16);
            if (vis <= 0.01f) continue;
            ServerParticleUtil.sendForce(level,
                    ParticleTypes.FLASH,
                    p.pos.x, p.pos.y, p.pos.z,
                    0, 0.0, 0.0, 0.0, 1.0);
            emitted++;
        }
        return emitted;
    }

    private int spawnSpiral(Vec3 origin, Vec3 target) {
        int min = Math.max(1, countMin);
        int max = Math.max(min + 1, countMax);
        int count = random.nextInt(min, max);
        for (int i = 0; i < count; i++) {
            // Spawn on a sphere around origin
            double phi = Math.acos(1.0 - 2.0 * random.nextDouble());
            double theta = random.nextDouble() * Math.PI * 2.0;
            double radius = r + randomBetween(-radiusOffset, radiusOffset);
            Vec3 spawn = new Vec3(
                    origin.x + Math.sin(phi) * Math.cos(theta) * radius,
                    origin.y + Math.cos(phi) * radius,
                    origin.z + Math.sin(phi) * Math.sin(theta) * radius
            );

            // Compute initial spiral parameters relative to target
            double dx = spawn.x - target.x;
            double dz = spawn.z - target.z;
            double initR = Math.sqrt(dx * dx + dz * dz);
            double initTheta = Math.atan2(dz, dx);

                        // Spiral decay rate controls how fast particles converge
            double lambda = randomBetween(0.04, 0.09);
            // Initial angular velocity: higher = more orbits
            double omega = randomBetween(1.5, 4.0) * (random.nextBoolean() ? 1.0 : -1.0);

            mainParticles.add(new SpiralState(
                    spawn, initR, initTheta, spawn.y,
                    lambda, omega, random.nextInt(35, 60),
                    (float) randomBetween(0.5, 1.5)
            ));
        }
        if (mainParticles.size() > MAX_ACTIVE_MAIN) {
            mainParticles.subList(0, mainParticles.size() - MAX_ACTIVE_MAIN).clear();
        }
        return count;
    }

    private int spawnFlash(Vec3 target) {
        int spawned = 0;
        for (int i = 0; i < 5; i++) {
            double phi = Math.acos(1.0 - 2.0 * random.nextDouble());
            double theta = random.nextDouble() * Math.PI * 2.0;
            double d = randomBetween(0.3, 1.3);
            Vec3 flash = target.add(Math.sin(phi) * Math.cos(theta) * d,
                    Math.cos(phi) * d, Math.sin(phi) * Math.sin(theta) * d);
            flashParticles.add(new SpiralState(flash, 0, 0, flash.y, 0, 0, 40, 0));
            spawned++;
        }
        if (flashParticles.size() > MAX_ACTIVE_FLASH) {
            flashParticles.subList(0, flashParticles.size() - MAX_ACTIVE_FLASH).clear();
        }
        return spawned;
    }

    private double randomBetween(double min, double max) {
        double lo = Math.min(min, max);
        double hi = Math.max(min, max);
        return (Math.abs(hi - lo) < 1.0E-6) ? lo : lo + random.nextDouble() * (hi - lo);
    }

    private double smoothstep(double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private float fadeInOut(int age, int maxAge, int blend) {
        if (maxAge <= 0) return 0.0f;
        if (age < blend) return Mth.clamp(age / (float) blend, 0.0f, 1.0f);
        if (age > maxAge - blend) return Mth.clamp((maxAge - age) / (float) blend, 0.0f, 1.0f);
        return 1.0f;
    }

    private static final class SpiralState {
        private Vec3 pos;
        private final double initialRadius;
        private final double initialTheta;
        private final double initialY;
        private final double lambda;
        private final double omega0;
        private int age;
        private int maxAge;
        private final float baseSize;
        private boolean converged;

        private SpiralState(Vec3 pos, double initialRadius, double initialTheta, double initialY,
                            double lambda, double omega0, int maxAge, float baseSize) {
            this.pos = pos;
            this.initialRadius = Math.max(0.1, initialRadius);
            this.initialTheta = initialTheta;
            this.initialY = initialY;
            this.lambda = lambda;
            this.omega0 = omega0;
            this.maxAge = maxAge;
            this.baseSize = baseSize;
        }
    }
}



