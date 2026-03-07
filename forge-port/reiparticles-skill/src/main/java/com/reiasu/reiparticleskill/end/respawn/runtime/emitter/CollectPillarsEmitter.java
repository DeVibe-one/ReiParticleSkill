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

public final class CollectPillarsEmitter extends TimedRespawnEmitter {
    private static final Vector3f MAIN_COLOR = new Vector3f(210f / 255f, 80f / 255f, 1.0f);
    private static final int MAX_ACTIVE_MAIN = 10000;
    private static final int MAX_ACTIVE_TRAILS = 2400;

    private final RandomSource random = RandomSource.create();
    private final ArrayList<ParticleState> mainParticles = new ArrayList<>();
    private final ArrayList<ParticleState> trailParticles = new ArrayList<>();

    private int countMin = 60;
    private int countMax = 100;
    private double radiusMax = 8.0;
    private double radiusMin = 3.0;
    private double discrete = 0.3;
    private int particleMinAge = 40;
    private int particleMaxAge = 90;
    private double horizontalMinSpeedMultiplier = 0.03;
    private double horizontalMaxSpeedMultiplier = 1.0;
    private double verticalMinSpeedMultiplier = 0.1;
    private double verticalMaxSpeedMultiplier = 3.0;
    private float sizeMin = 0.3f;
    private float sizeMax = 1.0f;
    private double speed = 1.5;
    private Vec3 anchorOffset = Vec3.ZERO;

    public CollectPillarsEmitter(int maxTicks) {
        super(maxTicks);
    }

    public CollectPillarsEmitter(int maxTicks, double ringRadius) {
        super(maxTicks);
        this.radiusMin = Math.max(0.0, ringRadius);
        this.radiusMax = Math.max(this.radiusMin, ringRadius);
    }

    public CollectPillarsEmitter setCountMin(int countMin) {
        this.countMin = Math.max(1, countMin);
        return this;
    }

    public CollectPillarsEmitter setCountMax(int countMax) {
        this.countMax = Math.max(1, countMax);
        return this;
    }

    public CollectPillarsEmitter setRadiusMax(double radiusMax) {
        this.radiusMax = Math.max(0.0, radiusMax);
        return this;
    }

    public CollectPillarsEmitter setRadiusMin(double radiusMin) {
        this.radiusMin = Math.max(0.0, radiusMin);
        return this;
    }

    public CollectPillarsEmitter setDiscrete(double discrete) {
        this.discrete = Math.max(0.0, discrete);
        return this;
    }

    public CollectPillarsEmitter setParticleMinAge(int particleMinAge) {
        this.particleMinAge = Math.max(1, particleMinAge);
        return this;
    }

    public CollectPillarsEmitter setParticleMaxAge(int particleMaxAge) {
        this.particleMaxAge = Math.max(1, particleMaxAge);
        return this;
    }

    public CollectPillarsEmitter setHorizontalMinSpeedMultiplier(double value) {
        this.horizontalMinSpeedMultiplier = value;
        return this;
    }

    public CollectPillarsEmitter setHorizontalMaxSpeedMultiplier(double value) {
        this.horizontalMaxSpeedMultiplier = value;
        return this;
    }

    public CollectPillarsEmitter setVerticalMinSpeedMultiplier(double value) {
        this.verticalMinSpeedMultiplier = value;
        return this;
    }

    public CollectPillarsEmitter setVerticalMaxSpeedMultiplier(double value) {
        this.verticalMaxSpeedMultiplier = value;
        return this;
    }

    public CollectPillarsEmitter setSizeMin(float sizeMin) {
        this.sizeMin = sizeMin;
        return this;
    }

    public CollectPillarsEmitter setSizeMax(float sizeMax) {
        this.sizeMax = sizeMax;
        return this;
    }

    public CollectPillarsEmitter setSpeed(double speed) {
        this.speed = Math.max(0.01, speed);
        return this;
    }

    public CollectPillarsEmitter setAnchorOffset(Vec3 anchorOffset) {
        this.anchorOffset = anchorOffset;
        return this;
    }

