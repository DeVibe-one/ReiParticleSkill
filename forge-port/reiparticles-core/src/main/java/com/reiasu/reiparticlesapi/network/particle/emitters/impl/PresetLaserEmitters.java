// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.impl;

import com.reiasu.reiparticlesapi.network.particle.emitters.ClassParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.data.SerializableData;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.builder.PointsBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Laser beam emitter that spawns particles along a line from origin to target,
 * with optional ring cross-sections for visual depth.
 */
public final class PresetLaserEmitters extends ClassParticleEmitters {

    public static final ResourceLocation ID = new ResourceLocation("reiparticlesapi", "preset_laser");

    private ControllableParticleData templateData = new ControllableParticleData();
    private final Random random = new Random(System.currentTimeMillis());
    private Vec3 target = Vec3.ZERO;
    private double beamRadius = 0.3;
    private int beamSegments = 20;
    private int ringCount = 8;

    public PresetLaserEmitters(Vec3 pos, Level world) {
        super(pos, world);
    }

    public ControllableParticleData getTemplateData() {
        return templateData;
    }

    public void setTemplateData(ControllableParticleData templateData) {
        this.templateData = templateData;
    }

    public Vec3 getTarget() {
        return target;
    }

    public void setTarget(Vec3 target) {
        this.target = target;
    }

    public double getBeamRadius() {
        return beamRadius;
    }

    public void setBeamRadius(double beamRadius) {
        this.beamRadius = beamRadius;
    }

    public int getBeamSegments() {
        return beamSegments;
    }

    public void setBeamSegments(int beamSegments) {
        this.beamSegments = beamSegments;
    }

    public int getRingCount() {
        return ringCount;
    }

    public void setRingCount(int ringCount) {
        this.ringCount = ringCount;
    }

    @Override
    public void doTick() {
    }

    @Override
    public List<Map.Entry<SerializableData, RelativeLocation>> genParticles(float lerpProgress) {
        Vec3 origin = position();
        if (origin == null) origin = Vec3.ZERO;
        Vec3 direction = target.subtract(origin);
        double length = direction.length();
        if (length < 0.01) return new ArrayList<>();

        Vec3 dir = direction.normalize();
        List<Map.Entry<SerializableData, RelativeLocation>> result = new ArrayList<>();

        for (int i = 0; i <= beamSegments; i++) {
            double t = (double) i / beamSegments;
            RelativeLocation linePos = new RelativeLocation(
                    direction.x * t, direction.y * t, direction.z * t);

            // Center particle
            ControllableParticleData data = templateData.clone();
            result.add(new AbstractMap.SimpleEntry<>(data, linePos));

            // Ring particles around beam axis
            if (beamRadius > 0 && ringCount > 0) {
                for (int j = 0; j < ringCount; j++) {
                    double angle = (Math.PI * 2.0 / ringCount) * j;
                    RelativeLocation offset = getPerpendicularOffset(dir, angle, beamRadius);
                    RelativeLocation ringPos = new RelativeLocation(
                            linePos.getX() + offset.getX(),
                            linePos.getY() + offset.getY(),
                            linePos.getZ() + offset.getZ());
                    ControllableParticleData ringData = templateData.clone();
                    result.add(new AbstractMap.SimpleEntry<>(ringData, ringPos));
                }
            }
        }
        return result;
    }

    private RelativeLocation getPerpendicularOffset(Vec3 dir, double angle, double radius) {
        Vec3 up = Math.abs(dir.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 forward = right.cross(dir).normalize();
        double x = Math.cos(angle) * radius;
        double y = Math.sin(angle) * radius;
        return new RelativeLocation(
                right.x * x + forward.x * y,
                right.y * x + forward.y * y,
                right.z * x + forward.z * y);
    }

    @Override
    public void singleParticleAction(Controllable<?> controller, SerializableData data,
                                      RelativeLocation spawnPos, Level spawnWorld,
                                      float particleLerpProgress, float posLerpProgress) {
    }

    @Override
    public ResourceLocation getEmittersID() {
        return ID;
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        ClassParticleEmitters.Companion.encodeBase(this, buf);
        buf.writeDouble(target.x);
        buf.writeDouble(target.y);
        buf.writeDouble(target.z);
        buf.writeDouble(beamRadius);
        buf.writeInt(beamSegments);
        buf.writeInt(ringCount);
        templateData.writeToBuf(buf);
    }

    @Override
    protected void readPayload(FriendlyByteBuf buf) {
        ClassParticleEmitters.Companion.decodeBase(this, buf);
        target = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        beamRadius = buf.readDouble();
        beamSegments = buf.readInt();
        ringCount = buf.readInt();
        templateData.readFromBuf(buf);
    }

    @Override
    public void update(ParticleEmitters emitters) {
        super.update(emitters);
        if (emitters instanceof PresetLaserEmitters other) {
            this.templateData = other.templateData;
            this.target = other.target;
            this.beamRadius = other.beamRadius;
            this.beamSegments = other.beamSegments;
            this.ringCount = other.ringCount;
        }
    }
}
