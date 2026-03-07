// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn.runtime.emitter.client;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.particle.emitters.AutoParticleEmitters;
import com.reiasu.reiparticleskill.util.ClientParticleHelper;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Client-rendered ambient dust emitter.
 * Server syncs params only; client generates particles locally via addParticle().
 * Replaces per-particle sendForce() calls — zero particle network packets.
 */
@ReiAutoRegister
public final class ClientDustEmitter extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "client_dust");
    private static final Vector3f MAIN_COLOR = new Vector3f(210f / 255f, 80f / 255f, 1.0f);
    private static final double TAU = Math.PI * 2.0;
    private static final int SCALE_TICKS = 24;

    private final RandomSource random = RandomSource.create();

    // Params (synced via writePayload/readPayload)
    private int count = 160;
    private double maxRadius = 128.0;
    private double sizeMax = 1.0;
    private double sizeMin = 0.2;
    private double rotateSpeed = 0.007;
    private double yOffset;

    public ClientDustEmitter() {
        super();
    }

    public ClientDustEmitter(Vec3 center, Level level, int maxTick,
                             int count, double maxRadius, double rotateSpeed,
                             double sizeMin, double sizeMax, double yOffset) {
        super();
        if (center != null && level != null) {
            bind(level, center.x, center.y, center.z);
        }
        setMaxTick(maxTick);
        this.count = count;
        this.maxRadius = maxRadius;
        this.rotateSpeed = rotateSpeed;
        this.sizeMin = sizeMin;
        this.sizeMax = sizeMax;
        this.yOffset = yOffset;
    }

    @Override
    protected void emitTick() {
        Level level = level();
        if (level == null || !level.isClientSide()) return;
        renderDust(level, position(), getTick());
    }

    private void renderDust(Level level, Vec3 center, int tick) {
        double scale = easeScale(tick);
        double rotation = tick * rotateSpeed;
        double majorR = maxRadius * 0.6 * scale;
        double minorR = maxRadius * 0.4 * scale;

        for (int i = 0; i < count; i++) {
            double poloidalAngle = random.nextDouble() * TAU;
            double toroidalAngle = random.nextDouble() * TAU;
            double tubeDist = random.nextDouble();
            tubeDist = tubeDist * tubeDist * tubeDist;
            double tubeR = minorR * (0.3 + 0.7 * tubeDist);

            double ringX = (majorR + tubeR * Math.cos(poloidalAngle)) * Math.cos(toroidalAngle);
            double ringZ = (majorR + tubeR * Math.cos(poloidalAngle)) * Math.sin(toroidalAngle);
            double ringY = tubeR * Math.sin(poloidalAngle);

            double particlePhase = random.nextDouble() * 0.3 - 0.15;
            double cos = Math.cos(rotation + particlePhase);
            double sin = Math.sin(rotation + particlePhase);
            double px = ringX * cos - ringZ * sin;
            double pz = ringX * sin + ringZ * cos;

            double tangentX = -Math.sin(toroidalAngle + rotation) * 0.02;
            double tangentZ =  Math.cos(toroidalAngle + rotation) * 0.02;

            float size = Mth.clamp((float) randomBetween(sizeMin, sizeMax) * 2.0f, 0.2f, 4.0f);
            ClientParticleHelper.addForce(level,
                    new DustParticleOptions(MAIN_COLOR, size),
                    center.x + px, center.y + yOffset + ringY, center.z + pz,
                    3, 0.15 + Math.abs(tangentX), 0.12, 0.15 + Math.abs(tangentZ), 0.02);
        }
    }

    private double randomBetween(double min, double max) {
        double lo = Math.min(min, max);
        double hi = Math.max(min, max);
        return (Math.abs(hi - lo) < 1.0E-6) ? lo : lo + random.nextDouble() * (hi - lo);
    }

    private double easeScale(int tick) {
        if (tick <= 0) return 0.01;
        if (tick >= SCALE_TICKS) return 1.0;
        double t = tick / (double) SCALE_TICKS;
        double inv = 1.0 - t;
        return 0.01 + 0.99 * (1.0 - inv * inv * inv * inv * inv);
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        buf.writeInt(count);
        buf.writeDouble(maxRadius);
        buf.writeDouble(sizeMax);
        buf.writeDouble(sizeMin);
        buf.writeDouble(rotateSpeed);
        buf.writeDouble(yOffset);
    }

    @Override
    protected void readPayload(FriendlyByteBuf buf) {
        count = buf.readInt();
        maxRadius = buf.readDouble();
        sizeMax = buf.readDouble();
        sizeMin = buf.readDouble();
        rotateSpeed = buf.readDouble();
        yOffset = buf.readDouble();
    }

    public static ClientDustEmitter decode(FriendlyByteBuf buf) {
        ClientDustEmitter e = new ClientDustEmitter();
        e.decodeFromBuffer(buf);
        return e;
    }
}