    @Override
    protected int emit(ServerLevel level, Vec3 center, int tick) {
        Vec3 origin = center.add(anchorOffset);
        int emitted = 0;
        emitted += tickMain(level, origin);
        emitted += tickTrails(level);

        int min = Math.max(1, countMin);
        int maxExclusive = Math.max(min + 1, countMax);
        int count = random.nextInt(min, maxExclusive);
        double spawnRadius = randomBetween(radiusMin, radiusMax);
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0 * i) / (double) count;
            Vec3 ring = discretePoint(spawnRadius, discrete, angle);
            Vec3 spawn = origin.add(ring);
            int minAge = Math.max(1, particleMinAge);
            int maxAge = Math.max(minAge + 1, particleMaxAge);
            int selectedAge = random.nextInt(minAge, maxAge);
            float selectedSize = Mth.clamp((float) randomBetween(sizeMin, sizeMax), 0.05f, 4.0f);
            mainParticles.add(new ParticleState(spawn, ring.length(), speed, selectedAge, selectedSize));
        }
        emitted += count;
        if (mainParticles.size() > MAX_ACTIVE_MAIN) {
            mainParticles.subList(0, mainParticles.size() - MAX_ACTIVE_MAIN).clear();
        }

        // Early vertical trails from the original emitter's sign=2 branch.
        if (tick < 5) {
            for (int arm = 0; arm < 8; arm++) {
                double a = (Math.PI * 2.0 * arm) / 8.0;
                for (int i = 0; i < 180; i++) {
                    double y = (120.0 * i) / 179.0;
                    Vec3 pos = new Vec3(
                            origin.x + Math.cos(a) * 0.4,
                            origin.y + y,
                            origin.z + Math.sin(a) * 0.4
                    );
                    trailParticles.add(new ParticleState(
                            pos,
                            0.0,
                            0.0,
                            26 + random.nextInt(20),
                            (float) randomBetween(0.12, 0.28)
                    ));
                    emitted++;
                }
            }
            if (trailParticles.size() > MAX_ACTIVE_TRAILS) {
                trailParticles.subList(0, trailParticles.size() - MAX_ACTIVE_TRAILS).clear();
            }
        }
        return emitted;
    }

    private int tickMain(ServerLevel level, Vec3 origin) {
        int emitted = 0;
        Iterator<ParticleState> it = mainParticles.iterator();
        while (it.hasNext()) {
            ParticleState particle = it.next();
            particle.age++;
            if (particle.age > particle.maxAge) {
                it.remove();
                continue;
            }

            Vec3 rel = origin.subtract(particle.pos);
            double horizontalLength = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
            double progress = Mth.clamp(horizontalLength / Math.max(1.0E-6, particle.genLength), 0.0, 1.0);
            double lerpVertical = Mth.lerp(progress, verticalMaxSpeedMultiplier, verticalMinSpeedMultiplier);
            double lerpHorizontal = Mth.lerp(progress, horizontalMinSpeedMultiplier, horizontalMaxSpeedMultiplier);
            Vec3 dir = rel.lengthSqr() > 1.0E-6 ? rel.normalize() : Vec3.ZERO;
            Vec3 velocity = new Vec3(
                    dir.x * particle.baseSpeed * lerpHorizontal,
                    Math.abs(dir.y * particle.baseSpeed * lerpVertical),
                    dir.z * particle.baseSpeed * lerpHorizontal
            );
            particle.pos = particle.pos.add(velocity);
            float ageInOut = Mth.clamp(
                    (particle.age <= 20 ? particle.age : Math.max(0, particle.maxAge - particle.age)) / 20.0f,
                    0.0f,
                    1.0f
            );
            float size = Mth.clamp(particle.baseSize * ageInOut * 1.8f, 0.1f, 4.0f);

            ServerParticleUtil.sendForce(level,
                    ParticleTypes.PORTAL,
                    particle.pos.x,
                    particle.pos.y,
                    particle.pos.z,
                    0,
                    velocity.x * 0.04,
                    velocity.y * 0.04,
                    velocity.z * 0.04,
                    0.02
            );
            emitted++;

            ServerParticleUtil.sendForce(level,
                    new DustParticleOptions(MAIN_COLOR, size),
                    particle.pos.x,
                    particle.pos.y,
                    particle.pos.z,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
            emitted++;
        }
        return emitted;
    }

    private int tickTrails(ServerLevel level) {
        int emitted = 0;
        Iterator<ParticleState> it = trailParticles.iterator();
        while (it.hasNext()) {
            ParticleState particle = it.next();
            particle.age++;
            if (particle.age > particle.maxAge) {
                it.remove();
                continue;
            }
            float ageInOut = Mth.clamp(
                    (particle.age <= 20 ? particle.age : Math.max(0, particle.maxAge - particle.age)) / 20.0f,
                    0.0f,
                    1.0f
            );
            if (ageInOut <= 0.0f) {
                continue;
            }
            ServerParticleUtil.sendForce(level,
                    ParticleTypes.PORTAL,
                    particle.pos.x,
                    particle.pos.y,
                    particle.pos.z,
                    0,
                    0.0,
                    0.0,
                    0.0,
                    0.02
            );
            emitted++;
            ServerParticleUtil.sendForce(level,
                    new DustParticleOptions(MAIN_COLOR, Mth.clamp(particle.baseSize * ageInOut * 1.8f, 0.1f, 4.0f)),
                    particle.pos.x,
                    particle.pos.y,
                    particle.pos.z,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
            emitted++;
        }
        return emitted;
    }

    private Vec3 discretePoint(double radius, double discrete, double angle) {
        double x = Math.cos(angle) * radius;
        double z = Math.sin(angle) * radius;
        if (discrete <= 0.0) {
            return new Vec3(x, 0.0, z);
        }
        double randomR = random.nextDouble() * discrete;
        double jitterAngle = randomBetween(-Math.PI, Math.PI);
        double ox = Math.cos(jitterAngle) * randomR;
        double oz = Math.sin(jitterAngle) * randomR;
        return new Vec3(x + ox, 0.0, z + oz);
    }

    private double randomBetween(double min, double max) {
        double lo = Math.min(min, max);
        double hi = Math.max(min, max);
        if (Math.abs(hi - lo) < 1.0E-6) {
            return lo;
        }
        return lo + random.nextDouble() * (hi - lo);
    }

    private static final class ParticleState {
        private Vec3 pos;
        private final double genLength;
        private final double baseSpeed;
        private final int maxAge;
        private final float baseSize;
        private int age;

        private ParticleState(Vec3 pos, double genLength, double baseSpeed, int maxAge, float baseSize) {
            this.pos = pos;
            this.genLength = genLength;
            this.baseSpeed = baseSpeed;
            this.maxAge = maxAge;
            this.baseSize = baseSize;
        }
    }
}


