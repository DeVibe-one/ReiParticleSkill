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
 * Sword formation explosion emitter producing sphere-wave and disc-wave particles
 * at a formation collapse point, matching the Fabric original.
 */
@ReiAutoRegister
 public final class SwordFormationExplosionEmitter extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "sword_formation_explosion");

    private final RandomSource random = RandomSource.create();
    private final List<ExplParticle> particles = new ArrayList<>();
    private static final int MAX_ACTIVE = 6000;

    private Vec3 direction = new Vec3(0.0, 1.0, 0.0);

    // Sphere wave
    private int sphereCountMin = 30;
    private int sphereCountMax = 60;
    private double sphereRadius = 0.5;
    private Vector3f sphereColorLeft = new Vector3f(0.95f, 0.82f, 0.35f);
    private Vector3f sphereColorRight = new Vector3f(1.0f, 0.6f, 0.2f);

    // Disc wave
    private int waveCountMin = 20;
    private int waveCountMax = 40;
    private Vector3f waveColorLeft = new Vector3f(0.62f, 0.88f, 1.0f);
    private Vector3f waveColorRight = new Vector3f(0.3f, 0.5f, 0.9f);

    // Physics
    private double spreadSpeed = 0.3;
    private double damping = 0.92;

    public SwordFormationExplosionEmitter(Vec3 pos, Level world) {
        Vec3 spawn = pos == null ? Vec3.ZERO : pos;
        bind(world, spawn.x, spawn.y, spawn.z);
        setMaxTick(1);
    }

    public SwordFormationExplosionEmitter setDirection(Vec3 direction) {
        this.direction = direction.normalize();
        return this;
    }

    public SwordFormationExplosionEmitter setSphereColors(Vector3f left, Vector3f right) {
        this.sphereColorLeft = left;
        this.sphereColorRight = right;
        return this;
    }

    public SwordFormationExplosionEmitter setWaveColors(Vector3f left, Vector3f right) {
        this.waveColorLeft = left;
        this.waveColorRight = right;
        return this;
    }

    public SwordFormationExplosionEmitter setSpreadSpeed(double speed) {
        this.spreadSpeed = speed;
        return this;
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeDouble(direction.x);
        buf.writeDouble(direction.y);
        buf.writeDouble(direction.z);
        buf.writeDouble(spreadSpeed);
    }

    public static SwordFormationExplosionEmitter decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 dir = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double speed = buf.readDouble();

        SwordFormationExplosionEmitter emitter = new SwordFormationExplosionEmitter(pos, null);
        emitter.setUuid(uuid);
        emitter.setMaxTick(maxTick);
        emitter.setTick(tick);
        emitter.direction = dir;
        emitter.spreadSpeed = speed;
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

        // Build basis
        Vec3 dir = direction.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : direction.normalize();
        Vec3 basisA = dir.cross(new Vec3(0.0, 1.0, 0.0));
        if (basisA.lengthSqr() < 1.0E-6) {
            basisA = dir.cross(new Vec3(1.0, 0.0, 0.0));
        }
        basisA = basisA.normalize();
        Vec3 basisB = dir.cross(basisA).normalize();

        // Sphere wave: random points on a sphere surface
        int sphereCount = random.nextInt(sphereCountMin, sphereCountMax + 1);
        for (int i = 0; i < sphereCount; i++) {
            double u = random.nextDouble();
            double v = random.nextDouble();
            double theta = Math.PI * 2 * u;
            double phi = Math.acos(2.0 * v - 1.0);
            double dx = Math.sin(phi) * Math.cos(theta);
            double dy = Math.cos(phi);
            double dz = Math.sin(phi) * Math.sin(theta);
            double rr = sphereRadius * Math.cbrt(random.nextDouble());
            Vec3 offset = new Vec3(dx * rr, dy * rr, dz * rr);
            Vec3 velocity = new Vec3(dx, dy, dz).normalize().scale(spreadSpeed);

            int maxAge = random.nextInt(8, 18);
            float size = (float) (0.2 + random.nextDouble() * 0.4);
            particles.add(new ExplParticle(center.add(offset), velocity, maxAge, 0, size));
        }

        // Disc wave: particles in a disc perpendicular to direction
        int waveCount = random.nextInt(waveCountMin, waveCountMax + 1);
        for (int i = 0; i < waveCount; i++) {
            double angle = (Math.PI * 2.0 * i) / waveCount;
            double jitter = random.nextDouble() * 0.05;
            Vec3 local = basisA.scale(Math.cos(angle) * (0.5 + jitter))
                    .add(basisB.scale(Math.sin(angle) * (0.5 + jitter)));
            Vec3 velocity = local.normalize().scale(spreadSpeed * 0.8);

            int maxAge = random.nextInt(6, 14);
            float size = (float) (0.15 + random.nextDouble() * 0.35);
            particles.add(new ExplParticle(center.add(local), velocity, maxAge, 1, size));
        }

        // Tick and render
        Iterator<ExplParticle> it = particles.iterator();
        while (it.hasNext()) {
            ExplParticle p = it.next();
            p.age++;
            if (p.age > p.maxAge) {
                it.remove();
                continue;
            }

            p.pos = p.pos.add(p.velocity);
            p.velocity = p.velocity.scale(damping);

            float t = Mth.clamp((float) p.age / Math.max(1, p.maxAge), 0.0f, 1.0f);
            Vector3f color;
            float alpha;

            if (p.sign == 0) {
                color = lerpColor(t, sphereColorLeft, sphereColorRight);
                alpha = Mth.clamp(1.0f - t * 0.8f, 0.0f, 1.0f);
            } else {
                color = lerpColor(t, waveColorLeft, waveColorRight);
                alpha = Mth.clamp(1.0f - t, 0.0f, 1.0f);
            }

            if (alpha < 0.02f) continue;

            float renderSize = Mth.clamp(p.baseSize * alpha, 0.05f, 4.0f);
            serverLevel.sendParticles(
                    new DustParticleOptions(color, renderSize),
                    p.pos.x, p.pos.y, p.pos.z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Central flash
        serverLevel.sendParticles(ParticleTypes.FLASH,
                center.x, center.y, center.z,
                0, 0.0, 0.0, 0.0, 1.0);
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                center.x, center.y + 0.1, center.z,
                6, 0.15, 0.1, 0.15, 0.0);

        if (particles.size() > MAX_ACTIVE) {
            particles.subList(0, particles.size() - MAX_ACTIVE).clear();
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

    private static final class ExplParticle {
        private Vec3 pos;
        private Vec3 velocity;
        private int age;
        private final int maxAge;
        private final int sign;
        private final float baseSize;

        private ExplParticle(Vec3 pos, Vec3 velocity, int maxAge, int sign, float baseSize) {
            this.pos = pos;
            this.velocity = velocity;
            this.maxAge = maxAge;
            this.sign = sign;
            this.baseSize = baseSize;
        }
    }
}
