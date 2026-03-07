// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

import com.reiasu.reiparticlesapi.network.particle.data.SerializableData;
import com.reiasu.reiparticlesapi.particles.ParticleDisplayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Mutable particle data that commands operate on.
 * <p>
 * Fields are aligned with the original Fabric {@code ControllableParticleData}:
 * velocity, uuid, age/maxAge, size, alpha, color, speed, speedLimit, rotation,
 * plus the existing emitter-range fields (minCount/maxCount/minAge/maxAge/minSize/maxSize/minSpeed/maxSpeed).
 */
public class ControllableParticleData implements SerializableData {

    // ---- Emitter-range fields (existing) ----
    private int color;
    private int minCount;
    private int maxCount;
    private int minAge;
    private int maxAge;
    private double minSize;
    private double maxSize;
    private double minSpeed;
    private double maxSpeed;

    // ---- Per-particle runtime fields (aligned with Fabric original) ----
    private UUID uuid = UUID.randomUUID();
    private Vec3 velocity = Vec3.ZERO;
    private Vec3 position = Vec3.ZERO;
    private float size;
    private float alpha = 1.0f;
    private int age;
    private int particleMaxAge = 120;
    private double speed = 1.0;
    private double speedLimit = 32.0;
    private Vector3f particleColor = new Vector3f(1.0f, 1.0f, 1.0f);
    private float yaw;
    private float pitch;
    private float roll;
    private boolean faceToCamera = true;
    private int light = 15;
    private float visibleRange = 128.0f;

    // ---- Emitter-range getters/setters ----

    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public int getMinCount() { return minCount; }
    public void setMinCount(int minCount) { this.minCount = minCount; }
    public int getMaxCount() { return maxCount; }
    public void setMaxCount(int maxCount) { this.maxCount = maxCount; }
    public int getMinAge() { return minAge; }
    public void setMinAge(int minAge) { this.minAge = minAge; }
    public int getMaxAge() { return maxAge; }
    public void setMaxAge(int maxAge) { this.maxAge = maxAge; }
    public double getMinSize() { return minSize; }
    public void setMinSize(double minSize) { this.minSize = minSize; }
    public double getMaxSize() { return maxSize; }
    public void setMaxSize(double maxSize) { this.maxSize = maxSize; }
    public double getMinSpeed() { return minSpeed; }
    public void setMinSpeed(double minSpeed) { this.minSpeed = minSpeed; }
    public double getMaxSpeed() { return maxSpeed; }
    public void setMaxSpeed(double maxSpeed) { this.maxSpeed = maxSpeed; }

    // ---- Per-particle runtime getters/setters ----

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public Vec3 getVelocity() { return velocity; }
    public void setVelocity(Vec3 velocity) { this.velocity = velocity; }

    public Vec3 getPosition() { return position; }
    public void setPosition(Vec3 position) { this.position = position; }

    public float getParticleSize() { return size; }
    public void setParticleSize(float size) { this.size = size; }

    public float getAlpha() { return alpha; }
    public void setAlpha(float alpha) { this.alpha = alpha; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public int getParticleMaxAge() { return particleMaxAge; }
    public void setParticleMaxAge(int particleMaxAge) { this.particleMaxAge = particleMaxAge; }

    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    public double getSpeedLimit() { return speedLimit; }
    public void setSpeedLimit(double speedLimit) { this.speedLimit = speedLimit; }

    public Vector3f getParticleColor() { return particleColor; }
    public void setParticleColor(Vector3f particleColor) { this.particleColor = particleColor; }

    public float getYaw() { return yaw; }
    public void setYaw(float yaw) { this.yaw = yaw; }

    public float getPitch() { return pitch; }
    public void setPitch(float pitch) { this.pitch = pitch; }

    public float getRoll() { return roll; }
    public void setRoll(float roll) { this.roll = roll; }

    public boolean isFaceToCamera() { return faceToCamera; }
    public void setFaceToCamera(boolean faceToCamera) { this.faceToCamera = faceToCamera; }

    public int getLight() { return light; }
    public void setLight(int light) { this.light = light; }

    public float getVisibleRange() { return visibleRange; }
    public void setVisibleRange(float visibleRange) { this.visibleRange = visibleRange; }

    // ---- Clone ----

    @Override
    public ParticleDisplayer createDisplayer() {
        return null; // Client-side only; server port returns null
    }

    @Override
    public ControllableParticleData clone() {
        ControllableParticleData copy = new ControllableParticleData();
        copy.color = this.color;
        copy.minCount = this.minCount;
        copy.maxCount = this.maxCount;
        copy.minAge = this.minAge;
        copy.maxAge = this.maxAge;
        copy.minSize = this.minSize;
        copy.maxSize = this.maxSize;
        copy.minSpeed = this.minSpeed;
        copy.maxSpeed = this.maxSpeed;
        copy.uuid = UUID.randomUUID();
        copy.velocity = this.velocity;
        copy.position = this.position;
        copy.size = this.size;
        copy.alpha = this.alpha;
        copy.age = this.age;
        copy.particleMaxAge = this.particleMaxAge;
        copy.speed = this.speed;
        copy.speedLimit = this.speedLimit;
        copy.particleColor = new Vector3f(this.particleColor);
        copy.yaw = this.yaw;
        copy.pitch = this.pitch;
        copy.roll = this.roll;
        copy.faceToCamera = this.faceToCamera;
        copy.light = this.light;
        copy.visibleRange = this.visibleRange;
        return copy;
    }

    // ---- Serialization ----

    public void writeToBuf(FriendlyByteBuf buf) {
        buf.writeInt(color);
        buf.writeInt(minCount);
        buf.writeInt(maxCount);
        buf.writeInt(minAge);
        buf.writeInt(maxAge);
        buf.writeDouble(minSize);
        buf.writeDouble(maxSize);
        buf.writeDouble(minSpeed);
        buf.writeDouble(maxSpeed);
        buf.writeUUID(uuid);
        buf.writeDouble(velocity.x);
        buf.writeDouble(velocity.y);
        buf.writeDouble(velocity.z);
        buf.writeFloat(size);
        buf.writeFloat(alpha);
        buf.writeInt(age);
        buf.writeInt(particleMaxAge);
        buf.writeDouble(speed);
        buf.writeDouble(speedLimit);
        buf.writeFloat(particleColor.x());
        buf.writeFloat(particleColor.y());
        buf.writeFloat(particleColor.z());
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeFloat(roll);
        buf.writeBoolean(faceToCamera);
        buf.writeInt(light);
        buf.writeFloat(visibleRange);
    }

    public void readFromBuf(FriendlyByteBuf buf) {
        color = buf.readInt();
        minCount = buf.readInt();
        maxCount = buf.readInt();
        minAge = buf.readInt();
        maxAge = buf.readInt();
        minSize = buf.readDouble();
        maxSize = buf.readDouble();
        minSpeed = buf.readDouble();
        maxSpeed = buf.readDouble();
        uuid = buf.readUUID();
        velocity = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        size = buf.readFloat();
        alpha = buf.readFloat();
        age = buf.readInt();
        particleMaxAge = buf.readInt();
        speed = buf.readDouble();
        speedLimit = buf.readDouble();
        particleColor = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        yaw = buf.readFloat();
        pitch = buf.readFloat();
        roll = buf.readFloat();
        faceToCamera = buf.readBoolean();
        light = buf.readInt();
        visibleRange = buf.readFloat();
    }
}

