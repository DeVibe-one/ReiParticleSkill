// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.renderer;

import com.reiasu.reiparticlesapi.network.particle.ServerController;
import com.reiasu.reiparticlesapi.renderer.server.ServerRenderEntityManager;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Base class for custom render entities that are managed server-side
 * and rendered client-side with custom shaders/models.
 */
public abstract class RenderEntity implements ServerController<RenderEntity> {

    private Level world;
    private Vec3 pos;
    private double renderRange;
    private boolean init;
    private boolean alwaysToggle;
    private boolean syncOnce;
    private Vec3 lastRenderPos;
    private int age;
    private UUID uuid;
    private boolean dirty;
    private boolean canceled;

    public RenderEntity(Level world, Vec3 pos) {
        this.world = world;
        this.pos = pos;
        this.uuid = UUID.randomUUID();
        this.renderRange = 64.0;
        this.lastRenderPos = Vec3.ZERO;
    }

    public RenderEntity() {
        this(null, Vec3.ZERO);
    }

    @Override
    public void spawnInWorld(ServerLevel world, Vec3 pos) {
        setWorld(world);
        setPos(pos);
        ServerRenderEntityManager.INSTANCE.spawn(this);
    }

    // ---- Abstract methods ----

    public abstract void clientTick();
    public abstract void serverTick();
    public abstract ResourceLocation getRenderID();

    // ---- Lifecycle ----

    @Override
    public void tick() {
        age++;
        if (world != null && world.isClientSide) {
            clientTick();
        } else {
            serverTick();
        }
    }

    public void spawn() {
        if (world != null && !world.isClientSide) {
            ServerRenderEntityManager.INSTANCE.spawn(this);
        }
    }

    @Override
    public void cancel() {
        canceled = true;
    }

    public void remove() {
        canceled = true;
        ServerRenderEntityManager.INSTANCE.remove(this);
    }

    public void release() {
        remove();
    }

    @Override
    public void teleportTo(Vec3 pos) {
        this.pos = pos;
        markDirty();
    }

    public void teleportTo(double x, double y, double z) {
        teleportTo(new Vec3(x, y, z));
    }

    public void rotateToPoint(RelativeLocation to) {
        markDirty();
    }

    public void rotateToWithAngle(RelativeLocation to, double radian) {
        markDirty();
    }

    public void rotateAsAxis(double radian) {
        markDirty();
    }

    public void setPosition(Vec3 pos) {
        this.pos = pos;
        markDirty();
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void clearDirty() {
        this.dirty = false;
    }

    public boolean shouldSync() {
        return dirty;
    }

    public void loadProfileFromEntity(RenderEntity another) {
        this.world = another.world;
        this.pos = another.pos;
        this.renderRange = another.renderRange;
        this.uuid = another.uuid;
        this.age = another.age;
    }

    public RenderEntity getValue() {
        return this;
    }

    // ---- Getters / Setters ----

    public Level getWorld() { return world; }
    public void setWorld(Level world) { this.world = world; }
    public Vec3 getPos() { return pos; }
    public void setPos(Vec3 pos) { this.pos = pos; }
    public double getRenderRange() { return renderRange; }
    public void setRenderRange(double renderRange) { this.renderRange = renderRange; }
    public boolean getClient() { return world != null && world.isClientSide; }
    public boolean getInit() { return init; }
    public void setInit(boolean init) { this.init = init; }
    public boolean getAlwaysToggle() { return alwaysToggle; }
    public void setAlwaysToggle(boolean alwaysToggle) { this.alwaysToggle = alwaysToggle; }
    public Vec3 getLastRenderPos() { return lastRenderPos; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    public boolean getDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }
    @Override public boolean getCanceled() { return canceled; }
    public void setCanceled(boolean canceled) { this.canceled = canceled; }
}

