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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ender respawn wave enchantment style that renders enchantment-like particle rings
 * with Y offset, random angles, rotation, and status-driven alpha fade.
 * Color: purple (210, 80, 255). Server-side port of the Fabric original.
 */
@ReiAutoRegister
 public final class EnderRespawnWaveEnchantStyle extends ParticleGroupStyle {
    public static final ResourceLocation REGISTRY_KEY = new ResourceLocation("reiparticleskill", "ender_respawn_wave_enchant_style");
    private static final DustParticleOptions ENCHANT_COLOR =
            new DustParticleOptions(new Vector3f(210f / 255f, 80f / 255f, 1.0f), 0.5f);

    private final RandomSource random = RandomSource.create();

    private double yOffset = 0.0;
    private double rotateSpeed = 0.03;
    private double minSize = 0.3;
    private double maxSize = 0.8;
    private boolean particleRandomAngle = true;
    private boolean faceCamera = false;
    private float particleYawAngleSpeed = 0.05f;
    private float particlePitchAngleSpeed = 0.03f;
    private float particleRollAngleSpeed = 0.04f;
    private int count = 60;
    private double radius = 10.0;

    private int status = 1;
    private int statusTick = 0;
    private double scaleProgress = 0.01;
    private int age;

    public EnderRespawnWaveEnchantStyle() {
        this(UUID.randomUUID());
    }

    public EnderRespawnWaveEnchantStyle(UUID uuid) {
        setUuid(uuid == null ? UUID.randomUUID() : uuid);
        setVisibleRange(512.0);
        setAutoToggle(true);
    }

    public void setYOffset(double yOffset) {
        this.yOffset = yOffset;
    }

    public void setRotateSpeed(double rotateSpeed) {
        this.rotateSpeed = rotateSpeed;
    }

    public void setSizeRange(double min, double max) {
        this.minSize = min;
        this.maxSize = max;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setRadius(double radius) {
        this.radius = radius;
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
        if (scaleProgress < 1.0) {
            scaleProgress = Math.min(1.0, scaleProgress + 0.99 / 20.0);
        }

        // Status-driven fade
        float alpha = 1.0f;
        if (status == 2) {
            statusTick++;
            alpha = GraphMathHelper.lerp((float) statusTick / 20.0f, 1.0f, 0.0f);
            if (alpha <= 0.02f || statusTick > 20) {
                super.remove();
                return;
            }
        }

        Level world = getWorld();
        if (!(world instanceof ServerLevel level)) return;

        Vec3 center = getPos();
        double sc = scaleProgress;
        double rotAngle = age * rotateSpeed;
        double cos = Math.cos(rotAngle);
        double sin = Math.sin(rotAngle);

        // Render enchantment ring particles
        List<RelativeLocation> ring = new PointsBuilder()
                .addCircle(radius * sc, count)
                .create();

        for (RelativeLocation p : ring) {
            double px = p.getX();
            double pz = p.getZ();
            double rx = px * cos - pz * sin;
            double rz = px * sin + pz * cos;

            float size = (float) (minSize + random.nextDouble() * (maxSize - minSize));
            size *= alpha;
            if (size < 0.05f) continue;

            level.sendParticles(
                    new DustParticleOptions(ENCHANT_COLOR.getColor(), size),
                    center.x + rx,
                    center.y + yOffset + (random.nextDouble() * 0.3 - 0.15),
                    center.z + rz,
                    1, 0.0, 0.0, 0.0, 0.0);
        }

        // Enchantment ambient
        if (age % 3 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    center.x, center.y + yOffset + 0.1, center.z,
                    5, 0.5, 0.1, 0.5, 0.0);
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
        ParticleControllerDataBuffer<?> buf = args.get("status");
        if (buf != null && buf.getLoadedValue() instanceof Number n) {
            status = n.intValue();
        }
    }

    public static final class Provider implements ParticleStyleProvider<EnderRespawnWaveEnchantStyle> {
        @Override
        public EnderRespawnWaveEnchantStyle create() {
            return new EnderRespawnWaveEnchantStyle();
        }

        @Override
        public EnderRespawnWaveEnchantStyle createStyle(UUID uuid,
                                                         Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
            EnderRespawnWaveEnchantStyle style = new EnderRespawnWaveEnchantStyle(uuid);
            style.readPacketArgs(args);
            return style;
        }

        @Override
        public void changeStyle(EnderRespawnWaveEnchantStyle style,
                                Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
            style.readPacketArgs(args);
        }
    }
}

