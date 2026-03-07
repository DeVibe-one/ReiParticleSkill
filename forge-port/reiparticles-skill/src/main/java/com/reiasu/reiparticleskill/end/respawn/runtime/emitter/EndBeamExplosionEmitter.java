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

public final class EndBeamExplosionEmitter extends TimedRespawnEmitter {
    private static final Vector3f MAIN_COLOR = new Vector3f(210f / 255f, 80f / 255f, 1.0f);
    private static final int MAX_ACTIVE = 2048;

    private final RandomSource random = RandomSource.create();
    private final ArrayList<ExplosionState> activeParticles = new ArrayList<>();
    private double maxSpeed = 18.0;
    private double minSpeed = 1.0;
    private double discrete = 1.0;
    private int particleMinAge = 10;
    private int particleMaxAge = 20;
    private int countMin = 10;
    private int countMax = 30;
    private double sizeMax = 0.8;
    private double sizeMin = 0.2;
    private double drag = 0.99;
    private Vec3 anchorOffset = Vec3.ZERO;

    public EndBeamExplosionEmitter(int maxTicks) {
        super(maxTicks);
    }

    public EndBeamExplosionEmitter setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
        return this;
    }

    public EndBeamExplosionEmitter setMinSpeed(double minSpeed) {
        this.minSpeed = minSpeed;
        return this;
    }

    public EndBeamExplosionEmitter setDiscrete(double discrete) {
        this.discrete = Math.max(0.0, discrete);
        return this;
    }

    public EndBeamExplosionEmitter setParticleMinAge(int particleMinAge) {
        this.particleMinAge = Math.max(1, particleMinAge);
        return this;
    }

    public EndBeamExplosionEmitter setParticleMaxAge(int particleMaxAge) {
        this.particleMaxAge = Math.max(1, particleMaxAge);
        return this;
    }

    public EndBeamExplosionEmitter setCountMin(int countMin) {
        this.countMin = Math.max(1, countMin);
        return this;
    }

    public EndBeamExplosionEmitter setCountMax(int countMax) {
        this.countMax = Math.max(1, countMax);
        return this;
    }

    public EndBeamExplosionEmitter setSizeMax(double sizeMax) {
        this.sizeMax = sizeMax;
        return this;
    }

    public EndBeamExplosionEmitter setSizeMin(double sizeMin) {
        this.sizeMin = sizeMin;
        return this;
    }

    public EndBeamExplosionEmitter setDrag(double drag) {
        this.drag = drag;
        return this;
    }

    public EndBeamExplosionEmitter setAnchorOffset(Vec3 anchorOffset) {
        this.anchorOffset = anchorOffset;
        return this;
    }

    /**
     * Fibonacci sphere burst distribution with gravity pull-back.
     * Instead of a flat ring burst, particles are distributed on a sphere using
     * the golden angle for uniform coverage. After the initial burst,
     * gravity pulls particles downward to create an arcing firework effect.
     *
     * @author Reiasu
     */
    @Override
    protected int emit(ServerLevel level, Vec3 center, int tick) {
        Vec3 origin = center.add(anchorOffset);
        int emitted = 0;
        if (tick == 0) {
            ServerParticleUtil.sendForce(level, ParticleTypes.EXPLOSION_EMITTER,
                    origin.x, origin.y, origin.z, 1, 0, 0, 0, 0);
            emitted++;
            int min = Math.max(1, countMin);
            int maxExcl = Math.max(min + 1, countMax);
            int count = random.nextInt(min, maxExcl);

            // Golden angle Fibonacci sphere distribution
            for (int i = 0; i < count; i++) {
                // Latitude: evenly spaced from -1 to 1
                double y = 1.0 - (2.0 * i + 1.0) / count;
                double lateralR = Math.sqrt(1.0 - y * y);
                // Longitude: golden angle increments
                double longitude = GOLDEN_ANGLE * i;
                // Add jitter for organic feel
                double jitter = randomBetween(0.0, discrete);
                double jAngle = random.nextDouble() * Math.PI * 2.0;

                Vec3 dir = new Vec3(
                        lateralR * Math.cos(longitude) + Math.cos(jAngle) * jitter * 0.1,
                        y,
                        lateralR * Math.sin(longitude) + Math.sin(jAngle) * jitter * 0.1
                ).normalize();

                double speed = randomBetween(minSpeed, maxSpeed);
                Vec3 velocity = dir.scale(speed);
                int minAge = Math.max(1, particleMinAge);
                int maxAgeExcl = Math.max(minAge + 1, particleMaxAge);
                float size = Mth.clamp((float) randomBetween(sizeMin, sizeMax), 0.05f, 4.0f);

                ExplosionState state = new ExplosionState(origin, velocity, size,
                        random.nextInt(minAge, maxAgeExcl));
                activeParticles.add(state);
                emitted += render(level, state);
            }
            if (activeParticles.size() > MAX_ACTIVE) {
                activeParticles.subList(0, activeParticles.size() - MAX_ACTIVE).clear();
            }
        }
        emitted += tickActive(level);
        return emitted;
    }

    private int tickActive(ServerLevel level) {
        int emitted = 0;
        Iterator<ExplosionState> it = activeParticles.iterator();
        while (it.hasNext()) {
            ExplosionState p = it.next();
            p.age++;
            if (p.age > p.maxAge) {
                it.remove();
                continue;
            }
            // Apply velocity + gravity pull-back
            p.pos = p.pos.add(p.velocity);
            p.velocity = new Vec3(
                    p.velocity.x * drag,
                    p.velocity.y * drag - GRAVITY,
                    p.velocity.z * drag
            );
            // Skip rendering every other tick for older particles to save bandwidth
            if (p.age > 10 && (p.age & 1) != 0) continue;
            emitted += render(level, p);
        }
        return emitted;
    }

    private int render(ServerLevel level, ExplosionState p) {
        double life = Mth.clamp(1.0 - (p.age / (double) Math.max(1, p.maxAge)), 0.0, 1.0);
        // Size grows then shrinks: peaks at 30% lifetime
        double sizeCurve = Math.sin(life * Math.PI);
        float size = Mth.clamp((float) (p.size * (0.6 + 1.4 * sizeCurve)), 0.2f, 4.0f);

        double spd = p.velocity.length();
        ServerParticleUtil.sendForce(level,
                ParticleTypes.PORTAL,
                p.pos.x, p.pos.y, p.pos.z,
                0,
                p.velocity.x * 0.04, p.velocity.y * 0.04, p.velocity.z * 0.04,
                Math.min(1.0, spd * 0.15));
        int emitted = 1;
        ServerParticleUtil.sendForce(level,
                new DustParticleOptions(MAIN_COLOR, size),
                p.pos.x, p.pos.y, p.pos.z,
                1, 0.0, 0.0, 0.0, 0.0);
        emitted++;
        return emitted;
    }

    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));
    private static final double GRAVITY = 0.04;

    private double randomBetween(double min, double max) {
        double lo = Math.min(min, max);
        double hi = Math.max(min, max);
        return (Math.abs(hi - lo) < 1.0E-6) ? lo : lo + random.nextDouble() * (hi - lo);
    }

    private static final class ExplosionState {
        private Vec3 pos;
        private Vec3 velocity;
        private final float size;
        private final int maxAge;
        private int age;

        private ExplosionState(Vec3 pos, Vec3 velocity, float size, int maxAge) {
            this.pos = pos;
            this.velocity = velocity;
            this.size = size;
            this.maxAge = maxAge;
        }
    }
}



