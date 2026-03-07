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
 * Client-rendered center geometric pattern emitter.
 * Rhodonea curve + dual pentagram + Lissajous figures + pulsing ring.
 * Server syncs params only; client generates particles locally.
 */
@ReiAutoRegister
public final class ClientCenterEmitter extends AutoParticleEmitters {
    public static final ResourceLocation CODEC_ID = new ResourceLocation("reiparticleskill", "client_center");
    private static final Vector3f MAIN_COLOR = new Vector3f(210f / 255f, 80f / 255f, 1.0f);
    private static final double TAU = Math.PI * 2.0;
    private static final int SCALE_TICKS = 24;

    private static final int LAYER1_ROSE_POINTS = 140;
    private static final int LAYER1_STAR_POINTS = 40;
    private static final int LAYER1_RING_POINTS = 30;
    private static final int LAYER2_LISSAJOUS_POINTS = 55;
    private static final int LAYER2_REPEAT = 3;
    private static final int LAYER3_RING_POINTS = 40;

    private final RandomSource random = RandomSource.create();

    private double yOffset;

    public ClientCenterEmitter() {
        super();
    }

    public ClientCenterEmitter(Vec3 center, Level level, int maxTick, double yOffset) {
        super();
        if (center != null && level != null) {
            bind(level, center.x, center.y, center.z);
        }
        setMaxTick(maxTick);
        this.yOffset = yOffset;
    }

    @Override
    protected void emitTick() {
        Level level = level();
        if (level == null || !level.isClientSide()) return;
        renderCenter(level, position(), getTick());
    }

    private void renderCenter(Level level, Vec3 center, int tick) {
        double scale = easeScale(tick);
        double breath = 1.0 + 0.15 * Math.sin(tick * 0.08);
        double rotA = tick * 0.008;
        double rotB = tick * -0.012;
        double rotC = tick * -0.006;

        DustParticleOptions dustMed = new DustParticleOptions(MAIN_COLOR, 3.0f);
        DustParticleOptions dustSm = new DustParticleOptions(MAIN_COLOR, 2.2f);
        DustParticleOptions dustLg = new DustParticleOptions(MAIN_COLOR, 4.0f);

        // Layer 1: Rhodonea curve r = cos(5/3 * theta)
        for (int i = 0; i < LAYER1_ROSE_POINTS; i++) {
            double theta = 3.0 * TAU * i / (double) LAYER1_ROSE_POINTS;
            double r = Math.cos(5.0 / 3.0 * theta) * 42.0 * scale * breath;
            double rx = r * Math.cos(theta);
            double rz = r * Math.sin(theta);
            Vec3 rel = rotateY(new Vec3(rx, 0.0, rz), rotA).add(0.0, yOffset, 0.0);
            addDust(level, center, rel, dustMed);
        }

        // Layer 1: Dual pentagram
        emitStar(level, center, yOffset, 5, 48.0 * scale * breath, 0.0, LAYER1_STAR_POINTS, rotA, dustMed);
        emitStar(level, center, yOffset, 5, 36.0 * scale * breath, Math.PI / 5.0, LAYER1_STAR_POINTS, rotA, dustMed);

        // Layer 1: Breathing inner ring
        double ringR = 28.0 * scale * breath;
        for (int i = 0; i < LAYER1_RING_POINTS; i++) {
            double a = rotA + TAU * i / (double) LAYER1_RING_POINTS;
            double wobble = 1.0 + 0.12 * Math.sin(a * 6.0 + tick * 0.1);
            Vec3 rel = new Vec3(Math.cos(a) * ringR * wobble, yOffset, Math.sin(a) * ringR * wobble);
            addDust(level, center, rel, dustMed);
            if (i % 3 == 0) {
                addEnchant(level, center, rel);
            }
        }

        // Layer 2: Three Lissajous figures
        for (int ring = 0; ring < LAYER2_REPEAT; ring++) {
            double pitch = (TAU / 9.0) * ring;
            double phase = ring * Math.PI / 3.0;
            for (int i = 0; i < LAYER2_LISSAJOUS_POINTS; i++) {
                double t = TAU * i / (double) LAYER2_LISSAJOUS_POINTS;
                double lx = Math.sin(3.0 * t + Math.PI / 4.0 + phase) * 14.0 * scale;
                double lz = Math.sin(4.0 * t) * 14.0 * scale;
                Vec3 rel = new Vec3(lx, 0.0, lz);
                rel = rotateX(rel, pitch);
                rel = rotateY(rel, rotB).add(0.0, yOffset, 0.0);
                addDust(level, center, rel, dustSm);
            }
        }

        // Layer 3: Large pulsing enchant ring
        double outerR = 72.0 * scale * breath;
        for (int i = 0; i < LAYER3_RING_POINTS; i++) {
            double a = rotC + TAU * i / (double) LAYER3_RING_POINTS;
            double wobble = 1.0 + 0.08 * Math.sin(a * 5.0 - tick * 0.06);
            Vec3 rel = new Vec3(Math.cos(a) * outerR * wobble, yOffset, Math.sin(a) * outerR * wobble);
            addDust(level, center, rel, dustLg);
            if (i % 2 == 0) {
                addEnchant(level, center, rel);
            }
        }
    }

    private void emitStar(Level level, Vec3 center, double yOff,
                          int points, double radius, double angleOffset,
                          int samples, double rotY, DustParticleOptions dust) {
        int skip = 2;
        for (int i = 0; i < samples; i++) {
            double u = (i / (double) samples) * points;
            int seg = (int) Math.floor(u);
            double frac = u - seg;
            int v0 = (seg * skip) % points;
            int v1 = ((seg + 1) * skip) % points;
            double a0 = angleOffset + TAU * v0 / (double) points;
            double a1 = angleOffset + TAU * v1 / (double) points;
            double x = Math.cos(a0) * radius + (Math.cos(a1) - Math.cos(a0)) * radius * frac;
            double z = Math.sin(a0) * radius + (Math.sin(a1) - Math.sin(a0)) * radius * frac;
            Vec3 rel = rotateY(new Vec3(x, 0.0, z), rotY).add(0.0, yOff, 0.0);
            addDust(level, center, rel, dust);
        }
    }

    private void addDust(Level level, Vec3 center, Vec3 rel, DustParticleOptions dust) {
        ClientParticleHelper.addForce(level, dust,
                center.x + rel.x, center.y + rel.y, center.z + rel.z,
                3, 0.2, 0.1, 0.2, 0.015);
    }

    private void addEnchant(Level level, Vec3 center, Vec3 rel) {
        ClientParticleHelper.addForce(level, ParticleTypes.ENCHANT,
                center.x + rel.x, center.y + rel.y, center.z + rel.z,
                0, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02,
                random.nextGaussian() * 0.02, 1.0);
    }

    private Vec3 rotateX(Vec3 p, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(p.x, p.y * cos - p.z * sin, p.y * sin + p.z * cos);
    }

    private Vec3 rotateY(Vec3 p, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(p.x * cos - p.z * sin, p.y, p.x * sin + p.z * cos);
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
        buf.writeDouble(yOffset);
    }

    @Override
    protected void readPayload(FriendlyByteBuf buf) {
        yOffset = buf.readDouble();
    }

    public static ClientCenterEmitter decode(FriendlyByteBuf buf) {
        ClientCenterEmitter e = new ClientCenterEmitter();
        e.decodeFromBuffer(buf);
        return e;
    }
}
