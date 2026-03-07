// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.impl;

import com.reiasu.reiparticlesapi.network.particle.emitters.ClassParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.data.SerializableData;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.utils.Math3DUtil;
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
 * Fire emitter that spawns flame-colored particles with upward velocity
 * and slight outward spread, simulating a fire column effect.
 */
public final class FireClassParticleEmitters extends ClassParticleEmitters {

    public static final ResourceLocation ID = new ResourceLocation("reiparticlesapi", "fire_class");

    private ControllableParticleData templateData = new ControllableParticleData();
    private final Random random = new Random(System.currentTimeMillis());
    private double fireRadius = 1.0;
    private double fireHeight = 3.0;

    public FireClassParticleEmitters(Vec3 pos, Level world) {
        super(pos, world);
    }

    public ControllableParticleData getTemplateData() {
        return templateData;
    }

    public void setTemplateData(ControllableParticleData templateData) {
        this.templateData = templateData;
    }

    public double getFireRadius() {
        return fireRadius;
    }

    public void setFireRadius(double fireRadius) {
        this.fireRadius = fireRadius;
    }

    public double getFireHeight() {
        return fireHeight;
    }

    public void setFireHeight(double fireHeight) {
        this.fireHeight = fireHeight;
    }

    @Override
    public void doTick() {
    }

    @Override
    public List<Map.Entry<SerializableData, RelativeLocation>> genParticles(float lerpProgress) {
        List<Map.Entry<SerializableData, RelativeLocation>> result = new ArrayList<>();
        int count = 20 + random.nextInt(30);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double r = random.nextDouble() * fireRadius;
            double x = Math.cos(angle) * r;
            double z = Math.sin(angle) * r;
            double y = random.nextDouble() * 0.5;
            RelativeLocation spawnPos = new RelativeLocation(x, y, z);

            ControllableParticleData data = templateData.clone();
            double upSpeed = 0.05 + random.nextDouble() * 0.15;
            double spreadX = (random.nextDouble() - 0.5) * 0.02;
            double spreadZ = (random.nextDouble() - 0.5) * 0.02;
            data.setVelocity(new Vec3(spreadX, upSpeed, spreadZ));
            data.setParticleColor(Math3DUtil.colorOf(
                    200 + random.nextInt(55),
                    100 + random.nextInt(100),
                    random.nextInt(50)));
            result.add(new AbstractMap.SimpleEntry<>(data, spawnPos));
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
        buf.writeDouble(fireRadius);
        buf.writeDouble(fireHeight);
        templateData.writeToBuf(buf);
    }

    @Override
    protected void readPayload(FriendlyByteBuf buf) {
        ClassParticleEmitters.Companion.decodeBase(this, buf);
        fireRadius = buf.readDouble();
        fireHeight = buf.readDouble();
        templateData.readFromBuf(buf);
    }

    @Override
    public void update(ParticleEmitters emitters) {
        super.update(emitters);
        if (emitters instanceof FireClassParticleEmitters other) {
            this.templateData = other.templateData;
            this.fireRadius = other.fireRadius;
            this.fireHeight = other.fireHeight;
        }
    }
}
