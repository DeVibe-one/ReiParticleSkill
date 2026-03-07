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

/**
 * Example emitter that spawns particles on a ball surface with inward velocity.
 */
public final class ExampleClassParticleEmitters extends ClassParticleEmitters {

    public static final ResourceLocation ID = new ResourceLocation("reiparticlesapi", "example_class");

    private ControllableParticleData templateData = new ControllableParticleData();
    private Vec3 moveDirection = Vec3.ZERO;

    public ExampleClassParticleEmitters(Vec3 pos, Level world) {
        super(pos, world);
    }

    public ControllableParticleData getTemplateData() {
        return templateData;
    }

    public void setTemplateData(ControllableParticleData templateData) {
        this.templateData = templateData;
    }

    public Vec3 getMoveDirection() {
        return moveDirection;
    }

    public void setMoveDirection(Vec3 moveDirection) {
        this.moveDirection = moveDirection;
    }

    @Override
    public void doTick() {
    }

    @Override
    public List<Map.Entry<SerializableData, RelativeLocation>> genParticles(float lerpProgress) {
        List<RelativeLocation> points = new PointsBuilder().addBall(2.0, 20).create();
        List<Map.Entry<SerializableData, RelativeLocation>> result = new ArrayList<>();
        for (RelativeLocation pt : points) {
            ControllableParticleData data = templateData.clone();
            data.setVelocity(pt.normalize().multiplyClone(-0.1).toVector());
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
        buf.writeDouble(moveDirection.x);
        buf.writeDouble(moveDirection.y);
        buf.writeDouble(moveDirection.z);
        templateData.writeToBuf(buf);
    }

    @Override
    protected void readPayload(FriendlyByteBuf buf) {
        ClassParticleEmitters.Companion.decodeBase(this, buf);
        moveDirection = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        templateData.readFromBuf(buf);
    }

    @Override
    public void update(ParticleEmitters emitters) {
        super.update(emitters);
        if (emitters instanceof ExampleClassParticleEmitters other) {
            this.templateData = other.templateData;
            this.moveDirection = other.moveDirection;
        }
    }
}
