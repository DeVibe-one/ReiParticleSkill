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
 * End crystal particle style rendering a rotating circle with Fourier-series glyph overlay.
 * Color: purple (210, 80, 255). Supports scale-in/out and status-driven removal.
 * Server-side port of the Fabric original.
 */
@ReiAutoRegister
 public final class EndCrystalStyle extends ParticleGroupStyle {
    public static final ResourceLocation REGISTRY_KEY = new ResourceLocation("reiparticleskill", "end_crystal_style");
    private static final DustParticleOptions CRYSTAL_COLOR =
            new DustParticleOptions(new Vector3f(210f / 255f, 80f / 255f, 1.0f), 0.55f);

    private static final List<RelativeLocation> CIRCLE_POINTS = new PointsBuilder()
            .addCircle(2.0, 120)
            .create();

    private RelativeLocation rotated = new RelativeLocation(0.0, 1.0, 0.0);
    private int status = 1; // 1=ENABLE, 2=DISABLE
    private int statusTick = 0;
    private double scaleProgress = 0.01;
    private int age;

    public EndCrystalStyle() {
        this(UUID.randomUUID());
    }

    public EndCrystalStyle(UUID uuid) {
        setUuid(uuid == null ? UUID.randomUUID() : uuid);
        setVisibleRange(256.0);
        setAutoToggle(true);
    }

    public RelativeLocation getRotated() {
        return rotated;
    }

    public void setRotated(RelativeLocation rotated) {
        this.rotated = rotated;
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

        // Scale in/out
        if (status == 1 && scaleProgress < 1.0) {
            scaleProgress = Math.min(1.0, scaleProgress + 0.99 / 20.0);
        } else if (status == 2) {
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
        double rotAngle = age * 0.02454369260617026;
        double cos = Math.cos(rotAngle);
        double sin = Math.sin(rotAngle);
        double sc = scaleProgress;

        // Render circle
        int step = age < 20 ? 3 : 2;
        for (int i = 0; i < CIRCLE_POINTS.size(); i += step) {
            RelativeLocation p = CIRCLE_POINTS.get(i);
            double px = p.getX() * sc;
            double pz = p.getZ() * sc;
            double py = p.getY() + 1.0;
            double rx = px * cos - pz * sin;
            double rz = px * sin + pz * cos;

            level.sendParticles(CRYSTAL_COLOR,
                    center.x + rx, center.y + py * sc, center.z + rz,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Ambient
        if (age % 4 == 0) {
            level.sendParticles(ParticleTypes.PORTAL,
                    center.x, center.y + 1.0, center.z,
                    3, 0.15, 0.1, 0.15, 0.0);
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

    public static final class Provider implements ParticleStyleProvider<EndCrystalStyle> {
        @Override
        public EndCrystalStyle create() {
            return new EndCrystalStyle();
        }

        @Override
        public EndCrystalStyle createStyle(UUID uuid,
                                            Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
            EndCrystalStyle style = new EndCrystalStyle(uuid);
            style.readPacketArgs(args);
            return style;
        }

        @Override
        public void changeStyle(EndCrystalStyle style,
                                Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
            style.readPacketArgs(args);
        }
    }
}
