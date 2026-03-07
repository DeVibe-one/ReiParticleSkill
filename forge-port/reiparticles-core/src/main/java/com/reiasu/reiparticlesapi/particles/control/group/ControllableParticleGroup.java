// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles.control.group;

import com.reiasu.reiparticlesapi.network.particle.style.ParticleGroupStyle;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import com.reiasu.reiparticlesapi.particles.ParticleDisplayer;
import com.reiasu.reiparticlesapi.particles.control.ControlParticleManager;
import com.reiasu.reiparticlesapi.particles.control.ParticleController;
import com.reiasu.reiparticlesapi.utils.Math3DUtil;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base class for client-side controllable particle groups.
 * <p>
 * A group manages a collection of {@link Controllable} objects arranged at relative
 * locations around an origin point. Supports scaling, rotation, and tick-based lifecycle.
 *
 * @deprecated Use {@link ParticleGroupStyle} instead.
 */
@Deprecated
public abstract class ControllableParticleGroup implements Controllable<ControllableParticleGroup> {

    private final UUID uuid;
    private final ConcurrentHashMap<UUID, Controllable<?>> particles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Controllable<?>, RelativeLocation> particlesLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Double> particlesDefaultScaleLengths = new ConcurrentHashMap<>();
    private final List<Consumer<ControllableParticleGroup>> invokeQueue = new ArrayList<>();

    private int tick;
    private int maxTick = 120;
    private boolean withTickDeath;
    private boolean valid = true;
    private boolean canceled;
    private Vec3 origin = Vec3.ZERO;
    private ClientLevel world;
    private double scale = 1.0;
    private RelativeLocation axis = new RelativeLocation(0.0, 1.0, 0.0);
    private boolean displayed;

    public ControllableParticleGroup(UUID uuid) {
        this.uuid = uuid;
    }

    // ---- Getters/Setters ----

    public UUID getUuid() { return uuid; }

    public ConcurrentHashMap<UUID, Controllable<?>> getParticles() { return particles; }

    public ConcurrentHashMap<Controllable<?>, RelativeLocation> getParticlesLocations() { return particlesLocations; }

    public ConcurrentHashMap<UUID, Double> getParticlesDefaultScaleLengths() { return particlesDefaultScaleLengths; }

    public int getTick() { return tick; }
    public void setTick(int tick) { this.tick = tick; }

    public int getMaxTick() { return maxTick; }
    public void setMaxTick(int maxTick) { this.maxTick = maxTick; }

    public boolean getWithTickDeath() { return withTickDeath; }
    public void setWithTickDeath(boolean v) { this.withTickDeath = v; }

    public boolean getValid() { return valid; }
    public void setValid(boolean v) { this.valid = v; }

    public boolean getCanceled() { return canceled; }
    public void setCanceled(boolean v) { this.canceled = v; }

    public Vec3 getOrigin() { return origin; }
    public void setOrigin(Vec3 origin) { this.origin = origin; }

    public ClientLevel getWorld() { return world; }
    public void setWorld(ClientLevel world) { this.world = world; }

    public double getScale() { return scale; }
    protected void setScale(double scale) { this.scale = scale; }

    public RelativeLocation getAxis() { return axis; }
    public void setAxis(RelativeLocation axis) { this.axis = axis; }

    public boolean getDisplayed() { return displayed; }
    public void setDisplayed(boolean v) { this.displayed = v; }

    // ---- Abstract methods ----

    /**
     * Define the particle layout: mapping from particle data to relative location.
     */
    public abstract Map<ParticleRelativeData, RelativeLocation> loadParticleLocations();

    /**
     * Called after display is complete.
     */
    public abstract void onGroupDisplay();

    /**
     * Called before particles are spawned. Can modify the locations map.
     */
    public void beforeDisplay(Map<ParticleRelativeData, RelativeLocation> locations) {
        // Default no-op
    }

    // ---- Factory helper ----

    /**
     * Create a {@link ParticleRelativeData} for use in {@link #loadParticleLocations()}.
     */
    public ParticleRelativeData withEffect(
            Function<UUID, ParticleDisplayer> effect,
            Consumer<ControllableParticle> invoker
    ) {
        return new ParticleRelativeData(effect, invoker);
    }

    // ---- Particle lifecycle ----

    public void clearParticles() {
        for (Controllable<?> c : particles.values()) {
            c.remove();
        }
        particles.clear();
        particlesLocations.clear();
        particlesDefaultScaleLengths.clear();
        valid = false;
    }

