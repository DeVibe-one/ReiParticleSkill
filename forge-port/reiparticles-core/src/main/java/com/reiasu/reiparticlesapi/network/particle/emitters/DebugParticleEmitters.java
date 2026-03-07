// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.UUID;

public class DebugParticleEmitters extends ParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticlesapi", "debug_particle");
    private final double x;
    private final double y;
    private final double z;
    private final float scale;
    private final float speed;
    private static final DustParticleOptions PROFILE_COLOR =
            new DustParticleOptions(new Vector3f(0.95f, 0.75f, 0.25f), 1.0f);

    public DebugParticleEmitters(double x, double y, double z, float scale, float speed) {
        this(null, x, y, z, scale, speed);
    }

    public DebugParticleEmitters(ServerLevel level, double x, double y, double z, float scale, float speed) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.scale = scale;
        this.speed = speed;
        bind(level, x, y, z);
        setMaxTick(Math.max(20, (int) (40 + speed * 8.0f)));
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeFloat(scale);
        buf.writeFloat(speed);
    }

    public static DebugParticleEmitters decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        int maxTick = buf.readInt();
        int tick = buf.readInt();
        boolean canceled = buf.readBoolean();
        double posX = buf.readDouble();
        double posY = buf.readDouble();
        double posZ = buf.readDouble();
        float scale = buf.readFloat();
        float speed = buf.readFloat();

        DebugParticleEmitters emitters = new DebugParticleEmitters(posX, posY, posZ, scale, speed);
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
        int ringCount = Math.max(18, (int) (24 * Math.max(1.0f, scale)));
        double radius = 0.4 + tick * (0.02 + speed * 0.01);
        double yOffset = tick * 0.02;

        for (int i = 0; i < ringCount; i++) {
            double t = (Math.PI * 2.0 * i) / ringCount + tick * 0.15;
            double px = x + Math.cos(t) * radius;
            double py = y + yOffset + Math.sin(t * 2.0) * 0.08;
            double pz = z + Math.sin(t) * radius;
            spawn(level, PROFILE_COLOR, px, py, pz, 1);
        }

        spawn(level, ParticleTypes.CRIT, x, y + yOffset + 0.1, z, 4);
        if (tick % 4 == 0) {
            spawn(level, ParticleTypes.END_ROD, x, y + yOffset + 0.25, z, 3);
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

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getScale() {
        return scale;
    }

    public float getSpeed() {
        return speed;
    }
}
