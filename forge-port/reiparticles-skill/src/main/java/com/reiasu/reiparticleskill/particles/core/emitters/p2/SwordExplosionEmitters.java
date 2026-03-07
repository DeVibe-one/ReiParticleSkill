// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.core.emitters.p2;

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
 * Sword explosion emitter producing three layers of particles:
 * flash (sign=0), spark (sign=1), and ring (sign=2).
 * Matches the visual intent of the Fabric original.
 */
@ReiAutoRegister
 public final class SwordExplosionEmitters extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "sword_explosion");

    private final RandomSource random = RandomSource.create();
    private final List<ExplosionParticle> particles = new ArrayList<>();
    private static final int MAX_ACTIVE = 6000;

    // Flash layer
    private int flashCountMin = 20;
    private int flashCountMax = 40;
    private Vector3f flashLeft = new Vector3f(1.0f, 0.95f, 0.7f);
    private Vector3f flashRight = new Vector3f(1.0f, 0.6f, 0.2f);

    // Spark layer
    private int sparkCountMin = 30;
    private int sparkCountMax = 60;
    private Vector3f sparkLeft = new Vector3f(0.95f, 0.82f, 0.35f);
    private Vector3f sparkRight = new Vector3f(1.0f, 0.45f, 0.1f);

    // Ring layer
    private boolean enableRing = true;
    private int ringCount = 24;
    private double ringUp = 0.15;
    private Vector3f ringLeft = new Vector3f(0.62f, 0.88f, 1.0f);
    private Vector3f ringRight = new Vector3f(0.3f, 0.5f, 0.9f);

    // Physics
    private double baseSpeed = 0.6;
    private double speedJitter = 0.4;
    private double spawnJitter = 0.15;
    private double upBias = 0.3;

    public SwordExplosionEmitters(Vec3 pos, Level world) {
        Vec3 spawn = pos == null ? Vec3.ZERO : pos;
        bind(world, spawn.x, spawn.y, spawn.z);
        setMaxTick(1);
    }

    public SwordExplosionEmitters setFlashColors(Vector3f left, Vector3f right) {
        this.flashLeft = left;
        this.flashRight = right;
        return this;
    }

    public SwordExplosionEmitters setSparkColors(Vector3f left, Vector3f right) {
        this.sparkLeft = left;
        this.sparkRight = right;
        return this;
    }

    public SwordExplosionEmitters setRingColors(Vector3f left, Vector3f right) {
        this.ringLeft = left;
        this.ringRight = right;
        return this;
    }

    public SwordExplosionEmitters setEnableRing(boolean enableRing) {
        this.enableRing = enableRing;
        return this;
    }

    public SwordExplosionEmitters setBaseSpeed(double baseSpeed) {
        this.baseSpeed = baseSpeed;
        return this;
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeDouble(baseSpeed);
        buf.writeBoolean(enableRing);
    }

    public static SwordExplosionEmitters decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double speed = buf.readDouble();
        boolean ring = buf.readBoolean();

        SwordExplosionEmitters emitter = new SwordExplosionEmitters(pos, null);
        emitter.setUuid(uuid);
        emitter.setMaxTick(maxTick);
        emitter.setTick(tick);
        emitter.baseSpeed = speed;
        emitter.enableRing = ring;
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
        float speedScale = 1.0f;

        // Flash layer (sign=0)
        int fCount = random.nextInt(flashCountMin, flashCountMax + 1);
        for (int i = 0; i < fCount; i++) {
            Vec3 dir = randomUnitVec(upBias);
            Vec3 vel = dir.scale(baseSpeed * (1.0 + random.nextDouble() * speedJitter) * speedScale);
            Vec3 offset = randomSmallOffset();
            int maxAge = random.nextInt(6, 14);
            particles.add(new ExplosionParticle(center.add(offset), vel, maxAge, 0,
                    randomBetween(0.3, 0.8)));
        }

        // Spark layer (sign=1)
        int sCount = random.nextInt(sparkCountMin, sparkCountMax + 1);
        for (int i = 0; i < sCount; i++) {
            Vec3 dir = randomUnitVec(upBias);
            Vec3 vel = dir.scale(baseSpeed * (1.0 + random.nextDouble() * speedJitter) * speedScale * 1.35);
            Vec3 offset = randomSmallOffset();
            int maxAge = random.nextInt(8, 18);
            particles.add(new ExplosionParticle(center.add(offset), vel, maxAge, 1,
                    randomBetween(0.2, 0.55)));
        }

        // Ring layer (sign=2)
        if (enableRing && ringCount > 0) {
            int steps = Math.max(ringCount, 24);
            double start = random.nextDouble() * Math.PI * 2;
            double step = Math.PI * 2 / steps;
            for (int i = 0; i < steps; i++) {
                double a = start + step * i;
                Vec3 dir = new Vec3(Math.cos(a), ringUp, Math.sin(a)).normalize();
                Vec3 vel = dir.scale(baseSpeed * 1.15 * speedScale);
                Vec3 offset = randomSmallOffset();
                int maxAge = Math.min(random.nextInt(8, 16), 14);
                particles.add(new ExplosionParticle(center.add(offset), vel, maxAge, 2,
                        randomBetween(0.2, 0.45)));
            }
        }

        // Tick and render all particles
        Iterator<ExplosionParticle> it = particles.iterator();
        while (it.hasNext()) {
            ExplosionParticle p = it.next();
            p.age++;
            if (p.age > p.maxAge) {
                it.remove();
                continue;
            }

            p.pos = p.pos.add(p.velocity);
            p.velocity = p.velocity.scale(0.94);

            float t = Mth.clamp((float) p.age / (float) Math.max(1, p.maxAge), 0.0f, 1.0f);
            Vector3f color;
            float alpha;
            float size;

            switch (p.sign) {
                case 0 -> {
                    color = lerpColor(t, flashLeft, flashRight);
                    alpha = Mth.clamp(1.0f - t, 0.0f, 1.0f);
                    size = (float) (p.baseSize * (1.0f + 0.55f * (1.0f - t)));
                }
                case 1 -> {
                    color = lerpColor(t, sparkLeft, sparkRight);
                    alpha = Mth.clamp(1.0f - t * 0.9f, 0.0f, 1.0f);
                    size = (float) (p.baseSize * (1.0f - 0.2f * t));
                }
                default -> {
                    color = lerpColor(t, ringLeft, ringRight);
                    alpha = Mth.clamp(0.85f - 0.85f * t, 0.0f, 1.0f);
                    size = (float) (p.baseSize * (1.0f - 0.35f * t));
                }
            }

            if (alpha < 0.02f) continue;

            serverLevel.sendParticles(
                    new DustParticleOptions(color, Mth.clamp(size, 0.05f, 4.0f)),
                    p.pos.x, p.pos.y, p.pos.z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Central flash
        serverLevel.sendParticles(ParticleTypes.FLASH,
                center.x, center.y, center.z,
                0, 0.0, 0.0, 0.0, 1.0);

        if (particles.size() > MAX_ACTIVE) {
            particles.subList(0, particles.size() - MAX_ACTIVE).clear();
        }
    }

    private Vec3 randomSmallOffset() {
        Vec3 o = randomUnitVec(0.0);
        double s = randomBetween(0.0, spawnJitter);
        return o.scale(s);
    }

    private Vec3 randomUnitVec(double upBias) {
        while (true) {
            double x = randomBetween(-1.0, 1.0);
            double y = randomBetween(-1.0, 1.0);
            double z = randomBetween(-1.0, 1.0);
            double len2 = x * x + y * y + z * z;
            if (len2 < 1.0E-6 || len2 > 1.0) continue;
            Vec3 v = new Vec3(x, y + upBias, z).normalize();
            return v;
        }
    }

    private double randomBetween(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private static Vector3f lerpColor(float t, Vector3f from, Vector3f to) {
        float ct = Mth.clamp(t, 0.0f, 1.0f);
        return new Vector3f(
                from.x() + (to.x() - from.x()) * ct,
                from.y() + (to.y() - from.y()) * ct,
                from.z() + (to.z() - from.z()) * ct
        );
    }

    private static final class ExplosionParticle {
        private Vec3 pos;
        private Vec3 velocity;
        private int age;
        private final int maxAge;
        private final int sign;
        private final double baseSize;

        private ExplosionParticle(Vec3 pos, Vec3 velocity, int maxAge, int sign, double baseSize) {
            this.pos = pos;
            this.velocity = velocity;
            this.maxAge = maxAge;
            this.sign = sign;
            this.baseSize = baseSize;
        }
    }
}
