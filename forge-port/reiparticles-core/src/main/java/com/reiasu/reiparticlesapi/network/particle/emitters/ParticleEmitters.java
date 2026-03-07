// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

import com.reiasu.reiparticlesapi.network.particle.ServerController;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for all particle emitters in the ReiParticles system.
 * <p>
 * Subclasses define custom particle effects by overriding {@link #emitTick()}
 * (called each server/client tick while alive) and optionally
 * {@link #writePayload(FriendlyByteBuf)} / {@link #readPayload(FriendlyByteBuf)}
 * for network synchronization of custom parameters.
 * <p>
 * Every emitter subclass registered via {@code @ReiAutoRegister} must declare:
 * <pre>{@code
 * public static final ResourceLocation CODEC_ID =
 *         new ResourceLocation("mymod", "my_emitter");
 *
 * public static MyEmitter decode(FriendlyByteBuf buf) {
 *     MyEmitter e = new MyEmitter();
 *     e.decodeFromBuffer(buf);
 *     return e;
 * }
 * }</pre>
 *
 * @see ParticleEmittersManager#spawnEmitters(Object, ServerLevel, double, double, double)
 * @see AutoParticleEmitters
 */
public abstract class ParticleEmitters implements ServerController<ParticleEmitters> {
    private ResourceLocation emittersID;
    private UUID uuid = UUID.randomUUID();
    private boolean canceled;
    private int maxTick = 1;
    private int tick;
    private final List<Runnable> tickHandlers = new ArrayList<>();
    private Level level;
    private Vec3 position = Vec3.ZERO;
    private double visibleRange = 256.0;
    private int throttleInterval = 1;

    @Override
    public void spawnInWorld(ServerLevel world, Vec3 pos) {
        ParticleEmittersManager.spawnEmitters(this, world, pos.x, pos.y, pos.z);
    }

    public ResourceLocation getEmittersID() {
        return emittersID;
    }

    public void setEmittersID(ResourceLocation emittersID) {
        this.emittersID = emittersID;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public int getMaxTick() {
        return maxTick;
    }

    public void setMaxTick(int maxTick) {
        this.maxTick = maxTick;
    }

    public int getTick() {
        return tick;
    }

    public void setTick(int tick) {
        this.tick = Math.max(0, tick);
    }

    /**
     * Binds this emitter to a world and position. Called automatically by
     * {@link ParticleEmittersManager#spawnEmitters}.
     */
    public ParticleEmitters bind(Level level, double x, double y, double z) {
        this.level = level;
        this.position = new Vec3(x, y, z);
        return this;
    }

    public Level level() {
        return level;
    }

    public Vec3 position() {
        return position;
    }

    public void teleportTo(Vec3 pos) {
        this.position = pos;
    }

    public ParticleEmitters addTickHandler(Runnable handler) {
        if (handler != null) {
            tickHandlers.add(handler);
        }
        return this;
    }

    public void update(ParticleEmitters emitter) {
        this.canceled = emitter.canceled;
        this.maxTick = emitter.maxTick;
        this.tick = emitter.tick;
        this.level = emitter.level;
        this.position = emitter.position;
    }

    /** Override to write custom emitter parameters for network sync. */
    protected void writePayload(FriendlyByteBuf buf) {
    }

    /** Override to read custom emitter parameters from network sync. */
    protected void readPayload(FriendlyByteBuf buf) {
    }

    /** Serializes the full emitter state (header + payload) into a byte array for network transmission. */
    public byte[] encodeToBytes() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUUID(uuid);
        buf.writeInt(maxTick);
        buf.writeInt(tick);
        buf.writeBoolean(canceled);
        buf.writeDouble(position.x);
        buf.writeDouble(position.y);
        buf.writeDouble(position.z);
        writePayload(buf);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    /** Deserializes emitter state from a network buffer. Typically called inside {@code decode()}. */
    public void decodeFromBuffer(FriendlyByteBuf buf) {
        this.uuid = buf.readUUID();
        this.maxTick = buf.readInt();
        this.tick = buf.readInt();
        this.canceled = buf.readBoolean();
        this.position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        readPayload(buf);
    }

    /** Called each tick while the emitter is alive. Override to spawn particles or update state. */
    protected void emitTick() {
    }

    public double getVisibleRange() {
        return visibleRange;
    }

    public void setVisibleRange(double range) {
        this.visibleRange = Math.max(1.0, range);
    }

    public int getThrottleInterval() {
        return throttleInterval;
    }

    /**
     * Set the tick interval for emission when players are beyond half of visibleRange.
     * 1 = every tick (default), 2 = every other tick, etc.
     */
    public void setThrottleInterval(int interval) {
        this.throttleInterval = Math.max(1, interval);
    }

    @Override
    public void tick() {
        if (canceled) {
            return;
        }
        // Distance-based throttling: skip emission when no player is in range
        if (level instanceof ServerLevel serverLevel && position != null) {
            double nearest = nearestPlayerDist(serverLevel);
            if (nearest > visibleRange) {
                tick++;
                if (maxTick > 0 && tick >= maxTick) canceled = true;
                return;
            }
            // Throttle when players are far (beyond half range)
            if (throttleInterval > 1 && nearest > visibleRange * 0.5 && (tick % throttleInterval) != 0) {
                tick++;
                if (maxTick > 0 && tick >= maxTick) canceled = true;
                return;
            }
        }
        emitTick();
        for (Runnable tickHandler : tickHandlers) {
            tickHandler.run();
        }
        tick++;
        if (maxTick > 0 && tick >= maxTick) {
            canceled = true;
        }
    }

    private double nearestPlayerDist(ServerLevel serverLevel) {
        double min = Double.MAX_VALUE;
        for (ServerPlayer p : serverLevel.players()) {
            if (p.isRemoved() || p.isSpectator()) continue;
            double d = p.position().distanceTo(position);
            if (d < min) min = d;
        }
        return min;
    }

    @Override
    public boolean getCanceled() {
        return canceled;
    }

    @Override
    public void cancel() {
        canceled = true;
    }
}

