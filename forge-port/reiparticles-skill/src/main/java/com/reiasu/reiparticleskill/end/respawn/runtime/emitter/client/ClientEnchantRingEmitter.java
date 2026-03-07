// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn.runtime.emitter.client;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.particle.emitters.AutoParticleEmitters;
import com.reiasu.reiparticleskill.util.ClientParticleHelper;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Client-rendered enchant sparkle ring emitter.
 * Server syncs params only; client generates particles locally.
 */
@ReiAutoRegister
public final class ClientEnchantRingEmitter extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "client_enchant_ring");
    private static final Vector3f MAIN_COLOR = new Vector3f(210f / 255f, 80f / 255f, 1.0f);
    private static final double TAU = Math.PI * 2.0;
    private static final int SCALE_TICKS = 24;

    private final RandomSource random = RandomSource.create();

    private double radius = 1.0;
    private double discrete = 0.0;
    private int countMin = 80;
    private int countMax = 260;
    private double rotateSpeed = 0.05;
    private double minSize = 0.2;
    private double maxSize = 1.0;
    private double yOffset;

    public ClientEnchantRingEmitter() {
        super();
    }

    public ClientEnchantRingEmitter(Vec3 center, Level level, int maxTick,
                                    double radius, int countMin, int countMax,
                                    double discrete, double rotateSpeed,
                                    double minSize, double maxSize, double yOffset) {
        super();
        if (center != null && level != null) {
            bind(level, center.x, center.y, center.z);
        }
        setMaxTick(maxTick);
        this.radius = radius;
        this.countMin = countMin;
        this.countMax = countMax;
        this.discrete = discrete;
        this.rotateSpeed = rotateSpeed;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.yOffset = yOffset;
    }

    @Override
    protected void emitTick() {
        Level level = level();
        if (level == null || !level.isClientSide()) return;
        renderEnchant(level, position(), getTick());
    }

    private void renderEnchant(Level level, Vec3 center, int tick) {
        int cMin = Math.max(1, countMin);
        int cMax = Math.max(cMin + 1, countMax);
        int count = random.nextInt(cMin, cMax);
        double scale = easeScale(tick);
        double rotation = tick * rotateSpeed;
        double breath = 1.0 + 0.04 * Math.sin(tick * 0.09);
        double r = radius * scale * breath;
        double disc = discrete * scale;
        double yOff = yOffset * scale;

        double waveNodes = 5.0;
        double waveAmp = radius * 0.06 * scale;
        double wavePhase = tick * -0.15;

        float dustSize = (float) Math.max(0.4, Math.min(4.0,
                randomBetween(minSize, maxSize) * scale * 2.5));
        DustParticleOptions dust = new DustParticleOptions(MAIN_COLOR, dustSize);

        for (int i = 0; i < count; i++) {
            double angleJitter = (random.nextDouble() - 0.5) * 0.15;
            double angle = (TAU * i) / (double) count + rotation + angleJitter;
            double radialH = 1.0 + 0.08 * Math.sin(angle * 2.0 - tick * 0.04);
            double localR = r * radialH;
            double waveY = waveAmp * Math.sin(waveNodes * angle + wavePhase)
                    * Math.cos(tick * 0.03);

            double jR = disc > 0 ? random.nextDouble() * disc : 0.0;
            double jA = random.nextDouble() * TAU;
            double px = Math.cos(angle) * localR + Math.cos(jA) * jR;
            double pz = Math.sin(angle) * localR + Math.sin(jA) * jR;

            double wx = center.x + px;
            double wy = center.y + yOff + waveY;
            double wz = center.z + pz;

            double tgx = -Math.sin(angle) * 0.025;
            double tgz =  Math.cos(angle) * 0.025;

            ClientParticleHelper.addForce(level, dust, wx, wy, wz,
                    3, 0.18 + Math.abs(tgx), 0.1, 0.18 + Math.abs(tgz), 0.02);

            if (i % 3 == 0) {
                ClientParticleHelper.addForce(level, ParticleTypes.ENCHANT, wx, wy, wz,
                        0, random.nextGaussian() * 0.03, random.nextGaussian() * 0.03,
                        random.nextGaussian() * 0.03, 1.0);
            }
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
        buf.writeDouble(radius);
        buf.writeDouble(discrete);
        buf.writeInt(countMin);
        buf.writeInt(countMax);
        buf.writeDouble(rotateSpeed);
        buf.writeDouble(minSize);
        buf.writeDouble(maxSize);
        buf.writeDouble(yOffset);
    }

    @Override
    protected void readPayload(FriendlyByteBuf buf) {
        radius = buf.readDouble();
        discrete = buf.readDouble();
        countMin = buf.readInt();
        countMax = buf.readInt();
        rotateSpeed = buf.readDouble();
        minSize = buf.readDouble();
        maxSize = buf.readDouble();
        yOffset = buf.readDouble();
    }

    public static ClientEnchantRingEmitter decode(FriendlyByteBuf buf) {
        ClientEnchantRingEmitter e = new ClientEnchantRingEmitter();
        e.decodeFromBuffer(buf);
        return e;
    }
}
