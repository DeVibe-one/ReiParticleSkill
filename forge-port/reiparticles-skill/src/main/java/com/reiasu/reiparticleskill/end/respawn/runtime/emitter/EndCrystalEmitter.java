// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn.runtime.emitter;

import com.reiasu.reiparticlesapi.network.particle.util.ServerParticleUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class EndCrystalEmitter extends TimedRespawnEmitter {
    private static final Vector3f MAIN_COLOR = new Vector3f(210f / 255f, 80f / 255f, 1.0f);

    private final RandomSource random = RandomSource.create();
    private final Vec3 source;
    private Vec3 target = Vec3.ZERO;
    private Vec3 currentPos;
    private Vec3 summonPos;
    private double movementSpeed = 0.5;
    private double maxRadius = 3.0;
    private int particleMinAge = 10;
    private int particleMaxAge = 20;
    private int countMin = 10;
    private int countMax = 30;
    private double rotationSpeed = 0.09817477042468103;
    private double currentRotation;
    private double lastRadius;
    private boolean reachedTarget;
    private double refiner = 1.0;

    public EndCrystalEmitter(Vec3 source, int maxTicks) {
        this(source, source, maxTicks);
    }

    public EndCrystalEmitter(Vec3 source, Vec3 target, int maxTicks) {
        super(maxTicks);
        this.source = source;
        this.target = target;
        this.currentPos = source;
        this.summonPos = source;
    }

    public EndCrystalEmitter setTarget(Vec3 target) {
        this.target = target;
        return this;
    }

    public EndCrystalEmitter setMovementSpeed(double movementSpeed) {
        this.movementSpeed = Math.max(0.01, movementSpeed);
        return this;
    }

    public EndCrystalEmitter setMaxRadius(double maxRadius) {
        this.maxRadius = Math.max(0.0, maxRadius);
        return this;
    }

    public EndCrystalEmitter setParticleMinAge(int particleMinAge) {
        this.particleMinAge = Math.max(1, particleMinAge);
        return this;
    }

    public EndCrystalEmitter setParticleMaxAge(int particleMaxAge) {
        this.particleMaxAge = Math.max(1, particleMaxAge);
        return this;
    }

    public EndCrystalEmitter setCountMin(int countMin) {
        this.countMin = Math.max(1, countMin);
        return this;
    }

    public EndCrystalEmitter setCountMax(int countMax) {
        this.countMax = Math.max(1, countMax);
        return this;
    }

    public EndCrystalEmitter setRotationSpeed(double rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
        return this;
    }

    public EndCrystalEmitter setCurrentRotation(double currentRotation) {
        this.currentRotation = currentRotation;
        return this;
    }

    public EndCrystalEmitter setRefiner(double refiner) {
        this.refiner = Math.max(1.0, refiner);
        return this;
    }

    @Override
    protected int emit(ServerLevel level, Vec3 center, int tick) {
        if (tick == 0) {
            currentPos = source;
            summonPos = source;
            currentRotation = currentRotation - rotationSpeed;
            lastRadius = 0.0;
            reachedTarget = false;
        }

        lastRadius = getCurrentRadius();
        moveAlongPath();
        if (reachedTarget) {
            return 0;
        }
        double currentRadius = getCurrentRadius();
        double radius = Mth.lerp(0.5, lastRadius, currentRadius);
        double lastRotation = currentRotation;
        currentRotation += rotationSpeed;
        double current = Mth.lerp(0.5, lastRotation, currentRotation);
        int emitted = 0;

        Vec3 direction = target.subtract(currentPos);
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

        int min = Math.max(1, countMin);
        int maxExclusive = Math.max(min + 1, countMax);
        int count = random.nextInt(min, maxExclusive);
        int refinedCount = Math.max(1, Mth.floor(count * refiner));
        for (int i = 0; i < refinedCount; i++) {
            Vec3 around = basisA.scale(Math.cos(current) * radius)
                    .add(basisB.scale(Math.sin(current) * radius));
            Vec3 jitter = randomUnitVector().scale(randomBetween(0.0, 0.1));
            Vec3 spawn = currentPos.add(around).add(jitter);
            Vec3 velocity = randomUnitVector().scale(randomBetween(0.0, 0.03));
            ServerParticleUtil.sendForce(level,
                    net.minecraft.core.particles.ParticleTypes.PORTAL,
                    spawn.x,
                    spawn.y,
                    spawn.z,
                    0,
                    velocity.x,
                    velocity.y,
                    velocity.z,
                    1.0
            );
            emitted++;
            if ((i & 1) == 0) {
                float size = Mth.clamp((float) randomBetween(0.6, 1.8), 0.2f, 4.0f);
                ServerParticleUtil.sendForce(level,
                        new DustParticleOptions(MAIN_COLOR, size),
                        spawn.x,
                        spawn.y,
                        spawn.z,
                        1,
                        0.0,
                        0.0,
                        0.0,
                        0.0
                );
                emitted++;
            }
        }
        return emitted;
    }

    private void moveAlongPath() {
        Vec3 toTarget = target.subtract(currentPos);
        double distance = toTarget.length();
        if (distance < 1.0E-6) {
            reachedTarget = true;
            return;
        }
        Vec3 dir = toTarget.normalize();
        Vec3 movement = dir.scale(movementSpeed);
        Vec3 moved = currentPos.add(movement);
        if (target.subtract(moved).dot(dir) < 0.0) {
            currentPos = target;
            reachedTarget = true;
        } else {
            currentPos = moved;
        }
        if (currentPos.distanceTo(target) <= 0.4) {
            reachedTarget = true;
        }
    }

    private double getCurrentRadius() {
        double originalDistance = summonPos.distanceTo(target);
        double distance = currentPos.distanceTo(target);
        if (originalDistance <= 1.0E-6 || distance <= 0.5) {
            return 0.0;
        }
        double progress = 1.0 - distance / originalDistance;
        double stepTo = Math.abs(progress - 0.5) * 2.0;
        return Mth.lerp(stepTo, maxRadius, 0.0);
    }

    private Vec3 randomUnitVector() {
        while (true) {
            double x = randomBetween(-1.0, 1.0);
            double y = randomBetween(-1.0, 1.0);
            double z = randomBetween(-1.0, 1.0);
            double len2 = x * x + y * y + z * z;
            if (len2 < 1.0E-6 || len2 > 1.0) {
                continue;
            }
            return new Vec3(x, y, z).normalize();
        }
    }

    private double randomBetween(double min, double max) {
        double lo = Math.min(min, max);
        double hi = Math.max(min, max);
        if (Math.abs(hi - lo) < 1.0E-6) {
            return lo;
        }
        return lo + random.nextDouble() * (hi - lo);
    }

    @Override
    protected boolean shouldStop(ServerLevel level, Vec3 center, int tick) {
        return reachedTarget;
    }

    private int randomIntExclusive(int min, int maxExclusiveCandidate) {
        int safeMin = Math.max(1, min);
        int safeMaxExclusive = Math.max(safeMin + 1, maxExclusiveCandidate);
        return random.nextInt(safeMin, safeMaxExclusive);
    }
}


