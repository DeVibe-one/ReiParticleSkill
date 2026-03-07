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
 * Explosion emitter that spawns particles on a ball surface with random outward velocity
 * and applies velocity drag each tick via physics.
 */
public final class ExplodeClassParticleEmitters extends ClassParticleEmitters {

    public static final ResourceLocation ID = new ResourceLocation("reiparticlesapi", "explode_class");

    private ControllableParticleData templateData = new ControllableParticleData();
    private final Random random = new Random(System.currentTimeMillis());

    public ExplodeClassParticleEmitters(Vec3 pos, Level world) {
        super(pos, world);
    }

    public ControllableParticleData getTemplateData() {
        return templateData;
    }

    public void setTemplateData(ControllableParticleData templateData) {
        this.templateData = templateData;
    }

    @Override
    public void doTick() {
    }

    @Override
    public List<Map.Entry<SerializableData, RelativeLocation>> genParticles(float lerpProgress) {
        List<RelativeLocation> velocityList = new PointsBuilder()
                .addBall(2.0, 40)
                .rotateAsAxis(random.nextDouble() * Math.PI * 2 - Math.PI)
                .create();
        List<Map.Entry<SerializableData, RelativeLocation>> result = new ArrayList<>();
        int count = 800 + random.nextInt(200);
        for (int i = 0; i < count; i++) {
            RelativeLocation vel = velocityList.get(random.nextInt(velocityList.size()));
            ControllableParticleData data = templateData.clone();
            data.setVelocity(vel.normalize().multiplyClone(0.5 + random.nextDouble() * 5.5).toVector());
            result.add(new AbstractMap.SimpleEntry<>(data, new RelativeLocation()));
        }
        return result;
    }

    @Override
    public void singleParticleAction(Controllable<?> controller, SerializableData data,
                                      RelativeLocation spawnPos, Level spawnWorld,
                                      float particleLerpProgress, float posLerpProgress) {
        if (data instanceof ControllableParticleData cpd) {
            cpd.setParticleColor(Math3DUtil.colorOf(
                    200 + random.nextInt(55),
                    200 + random.nextInt(55),
                    200 + random.nextInt(55)));
        }
    }

    @Override
    public ResourceLocation getEmittersID() {
        return ID;
    }

    @Override
    protected void writePayload(FriendlyByteBuf buf) {
        ClassParticleEmitters.Companion.encodeBase(this, buf);
        templateData.writeToBuf(buf);
    }

    @Override
    protected void readPayload(FriendlyByteBuf buf) {
        ClassParticleEmitters.Companion.decodeBase(this, buf);
        templateData.readFromBuf(buf);
    }

    @Override
    public void update(ParticleEmitters emitters) {
        super.update(emitters);
        if (emitters instanceof ExplodeClassParticleEmitters other) {
            this.templateData = other.templateData;
        }
    }
}
