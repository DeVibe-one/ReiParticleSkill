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
 * Sword aura emitter producing main arc particles and sub trail particles
 * that follow the sword swing direction, matching the Fabric original.
 */
@ReiAutoRegister
 public final class SwordAuraEmitters extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "sword_aura");

    private final RandomSource random = RandomSource.create();
    private final List<AuraParticle> particles = new ArrayList<>();
    private static final int MAX_ACTIVE = 4096;

    private Vec3 movement = new Vec3(0.0, 0.0, 1.0);

    // Main arc settings
    private double mainRadius = 2.0;
    private double mainRadianMin = 0.0;
    private double mainRadianMax = Math.PI;
    private int mainCountMin = 12;
    private int mainCountMax = 20;
    private double mainParticleOffsetMin = 0.0;
    private double mainParticleOffsetMax = 0.5;

    // Sub trail settings
    private double subRadius = 1.5;
    private double subRadianMin = 0.0;
    private double subRadianMax = Math.PI * 0.8;
    private int subCountMin = 8;
    private int subCountMax = 14;
    private double subParticleOffsetMin = 0.0;
    private double subParticleOffsetMax = 0.3;

    // Colors
    private Vector3f mainColorLeft = new Vector3f(0.95f, 0.82f, 0.35f);
    private Vector3f mainColorRight = new Vector3f(1.0f, 0.55f, 0.15f);
    private Vector3f subColorLeft = new Vector3f(0.55f, 0.78f, 1.0f);
    private Vector3f subColorRight = new Vector3f(0.3f, 0.5f, 0.9f);

    public SwordAuraEmitters(Vec3 pos, Level world) {
        Vec3 spawn = pos == null ? Vec3.ZERO : pos;
        bind(world, spawn.x, spawn.y, spawn.z);
        setMaxTick(60);
    }

    public SwordAuraEmitters setMovement(Vec3 movement) {
        this.movement = movement;
        return this;
    }

    public SwordAuraEmitters setMainRadius(double radius) {
        this.mainRadius = radius;
        return this;
    }

    public SwordAuraEmitters setSubRadius(double radius) {
        this.subRadius = radius;
        return this;
    }

    public SwordAuraEmitters setMainColors(Vector3f left, Vector3f right) {
        this.mainColorLeft = left;
        this.mainColorRight = right;
        return this;
    }

    public SwordAuraEmitters setSubColors(Vector3f left, Vector3f right) {
        this.subColorLeft = left;
        this.subColorRight = right;
        return this;
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeDouble(movement.x);
        buf.writeDouble(movement.y);
        buf.writeDouble(movement.z);
        buf.writeDouble(mainRadius);
        buf.writeDouble(subRadius);
    }

    public static SwordAuraEmitters decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 mov = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double mainR = buf.readDouble();
        double subR = buf.readDouble();

        SwordAuraEmitters emitter = new SwordAuraEmitters(pos, null);
        emitter.setUuid(uuid);
        emitter.setMaxTick(maxTick);
        emitter.setTick(tick);
        emitter.movement = mov;
        emitter.mainRadius = mainR;
        emitter.subRadius = subR;
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

        // Spawn main arc particles
        int mainCount = random.nextInt(mainCountMin, mainCountMax + 1);
        spawnArc(center, mainRadius, mainRadianMin, mainRadianMax, mainCount,
                mainParticleOffsetMin, mainParticleOffsetMax, 0);

        // Spawn sub trail particles
        int subCount = random.nextInt(subCountMin, subCountMax + 1);
        spawnArc(center, subRadius, subRadianMin, subRadianMax, subCount,
                subParticleOffsetMin, subParticleOffsetMax, 1);

        // Tick and render particles
        Iterator<AuraParticle> it = particles.iterator();
        while (it.hasNext()) {
            AuraParticle p = it.next();
            p.age++;
            if (p.age > p.maxAge) {
                it.remove();
                continue;
            }

            float progress = (float) p.age / (float) p.maxAge;
            Vector3f color;
            float size;
            if (p.sign == 0) {
                // Main arc: lerp from mainColorLeft to mainColorRight
                color = lerpColor(progress * 2.0f, mainColorLeft, mainColorRight);
                size = Mth.clamp(0.35f * (1.0f - progress * 0.5f), 0.05f, 1.0f);
            } else {
                // Sub trail: lerp from subColorLeft to subColorRight
                color = lerpColor(progress, subColorLeft, subColorRight);
                size = Mth.clamp(0.25f * (1.0f - progress * 0.4f), 0.05f, 0.8f);
            }

            // Apply velocity damping
            p.pos = p.pos.add(p.velocity);
            p.velocity = p.velocity.scale(0.92);

            serverLevel.sendParticles(
                    new DustParticleOptions(color, size),
                    p.pos.x, p.pos.y, p.pos.z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Cap
        if (particles.size() > MAX_ACTIVE) {
            particles.subList(0, particles.size() - MAX_ACTIVE).clear();
        }
    }

    private void spawnArc(Vec3 center, double radius, double radMin, double radMax,
                          int count, double offsetMin, double offsetMax, int sign) {
        Vec3 dir = movement.normalize();
        Vec3 right = dir.cross(new Vec3(0.0, 1.0, 0.0));
        if (right.lengthSqr() < 1.0E-6) {
            right = dir.cross(new Vec3(1.0, 0.0, 0.0));
        }
        right = right.normalize();
        Vec3 up = right.cross(dir).normalize();

        double step = (radMax - radMin) / Math.max(1, count);
        double start = radMin + Math.PI / 2.0;

        for (int i = 0; i < count; i++) {
            double angle = start + step * i;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            Vec3 local = right.scale(x).add(up.scale(z));
            Vec3 offset = randomSmallOffset(offsetMin, offsetMax);
            Vec3 spawnPos = center.add(local).add(offset);

            Vec3 vel = sign == 1
                    ? movement.reverse().normalize().scale(randomBetween(0.01, 0.04))
                    : Vec3.ZERO;

            int maxAge = random.nextInt(5, 12);
            particles.add(new AuraParticle(spawnPos, vel, maxAge, sign));
        }
    }

    private Vec3 randomSmallOffset(double min, double max) {
        double s = randomBetween(min, max);
        return randomUnitVector().scale(s);
    }

    private Vec3 randomUnitVector() {
        while (true) {
            double x = randomBetween(-1.0, 1.0);
            double y = randomBetween(-1.0, 1.0);
            double z = randomBetween(-1.0, 1.0);
            double len2 = x * x + y * y + z * z;
            if (len2 < 1.0E-6 || len2 > 1.0) continue;
            return new Vec3(x, y, z).normalize();
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

    private static final class AuraParticle {
        private Vec3 pos;
        private Vec3 velocity;
        private int age;
        private final int maxAge;
        private final int sign;

        private AuraParticle(Vec3 pos, Vec3 velocity, int maxAge, int sign) {
            this.pos = pos;
            this.velocity = velocity;
            this.maxAge = maxAge;
            this.sign = sign;
        }
    }
}
