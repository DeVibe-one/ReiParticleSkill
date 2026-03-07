// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.display.emitter;

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
 * Railgun explosion emitter producing a disc wave and sphere ball of particles,
 * matching the Fabric original's visual intent.
 */
@ReiAutoRegister
 public final class RailgunExplosionEmitter extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "railgun_explosion");

    private static final DustParticleOptions WAVE_COLOR =
            new DustParticleOptions(new Vector3f(210f / 255f, 80f / 255f, 1.0f), 0.65f);
    private static final DustParticleOptions BALL_COLOR =
            new DustParticleOptions(new Vector3f(0.85f, 0.55f, 1.0f), 0.5f);

    private final RandomSource random = RandomSource.create();
    private final List<ExplParticle> particles = new ArrayList<>();
    private static final int MAX_ACTIVE = 4096;

    private double waveDiscrete = 0.5;
    private double drag = 0.99;
    private int waveCountMin = 100;
    private int waveCountMax = 120;
    private int ballCount = 30;

    public RailgunExplosionEmitter(Vec3 pos, Level world) {
        Vec3 spawn = pos == null ? Vec3.ZERO : pos;
        bind(world, spawn.x, spawn.y, spawn.z);
        setMaxTick(1);
    }

    public RailgunExplosionEmitter setWaveDiscrete(double discrete) {
        this.waveDiscrete = discrete;
        return this;
    }

    public RailgunExplosionEmitter setDrag(double drag) {
        this.drag = drag;
        return this;
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeDouble(waveDiscrete);
        buf.writeDouble(drag);
    }

    public static RailgunExplosionEmitter decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        double discrete = buf.readDouble();
        double drag = buf.readDouble();

        RailgunExplosionEmitter emitter = new RailgunExplosionEmitter(pos, null);
        emitter.setUuid(uuid);
        emitter.setMaxTick(maxTick);
        emitter.setTick(tick);
        emitter.waveDiscrete = discrete;
        emitter.drag = drag;
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

        // Disc wave particles
        int waveCount = random.nextInt(waveCountMin, waveCountMax + 1);
        for (int i = 0; i < waveCount; i++) {
            double angle = (Math.PI * 2.0 * i) / waveCount;
            double jitter = random.nextDouble() * waveDiscrete;
            double dx = Math.cos(angle) * (1.0 + jitter);
            double dz = Math.sin(angle) * (1.0 + jitter);
            Vec3 velocity = new Vec3(dx, 0.0, dz).normalize()
                    .scale(0.3 + random.nextDouble() * 0.2);

            int maxAge = random.nextInt(8, 18);
            float size = 0.3f + random.nextFloat() * 0.35f;
            particles.add(new ExplParticle(center, velocity, maxAge, size, 0));
        }

        // Ball particles
        int bCount = (int) Math.round(Math.sqrt(ballCount));
        for (int i = 0; i < bCount; i++) {
            Vec3 dir = randomUnitVector();
            Vec3 velocity = dir.scale(0.2 + random.nextDouble() * 0.3);

            int maxAge = random.nextInt(6, 14);
            float size = 0.2f + random.nextFloat() * 0.3f;
            particles.add(new ExplParticle(center, velocity, maxAge, size, 1));
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
            float alpha = Mth.clamp(1.0f - t * 0.8f, 0.0f, 1.0f);
            if (alpha < 0.02f) continue;

            DustParticleOptions dust = p.sign == 0 ? WAVE_COLOR : BALL_COLOR;
            float renderSize = Mth.clamp(p.baseSize * alpha, 0.05f, 4.0f);
            serverLevel.sendParticles(
                    new DustParticleOptions(dust.getColor(), renderSize),
                    p.pos.x, p.pos.y, p.pos.z,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Central flash
        serverLevel.sendParticles(ParticleTypes.FLASH,
                center.x, center.y, center.z,
                0, 0.0, 0.0, 0.0, 1.0);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                center.x, center.y, center.z,
                1, 0.0, 0.0, 0.0, 0.0);

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

    private static final class ExplParticle {
        private Vec3 pos;
        private Vec3 velocity;
        private int age;
        private final int maxAge;
        private final float baseSize;
        private final int sign;

        private ExplParticle(Vec3 pos, Vec3 velocity, int maxAge, float baseSize, int sign) {
            this.pos = pos;
            this.velocity = velocity;
            this.maxAge = maxAge;
            this.baseSize = baseSize;
            this.sign = sign;
        }
    }
}