    public void flush() {
        if (canceled || !valid || !displayed) {
            return;
        }
        remove();
        valid = true;
        axis = new RelativeLocation(0.0, 1.0, 0.0);
        displayParticles(origin, world);
    }

    public void flushRelativeLocations() {
        for (Map.Entry<Controllable<?>, RelativeLocation> entry : particlesLocations.entrySet()) {
            Controllable<?> c = entry.getKey();
            RelativeLocation rl = entry.getValue();
            c.teleportTo(rl.getX() + origin.x, rl.getY() + origin.y, rl.getZ() + origin.z);
        }
    }

    /**
     * Called every client tick by {@link ClientParticleGroupManager}.
     */
    public void tick() {
        if (!valid || canceled) {
            clearParticles();
            return;
        }
        if (withTickDeath) {
            if (tick++ >= maxTick) {
                valid = false;
                return;
            }
        }

        // Run pre-tick actions
        for (Consumer<ControllableParticleGroup> action : invokeQueue) {
            action.accept(this);
        }

        // Tick child groups and styles
        for (Map.Entry<UUID, Controllable<?>> entry : particles.entrySet()) {
            Controllable<?> v = entry.getValue();
            if (v instanceof ControllableParticleGroup) {
                ((ControllableParticleGroup) v).tick();
            } else if (v instanceof ParticleGroupStyle) {
                ((ParticleGroupStyle) v).tick();
            }
        }
    }

    public void display(Vec3 pos, ClientLevel world) {
        if (displayed) {
            return;
        }
        this.origin = pos;
        this.world = world;
        this.displayed = true;
        displayParticles(pos, world);
        onGroupDisplay();
    }

    public void scale(double d) {
        this.scale = Math.max(d, 0.01);
        if (displayed) {
            toggleScaleDisplayed();
        }
    }

    // ---- Rotation ----

    @Override
    public void rotateToPoint(RelativeLocation to) {
        if (!displayed) return;
        List<RelativeLocation> locs = new ArrayList<>(particlesLocations.values());
        Math3DUtil.rotatePointsToPoint(locs, to, axis);
        teleportAllToRelative();
        axis = to.normalize();
    }

    @Override
    public void rotateToWithAngle(RelativeLocation to, double angle) {
        if (!displayed) return;
        List<RelativeLocation> locs = new ArrayList<>(particlesLocations.values());
        Math3DUtil.rotatePointsToPoint(locs, to, axis);
        List<RelativeLocation> locs2 = new ArrayList<>(particlesLocations.values());
        Math3DUtil.rotateAsAxis(locs2, to.normalize(), angle);
        teleportAllToRelative();
        axis = to.normalize();
    }

    @Override
    public void rotateAsAxis(double angle) {
        if (!displayed) return;
        List<RelativeLocation> locs = new ArrayList<>(particlesLocations.values());
        Math3DUtil.rotateAsAxis(locs, axis, angle);
        teleportAllToRelative();
    }

    // ---- Teleport ----

    @Deprecated
    public void teleportGroupTo(Vec3 pos) {
        this.origin = pos;
        teleportAllToRelative();
    }

    @Override
    public void teleportTo(Vec3 pos) {
        teleportGroupTo(pos);
    }

    @Override
    public void teleportTo(double x, double y, double z) {
        teleportGroupTo(new Vec3(x, y, z));
    }

    @Override
    public void remove() {
        clearParticles();
    }

    @Override
    public ControllableParticleGroup getControlObject() {
        return this;
    }

    @Override
    public UUID controlUUID() {
        return uuid;
    }

    // ---- Pre-tick actions ----

    protected ControllableParticleGroup addPreTickAction(Consumer<ControllableParticleGroup> action) {
        invokeQueue.add(action);
        return this;
    }

    // ---- Pre-rotate helpers ----

    public void preRotateTo(Map<ParticleRelativeData, RelativeLocation> map, RelativeLocation to) {
        Math3DUtil.rotatePointsToPoint(new ArrayList<>(map.values()), to, axis);
        axis = to;
    }

    public void preRotateAsAxis(Map<ParticleRelativeData, RelativeLocation> map, RelativeLocation axisParam, double angle) {
        Math3DUtil.rotateAsAxis(new ArrayList<>(map.values()), axisParam, angle);
        axis = axisParam;
    }

