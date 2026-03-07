// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.preview.styles;

import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffers;
import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleGroupStyle;
import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleStyleProvider;
import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.GraphMathHelper;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * End dust style that renders randomly positioned dust particles within a radius,
 * with rotation, status-driven alpha fade, and scale animation.
 * Color: purple (210, 80, 255). Server-side port of the Fabric original.
 */
@ReiAutoRegister
 public final class EndDustStyle extends ParticleGroupStyle {
    public static final ResourceLocation REGISTRY_KEY = new ResourceLocation("reiparticleskill", "end_dust_style");
    private static final DustParticleOptions DUST_COLOR =
            new DustParticleOptions(new Vector3f(210f / 255f, 80f / 255f, 1.0f), 0.55f);

    private final RandomSource random = RandomSource.create();

    private int count = 80;
    private double maxRadius = 5.0;
    private double sizeMin = 0.2;
    private double sizeMax = 0.6;
    private double rotateSpeed = 0.03;
    private int status = 1; // 1=ENABLE, 2=DISABLE
    private int statusTick = 0;
    private double scaleProgress = 0.01;
    private int age;

    public EndDustStyle() {
        this(UUID.randomUUID());
    }

    public EndDustStyle(UUID uuid) {
        setUuid(uuid == null ? UUID.randomUUID() : uuid);
        setVisibleRange(256.0);
        setAutoToggle(true);
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setMaxRadius(double maxRadius) {
        this.maxRadius = maxRadius;
    }

    public void setSizeRange(double min, double max) {
        this.sizeMin = min;
        this.sizeMax = max;
    }

    public void setRotateSpeed(double rotateSpeed) {
        this.rotateSpeed = rotateSpeed;
    }

    @Override
    public Map<StyleData, RelativeLocation> getCurrentFrames() {
        return Map.of();
    }

    @Override
    public void onDisplay() {
        age = 0;
        status = 1;
        statusTick = 0;
        scaleProgress = 0.01;
    }

    @Override
    public void remove() {
        if (status == 2 && statusTick > 10) {
            super.remove();
        } else {
            status = 2;
            statusTick = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (getCanceled()) return;

        age++;

        // Scale animation
        if (scaleProgress < 1.0) {
            scaleProgress = Math.min(1.0, scaleProgress + 0.99 / 20.0);
        }

        // Status handling
        if (status == 2) {
            statusTick++;
            float fadeAlpha = GraphMathHelper.lerp(
                    1.0f - (float) statusTick / 10.0f, 0.0f, 1.0f);
            if (fadeAlpha <= 0.02f || statusTick > 10) {
                super.remove();
                return;
            }
        }

        Level world = getWorld();
        if (!(world instanceof ServerLevel level)) return;

        Vec3 center = getPos();
        double rotAngle = age * rotateSpeed;
        double cos = Math.cos(rotAngle);
        double sin = Math.sin(rotAngle);

        // Render randomly positioned dust particles
        for (int i = 0; i < count; i++) {
            double rx = (random.nextDouble() * 2 - 1) * maxRadius * scaleProgress;
            double ry = (random.nextDouble() * 2 - 1) * maxRadius * scaleProgress;
            double rz = (random.nextDouble() * 2 - 1) * maxRadius * scaleProgress;

            // Rotate around Y axis
            double px = rx * cos - rz * sin;
            double pz = rx * sin + rz * cos;

            float size = (float) (sizeMin + random.nextDouble() * (sizeMax - sizeMin));
            level.sendParticles(
                    new DustParticleOptions(DUST_COLOR.getColor(), size),
                    center.x + px, center.y + ry, center.z + pz,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Ambient
        if (age % 5 == 0) {
            level.sendParticles(ParticleTypes.PORTAL,
                    center.x, center.y + 0.1, center.z,
                    4, 0.2, 0.15, 0.2, 0.0);
        }
    }

    @Override
    public Map<String, ParticleControllerDataBuffer<?>> writePacketArgs() {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put("status", ParticleControllerDataBuffers.INSTANCE.intValue(status));
        return args;
    }

    @Override
    public void readPacketArgs(Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
        if (args == null) return;
        ParticleControllerDataBuffer<?> statusBuf = args.get("status");
        if (statusBuf != null && statusBuf.getLoadedValue() instanceof Number n) {
            status = n.intValue();
        }
    }

    public static final class Provider implements ParticleStyleProvider<EndDustStyle> {
        @Override
        public EndDustStyle create() {
            return new EndDustStyle();
        }

        @Override
        public EndDustStyle createStyle(UUID uuid,
                                         Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
            EndDustStyle style = new EndDustStyle(uuid);
            style.readPacketArgs(args);
            return style;
        }

        @Override
        public void changeStyle(EndDustStyle style,
                                Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
            style.readPacketArgs(args);
        }
    }
}

