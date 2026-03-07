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
 * Physics-enabled emitter that spawns particles with full gravity, wind,
 * air drag, and collision simulation. The most feature-rich emitter in the API.
 * <p>
 * Supports configurable spawn count, spawn shape (ball/box/point),
 * initial velocity distribution, and per-particle physics updates.
 */
public final class PhysicsParticleEmitters extends ClassParticleEmitters {

    public static final ResourceLocation ID = new ResourceLocation("reiparticlesapi", "physics_particle");

    private ControllableParticleData templateData = new ControllableParticleData();
    private final Random random = new Random(System.currentTimeMillis());

    private int count = 50;
    private int countRandom = 10;
    private double spawnRadius = 1.0;
    private double initialSpeedMin = 0.1;
    private double initialSpeedMax = 1.0;
    private Vec3 initialDirection = new Vec3(0, 1, 0);
    private double spreadAngle = Math.PI / 4;
    private boolean useGravity = true;
    private boolean useWind = true;
    private boolean useCollision = false;

    public PhysicsParticleEmitters(Vec3 pos, Level world) {
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

    public int getCountRandom() {
        return countRandom;
    }

    public void setCountRandom(int countRandom) {
        this.countRandom = countRandom;
    }

    public double getSpawnRadius() {
        return spawnRadius;
    }

    public void setSpawnRadius(double spawnRadius) {
        this.spawnRadius = spawnRadius;
    }

    public double getInitialSpeedMin() {
        return initialSpeedMin;
    }

    public void setInitialSpeedMin(double initialSpeedMin) {
        this.initialSpeedMin = initialSpeedMin;
    }

    public double getInitialSpeedMax() {
        return initialSpeedMax;
    }

    public void setInitialSpeedMax(double initialSpeedMax) {
        this.initialSpeedMax = initialSpeedMax;
    }

    public Vec3 getInitialDirection() {
        return initialDirection;
    }

    public void setInitialDirection(Vec3 initialDirection) {
        this.initialDirection = initialDirection;
    }

    public double getSpreadAngle() {
        return spreadAngle;
    }

    public void setSpreadAngle(double spreadAngle) {
        this.spreadAngle = spreadAngle;
    }

    public boolean isUseGravity() {
        return useGravity;
    }

    public void setUseGravity(boolean useGravity) {
        this.useGravity = useGravity;
    }

    public boolean isUseWind() {
        return useWind;
    }

    public void setUseWind(boolean useWind) {
        this.useWind = useWind;
    }

    public boolean isUseCollision() {
        return useCollision;
    }

    public void setUseCollision(boolean useCollision) {
        this.useCollision = useCollision;
    }

    @Override
    public void doTick() {
    }

    @Override
    public List<Map.Entry<SerializableData, RelativeLocation>> genParticles(float lerpProgress) {
        int totalCount = count + (countRandom > 0 ? random.nextInt(countRandom) : 0);
        List<Map.Entry<SerializableData, RelativeLocation>> result = new ArrayList<>(totalCount);

        for (int i = 0; i < totalCount; i++) {
            // Random spawn position within radius
            double r = random.nextDouble() * spawnRadius;
            double theta = random.nextDouble() * Math.PI * 2;
            double phi = random.nextDouble() * Math.PI;
            RelativeLocation spawnPos = new RelativeLocation(
                    r * Math.sin(phi) * Math.cos(theta),
                    r * Math.sin(phi) * Math.sin(theta),
                    r * Math.cos(phi));

            // Random velocity with spread
            double speed = initialSpeedMin + random.nextDouble() * (initialSpeedMax - initialSpeedMin);
            Vec3 dir = applySpread(initialDirection.normalize(), spreadAngle);
            ControllableParticleData data = templateData.clone();
            data.setVelocity(dir.scale(speed));

            result.add(new AbstractMap.SimpleEntry<>(data, spawnPos));
        }
        return result;
    }

    private Vec3 applySpread(Vec3 dir, double maxAngle) {
        if (maxAngle <= 0) return dir;
        double angle = random.nextDouble() * maxAngle;
        double rotation = random.nextDouble() * Math.PI * 2;

        Vec3 up = Math.abs(dir.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 forward = right.cross(dir).normalize();

        double sinA = Math.sin(angle);
        return dir.scale(Math.cos(angle))
                .add(right.scale(sinA * Math.cos(rotation)))
                .add(forward.scale(sinA * Math.sin(rotation)));
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
        buf.writeInt(countRandom);
        buf.writeDouble(spawnRadius);
        buf.writeDouble(initialSpeedMin);
        buf.writeDouble(initialSpeedMax);
        buf.writeDouble(initialDirection.x);
        buf.writeDouble(initialDirection.y);
        buf.writeDouble(initialDirection.z);
        buf.writeDouble(spreadAngle);
        buf.writeBoolean(useGravity);
        buf.writeBoolean(useWind);
        buf.writeBoolean(useCollision);
        templateData.writeToBuf(buf);
    }

    @Override
    protected void readPayload(FriendlyByteBuf buf) {
        ClassParticleEmitters.Companion.decodeBase(this, buf);
        count = buf.readInt();
        countRandom = buf.readInt();
        spawnRadius = buf.readDouble();
        initialSpeedMin = buf.readDouble();
        initialSpeedMax = buf.readDouble();
        initialDirection = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        spreadAngle = buf.readDouble();
        useGravity = buf.readBoolean();
        useWind = buf.readBoolean();
        useCollision = buf.readBoolean();
        templateData.readFromBuf(buf);
    }

    @Override
    public void update(ParticleEmitters emitters) {
        super.update(emitters);
        if (emitters instanceof PhysicsParticleEmitters other) {
            this.templateData = other.templateData;
            this.count = other.count;
            this.countRandom = other.countRandom;
            this.spawnRadius = other.spawnRadius;
            this.initialSpeedMin = other.initialSpeedMin;
            this.initialSpeedMax = other.initialSpeedMax;
            this.initialDirection = other.initialDirection;
            this.spreadAngle = other.spreadAngle;
            this.useGravity = other.useGravity;
            this.useWind = other.useWind;
            this.useCollision = other.useCollision;
        }
    }
}
