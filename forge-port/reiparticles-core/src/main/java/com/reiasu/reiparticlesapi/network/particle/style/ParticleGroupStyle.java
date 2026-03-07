// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.style;

import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.network.particle.ServerController;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import com.reiasu.reiparticlesapi.particles.ParticleDisplayer;
import com.reiasu.reiparticlesapi.particles.control.ControlParticleManager;
import com.reiasu.reiparticlesapi.particles.control.ParticleController;
import com.reiasu.reiparticlesapi.utils.Math3DUtil;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base class for particle style groups.
 * <p>
 * Manages a collection of controllable particles arranged at relative locations,
 * with support for display, scaling, rotation, teleportation, and server↔client sync.
 */
public abstract class ParticleGroupStyle implements ServerController<ParticleGroupStyle>, Controllable<ParticleGroupStyle> {

    // ---- Core identity ----
    private UUID uuid;
    private ResourceLocation registryKey;
    private boolean canceled;
    private boolean client;
    private Level world;
    private Vec3 pos = Vec3.ZERO;
    private RelativeLocation axis = new RelativeLocation(0.0, 0.0, 1.0);
    private double rotate;
    private double scale = 1.0;
    private long lastUpdatedGameTime;
    private long displayedTime;
    private double visibleRange;
    private boolean autoToggle = true;
    private boolean displayed;

    // ---- Particle management ----
    private final ConcurrentHashMap<UUID, Controllable<?>> particles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Controllable<?>, RelativeLocation> particleLocations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Double> particleDefaultLength = new ConcurrentHashMap<>();

    // ---- Tick actions ----
    private final List<Consumer<ParticleGroupStyle>> preTickActions = new ArrayList<>();

    // ---- Constructors ----

    public ParticleGroupStyle() {
        this(96.0, UUID.randomUUID());
    }

    public ParticleGroupStyle(double visibleRange, UUID uuid) {
        this.visibleRange = visibleRange;
        this.uuid = uuid;
    }

    @Override
    public void spawnInWorld(ServerLevel world, Vec3 pos) {
        ParticleStyleManager.spawnStyle(world, pos, this);
    }

    // ---- Getters/Setters ----

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public ResourceLocation getRegistryKey() {
        return registryKey;
    }

    public void setRegistryKey(ResourceLocation registryKey) {
        this.registryKey = registryKey;
    }

    public Level getWorld() {
        return world;
    }

    public void setWorld(Level world) {
        this.world = world;
    }

    public Vec3 getPos() {
        return pos;
    }

    public void setPos(Vec3 pos) {
        this.pos = pos;
    }

    public RelativeLocation getAxis() {
        return axis;
    }

    public void setAxis(RelativeLocation axis) {
        this.axis = axis;
    }

    public double getRotate() {
        return rotate;
    }

