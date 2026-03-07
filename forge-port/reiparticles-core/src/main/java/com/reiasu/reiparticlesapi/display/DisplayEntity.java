// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.display;

import com.reiasu.reiparticlesapi.network.particle.ServerController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Base class for display entities (non-particle visual elements managed
 * by the ReiParticles system).
 * <p>
 * Carries position, rotation (yaw/pitch/roll), scale, and validity state.
 * Provides static {@link #encodeBase}/{@link #decodeBase} for network
 * serialization of the base fields.
 */
public abstract class DisplayEntity implements ServerController<DisplayEntity> {

    public static void encodeBase(DisplayEntity data, FriendlyByteBuf buf) {
        Vec3 pos = data.getPos() == null ? Vec3.ZERO : data.getPos();
        buf.writeDouble(pos.x());
        buf.writeDouble(pos.y());
        buf.writeDouble(pos.z());
        buf.writeFloat(data.getYaw());
        buf.writeFloat(data.getPitch());
        buf.writeFloat(data.getRoll());
        buf.writeFloat(data.getScale());
        buf.writeBoolean(data.getValid());
        buf.writeUUID(data.getControlUUID());
    }

    public static void decodeBase(DisplayEntity instance, FriendlyByteBuf buf) {
        instance.setPos(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
        instance.setYaw(buf.readFloat());
        instance.setPitch(buf.readFloat());
        instance.setRoll(buf.readFloat());
        instance.setScale(buf.readFloat());
        instance.setValid(buf.readBoolean());
        instance.setControlUUID(buf.readUUID());
    }

    private UUID controlUUID = UUID.randomUUID();
    private boolean valid = true;
    private Level level;
    private Vec3 pos = Vec3.ZERO;
    private float yaw;
    private float pitch;
    private float roll;
    private float scale = 1.0f;

    @Override
    public void spawnInWorld(ServerLevel world, Vec3 pos) {
        Vec3 safePos = pos == null ? Vec3.ZERO : pos;
        this.pos = safePos;
        setPos(safePos);
        DisplayEntityManager.INSTANCE.spawn(this, world);
    }

    public UUID getControlUUID() {
        return controlUUID;
    }

    public void setControlUUID(UUID controlUUID) {
        this.controlUUID = controlUUID;
    }

    public boolean getValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public Vec3 getPos() {
        return pos;
    }

    public void setPos(Vec3 pos) {
        this.pos = pos == null ? Vec3.ZERO : pos;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public String typeId() {
        return null;
    }

    public byte[] encodeToBytes() {
        return new byte[0];
    }

    public DisplayEntity bindLevel(Level level) {
        this.level = level;
        return this;
    }

    protected Level level() {
        return level;
    }

    public void update(DisplayEntity other) {
        setValid(other.getValid());
        setPos(other.getPos());
        setYaw(other.getYaw());
        setPitch(other.getPitch());
        setRoll(other.getRoll());
        setScale(other.getScale());
    }

    @Override
    public boolean getCanceled() {
        return !valid;
    }

    @Override
    public void cancel() {
        this.valid = false;
    }
}

