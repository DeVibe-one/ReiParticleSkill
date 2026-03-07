// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.core.emitters.p1;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.particle.emitters.AutoParticleEmitters;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.builder.PointsBuilder;
import com.reiasu.reiparticlesapi.utils.GraphMathHelper;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Emitter that collects ender-power particles toward a target position.
 * Particles spawn outward and converge on the target with alpha fade,
 * rotation, and velocity damping near the destination.
 * Server-side port of the Fabric original.
 */
public final class CollectEnderPowerEmitter extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "collect_ender_power");

    private static final DustParticleOptions ENDER_COLOR =
            new DustParticleOptions(new Vector3f(0.45f, 0.15f, 0.75f), 0.5f);
    private static final DustParticleOptions FLASH_COLOR =
            new DustParticleOptions(new Vector3f(0.6f, 0.3f, 0.9f), 0.65f);

    private final RandomSource random = RandomSource.create();
    private final List<PowerParticle> particles = new ArrayList<>();

    private Vec3 targetPos = Vec3.ZERO;
    private double speed = 0.8;
    private int countMin = 30;
    private int countMax = 50;
    private double spawnRadius = 8.0;

    public CollectEnderPowerEmitter(Vec3 pos, Level world) {
        Vec3 spawn = pos == null ? Vec3.ZERO : pos;
        bind(world, spawn.x, spawn.y, spawn.z);
        setMaxTick(200);
    }

    public CollectEnderPowerEmitter setTargetPos(Vec3 target) {
        this.targetPos = target;
        return this;
    }

    public CollectEnderPowerEmitter setSpeed(double speed) {
        this.speed = speed;
        return this;
    }

    public CollectEnderPowerEmitter setCountRange(int min, int max) {
        this.countMin = min;
        this.countMax = max;
        return this;
    }

    public CollectEnderPowerEmitter setSpawnRadius(double radius) {
        this.spawnRadius = radius;
        return this;
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeDouble(targetPos.x);
        buf.writeDouble(targetPos.y);
        buf.writeDouble(targetPos.z);
        buf.writeDouble(speed);
        buf.writeDouble(spawnRadius);
    }

    public static CollectEnderPowerEmitter decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 target = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double spd = buf.readDouble();
        double radius = buf.readDouble();

        CollectEnderPowerEmitter emitter = new CollectEnderPowerEmitter(pos, null);
        emitter.setUuid(uuid);
        emitter.setMaxTick(maxTick);
        emitter.setTick(tick);
        emitter.targetPos = target;
        emitter.speed = spd;
        emitter.spawnRadius = radius;
        if (canceled) emitter.cancel();
        return emitter;
    }

    @Override
    protected void emitTick() {
        Level level = level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 center = position();
        int tick = getTick();
        int maxTick = getMaxTick();

        // Spawn converging particles
        int count = random.nextInt(countMin, countMax + 1);
        for (int i = 0; i < count; i++) {
            Vec3 offset = randomSphereOffset(spawnRadius);
            Vec3 spawnPos = center.add(offset);
            Vec3 dir = targetPos.subtract(spawnPos);
            double dist = dir.length();
            if (dist < 0.01) continue;
            Vec3 velocity = dir.normalize().scale(speed + random.nextDouble() * 0.3);

            int maxAge = 20 + random.nextInt(40);
            float size = 0.15f + random.nextFloat() * 0.35f;
            particles.add(new PowerParticle(spawnPos, velocity, maxAge, size, 0));
        }

        // After tick 100, spawn flash ball particles
        if (tick > 100) {
            int flashCount = 3 + random.nextInt(5);
            for (int i = 0; i < flashCount; i++) {
                Vec3 offset = randomSphereOffset(random.nextDouble() * 1.3 + 0.3);
                Vec3 flashPos = targetPos.add(offset);
                particles.add(new PowerParticle(flashPos, Vec3.ZERO, 40,
                        (float)(random.nextDouble() * 0.4 + 0.4), 1));
            }
        }

        // Tick and render particles
        Iterator<PowerParticle> it = particles.iterator();
        while (it.hasNext()) {
            PowerParticle p = it.next();
            p.age++;
            if (p.age > p.maxAge) {
                it.remove();
                continue;
            }

            // Alpha based on age
            float progress;
            if (p.age < 20) {
                progress = (float) p.age / 20.0f;
            } else if (p.age > p.maxAge - 20) {
                progress = (float)(p.maxAge - p.age) / 20.0f;
            } else {
                progress = 1.0f;
            }
            float alpha = GraphMathHelper.lerp(progress, 0.0f, 1.0f);
            if (alpha < 0.02f) continue;

            if (p.sign == 0) {
                // Converging particle
                double distToTarget = targetPos.distanceTo(p.pos);
                if (distToTarget <= 5.0) {
                    p.velocity = p.velocity.scale(0.9);
                }
                if (distToTarget <= 0.7) {
                    p.velocity = Vec3.ZERO;
                    p.maxAge = Math.min(p.maxAge, p.age + 20);
                }
                p.pos = p.pos.add(p.velocity);

                // Check overshoot
                Vec3 toTarget = targetPos.subtract(p.pos);
                if (toTarget.dot(p.velocity) < 0 && p.velocity.lengthSqr() > 0.001) {
                    p.pos = targetPos;
                    p.velocity = Vec3.ZERO;
                }
            }

            float renderSize = Mth.clamp(p.baseSize * alpha, 0.05f, 2.0f);
            DustParticleOptions dust = p.sign == 0 ? ENDER_COLOR : FLASH_COLOR;
            serverLevel.sendParticles(
                    new DustParticleOptions(dust.getColor(), renderSize),
                    p.pos.x, p.pos.y, p.pos.z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Ambient at target
        if (tick % 4 == 0) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    targetPos.x, targetPos.y + 0.1, targetPos.z,
                    5, 0.3, 0.3, 0.3, 0.0);
        }

        // Trim
        if (particles.size() > 4096) {
            particles.subList(0, particles.size() - 4096).clear();
        }
    }

    private Vec3 randomSphereOffset(double radius) {
        while (true) {
            double x = random.nextDouble() * 2 - 1;
            double y = random.nextDouble() * 2 - 1;
            double z = random.nextDouble() * 2 - 1;
            double len2 = x * x + y * y + z * z;
            if (len2 < 1.0E-6 || len2 > 1.0) continue;
            double scale = radius / Math.sqrt(len2);
            return new Vec3(x * scale, y * scale, z * scale);
        }
    }

    private static final class PowerParticle {
        private Vec3 pos;
        private Vec3 velocity;
        private int age;
        private int maxAge;
        private final float baseSize;
        private final int sign;

        private PowerParticle(Vec3 pos, Vec3 velocity, int maxAge, float baseSize, int sign) {
            this.pos = pos;
            this.velocity = velocity;
            this.maxAge = maxAge;
            this.baseSize = baseSize;
            this.sign = sign;
        }
    }
}


