// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

public final class DebugRailgunEmitters extends ParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticlesapi", "debug_railgun");
    private static final DustParticleOptions BEAM_COLOR =
            new DustParticleOptions(new Vector3f(1.0f, 0.55f, 0.62f), 1.2f);
    private final Vec3 from;
    private final Vec3 target;
    private final Vec3 diff;
    private final int beamSteps;

    public DebugRailgunEmitters(ServerLevel level, Vec3 from, Vec3 target) {
        this.from = from;
        this.target = target;
        this.diff = target.subtract(from);
        this.beamSteps = Math.max(14, (int) (diff.length() * 2.0));
        bind(level, from.x, from.y, from.z);
        setMaxTick(28);
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeDouble(from.x);
        buf.writeDouble(from.y);
        buf.writeDouble(from.z);
        buf.writeDouble(target.x);
        buf.writeDouble(target.y);
        buf.writeDouble(target.z);
    }

    public static DebugRailgunEmitters decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        buf.readDouble();
        buf.readDouble();
        buf.readDouble();
        Vec3 from = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3 target = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());

        DebugRailgunEmitters emitters = new DebugRailgunEmitters(null, from, target);
        emitters.setUuid(uuid);
        emitters.setMaxTick(maxTick);
        emitters.setTick(tick);
        if (canceled) {
            emitters.cancel();
        }
        return emitters;
    }

    @Override
    protected void emitTick() {
        Level level = level();
        if (level == null) {
            return;
        }

        int tick = getTick();
        double progress = Math.min(1.0, (tick + 1) / 18.0);
        int activeSteps = Math.max(1, (int) (beamSteps * progress));
        for (int i = 0; i <= activeSteps; i++) {
            double t = (double) i / beamSteps;
            double x = from.x + diff.x * t;
            double y = from.y + diff.y * t;
            double z = from.z + diff.z * t;
            spawn(level, BEAM_COLOR, x, y, z, 1);
        }

        if (tick >= 16) {
            spawn(level, ParticleTypes.EXPLOSION, target.x, target.y, target.z, 6);
            spawn(level, ParticleTypes.DRAGON_BREATH, target.x, target.y, target.z, 18);
        }
    }

    private static void spawn(Level level, net.minecraft.core.particles.ParticleOptions particle, double x, double y, double z, int count) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, x, y, z, count, 0.0, 0.0, 0.0, 0.0);
            return;
        }
        int safe = Math.max(1, count);
        for (int i = 0; i < safe; i++) {
            level.addParticle(particle, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
