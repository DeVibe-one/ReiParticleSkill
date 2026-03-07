// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.preview.display;

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
 * Railgun charging ring style that renders concentric rotating rings
 * with a polygon glyph overlay, matching the Fabric original.
 */
@ReiAutoRegister
 public final class RailgunChargingRingStyle extends ParticleGroupStyle {
    public static final ResourceLocation REGISTRY_KEY = new ResourceLocation("reiparticleskill", "railgun_charging_ring_style");
    private static final DustParticleOptions RING_COLOR =
            new DustParticleOptions(new Vector3f(210f / 255f, 80f / 255f, 1.0f), 0.6f);
    private static final DustParticleOptions GLYPH_COLOR =
            new DustParticleOptions(new Vector3f(0.75f, 0.4f, 1.0f), 0.45f);

    private static final List<RelativeLocation> INNER_RING = new PointsBuilder()
            .addCircle(8.0, 180)
            .create();
    private static final List<RelativeLocation> OUTER_RING = new PointsBuilder()
            .addCircle(9.0, 160)
            .addPolygonInCircle(4, 100, 9.0)
            .rotateAsAxis(Math.PI / 4.0)
            .withBuilder(new PointsBuilder().addPolygonInCircle(4, 100, 9.0))
            .create();

    private int age;
    private int maxAge = 80;
    private double scaleProgress = 0.01;

    public RailgunChargingRingStyle() {
        this(UUID.randomUUID());
    }

    public RailgunChargingRingStyle(UUID uuid) {
        setUuid(uuid == null ? UUID.randomUUID() : uuid);
        setVisibleRange(512.0);
        setAutoToggle(true);
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public Map<StyleData, RelativeLocation> getCurrentFrames() {
        return Map.of();
    }

    @Override
    public void onDisplay() {
        age = 0;
        scaleProgress = 0.01;
    }

    @Override
    public void tick() {
        super.tick();
        if (getCanceled()) {
            return;
        }

        age++;

        // Scale in with bezier-like ease over 20 ticks
        if (age <= 20) {
            double t = (double) age / 20.0;
            scaleProgress = 0.01 + 0.99 * easeOutCubic(t);
        } else {
            scaleProgress = 1.0;
        }

        if (age > maxAge) {
            remove();
            return;
        }

        Level world = getWorld();
        if (!(world instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = getPos();
        double scale = scaleProgress;

        // Inner ring - slow rotation
        double innerAngle = age * 0.02454369260617026;
        double innerCos = Math.cos(innerAngle);
        double innerSin = Math.sin(innerAngle);
        int innerStep = age < 20 ? 3 : 2;
        for (int i = 0; i < INNER_RING.size(); i += innerStep) {
            RelativeLocation p = INNER_RING.get(i);
            double px = p.getX() * scale;
            double pz = p.getZ() * scale;
            double rx = px * innerCos - pz * innerSin;
            double rz = px * innerSin + pz * innerCos;
            level.sendParticles(RING_COLOR,
                    center.x + rx, center.y + 0.05, center.z + rz,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Outer ring with polygon - faster counter-rotation
        double outerAngle = -age * 0.09817477042468103;
        double outerCos = Math.cos(outerAngle);
        double outerSin = Math.sin(outerAngle);
        int outerStep = age < 20 ? 4 : 3;
        for (int i = 0; i < OUTER_RING.size(); i += outerStep) {
            RelativeLocation p = OUTER_RING.get(i);
            double px = p.getX() * scale;
            double pz = p.getZ() * scale;
            double rx = px * outerCos - pz * outerSin;
            double rz = px * outerSin + pz * outerCos;
            level.sendParticles(GLYPH_COLOR,
                    center.x + rx, center.y + 0.03, center.z + rz,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Ambient effects
        if (age % 3 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    center.x, center.y + 0.1, center.z,
                    6, 0.3, 0.05, 0.3, 0.0);
        }
        if (age % 5 == 0) {
            level.sendParticles(ParticleTypes.END_ROD,
                    center.x, center.y + 0.12, center.z,
                    3, 0.2, 0.04, 0.2, 0.0);
        }
    }

    @Override
    public Map<String, ParticleControllerDataBuffer<?>> writePacketArgs() {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put("age", ParticleControllerDataBuffers.INSTANCE.intValue(age));
        args.put("maxAge", ParticleControllerDataBuffers.INSTANCE.intValue(maxAge));
        return args;
    }

    @Override
    public void readPacketArgs(Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
        if (args == null) return;
        ParticleControllerDataBuffer<?> ageBuf = args.get("age");
        if (ageBuf != null && ageBuf.getLoadedValue() instanceof Number n) {
            age = Math.max(0, n.intValue());
        }
        ParticleControllerDataBuffer<?> maxAgeBuf = args.get("maxAge");
        if (maxAgeBuf != null && maxAgeBuf.getLoadedValue() instanceof Number n) {
            maxAge = Math.max(1, n.intValue());
        }
    }

    private static double easeOutCubic(double t) {
        double inv = 1.0 - t;
        return 1.0 - inv * inv * inv;
    }

    public static final class Provider implements ParticleStyleProvider<RailgunChargingRingStyle> {
        @Override
        public RailgunChargingRingStyle create() {
            return new RailgunChargingRingStyle();
        }

        @Override
        public RailgunChargingRingStyle createStyle(UUID uuid,
                                                     Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
            RailgunChargingRingStyle style = new RailgunChargingRingStyle(uuid);
            style.readPacketArgs(args);
            return style;
        }

        @Override
        public void changeStyle(RailgunChargingRingStyle style,
                                Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
            style.readPacketArgs(args);
        }
    }
}
