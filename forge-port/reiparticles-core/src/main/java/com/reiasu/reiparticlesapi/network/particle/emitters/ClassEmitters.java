// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

import com.mojang.logging.LogUtils;
import com.reiasu.reiparticlesapi.network.particle.emitters.environment.wind.GlobalWindDirection;
import com.reiasu.reiparticlesapi.network.particle.emitters.environment.wind.WindDirection;
import com.reiasu.reiparticlesapi.network.particle.emitters.environment.wind.WindDirections;
import com.reiasu.reiparticlesapi.network.particle.emitters.event.ParticleEventHandler;
import com.reiasu.reiparticlesapi.network.particle.emitters.event.ParticleEventHandlerManager;
import com.reiasu.reiparticlesapi.network.particle.data.SerializableData;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.interpolator.Interpolator;
import com.reiasu.reiparticlesapi.utils.interpolator.emitters.LineEmitterInterpolator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract emitter class that adds physics simulation, wind, event handlers,
 * interpolation, and particle spawning on top of ParticleEmitters.
 * Server-side port of the Fabric ClassEmitters.
 */
public abstract class ClassEmitters extends ParticleEmitters {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Companion Companion = new Companion();

    private int delay;
    private boolean playing;
    private double airDensity = PhysicConstant.SEA_AIR_DENSITY;
    private double gravity = PhysicConstant.EARTH_GRAVITY;
    private double mass = 1.0;

    private final ConcurrentHashMap<String, SortedMap<ParticleEventHandler, Boolean>> handlerList =
            new ConcurrentHashMap<>();

    private boolean enableInterpolator;
    private Interpolator emittersInterpolator = new LineEmitterInterpolator().setRefiner(5.0);
    private WindDirection wind = new GlobalWindDirection(Vec3.ZERO);

    public ClassEmitters(Vec3 pos, Level world) {
        if (pos != null && world != null) {
            bind(world, pos.x, pos.y, pos.z);
        }
    }

    // ---- Event handlers ----

    public void addEventHandler(ParticleEventHandler handler, boolean innerClass) {
        if (handler == null) return;
        String id = handler.getHandlerID();
        handlerList.computeIfAbsent(id, k -> Collections.synchronizedSortedMap(new TreeMap<>()));
        handlerList.get(id).put(handler, innerClass);
    }

    public void addEventHandlerList(List<ParticleEventHandler> list) {
        if (list == null) return;
        for (ParticleEventHandler handler : list) {
            addEventHandler(handler, false);
        }
    }

    public List<ParticleEventHandler> collectEventHandles() {
        List<ParticleEventHandler> result = new ArrayList<>();
        for (SortedMap<ParticleEventHandler, Boolean> map : handlerList.values()) {
            result.addAll(map.keySet());
        }
        return result;
    }

    public ConcurrentHashMap<String, SortedMap<ParticleEventHandler, Boolean>> getHandlerList() {
        return handlerList;
    }

    // ---- Physics properties ----

    public double getAirDensity() {
        return airDensity;
    }

    public void setAirDensity(double airDensity) {
        this.airDensity = airDensity;
    }

    public double getGravity() {
        return gravity;
    }

    public void setGravity(double gravity) {
        this.gravity = gravity;
    }

    public double getMass() {
        return mass;
    }

    public void setMass(double mass) {
        this.mass = mass;
    }

    // ---- Wind ----

    public WindDirection getWind() {
        return wind;
    }

    public void setWind(WindDirection wind) {
        if (wind != null) {
            this.wind = wind;
        }
    }

    // ---- Interpolator ----

    public boolean getEnableInterpolator() {
        return enableInterpolator;
    }

    public void setEnableInterpolator(boolean enable) {
        this.enableInterpolator = enable;
    }

    public Interpolator getEmittersInterpolator() {
        return emittersInterpolator;
    }

    public void setEmittersInterpolator(Interpolator interpolator) {
        if (interpolator != null) {
            this.emittersInterpolator = interpolator;
        }
    }

    // ---- Delay / Playing ----

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public boolean getPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    // ---- Lifecycle ----

    public void start() {
        playing = true;
    }

    public void stop() {
        playing = false;
    }

    public void spawn(Level world, Vec3 pos) {
        if (world == null || pos == null) return;
        bind(world, pos.x, pos.y, pos.z);
    }

    // ---- Abstract methods ----

    /**
     * Called each tick when the emitter is playing and past its delay.
     */
    public abstract void doTick();

    /**
     * Per-particle action called during spawning.
     */
    public abstract void singleParticleAction(
            Controllable<?> controller, SerializableData data,
            RelativeLocation spawnPos, Level spawnWorld,
            float particleLerpProgress, float posLerpProgress);

