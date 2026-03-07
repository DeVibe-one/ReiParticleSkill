// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.core.emitters.p1;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.particle.emitters.AutoParticleEmitters;
import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
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
 * Emitter that spawns particles on a discrete circle and moves them
 * upward toward the emitter center, simulating pillar collection.
 * Also spawns vertical pillar lines during the first few ticks.
 * Server-side port of the Fabric original.
 */
@ReiAutoRegister
 public final class CollectPillarsEmitters extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "collect_pillars");

    private static final DustParticleOptions PILLAR_COLOR =
            new DustParticleOptions(new Vector3f(0.45f, 0.15f, 0.75f), 0.5f);

    private final RandomSource random = RandomSource.create();
    private final List<PillarParticle> particles = new ArrayList<>();

    private double radiusMin = 5.0;
    private double radiusMax = 12.0;
    private double discrete = 1.0;
    private int countMin = 30;
    private int countMax = 50;
    private int particleMinAge = 30;
    private int particleMaxAge = 60;
    private double speed = 1.0;
    private float sizeMin = 0.2f;
    private float sizeMax = 0.5f;
    private double horizontalMinSpeedMultiplier = 0.5;
    private double horizontalMaxSpeedMultiplier = 2.0;
    private double verticalMinSpeedMultiplier = 1.0;
    private double verticalMaxSpeedMultiplier = 3.0;

    public CollectPillarsEmitters(Vec3 pos, Level world) {
        Vec3 spawn = pos == null ? Vec3.ZERO : pos;
        bind(world, spawn.x, spawn.y, spawn.z);
        setMaxTick(200);
    }

    // Setters for configuration
    public CollectPillarsEmitters setRadiusRange(double min, double max) {
        this.radiusMin = min;
        this.radiusMax = max;
        return this;
    }

    public CollectPillarsEmitters setDiscrete(double discrete) {
        this.discrete = discrete;
        return this;
    }

    public CollectPillarsEmitters setCountRange(int min, int max) {
        this.countMin = min;
        this.countMax = max;
        return this;
    }

    public CollectPillarsEmitters setParticleAgeRange(int min, int max) {
        this.particleMinAge = min;
        this.particleMaxAge = max;
        return this;
    }

    public CollectPillarsEmitters setSpeed(double speed) {
        this.speed = speed;
        return this;
    }

    public CollectPillarsEmitters setSizeRange(float min, float max) {
        this.sizeMin = min;
        this.sizeMax = max;
        return this;
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeDouble(radiusMin);
        buf.writeDouble(radiusMax);
        buf.writeDouble(discrete);
        buf.writeDouble(speed);
    }

    public static CollectPillarsEmitters decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double rMin = buf.readDouble();
        double rMax = buf.readDouble();
        double disc = buf.readDouble();
        double spd = buf.readDouble();

        CollectPillarsEmitters emitter = new CollectPillarsEmitters(pos, null);
        emitter.setUuid(uuid);
        emitter.setMaxTick(maxTick);
        emitter.setTick(tick);
        emitter.radiusMin = rMin;
        emitter.radiusMax = rMax;
        emitter.discrete = disc;
        emitter.speed = spd;
        if (canceled) emitter.cancel();
        return emitter;
    }

    @Override
    protected void emitTick() {
        Level level = level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 center = position();
        int tick = getTick();

        // Spawn disc particles converging upward
        double radius = radiusMin + random.nextDouble() * (radiusMax - radiusMin);
        int count = random.nextInt(countMin, countMax + 1);
        List<RelativeLocation> circlePoints = new PointsBuilder()
                .addCircle(radius, count)
                .create();

        for (RelativeLocation rp : circlePoints) {
            Vec3 spawnPos = new Vec3(center.x + rp.getX(), center.y, center.z + rp.getZ());
            Vec3 toCenter = center.subtract(spawnPos);
            double genLength = toCenter.length();
            if (genLength < 0.01) continue;

            Vec3 dir = toCenter.normalize();
            Vec3 velocity = new Vec3(
                    dir.x * speed * horizontalMinSpeedMultiplier,
                    Math.abs(dir.y) * speed * verticalMaxSpeedMultiplier,
                    dir.z * speed * horizontalMinSpeedMultiplier
            );

            int maxAge = random.nextInt(particleMinAge, particleMaxAge + 1);
            float size = sizeMin + random.nextFloat() * (sizeMax - sizeMin);
            particles.add(new PillarParticle(spawnPos, velocity, maxAge, size, genLength, 0));
        }

        // During first 5 ticks, spawn vertical pillar lines
        if (tick < 5) {
            List<RelativeLocation> ringPts = new PointsBuilder().addCircle(0.4, 8).create();
            for (RelativeLocation rp : ringPts) {
                for (int y = 0; y < 180; y += 3) {
                    double py = (double) y * (120.0 / 180.0);
                    Vec3 pillarPos = new Vec3(
                            center.x + rp.getX(),
                            center.y + py,
                            center.z + rp.getZ()
                    );
                    particles.add(new PillarParticle(pillarPos, Vec3.ZERO,
                            40 + random.nextInt(20), 0.3f, 0, 2));
                }
            }
        }

        // Tick and render
        Iterator<PillarParticle> it = particles.iterator();
        while (it.hasNext()) {
            PillarParticle p = it.next();
            p.age++;
            if (p.age > p.maxAge) {
                it.remove();
                continue;
            }

            // Alpha: fade in first 20 ticks, fade out last 20 ticks
            int ageForAlpha = p.age > p.maxAge - 20
                    ? p.maxAge - p.age
                    : Math.min(p.age, 20);
            float ageProgress = Mth.clamp((float) ageForAlpha / 20.0f, 0.0f, 1.0f);
            float alpha = GraphMathHelper.lerp(ageProgress, 0.0f, 1.0f);
            if (alpha < 0.02f) continue;

            if (p.sign == 0) {
                // Converging particle: interpolate speed based on horizontal distance
                Vec3 rel = center.subtract(p.pos);
                Vec3 horizontal = new Vec3(rel.x, 0, rel.z);
                double hLen = horizontal.length();
                double progress = p.genLength > 0.01
                        ? Mth.clamp(hLen / p.genLength, 0.0, 1.0)
                        : 0.0;

                double lerpVertical = GraphMathHelper.lerp(
                        (float) progress, verticalMaxSpeedMultiplier, verticalMinSpeedMultiplier);
                double lerpHorizontal = GraphMathHelper.lerp(
                        (float) progress, horizontalMinSpeedMultiplier, horizontalMaxSpeedMultiplier);

                Vec3 dir = rel.normalize();
                p.velocity = new Vec3(
                        dir.x * speed * lerpHorizontal,
                        Math.abs(dir.y) * speed * lerpVertical,
                        dir.z * speed * lerpHorizontal
                );
                p.pos = p.pos.add(p.velocity.scale(0.05));
            }

            float renderSize = Mth.clamp(p.baseSize * alpha, 0.05f, 2.0f);
            serverLevel.sendParticles(
                    new DustParticleOptions(PILLAR_COLOR.getColor(), renderSize),
                    p.pos.x, p.pos.y, p.pos.z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Ambient
        if (tick % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    center.x, center.y + 0.15, center.z,
                    4, 0.2, 0.1, 0.2, 0.0);
        }

        if (particles.size() > 8192) {
            particles.subList(0, particles.size() - 8192).clear();
        }
    }

    private static final class PillarParticle {
        private Vec3 pos;
        private Vec3 velocity;
        private int age;
        private final int maxAge;
        private final float baseSize;
        private final double genLength;
        private final int sign;

        private PillarParticle(Vec3 pos, Vec3 velocity, int maxAge, float baseSize,
                               double genLength, int sign) {
            this.pos = pos;
            this.velocity = velocity;
            this.maxAge = maxAge;
            this.baseSize = baseSize;
            this.genLength = genLength;
            this.sign = sign;
        }
    }
}