    public void preRotateAsAxis(Map<ParticleRelativeData, RelativeLocation> map, double angle) {
        Math3DUtil.rotateAsAxis(new ArrayList<>(map.values()), axis, angle);
    }

    // ---- Internal ----

    protected void toggleScaleDisplayed() {
        if (scale == 1.0) return;
        for (Map.Entry<Controllable<?>, RelativeLocation> entry : particlesLocations.entrySet()) {
            UUID puuid = entry.getKey().controlUUID();
            Double defLen = particlesDefaultScaleLengths.get(puuid);
            if (defLen == null) continue;
            RelativeLocation rl = entry.getValue();
            double currentLen = rl.length();
            if (currentLen > 0.001) {
                rl.multiply(defLen * scale / currentLen);
            }
        }
    }

    private void teleportAllToRelative() {
        for (Map.Entry<Controllable<?>, RelativeLocation> entry : particlesLocations.entrySet()) {
            Controllable<?> c = entry.getKey();
            RelativeLocation rl = entry.getValue();
            c.teleportTo(rl.getX() + origin.x, rl.getY() + origin.y, rl.getZ() + origin.z);
        }
    }

    private void displayParticles(Vec3 pos, ClientLevel world) {
        Map<ParticleRelativeData, RelativeLocation> locations = loadParticleLocations();
        beforeDisplay(locations);
        toggleScale(locations);

        for (Map.Entry<ParticleRelativeData, RelativeLocation> entry : locations.entrySet()) {
            ParticleRelativeData v = entry.getKey();
            RelativeLocation rl = entry.getValue();
            UUID particleUUID = v.getUuid();

            ParticleDisplayer displayer = v.getEffect().apply(particleUUID);
            if (displayer instanceof ParticleDisplayer.SingleParticleDisplayer) {
                ParticleController controller = ControlParticleManager.INSTANCE.createControl(particleUUID);
                controller.setInitInvoker(v.getInvoker());
            }

            Vec3 toPos = new Vec3(pos.x + rl.getX(), pos.y + rl.getY(), pos.z + rl.getZ());
            Controllable<?> controllable = displayer.display(toPos, world);
            if (controllable == null) continue;

            if (controllable instanceof ParticleController) {
                v.getControllerAction().accept((ParticleController) controllable);
            }
            particles.put(particleUUID, controllable);
            particlesLocations.put(controllable, rl);
        }
    }

    private void toggleScale(Map<ParticleRelativeData, RelativeLocation> locations) {
        if (particlesDefaultScaleLengths.isEmpty()) {
            for (Map.Entry<ParticleRelativeData, RelativeLocation> entry : locations.entrySet()) {
                particlesDefaultScaleLengths.put(entry.getKey().getUuid(), entry.getValue().length());
            }
        }
        if (scale == 1.0) return;
        for (Map.Entry<ParticleRelativeData, RelativeLocation> entry : locations.entrySet()) {
            UUID puuid = entry.getKey().getUuid();
            Double defLen = particlesDefaultScaleLengths.get(puuid);
            if (defLen == null) continue;
            RelativeLocation rl = entry.getValue();
            double currentLen = rl.length();
            if (currentLen > 0.001) {
                rl.multiply(defLen * scale / currentLen);
            }
        }
    }

    // ---- Inner class: ParticleRelativeData ----

    /**
     * Data holder for a single particle in the group layout:
     * contains the effect factory, particle initializer, and optional controller action.
     */
    public static class ParticleRelativeData {
        private final Function<UUID, ParticleDisplayer> effect;
        private final Consumer<ControllableParticle> invoker;
        private final UUID uuid;
        private Consumer<ParticleController> controllerAction;

        public ParticleRelativeData(Function<UUID, ParticleDisplayer> effect, Consumer<ControllableParticle> invoker) {
            this.effect = effect;
            this.invoker = invoker;
            this.uuid = UUID.randomUUID();
            this.controllerAction = c -> {};
        }

        public Function<UUID, ParticleDisplayer> getEffect() { return effect; }
        public Consumer<ControllableParticle> getInvoker() { return invoker; }
        public UUID getUuid() { return uuid; }
        public Consumer<ParticleController> getControllerAction() { return controllerAction; }

        /**
         * Set an action to be run on the ParticleController after creation.
         */
        public ParticleRelativeData withController(Consumer<ParticleController> controller) {
            this.controllerAction = controller;
            return this;
        }
    }
}