    /**
     * Generate particle data list for the current tick.
     */
    public List<Map.Entry<SerializableData, RelativeLocation>> genParticles(float lerpProgress) {
        return Collections.emptyList();
    }

    // ---- Physics ----

    /**
     * Update physics for a single particle: apply gravity, wind, and drag.
     */
    public void updatePhysics(Vec3 pos, ControllableParticleData data, ControllableParticle particle) {
        if (data == null || particle == null) return;

        Vec3 velocity = data.getVelocity();
        if (velocity == null) velocity = Vec3.ZERO;

        // Apply gravity
        double gy = -gravity * 0.05;
        velocity = velocity.add(0.0, gy, 0.0);

        // Apply wind via WindDirections helper
        if (wind != null && wind.inRange(pos)) {
            Vec3 windAccel = WindDirections.INSTANCE.handleWindForce(
                    wind, pos, airDensity,
                    PhysicConstant.DRAG_COEFFICIENT,
                    PhysicConstant.CROSS_SECTIONAL_AREA, velocity);
            velocity = velocity.add(windAccel);
        }

        data.setVelocity(velocity);
    }

    // ---- Tick override ----

    @Override
    protected void emitTick() {
        if (!playing) return;
        int currentTick = getTick();
        if (currentTick < delay) return;
        doTick();
    }

    /**
     * Spawn a particle at the given position with lerp progress.
     */
    public void spawnParticle(Vec3 spawnPos, float lerpProgress) {
        // Server-side: override in subclasses for actual spawning
    }

    // ---- Encode/Decode ----

    public static final class Companion {
        private Companion() {
        }

        public void encodeBase(ClassEmitters data, FriendlyByteBuf buf) {
            if (data == null || buf == null) return;

            List<ParticleEventHandler> handles = data.collectEventHandles();
            buf.writeInt(handles.size());
            for (ParticleEventHandler handler : handles) {
                buf.writeUtf(handler.getHandlerID());
            }

            buf.writeDouble(data.position().x);
            buf.writeDouble(data.position().y);
            buf.writeDouble(data.position().z);
            buf.writeInt(data.getTick());
            buf.writeInt(data.getMaxTick());
            buf.writeInt(data.getDelay());
            buf.writeUUID(data.getUuid());
            buf.writeBoolean(data.getCanceled());
            buf.writeBoolean(data.getPlaying());
            buf.writeDouble(data.getGravity());
            buf.writeDouble(data.getAirDensity());
            buf.writeDouble(data.getMass());
            buf.writeBoolean(data.getEnableInterpolator());
            buf.writeDouble(data.getEmittersInterpolator().getRefinerCount());
            buf.writeUtf(data.getWind().getID());
        }

        public void decodeBase(ClassEmitters container, FriendlyByteBuf buf) {
            if (container == null || buf == null) return;

            int handlerCount = buf.readInt();
            List<ParticleEventHandler> handlerListDecoded = new ArrayList<>();
            for (int i = 0; i < handlerCount; i++) {
                String handleID = buf.readUtf();
                ParticleEventHandler handler = ParticleEventHandlerManager.INSTANCE.getHandlerById(handleID);
                if (handler != null) {
                    handlerListDecoded.add(handler);
                }
            }
            container.addEventHandlerList(handlerListDecoded);

            Vec3 pos = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            int tick = buf.readInt();
            int maxTick = buf.readInt();
            int delay = buf.readInt();
            UUID uuid = buf.readUUID();
            boolean canceled = buf.readBoolean();
            boolean playing = buf.readBoolean();
            double gravity = buf.readDouble();
            double airDensity = buf.readDouble();
            double mass = buf.readDouble();
            boolean enableInterpolator = buf.readBoolean();
            double interpolatorCount = buf.readDouble();
            String windId = buf.readUtf();

            container.bind(container.level(), pos.x, pos.y, pos.z);
            container.setTick(tick);
            container.setMaxTick(maxTick);
            container.setDelay(delay);
            container.setUuid(uuid);
            if (canceled) container.cancel();
            container.setPlaying(playing);
            container.setGravity(gravity);
            container.setAirDensity(airDensity);
            container.setMass(mass);
            container.setEnableInterpolator(enableInterpolator);
            container.getEmittersInterpolator().setRefiner(interpolatorCount);

            try {
                WindDirection decodedWind = WindDirections.INSTANCE.getFactoryFromID(windId).get();
                container.setWind(decodedWind);
            } catch (Exception e) {
                LOGGER.debug("Unknown wind ID '{}': {}", windId, e.getMessage());
            }
        }
    }
}
