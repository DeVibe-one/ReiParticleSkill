// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.core.emitters.p2.formation;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.particle.emitters.AutoParticleEmitters;
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
 * Sword formation wave emitter producing spiral radian-based particles
 * that expand outward from the formation center, matching the Fabric original.
 */
@ReiAutoRegister
 public final class SwordFormationWaveEmitter extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "sword_formation_wave");

    private final RandomSource random = RandomSource.create();
    private final List<WaveParticle> particles = new ArrayList<>();
    private static final int MAX_ACTIVE = 4096;

    private Vec3 direction = new Vec3(0.0, 1.0, 0.0);

    // Spiral settings
    private double minRadian = 0.0;
    private double maxRadian = Math.PI * 4.0;
    private int minCount = 30;
    private int maxCount = 60;
    private int radianCountMin = 2;
    private int radianCountMax = 5;
    private double randomOffsetMin = 0.0;
    private double randomOffsetMax = 0.8;

    // Particle settings
    private int minParticleAge = 10;
    private int maxParticleAge = 20;
    private double minParticleSpeed = 0.05;
    private double maxParticleSpeed = 0.15;
    private double radianProgressMinSpeedScale = 0.5;
    private double radianProgressMaxSpeedScale = 1.5;

    // Colors
    private Vector3f randomColorLeft = new Vector3f(0.62f, 0.88f, 1.0f);
    private Vector3f randomColorRight = new Vector3f(0.3f, 0.5f, 0.9f);

    public SwordFormationWaveEmitter(Vec3 pos, Level world) {
        Vec3 spawn = pos == null ? Vec3.ZERO : pos;
        bind(world, spawn.x, spawn.y, spawn.z);
        setMaxTick(1);
    }

    public SwordFormationWaveEmitter setDirection(Vec3 direction) {
        this.direction = direction.normalize();
        return this;
    }

    public SwordFormationWaveEmitter setRadianRange(double min, double max) {
        this.minRadian = min;
        this.maxRadian = max;
        return this;
    }

    public SwordFormationWaveEmitter setColors(Vector3f left, Vector3f right) {
        this.randomColorLeft = left;
        this.randomColorRight = right;
        return this;
    }

    public SwordFormationWaveEmitter setSpeedScale(double min, double max) {
        this.radianProgressMinSpeedScale = min;
        this.radianProgressMaxSpeedScale = max;
        return this;
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeDouble(direction.x);
        buf.writeDouble(direction.y);
        buf.writeDouble(direction.z);
        buf.writeDouble(minRadian);
        buf.writeDouble(maxRadian);
    }

    public static SwordFormationWaveEmitter decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 dir = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double radMin = buf.readDouble();
        double radMax = buf.readDouble();

        SwordFormationWaveEmitter emitter = new SwordFormationWaveEmitter(pos, null);
        emitter.setUuid(uuid);
        emitter.setMaxTick(maxTick);
        emitter.setTick(tick);
        emitter.direction = dir;
        emitter.minRadian = radMin;
        emitter.maxRadian = radMax;
        if (canceled) {
            emitter.cancel();
        }
        return emitter;
    }

    @Override
    protected void emitTick() {
        Level level = level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 center = position();
        float lerpProgress = (float) getTick() / Math.max(1, getMaxTick());

        // Build basis vectors
        Vec3 dir = direction.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : direction.normalize();
        Vec3 basisA = dir.cross(new Vec3(0.0, 1.0, 0.0));
        if (basisA.lengthSqr() < 1.0E-6) {
            basisA = dir.cross(new Vec3(1.0, 0.0, 0.0));
        }
        basisA = basisA.normalize();
        Vec3 basisB = dir.cross(basisA).normalize();

        // Generate spiral arms
        double radian = minRadian + random.nextDouble() * (maxRadian - minRadian);
        double radius = 0.1;
        int count = random.nextInt(minCount, maxCount + 1);
        double step = radian / Math.max(1, count);
        int armCount = random.nextInt(radianCountMin, radianCountMax + 1);

        // Random rotation for this batch
        double rotAngle = random.nextDouble() * Math.PI * 2;

        // Random offset for all particles in this batch
        Vec3 randomOffset = randomUnitVector().scale(
                random.nextDouble() * (randomOffsetMax - randomOffsetMin) + randomOffsetMin);

        for (int arm = 0; arm < armCount; arm++) {
            for (int i = 0; i < count; i++) {
                double angle = step * i + rotAngle + (Math.PI * 2.0 * arm / armCount);
                double lx = Math.cos(angle) * radius;
                double lz = Math.sin(angle) * radius;

                Vec3 local = basisA.scale(lx).add(basisB.scale(lz));
                Vec3 spawnPos = center.add(local).add(randomOffset);

                // Velocity outward from center along the spiral normal
                Vec3 outward = local.normalize();
                double speedScale = Mth.lerp(lerpProgress,
                        radianProgressMinSpeedScale, radianProgressMaxSpeedScale);
                double pSpeed = (minParticleSpeed + random.nextDouble() *
                        (maxParticleSpeed - minParticleSpeed)) * speedScale;
                Vec3 velocity = outward.scale(pSpeed);

                int maxAge = random.nextInt(minParticleAge, maxParticleAge + 1);
                float size = 0.2f + random.nextFloat() * 0.5f;

                // Random color in gradient
                Vector3f color = lerpColor(random.nextFloat(), randomColorLeft, randomColorRight);

                particles.add(new WaveParticle(spawnPos, velocity, maxAge, size, color));
            }
        }

        // Tick and render
        Iterator<WaveParticle> it = particles.iterator();
        while (it.hasNext()) {
            WaveParticle p = it.next();
            p.age++;
            if (p.age > p.maxAge) {
                it.remove();
                continue;
            }

            p.pos = p.pos.add(p.velocity);

            double progress = (double) p.age / (double) p.maxAge;
            // Ease-out cubic for alpha
            float alpha = Mth.clamp((float) (1.0 - progress * progress * progress), 0.0f, 1.0f);
            if (alpha < 0.02f) continue;

            float renderSize = Mth.clamp(p.baseSize * alpha, 0.05f, 4.0f);
            serverLevel.sendParticles(
                    new DustParticleOptions(p.color, renderSize),
                    p.pos.x, p.pos.y, p.pos.z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Ambient
        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                center.x, center.y + 0.08, center.z,
                3, 0.1, 0.05, 0.1, 0.0);

        if (particles.size() > MAX_ACTIVE) {
            particles.subList(0, particles.size() - MAX_ACTIVE).clear();
        }
    }

    private Vec3 randomUnitVector() {
        while (true) {
            double x = random.nextDouble() * 2 - 1;
            double y = random.nextDouble() * 2 - 1;
            double z = random.nextDouble() * 2 - 1;
            double len2 = x * x + y * y + z * z;
            if (len2 < 1.0E-6 || len2 > 1.0) continue;
            return new Vec3(x, y, z).normalize();
        }
    }

    private static Vector3f lerpColor(float t, Vector3f from, Vector3f to) {
        float ct = Mth.clamp(t, 0.0f, 1.0f);
        return new Vector3f(
                from.x() + (to.x() - from.x()) * ct,
                from.y() + (to.y() - from.y()) * ct,
                from.z() + (to.z() - from.z()) * ct
        );
    }

    private static final class WaveParticle {
        private Vec3 pos;
        private final Vec3 velocity;
        private int age;
        private final int maxAge;
        private final float baseSize;
        private final Vector3f color;

        private WaveParticle(Vec3 pos, Vec3 velocity, int maxAge, float baseSize, Vector3f color) {
            this.pos = pos;
            this.velocity = velocity;
            this.maxAge = maxAge;
            this.baseSize = baseSize;
            this.color = color;
        }
    }
}
