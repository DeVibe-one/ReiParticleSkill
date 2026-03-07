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
import com.reiasu.reiparticlesapi.utils.builder.FourierSeriesBuilder;
import com.reiasu.reiparticlesapi.annotations.ReiAutoRegister;
import com.reiasu.reiparticlesapi.utils.builder.PointsBuilder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
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

@ReiAutoRegister
 public final class LargeMagicCircleStyle extends ParticleGroupStyle {
    public static final ResourceLocation REGISTRY_KEY = new ResourceLocation("reiparticleskill", "large_magic_circle_style");
    private static final DustParticleOptions PRIMARY_COLOR =
            new DustParticleOptions(new Vector3f(0.95f, 0.82f, 0.35f), 1.0f);
    private static final DustParticleOptions SECONDARY_COLOR =
            new DustParticleOptions(new Vector3f(0.62f, 0.88f, 1.0f), 0.85f);
    private static final List<RelativeLocation> BASE_POINTS = new PointsBuilder()
            .addFourierSeries(
                    new FourierSeriesBuilder()
                            .scale(1.0)
                            .count(320)
                            .addFourier(1.0, 1.0, 0.0)
                            .addFourier(0.35, 3.0, 0.0)
            )
            .addPolygonInCircle(3, 48, 3.6)
            .withBuilder(new PointsBuilder().addPolygonInCircle(3, 48, 3.6).rotateAsAxis(Math.PI / 3.0))
            .create();

    private int age;

    public LargeMagicCircleStyle() {
        this(UUID.randomUUID());
    }

    public LargeMagicCircleStyle(UUID uuid) {
        setUuid(uuid == null ? UUID.randomUUID() : uuid);
        setVisibleRange(256.0);
        setAutoToggle(true);
        setScale(0.0);
        addPreTickAction(style -> style.rotateAsAxis(0.04908738521234052));
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = Math.max(0, age);
    }


    @Override
    public Map<StyleData, RelativeLocation> getCurrentFrames() {
        return Map.of();
    }

    @Override
    public void onDisplay() {
        age = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (getCanceled()) {
            return;
        }

        age++;
        double envelope = envelope(age);
        setScale(envelope);
        if (envelope <= 0.0001 || age > 190) {
            remove();
            return;
        }

        Level world = getWorld();
        if (!(world instanceof ServerLevel level)) {
            return;
        }

        Vec3 center = getPos();
        double angle = getRotate();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double baseScale = 2.0 * Math.max(0.15, getScale());

        int step = age < 25 ? 3 : 2;
        for (int i = 0; i < BASE_POINTS.size(); i += step) {
            RelativeLocation point = BASE_POINTS.get(i);
            double px = point.getX() * baseScale;
            double pz = point.getZ() * baseScale;
            double rx = px * cos - pz * sin;
            double rz = px * sin + pz * cos;
            double x = center.x + rx;
            double y = center.y + 0.06 + Math.sin((i * 0.08) + age * 0.2) * 0.04;
            double z = center.z + rz;

            spawn(level, PRIMARY_COLOR, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
            if ((i & 3) == 0) {
                spawn(level, SECONDARY_COLOR, x, y + 0.02, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        if ((age & 3) == 0) {
            spawn(level, ParticleTypes.ENCHANT, center.x, center.y + 0.08, center.z, 10, 0.25, 0.02, 0.25, 0.0);
        }
        if ((age & 7) == 0) {
            spawn(level, ParticleTypes.END_ROD, center.x, center.y + 0.1, center.z, 6, 0.3, 0.02, 0.3, 0.0);
        }
    }

    @Override
    public Map<String, ParticleControllerDataBuffer<?>> writePacketArgs() {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put("age", ParticleControllerDataBuffers.INSTANCE.intValue(age));
        return args;
    }

    @Override
    public void readPacketArgs(Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
        if (args == null) {
            return;
        }
        ParticleControllerDataBuffer<?> ageBuffer = args.get("age");
        Object loaded = ageBuffer == null ? null : ageBuffer.getLoadedValue();
        if (loaded instanceof Number number) {
            age = Math.max(0, number.intValue());
        }
    }

    private static void spawn(
            ServerLevel level,
            ParticleOptions particle,
            double x,
            double y,
            double z,
            int count,
            double ox,
            double oy,
            double oz,
            double speed
    ) {
        level.sendParticles(particle, x, y, z, count, ox, oy, oz, speed);
    }

    private static double envelope(int age) {
        if (age < 20) {
            return age / 20.0;
        }
        if (age > 150) {
            return Math.max(0.0, 1.0 - (age - 150) / 40.0);
        }
        return 1.0;
    }

    public static final class Provider implements ParticleStyleProvider<LargeMagicCircleStyle> {
        @Override
        public LargeMagicCircleStyle create() {
            return new LargeMagicCircleStyle();
        }

        @Override
        public LargeMagicCircleStyle createStyle(
                UUID uuid,
                Map<String, ? extends ParticleControllerDataBuffer<?>> args
        ) {
            LargeMagicCircleStyle style = new LargeMagicCircleStyle(uuid);
            style.readPacketArgs(args);
            return style;
        }

        @Override
        public void changeStyle(LargeMagicCircleStyle style, Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
            style.readPacketArgs(args);
        }
    }
}
