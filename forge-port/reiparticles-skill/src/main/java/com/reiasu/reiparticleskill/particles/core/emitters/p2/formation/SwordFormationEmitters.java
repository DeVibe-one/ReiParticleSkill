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
 * Sword formation emitter producing vortex and outer spread particles
 * around a formation center, matching the Fabric original.
 */
@ReiAutoRegister
 public final class SwordFormationEmitters extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "sword_formation");

    private final RandomSource random = RandomSource.create();
    private final List<FormationParticle> particles = new ArrayList<>();
    private static final int MAX_ACTIVE = 4096;

    private Vec3 direction = new Vec3(0.0, 1.0, 0.0);

    // Vortex settings
    private double vortexRadiusMin = 1.5;
    private double vortexRadiusMax = 3.0;
    private int vortexCountMin = 20;
    private int vortexCountMax = 40;

    // Outer settings
    private int outerCountMin = 10;
    private int outerCountMax = 20;

    // Colors
    private Vector3f vortexColorStart = new Vector3f(0.62f, 0.88f, 1.0f);
    private Vector3f vortexColorEnd = new Vector3f(160f / 255f, 1.0f, 221f / 255f);
    private Vector3f outerColorStart = new Vector3f(0.95f, 0.82f, 0.35f);
    private Vector3f outerColorEnd = new Vector3f(219f / 255f, 220f / 255f, 193f / 255f);

    // Vortex rotation
    private double vortexSpeed = 0.08;
    private double outerSpreadSpeed = 0.04;
    private double alphaEaseScale = 10.0;

    public SwordFormationEmitters(Vec3 pos, Level world) {
        Vec3 spawn = pos == null ? Vec3.ZERO : pos;
        bind(world, spawn.x, spawn.y, spawn.z);
        setMaxTick(120);
    }

    public SwordFormationEmitters setDirection(Vec3 direction) {
        this.direction = direction.normalize();
        return this;
    }

    public SwordFormationEmitters setVortexRadius(double min, double max) {
        this.vortexRadiusMin = min;
        this.vortexRadiusMax = max;
        return this;
    }

    public SwordFormationEmitters setVortexColors(Vector3f start, Vector3f end) {
        this.vortexColorStart = start;
        this.vortexColorEnd = end;
        return this;
    }

    public SwordFormationEmitters setOuterColors(Vector3f start, Vector3f end) {
        this.outerColorStart = start;
        this.outerColorEnd = end;
        return this;
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeDouble(direction.x);
        buf.writeDouble(direction.y);
        buf.writeDouble(direction.z);
        buf.writeDouble(vortexRadiusMin);
        buf.writeDouble(vortexRadiusMax);
    }

    public static SwordFormationEmitters decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 dir = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double vrMin = buf.readDouble();
        double vrMax = buf.readDouble();

        SwordFormationEmitters emitter = new SwordFormationEmitters(pos, null);
        emitter.setUuid(uuid);
        emitter.setMaxTick(maxTick);
        emitter.setTick(tick);
        emitter.direction = dir;
        emitter.vortexRadiusMin = vrMin;
        emitter.vortexRadiusMax = vrMax;
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
        int tick = getTick();

        // Build basis vectors from direction
        Vec3 dir = direction.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 1.0, 0.0) : direction.normalize();
        Vec3 basisA = dir.cross(new Vec3(0.0, 1.0, 0.0));
        if (basisA.lengthSqr() < 1.0E-6) {
            basisA = dir.cross(new Vec3(1.0, 0.0, 0.0));
        }
        basisA = basisA.normalize();
        Vec3 basisB = dir.cross(basisA).normalize();

        // Spawn vortex ring particles
        int vortexCount = random.nextInt(vortexCountMin, vortexCountMax + 1);
        double vortexRadius = vortexRadiusMin + random.nextDouble() * (vortexRadiusMax - vortexRadiusMin);
        for (int i = 0; i < vortexCount; i++) {
            double angle = (Math.PI * 2.0 * i) / vortexCount;
            Vec3 local = basisA.scale(Math.cos(angle) * vortexRadius)
                    .add(basisB.scale(Math.sin(angle) * vortexRadius));
            Vec3 spawnPos = center.add(local);
            int maxAge = random.nextInt(15, 30);
            particles.add(new FormationParticle(spawnPos, Vec3.ZERO, maxAge, 1));
        }

        // Spawn outer spread particles
        int outerCount = random.nextInt(outerCountMin, outerCountMax + 1);
        for (int i = 0; i < outerCount; i++) {
            double angle = (Math.PI * 2.0 * i) / outerCount;
            Vec3 local = basisA.scale(Math.cos(angle) * 0.5)
                    .add(basisB.scale(Math.sin(angle) * 0.5));
            Vec3 spawnPos = center.add(local);
            int maxAge = random.nextInt(12, 25);
            particles.add(new FormationParticle(spawnPos, Vec3.ZERO, maxAge, 0));
        }

        // Tick and render
        var vortexAngle = tick * vortexSpeed;
        Iterator<FormationParticle> it = particles.iterator();
        while (it.hasNext()) {
            FormationParticle p = it.next();
            p.age++;
            if (p.age > p.maxAge) {
                it.remove();
                continue;
            }

            double progressColor = (double) p.age / 25.0;
            float size;
            Vector3f color;
            float alpha;

            if (p.sign == 1) {
                // Vortex particle: translucent, rotating
                color = lerpColor((float) progressColor, vortexColorStart, vortexColorEnd);
                double alphaProgress = (double) p.age / alphaEaseScale;
                alpha = Mth.clamp((float) (alphaProgress / (1.0 + alphaProgress)), 0.0f, 1.0f);
                size = Mth.clamp(0.4f * alpha, 0.05f, 1.0f);

                // Apply vortex rotation
                Vec3 toCenter = center.subtract(p.pos);
                double dist = toCenter.horizontalDistance();
                if (dist > 0.1) {
                    Vec3 tangent = toCenter.cross(dir).normalize();
                    p.pos = p.pos.add(tangent.scale(vortexSpeed * dist));
                }
            } else {
                // Outer spread particle
                color = lerpColor((float) progressColor, outerColorStart, outerColorEnd);
                alpha = 1.0f;
                size = Mth.clamp(0.3f, 0.05f, 1.0f);

                // Spread outward
                Vec3 outward = p.pos.subtract(center);
                if (outward.lengthSqr() > 1.0E-6) {
                    p.pos = p.pos.add(outward.normalize().scale(outerSpreadSpeed));
                }
            }

            if (alpha < 0.02f) continue;

            serverLevel.sendParticles(
                    new DustParticleOptions(color, size),
                    p.pos.x, p.pos.y, p.pos.z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Ambient
        if (tick % 4 == 0) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    center.x, center.y + 0.1, center.z,
                    4, 0.2, 0.1, 0.2, 0.0);
        }

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

    private static final class FormationParticle {
        private Vec3 pos;
        private Vec3 velocity;
        private int age;
        private final int maxAge;
        private final int sign;

        private FormationParticle(Vec3 pos, Vec3 velocity, int maxAge, int sign) {
            this.pos = pos;
            this.velocity = velocity;
            this.maxAge = maxAge;
            this.sign = sign;
        }
    }
}
