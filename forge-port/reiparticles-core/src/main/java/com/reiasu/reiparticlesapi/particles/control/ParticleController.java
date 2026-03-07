// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles.control;

import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Controller associated with a single {@link ControllableParticle}.
 * <p>
 * Implements {@link Controllable} so it can participate in composition/helper
 * systems. Pre-tick actions are stored and replayed every tick.
 */
public final class ParticleController implements Controllable<ControllableParticle> {

    private final UUID uuid;
    private ControllableParticle particle;
    private boolean init;
    private final List<Consumer<ControllableParticle>> invokeQueue = new ArrayList<>();
    private final ConcurrentHashMap<String, Object> bufferedData = new ConcurrentHashMap<>();
    private Consumer<ControllableParticle> initInvoker;

    public ParticleController(UUID uuid) {
        this.uuid = uuid;
    }

    public ControllableParticle getParticle() {
        if (particle == null) {
            throw new IllegalStateException("Particle not loaded yet");
        }
        return particle;
    }

    public ConcurrentHashMap<String, Object> getBufferedData() {
        return bufferedData;
    }

    public Consumer<ControllableParticle> getInitInvoker() {
        return initInvoker;
    }

    public void setInitInvoker(Consumer<ControllableParticle> initInvoker) {
        this.initInvoker = initInvoker;
    }

    /**
     * Add an action to run every tick before the particle ticks.
     */
    public ParticleController addPreTickAction(Consumer<ControllableParticle> action) {
        invokeQueue.add(action);
        return this;
    }

    /**
     * Immediately invoke an action on the particle.
     */
    public ParticleController controlAction(Consumer<ControllableParticle> action) {
        action.accept(getParticle());
        return this;
    }

    /**
     * Load the particle reference. Only the first call has effect.
     */
    public void loadParticle(ControllableParticle particle) {
        if (this.particle != null) {
            return;
        }
        if (!particle.getControlUUID().equals(this.uuid)) {
            throw new IllegalArgumentException("Particle uuid invalid");
        }
        this.particle = particle;
    }

    /**
     * Run the init invoker if not already done.
     */
    public void particleInit() {
        if (init) {
            return;
        }
        if (initInvoker == null) {
            initInvoker = p -> {};
        }
        initInvoker.accept(getParticle());
        init = true;
    }

    /**
     * Run all pre-tick actions and check for death.
     */
    public void doTick() {
        for (Consumer<ControllableParticle> action : invokeQueue) {
            action.accept(getParticle());
        }
        if (getParticle().getDeath()) {
            ControlParticleManager.INSTANCE.removeControl(this.uuid);
        }
    }

    public void rotateParticleTo(RelativeLocation target) {
        rotateParticleTo(new Vector3f((float) target.getX(), (float) target.getY(), (float) target.getZ()));
    }

    public void rotateParticleTo(Vec3 target) {
        rotateParticleTo(target.toVector3f());
    }

    public void rotateParticleTo(Vector3f target) {
        getParticle().rotateParticleTo(target);
    }

    // ---- Controllable interface ----

    @Override
    public UUID controlUUID() {
        return uuid;
    }

    @Override
    public void rotateToPoint(RelativeLocation to) {
        // No-op in particle controller context
    }

    @Override
    public void rotateToWithAngle(RelativeLocation to, double angle) {
        // No-op in particle controller context
    }

    @Override
    public void rotateAsAxis(double angle) {
        // No-op in particle controller context
    }

    @Override
    public void teleportTo(Vec3 pos) {
        getParticle().teleportTo(pos);
    }

    @Override
    public void teleportTo(double x, double y, double z) {
        getParticle().teleportTo(x, y, z);
    }

    @Override
    public void remove() {
        getParticle().remove();
    }

    @Override
    public ControllableParticle getControlObject() {
        return getParticle();
    }
}
