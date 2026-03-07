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
 * Railgun beam style that renders a long beam of particles along a direction,
 * with hexagonal vertex anchors and a rotating central column.
 * Server-side port of the Fabric original.
 */
@ReiAutoRegister
 public final class RailgunBeamStyle extends ParticleGroupStyle {
    public static final ResourceLocation REGISTRY_KEY = new ResourceLocation("reiparticleskill", "railgun_beam_style");
    private static final DustParticleOptions BEAM_COLOR =
            new DustParticleOptions(new Vector3f(210f / 255f, 80f / 255f, 1.0f), 0.75f);
    private static final DustParticleOptions GLOW_COLOR =
            new DustParticleOptions(new Vector3f(0.85f, 0.55f, 1.0f), 0.5f);

    private static final List<RelativeLocation> BEAM_POINTS = new PointsBuilder()
            .addLine(new RelativeLocation(0.0, -100.0, 0.0),
                     new RelativeLocation(0.0, 200.0, 0.0), 2000)
            .create();

    private RelativeLocation movement = new RelativeLocation(0.0, 1.0, 0.0);
    private int age;
    private int maxAge = 60;
    private double rotateSpeed = 0.04908738521234052;

    public RailgunBeamStyle() {
        this(UUID.randomUUID());
    }

    public RailgunBeamStyle(UUID uuid) {
        setUuid(uuid == null ? UUID.randomUUID() : uuid);
        setVisibleRange(512.0);
        setAutoToggle(true);
    }

    public RelativeLocation getMovement() {
        return movement;
    }

    public void setMovement(RelativeLocation movement) {
        this.movement = movement;
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
        addPreTickAction(style -> style.rotateAsAxis(rotateSpeed));
    }

    @Override
    public void tick() {
        super.tick();
        if (getCanceled()) {
            return;
        }

        age++;
        rotateSpeed += 0.0030679615757712823;

        if (age > maxAge) {
            remove();
            return;
        }

        Level world = getWorld();
        if (!(world instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = getPos();
        double angle = getRotate();
        double scale = scaleAtAge(age);

        // Build basis from movement direction
        RelativeLocation dir = movement.copy().normalize();
        if (dir.length() < 1.0E-6) {
            dir = new RelativeLocation(0.0, 1.0, 0.0);
        }

        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        // Render beam points along the direction
        int step = age < 15 ? 4 : 3;
        for (int i = 0; i < BEAM_POINTS.size(); i += step) {
            RelativeLocation p = BEAM_POINTS.get(i);
            double px = p.getX() * scale;
            double py = p.getY();
            double pz = p.getZ() * scale;
            double rx = px * cos - pz * sin;
            double rz = px * sin + pz * cos;

            // Project onto movement direction
            double dx = dir.getX() * py + rx;
            double dy = dir.getY() * py;
            double dz = dir.getZ() * py + rz;

            level.sendParticles(BEAM_COLOR,
                    center.x + dx, center.y + dy, center.z + dz,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Hexagonal vertex glow anchors
        List<RelativeLocation> hexVerts = new PointsBuilder()
                .addCircle(8.0 * scale, 6)
                .create();
        for (RelativeLocation v : hexVerts) {
            level.sendParticles(GLOW_COLOR,
                    center.x + v.getX(), center.y + 0.1, center.z + v.getZ(),
                    2, 0.05, 0.05, 0.05, 0.0);
        }

        // Ambient
        if (age % 2 == 0) {
            level.sendParticles(ParticleTypes.END_ROD,
                    center.x, center.y + 0.15, center.z,
                    3, 0.1, 0.08, 0.1, 0.0);
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

    private static double scaleAtAge(int age) {
        if (age <= 0) return 0.01;
        if (age < 20) return 0.01 + 0.99 * ((double) age / 20.0);
        return 1.0;
    }

    public static final class Provider implements ParticleStyleProvider<RailgunBeamStyle> {
        @Override
        public RailgunBeamStyle create() {
            return new RailgunBeamStyle();
        }

        @Override
        public RailgunBeamStyle createStyle(UUID uuid,
                                             Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
            RailgunBeamStyle style = new RailgunBeamStyle(uuid);
            style.readPacketArgs(args);
            return style;
        }

        @Override
        public void changeStyle(RailgunBeamStyle style,
                                Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
            style.readPacketArgs(args);
        }
    }
}
