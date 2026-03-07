// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle;

import com.reiasu.reiparticlesapi.network.ReiParticlesNetwork;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffers;
import com.reiasu.reiparticlesapi.network.packet.PacketParticleGroupS2C;
import com.reiasu.reiparticlesapi.particles.control.ControlType;
import com.reiasu.reiparticlesapi.particles.control.group.ControllableParticleGroup;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Server-side particle group that manages a set of particles with shared
 * lifecycle, transform, and network synchronization.
 * <p>
 * Subclasses define the particle shape via {@link #getClientType()} and
 * {@link #otherPacketArgs()}, and respond to lifecycle callbacks.
 */
public abstract class ServerParticleGroup implements ServerController<ServerParticleGroup> {

    private double visibleRange;
    private final UUID uuid;
    private Vec3 pos;
    private Level world;
    private boolean valid;
    private boolean canceled;
    private int clientTick;
    private int clientMaxTick;
    private double scale;
    private int tick;
    private int maxTick;
    private RelativeLocation axis;

    public ServerParticleGroup(double visibleRange) {
        this.visibleRange = visibleRange;
        this.uuid = UUID.randomUUID();
        this.pos = Vec3.ZERO;
        this.valid = true;
        this.clientMaxTick = 120;
        this.scale = 1.0;
        this.maxTick = 120;
        this.axis = new RelativeLocation(0.0, 1.0, 0.0);
    }

    public ServerParticleGroup() {
        this(32.0);
    }

    @Override
    public void spawnInWorld(ServerLevel world, Vec3 pos) {
        ServerParticleGroupManager.INSTANCE.addParticleGroup(this, pos, world);
    }

    // ---- Getters / Setters ----

    public double getVisibleRange() { return visibleRange; }
    public void setVisibleRange(double visibleRange) { this.visibleRange = visibleRange; }
    public UUID getUuid() { return uuid; }
    public Vec3 getPos() { return pos; }
    public void setPos(Vec3 pos) { this.pos = pos; }
    public Level getWorld() { return world; }
    public void setWorld(Level world) { this.world = world; }
    public boolean getValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    @Override public boolean getCanceled() { return canceled; }
    public void setCanceled(boolean canceled) { this.canceled = canceled; }
    public int getClientTick() { return clientTick; }
    public void setClientTick(int clientTick) { this.clientTick = clientTick; }
    public int getClientMaxTick() { return clientMaxTick; }
    public void setClientMaxTick(int clientMaxTick) { this.clientMaxTick = clientMaxTick; }
    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = scale; }
    public int getTick() { return tick; }
    public void setTick(int tick) { this.tick = tick; }
    public int getMaxTick() { return maxTick; }
    public void setMaxTick(int maxTick) { this.maxTick = maxTick; }
    public RelativeLocation getAxis() { return axis; }
    public void setAxis(RelativeLocation axis) { this.axis = axis; }

    // ---- Abstract methods ----

    public abstract Map<String, ParticleControllerDataBuffer<?>> otherPacketArgs();
    public abstract Class<? extends ControllableParticleGroup> getClientType();
    public abstract void onGroupDisplay(Vec3 pos, ServerLevel world);
    public abstract void onTickAliveDeath();
    public abstract void onClientViewDeath();
    public abstract void doTickClient();
    public abstract void doTickAlive();

    // ---- Lifecycle ----

    @Override
    public void tick() {
        if (canceled) return;
        doTickAlive();
        tick++;
        if (maxTick > 0 && tick >= maxTick) {
            kill();
        }
    }

    public void doTickAlive(int dummy) {
        // Overload for compatibility
        doTickAlive();
    }

    public void kill() {
        canceled = true;
        valid = false;
        onTickAliveDeath();
        remove();
    }

    public void remove() {
        if (world == null) return;
        Set<UUID> visible = ServerParticleGroupManager.INSTANCE.filterVisiblePlayer(this);
        for (UUID playerId : visible) {
            if (world.getPlayerByUUID(playerId) instanceof ServerPlayer sp) {
                PacketParticleGroupS2C packet = new PacketParticleGroupS2C(
                        uuid, ControlType.REMOVE, new HashMap<>());
                ReiParticlesNetwork.sendTo(sp, packet);
            }
        }
        ServerParticleGroupManager.INSTANCE.removeParticleGroup(this);
    }

    // ---- Player / Entity binding ----

    public void withPlayerStats(Player player) {
        if (player == null) return;
        this.pos = player.position();
    }

    public void withEntityStats(LivingEntity entity) {
        if (entity == null) return;
        this.pos = entity.position();
    }

    // ---- Transform operations ----

    public void setAxis(Vec3 axisVec) {
        this.axis = RelativeLocation.Companion.of(axisVec);
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(PacketParticleGroupS2C.PacketArgsType.AXIS.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.vec3d(axisVec));
        change(g -> g.axis = RelativeLocation.Companion.of(axisVec), args);
    }

    public void setPosOnServer(Vec3 newPos) {
        this.pos = newPos;
    }

    public void setRotateToOnServer(RelativeLocation to) {
        // Server-side only rotation tracking
    }

    public void teleportGroupTo(Vec3 newPos) {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(PacketParticleGroupS2C.PacketArgsType.POS.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.vec3d(newPos));
        change(g -> g.pos = newPos, args);
    }

    @Override
    public void teleportTo(Vec3 pos) {
        teleportGroupTo(pos);
    }

    public void scaleOnServer(double newScale) {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(PacketParticleGroupS2C.PacketArgsType.SCALE.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.doubleValue(newScale));
        change(g -> g.scale = newScale, args);
    }

    public void rotateAsAxis(RelativeLocation axis, double angle) {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(PacketParticleGroupS2C.PacketArgsType.ROTATE_AXIS.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.vec3d(axis.toVector()));
        args.put(PacketParticleGroupS2C.PacketArgsType.ROTATE_TO.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.doubleValue(angle));
        change(g -> {}, args);
    }

    public void rotateToWithAngle(RelativeLocation to, double angle) {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(PacketParticleGroupS2C.PacketArgsType.ROTATE_TO.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.vec3d(to.toVector()));
        change(g -> {}, args);
    }

    public void rotateToPoint(RelativeLocation to) {
        rotateParticlesToPoint(to.toVector());
    }

    public void rotateParticlesToPoint(Vec3 to) {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(PacketParticleGroupS2C.PacketArgsType.ROTATE_TO.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.vec3d(to));
        change(g -> {}, args);
    }

    public void changeTick(int newTick) {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(PacketParticleGroupS2C.PacketArgsType.CURRENT_TICK.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.intValue(newTick));
        change(g -> g.clientTick = newTick, args);
    }

    public void changeMaxTick(int newMaxTick) {
        Map<String, ParticleControllerDataBuffer<?>> args = new HashMap<>();
        args.put(PacketParticleGroupS2C.PacketArgsType.MAX_TICK.getOfArgs(),
                ParticleControllerDataBuffers.INSTANCE.intValue(newMaxTick));
        change(g -> g.maxTick = newMaxTick, args);
    }

    // ---- Spawn ----

    public void spawn(Level world, Vec3 pos) {
        if (!(world instanceof ServerLevel serverLevel)) return;
        ServerParticleGroupManager.INSTANCE.addParticleGroup(this, pos, serverLevel);
    }

    // ---- Change broadcast ----

    public void change(Consumer<ServerParticleGroup> toggleMethod, Map<String, ParticleControllerDataBuffer<?>> args) {
        if (world == null) return;
        Set<UUID> visible = ServerParticleGroupManager.INSTANCE.filterVisiblePlayer(this);
        toggleMethod.accept(this);
        for (UUID playerId : visible) {
            if (world.getPlayerByUUID(playerId) instanceof ServerPlayer sp) {
                PacketParticleGroupS2C packet = new PacketParticleGroupS2C(uuid, ControlType.CHANGE, args);
                ReiParticlesNetwork.sendTo(sp, packet);
            }
        }
    }

    // ---- Init ----

    public void initServerGroup(Vec3 pos, Level world, int maxTick) {
        this.pos = pos;
        this.world = world;
        this.clientMaxTick = maxTick;
    }

    public void initServerGroup(Vec3 pos, Level world) {
        initServerGroup(pos, world, 120);
    }

    @Override
    public void cancel() {
        kill();
    }

    public ServerParticleGroup getValue() {
        return this;
    }
}
