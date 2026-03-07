// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.core.emitters.p1;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.particle.emitters.AutoParticleEmitters;
import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.utils.builder.PointsBuilder;
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
 * End beam explosion emitter that spawns a disc wave of particles
 * expanding outward with velocity drag. Server-side port of the Fabric original.
 */
@ReiAutoRegister
 public final class EndBeamExplosionEmitter extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "end_beam_explosion");

    private static final DustParticleOptions EXPLOSION_COLOR =
            new DustParticleOptions(new Vector3f(0.45f, 0.15f, 0.75f), 0.55f);

    private final RandomSource random = RandomSource.create();
    private final List<ExplParticle> particles = new ArrayList<>();

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

    public EndBeamExplosionEmitter(Vec3 pos, Level world) {
        Vec3 spawn = pos == null ? Vec3.ZERO : pos;
        bind(world, spawn.x, spawn.y, spawn.z);
        setMaxTick(1);
    }

    public EndBeamExplosionEmitter setSpeedRange(double min, double max) {
        this.minSpeed = min;
        this.maxSpeed = max;
        return this;
    }

    public EndBeamExplosionEmitter setDiscrete(double discrete) {
        this.discrete = discrete;
        return this;
    }

    public EndBeamExplosionEmitter setParticleAgeRange(int min, int max) {
        this.particleMinAge = min;
        this.particleMaxAge = max;
        return this;
    }

    public EndBeamExplosionEmitter setCountRange(int min, int max) {
        this.countMin = min;
        this.countMax = max;
        return this;
    }

    public EndBeamExplosionEmitter setSizeRange(double min, double max) {
        this.sizeMin = min;
        this.sizeMax = max;
        return this;
    }

    public EndBeamExplosionEmitter setDrag(double drag) {
        this.drag = drag;
        return this;
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeDouble(maxSpeed);
        buf.writeDouble(minSpeed);
        buf.writeDouble(discrete);
        buf.writeDouble(drag);
    }

    public static EndBeamExplosionEmitter decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double maxSpd = buf.readDouble();
        double minSpd = buf.readDouble();
        double disc = buf.readDouble();
        double drg = buf.readDouble();

        EndBeamExplosionEmitter emitter = new EndBeamExplosionEmitter(pos, null);
        emitter.setUuid(uuid);
        emitter.setMaxTick(maxTick);
        emitter.setTick(tick);
        emitter.maxSpeed = maxSpd;
        emitter.minSpeed = minSpd;
        emitter.discrete = disc;
        emitter.drag = drg;
        if (canceled) emitter.cancel();
        return emitter;
    }

    @Override
    protected void emitTick() {
        Level level = level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 center = position();

        // Spawn disc wave particles
        int count = random.nextInt(countMin, countMax + 1);
        List<RelativeLocation> discPoints = new PointsBuilder()
                .addCircle(1.0, count)
                .create();

        for (RelativeLocation rp : discPoints) {
            Vec3 dir = new Vec3(rp.getX(), 0.0, rp.getZ()).normalize();
            double spd = minSpeed + random.nextDouble() * (maxSpeed - minSpeed);
            Vec3 velocity = dir.scale(spd);

            float size = (float)(sizeMin + random.nextDouble() * (sizeMax - sizeMin));
            int maxAge = random.nextInt(particleMinAge, particleMaxAge + 1);

            particles.add(new ExplParticle(center, velocity, maxAge, size));
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
            p.velocity = p.velocity.scale(drag);

            float t = Mth.clamp((float) p.age / Math.max(1, p.maxAge), 0.0f, 1.0f);
            float alpha = Mth.clamp(1.0f - t * 0.7f, 0.0f, 1.0f);
            if (alpha < 0.02f) continue;

            float renderSize = Mth.clamp(p.baseSize * alpha, 0.05f, 4.0f);
            serverLevel.sendParticles(
                    new DustParticleOptions(EXPLOSION_COLOR.getColor(), renderSize),
                    p.pos.x, p.pos.y, p.pos.z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Central flash
        serverLevel.sendParticles(ParticleTypes.FLASH,
                center.x, center.y, center.z,
                0, 0.0, 0.0, 0.0, 1.0);

        if (particles.size() > 4096) {
            particles.subList(0, particles.size() - 4096).clear();
        }
    }

    private static final class ExplParticle {
        private Vec3 pos;
        private Vec3 velocity;
        private int age;
        private final int maxAge;
        private final float baseSize;

        private ExplParticle(Vec3 pos, Vec3 velocity, int maxAge, float baseSize) {
            this.pos = pos;
            this.velocity = velocity;
            this.maxAge = maxAge;
            this.baseSize = baseSize;
        }
    }
}