    public void setRotate(double rotate) {
        this.rotate = rotate;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public long getLastUpdatedGameTime() {
        return lastUpdatedGameTime;
    }

    public void setLastUpdatedGameTime(long lastUpdatedGameTime) {
        this.lastUpdatedGameTime = lastUpdatedGameTime;
    }

    public long getDisplayedTime() {
        return displayedTime;
    }

    public void setDisplayedTime(long displayedTime) {
        this.displayedTime = displayedTime;
    }

    public double getVisibleRange() {
        return visibleRange;
    }

    public void setVisibleRange(double visibleRange) {
        this.visibleRange = visibleRange;
    }

    public boolean getAutoToggle() {
        return autoToggle;
    }

    public void setAutoToggle(boolean autoToggle) {
        this.autoToggle = autoToggle;
    }

    public boolean getClient() {
        return client;
    }

    public void setClient(boolean client) {
        this.client = client;
    }

    public boolean getDisplayed() {
        return displayed;
    }

    public void setDisplayed(boolean displayed) {
        this.displayed = displayed;
    }

    public ConcurrentHashMap<UUID, Controllable<?>> getParticles() {
        return particles;
    }

    public ConcurrentHashMap<Controllable<?>, RelativeLocation> getParticleLocations() {
        return particleLocations;
    }

    public ConcurrentHashMap<UUID, Double> getParticleDefaultLength() {
        return particleDefaultLength;
    }

    // ---- Abstract methods that subclasses must implement ----

    /**
     * Return the current frame of particles: mapping from StyleData to relative location.
     * Non-sequenced styles use this; sequenced styles return empty and use their own method.
     */
    public abstract Map<StyleData, RelativeLocation> getCurrentFrames();

    /**
     * Called when the style is first displayed (after particles are spawned).
     */
    public abstract void onDisplay();

    // ---- Display lifecycle ----

    /**
     * Called before particles are spawned. Subclasses can modify the locations map.
     */
    public void beforeDisplay(Map<StyleData, RelativeLocation> styles) {
        // Default no-op
    }

    /**
     * Display the style at the given position in the given world.
     */
    public void display(Vec3 pos, Level world) {
        if (displayed) {
            return;
        }
        this.displayed = true;
        this.pos = pos;
        this.world = world;
        this.client = world.isClientSide;
        if (!client) {
            onDisplay();
            return;
        }
        flush();
        onDisplay();
    }

    /**
     * Refresh the display: clear existing particles and re-display.
     */
    public void flush() {
        if (!particles.isEmpty()) {
            clear(true);
        }
        displayParticles();
    }

    /**
     * Spawn particles from getCurrentFrames().
     */
    protected void displayParticles() {
        Map<StyleData, RelativeLocation> locations = getCurrentFrames();
        beforeDisplay(locations);
        toggleScale(locations);
        // Rotate initial positions
        List<RelativeLocation> locs = new ArrayList<>(locations.values());
        Math3DUtil.rotateAsAxis(locs, axis, rotate);

        for (Map.Entry<StyleData, RelativeLocation> entry : locations.entrySet()) {
            StyleData data = entry.getKey();
            RelativeLocation rl = entry.getValue();
            UUID particleUUID = data.getUuid();

            ParticleDisplayer displayer = data.getDisplayerBuilder().apply(particleUUID);
            if (displayer instanceof ParticleDisplayer.SingleParticleDisplayer) {
                ParticleController controller = ControlParticleManager.INSTANCE.createControl(particleUUID);
                controller.setInitInvoker(data.getParticleHandler());
            }

            Vec3 toPos = new Vec3(pos.x + rl.getX(), pos.y + rl.getY(), pos.z + rl.getZ());
            Controllable<?> controllable = displayer.display(toPos, (ClientLevel) world);
            if (controllable == null) {
                continue;
            }
            if (controllable instanceof ParticleController) {
                data.getParticleControllerHandler().accept((ParticleController) controllable);
            }
            particles.put(particleUUID, controllable);
            particleLocations.put(controllable, rl);
        }
    }

    /**
     * Clear all particles. If valid is true, keep the style alive; otherwise mark invalid.
     */
    public void clear(boolean valid) {
        for (Controllable<?> c : particles.values()) {
            c.remove();
        }
        particles.clear();
        particleLocations.clear();
        particleDefaultLength.clear();
    }

    /**
     * Record default scale lengths and apply current scale factor.
     */
    protected void toggleScale(Map<? extends StyleData, RelativeLocation> locations) {
        if (particleDefaultLength.isEmpty()) {
            for (Map.Entry<? extends StyleData, RelativeLocation> entry : locations.entrySet()) {
                particleDefaultLength.put(entry.getKey().getUuid(), entry.getValue().length());
            }
        }
        if (scale == 1.0) {
            return;
        }
        for (Map.Entry<? extends StyleData, RelativeLocation> entry : locations.entrySet()) {
            UUID uuid = entry.getKey().getUuid();
            Double defLen = particleDefaultLength.get(uuid);
            if (defLen == null) continue;
            RelativeLocation rl = entry.getValue();
            double currentLen = rl.length();
            if (currentLen > 0.001) {
                rl.multiply(defLen * scale / currentLen);
            }
        }
    }

    /**
     * Update displayed particles' scale.
     */
    protected void toggleScaleDisplayed() {
        if (scale == 1.0) {
            return;
        }
        for (Map.Entry<Controllable<?>, RelativeLocation> entry : particleLocations.entrySet()) {
            UUID puuid = entry.getKey().controlUUID();
            Double defLen = particleDefaultLength.get(puuid);
            if (defLen == null) continue;
            RelativeLocation rl = entry.getValue();
            double currentLen = rl.length();
            if (currentLen > 0.001) {
                rl.multiply(defLen * scale / currentLen);
            }
        }
    }

    /**
     * Teleport all particles to their current relative positions based on origin.
     */
    protected void toggleRelative() {
        for (Map.Entry<Controllable<?>, RelativeLocation> entry : particleLocations.entrySet()) {
            Controllable<?> c = entry.getKey();
            RelativeLocation rl = entry.getValue();
            c.teleportTo(rl.getX() + pos.x, rl.getY() + pos.y, rl.getZ() + pos.z);
        }
    }

    // ---- Transform operations ----

    public void remove() {
        canceled = true;
        clear(false);
    }

    public void teleportTo(Vec3 to) {
        this.pos = to;
        toggleRelative();
    }

    public void rotateAsAxis(double radian) {
        List<RelativeLocation> locs = new ArrayList<>(particleLocations.values());
        Math3DUtil.rotateAsAxis(locs, axis, radian);
        this.rotate += radian;
        if (this.rotate >= Math.PI * 2) {
            this.rotate -= Math.PI * 2;
        }
        toggleRelative();
    }

    public void rotateToPoint(RelativeLocation to) {
        List<RelativeLocation> locs = new ArrayList<>(particleLocations.values());
        Math3DUtil.rotatePointsToPoint(locs, to, axis);
        this.axis = to;
        toggleRelative();
    }

    public void rotateToWithAngle(RelativeLocation to, double radian) {
        List<RelativeLocation> locs = new ArrayList<>(particleLocations.values());
        Math3DUtil.rotatePointsToPoint(locs, to, axis);
        List<RelativeLocation> locs2 = new ArrayList<>(particleLocations.values());
        Math3DUtil.rotateAsAxis(locs2, to.normalize(), radian);
        this.axis = to;
        this.rotate += radian;
        if (this.rotate >= Math.PI * 2) {
            this.rotate -= Math.PI * 2;
        }
        toggleRelative();
    }

    public void scale(double scale) {
        setScale(scale);
        if (displayed) {
            toggleScaleDisplayed();
        }
    }

    public void addPreTickAction(Consumer<ParticleGroupStyle> action) {
        if (action != null) {
            preTickActions.add(action);
        }
    }

    // ---- Packet args ----

    public Map<String, ParticleControllerDataBuffer<?>> writePacketArgs() {
        return new HashMap<>();
    }

    public void readPacketArgs(Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
    }

    // ---- Change with callback ----

    public void change(Consumer<ParticleGroupStyle> invoker, Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
        if (invoker != null) {
            invoker.accept(this);
        }
        // Server-side would sync here; for now just apply locally
        readPacketArgs(args);
    }

    // ---- ServerController / Controllable interface ----

    @Override
    public void load(Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
        readPacketArgs(args);
    }

    @Override
    public Map<String, ParticleControllerDataBuffer<?>> toArgs() {
        return writePacketArgs();
    }

    @Override
    public void change(Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
        readPacketArgs(args);
    }

    @Override
    public boolean getCanceled() {
        return canceled;
    }

    @Override
    public void cancel() {
        canceled = true;
    }

    @Override
    public UUID controlUUID() {
        return uuid;
    }

    @Override
    public void teleportTo(double x, double y, double z) {
        teleportTo(new Vec3(x, y, z));
    }

    @Override
    public ParticleGroupStyle getControlObject() {
        return this;
    }

    @Override
    public void tick() {
        for (Consumer<ParticleGroupStyle> action : preTickActions) {
            action.accept(this);
        }
        displayedTime++;
    }

    // ---- Inner class: StyleData ----

    /**
     * Holds the data needed to create a single particle in a style:
     * the displayer builder, particle handler, and controller handler.
     */
    public static class StyleData {
        private final UUID uuid;
        private final Function<UUID, ParticleDisplayer> displayerBuilder;
        private Consumer<ControllableParticle> particleHandler;
        private Consumer<ParticleController> particleControllerHandler;

        public StyleData(Function<UUID, ParticleDisplayer> displayerBuilder) {
            this.uuid = UUID.randomUUID();
            this.displayerBuilder = displayerBuilder;
            this.particleHandler = p -> {};
            this.particleControllerHandler = c -> {};
        }

        public UUID getUuid() {
            return uuid;
        }

        public Function<UUID, ParticleDisplayer> getDisplayerBuilder() {
            return displayerBuilder;
        }

        public Consumer<ControllableParticle> getParticleHandler() {
            return particleHandler;
        }

        public Consumer<ParticleController> getParticleControllerHandler() {
            return particleControllerHandler;
        }

        /**
         * Set a handler that configures the particle after creation.
         */
        public StyleData withParticleHandler(Consumer<ControllableParticle> handler) {
            this.particleHandler = handler;
            return this;
        }

        /**
         * Set a handler that configures the particle controller after creation.
         */
        public StyleData withParticleControllerHandler(Consumer<ParticleController> handler) {
            this.particleControllerHandler = handler;
            return this;
        }
    }
}
