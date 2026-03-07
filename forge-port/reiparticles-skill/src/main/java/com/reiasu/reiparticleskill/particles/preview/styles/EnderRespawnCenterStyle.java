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
import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.utils.builder.PointsBuilder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ender respawn center style that renders concentric Fourier-series glyphs
 * and a large outer circle with enchantment particles.
 * Features multiple rotating layers with different speeds and scale-in animation.
 * Color: purple (210, 80, 255). Server-side port of the Fabric original.
 */
@ReiAutoRegister
 public final class EnderRespawnCenterStyle extends ParticleGroupStyle {
    public static final ResourceLocation REGISTRY_KEY = new ResourceLocation("reiparticleskill", "ender_respawn_center_style");
    private static final DustParticleOptions CENTER_COLOR =
            new DustParticleOptions(new Vector3f(210f / 255f, 80f / 255f, 1.0f), 0.6f);
    private static final DustParticleOptions GLYPH_COLOR =
            new DustParticleOptions(new Vector3f(0.65f, 0.25f, 0.85f), 0.45f);

    private static final List<RelativeLocation> INNER_RING = new PointsBuilder()
            .addCircle(35.0, 400)
            .create();
    private static final List<RelativeLocation> OUTER_RING = new PointsBuilder()
            .addCircle(75.0, 120)
            .create();

    private int status = 1;
    private int statusTick = 0;
    private double scaleProgress = 0.01;
    private int age;

    public EnderRespawnCenterStyle() {
        this(UUID.randomUUID());
    }

    public EnderRespawnCenterStyle(UUID uuid) {
        setUuid(uuid == null ? UUID.randomUUID() : uuid);
        setVisibleRange(512.0);
        setAutoToggle(true);
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
        if (status == 2 && statusTick > 20) {
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
        if (age <= 20) {
            double t = (double) age / 20.0;
            scaleProgress = 0.01 + 0.99 * easeOutCubic(t);
        } else {
            scaleProgress = 1.0;
        }

        if (status == 2) {
            statusTick++;
            scaleProgress = Math.max(0.01, scaleProgress - 0.99 / 20.0);
            if (scaleProgress <= 0.01) {
                super.remove();
                return;
            }
        }

        Level world = getWorld();
        if (!(world instanceof ServerLevel level)) return;

        Vec3 center = getPos();
        double sc = scaleProgress;

        // Inner ring - slow rotation
        double innerAngle = age * 0.01227184630308513;
        double innerCos = Math.cos(innerAngle);
        double innerSin = Math.sin(innerAngle);
        int innerStep = 4;
        for (int i = 0; i < INNER_RING.size(); i += innerStep) {
            RelativeLocation p = INNER_RING.get(i);
            double px = p.getX() * sc;
            double pz = p.getZ() * sc;
            double rx = px * innerCos - pz * innerSin;
            double rz = px * innerSin + pz * innerCos;
            level.sendParticles(CENTER_COLOR,
                    center.x + rx, center.y + 0.05, center.z + rz,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Outer ring - counter-rotation
        double outerAngle = -age * 0.01227184630308513;
        double outerCos = Math.cos(outerAngle);
        double outerSin = Math.sin(outerAngle);
        int outerStep = 2;
        for (int i = 0; i < OUTER_RING.size(); i += outerStep) {
            RelativeLocation p = OUTER_RING.get(i);
            double px = p.getX() * sc;
            double pz = p.getZ() * sc;
            double rx = px * outerCos - pz * outerSin;
            double rz = px * outerSin + pz * outerCos;
            level.sendParticles(GLYPH_COLOR,
                    center.x + rx, center.y + 0.03, center.z + rz,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Enchantment ambient
        if (age % 3 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    center.x, center.y + 0.15, center.z,
                    8, 1.5, 0.1, 1.5, 0.0);
        }
        if (age % 5 == 0) {
            level.sendParticles(ParticleTypes.PORTAL,
                    center.x, center.y + 0.1, center.z,
                    6, 2.0, 0.08, 2.0, 0.0);
        }
    }

    private static double easeOutCubic(double t) {
        double inv = 1.0 - t;
        return 1.0 - inv * inv * inv;
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
        ParticleControllerDataBuffer<?> buf = args.get("status");
        if (buf != null && buf.getLoadedValue() instanceof Number n) {
            status = n.intValue();
        }
    }

    public static final class Provider implements ParticleStyleProvider<EnderRespawnCenterStyle> {
        @Override
        public EnderRespawnCenterStyle create() {
            return new EnderRespawnCenterStyle();
        }

        @Override
        public EnderRespawnCenterStyle createStyle(UUID uuid,
                                                    Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
            EnderRespawnCenterStyle style = new EnderRespawnCenterStyle(uuid);
            style.readPacketArgs(args);
            return style;
        }

        @Override
        public void changeStyle(EnderRespawnCenterStyle style,
                                Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
            style.readPacketArgs(args);
        }
    }
}
