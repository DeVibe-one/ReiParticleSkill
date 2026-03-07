// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.display.emitter;

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

import java.util.List;
import java.util.UUID;

/**
 * Railgun beam emitter that spawns a long vertical beam of particles
 * with scattered disc particles at the base, matching the Fabric original.
 */
@ReiAutoRegister
 public final class RailgunBeamEmitter extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "railgun_beam");

    private static final DustParticleOptions BEAM_COLOR =
            new DustParticleOptions(new Vector3f(210f / 255f, 80f / 255f, 1.0f), 0.7f);

    private static final List<RelativeLocation> BEAM_POINTS = new PointsBuilder()
            .addLine(new RelativeLocation(0.0, -100.0, 0.0),
                     new RelativeLocation(0.0, 100.0, 0.0), 300)
            .create();

    private final RandomSource random = RandomSource.create();

    public RailgunBeamEmitter(Vec3 pos, Level world) {
        Vec3 spawn = pos == null ? Vec3.ZERO : pos;
        bind(world, spawn.x, spawn.y, spawn.z);
        setMaxTick(30);
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        // no extra payload
    }

    public static RailgunBeamEmitter decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());

        RailgunBeamEmitter emitter = new RailgunBeamEmitter(pos, null);
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
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 center = position();
        int tick = getTick();

        // Render beam column
        int step = tick < 10 ? 5 : 3;
        for (int i = 0; i < BEAM_POINTS.size(); i += step) {
            RelativeLocation p = BEAM_POINTS.get(i);
            serverLevel.sendParticles(BEAM_COLOR,
                    center.x + p.getX(),
                    center.y + p.getY(),
                    center.z + p.getZ(),
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Scattered disc particles at base
        int discCount = random.nextInt(8, 16);
        for (int i = 0; i < discCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double r = 0.5 * Math.sqrt(random.nextDouble());
            double dx = Math.cos(angle) * r;
            double dz = Math.sin(angle) * r;
            double vy = -1.0 * (0.5 + random.nextDouble() * 0.5);

            serverLevel.sendParticles(BEAM_COLOR,
                    center.x + dx,
                    center.y + random.nextDouble() * 2.0,
                    center.z + dz,
                    1, 0.0, vy * 0.02, 0.0, 0.0);
        }

        // Ambient glow
        if (tick % 2 == 0) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    center.x, center.y + 0.2, center.z,
                    3, 0.08, 0.06, 0.08, 0.0);
        }
    }
}
