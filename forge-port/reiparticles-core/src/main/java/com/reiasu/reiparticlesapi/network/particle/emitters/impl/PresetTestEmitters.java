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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Preset test emitter for development/debug purposes.
 * Spawns particles in a simple ring pattern.
 */
public final class PresetTestEmitters extends ClassParticleEmitters {

    public static final ResourceLocation ID = new ResourceLocation("reiparticlesapi", "preset_test");

    private ControllableParticleData templateData = new ControllableParticleData();
    private final Random random = new Random(System.currentTimeMillis());
    private int count = 20;
    private double radius = 2.0;

    public PresetTestEmitters(Vec3 pos, Level world) {
        super(pos, world);
    }

    public ControllableParticleData getTemplateData() {
        return templateData;
    }

    public void setTemplateData(ControllableParticleData templateData) {
        this.templateData = templateData;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    @Override
    public void doTick() {
    }

    @Override
    public List<Map.Entry<SerializableData, RelativeLocation>> genParticles(float lerpProgress) {
        List<RelativeLocation> points = new PointsBuilder()
                .addCircle(radius, count)
                .create();
        List<Map.Entry<SerializableData, RelativeLocation>> result = new ArrayList<>();
        for (RelativeLocation pt : points) {
            ControllableParticleData data = templateData.clone();
            result.add(new AbstractMap.SimpleEntry<>(data, pt));
        }
        return result;
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
        buf.writeInt(count);
        buf.writeDouble(radius);
        templateData.writeToBuf(buf);
    }

    @Override
    protected void readPayload(FriendlyByteBuf buf) {
        ClassParticleEmitters.Companion.decodeBase(this, buf);
        count = buf.readInt();
        radius = buf.readDouble();
        templateData.readFromBuf(buf);
    }

    @Override
    public void update(ParticleEmitters emitters) {
        super.update(emitters);
        if (emitters instanceof PresetTestEmitters other) {
            this.templateData = other.templateData;
            this.count = other.count;
            this.radius = other.radius;
        }
    }
}
