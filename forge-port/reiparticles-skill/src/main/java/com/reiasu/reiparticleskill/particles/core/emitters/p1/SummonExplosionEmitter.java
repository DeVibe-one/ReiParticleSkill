// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.core.emitters.p1;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.particle.emitters.AutoParticleEmitters;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Emits a spherical burst of particles at the summon position.
 * Server-side emitter with a single-tick explosion effect.
 */
@ReiAutoRegister
public final class SummonExplosionEmitter extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "summon_explosion");

    private static final int PARTICLE_COUNT = 40;

    public SummonExplosionEmitter(Vec3 pos, Level world) {
        Vec3 spawn = pos == null ? Vec3.ZERO : pos;
        bind(world, spawn.x, spawn.y, spawn.z);
        setMaxTick(1);
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        // no payload
    }

    public static SummonExplosionEmitter decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());

        SummonExplosionEmitter emitter = new SummonExplosionEmitter(pos, null);
        emitter.setUuid(uuid);
        emitter.setMaxTick(maxTick);
        emitter.setTick(tick);
        if (canceled) {
            emitter.cancel();
        }
        return emitter;
    }

    @Override
    protected void emitTick() {
        Level level = level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 center = position();
        java.util.Random rng = new java.util.Random();

        // Central flash
        serverLevel.sendParticles(ParticleTypes.FLASH,
                center.x, center.y, center.z,
                0, 0.0, 0.0, 0.0, 1.0);

        // Spherical burst of END_ROD particles
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double theta = 2.0 * Math.PI * rng.nextDouble();
            double phi = Math.acos(2.0 * rng.nextDouble() - 1.0);
            double radius = 0.5 + rng.nextDouble() * 2.0;
            double dx = radius * Math.sin(phi) * Math.cos(theta);
            double dy = radius * Math.sin(phi) * Math.sin(theta);
            double dz = radius * Math.cos(phi);
            double vx = dx * 0.3;
            double vz = dz * 0.3;

            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    center.x + dx, center.y + dy, center.z + dz,
                    1, vx, 0.1, vz, 0.05);
        }
    }
}
