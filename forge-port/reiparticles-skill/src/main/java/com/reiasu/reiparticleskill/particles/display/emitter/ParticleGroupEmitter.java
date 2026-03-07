// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.display.emitter;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.particle.emitters.AutoParticleEmitters;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

@ReiAutoRegister
 public final class ParticleGroupEmitter extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "particle_group");

    private static final DustParticleOptions CORE_COLOR =
            new DustParticleOptions(new Vector3f(0.95f, 0.82f, 0.35f), 1.0f);
    private static final DustParticleOptions ACCENT_COLOR =
            new DustParticleOptions(new Vector3f(0.55f, 0.78f, 1.0f), 0.9f);

    private int localAge;

    public ParticleGroupEmitter(Vec3 pos, Level world) {
        Vec3 spawn = pos == null ? Vec3.ZERO : pos;
        bind(world, spawn.x, spawn.y, spawn.z);
        setMaxTick(90);
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeInt(localAge);
    }

    public static ParticleGroupEmitter decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        int localAge = buf.readInt();

        ParticleGroupEmitter emitter = new ParticleGroupEmitter(pos, null);
        emitter.setUuid(uuid);
        emitter.setMaxTick(maxTick);
        emitter.setTick(tick);
        emitter.localAge = Math.max(0, localAge);
        if (canceled) {
            emitter.cancel();
        }
        return emitter;
    }

    @Override
    protected void emitTick() {
        Level level = level();
        if (level == null) {
            return;
        }

        localAge = getTick();
        Vec3 center = position();
        double spin = localAge * 0.18;
        double radius = 0.4 + localAge * 0.026;
        int samples = 18 + (localAge % 12);

        for (int i = 0; i < samples; i++) {
            double t = (Math.PI * 2.0 * i) / samples + spin;
            double x = center.x + Math.cos(t) * radius;
            double y = center.y + Math.sin(t * 2.0) * 0.15 + localAge * 0.01;
            double z = center.z + Math.sin(t) * radius;
            spawn(level, CORE_COLOR, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            if (i % 3 == 0) {
                spawn(level, ACCENT_COLOR, x, y, z, 1, 0.01, 0.01, 0.01, 0.0);
            }
        }

        spawn(level, ParticleTypes.END_ROD, center.x, center.y + 0.12, center.z, 4, 0.08, 0.04, 0.08, 0.0);
        if (localAge % 4 == 0) {
            spawn(level, ParticleTypes.ENCHANT, center.x, center.y + 0.08, center.z, 6, 0.15, 0.1, 0.15, 0.0);
        }
    }

    private static void spawn(
            Level level,
            ParticleOptions particle,
            double x,
            double y,
            double z,
            int count,
            double ox,
            double oy,
            double oz,
            double speed
    ) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, x, y, z, count, ox, oy, oz, speed);
            return;
        }
        int safe = Math.max(1, count);
        for (int i = 0; i < safe; i++) {
            level.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
