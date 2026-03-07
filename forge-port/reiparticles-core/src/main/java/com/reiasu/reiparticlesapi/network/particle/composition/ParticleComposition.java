// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.composition;

import com.reiasu.reiparticlesapi.annotations.codec.CodecHelper;
import com.reiasu.reiparticlesapi.network.particle.ServerController;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.particles.ParticleDisplayer;
import com.reiasu.reiparticlesapi.utils.Math3DUtil;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.helper.impl.composition.CompositionStatusHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Base class for particle compositions — groups of particles managed as a unit.
 * <p>
 * Manages both server-side state (serialization, rotation, scaling, pre-tick
 * action queue) and client-side particle display via {@link ParticleDisplayer}.
 * Each particle entry is represented by a {@link CompositionData} which can
 * carry a displayer builder, init callback, and a {@link Controllable} handle.
 */
public abstract class ParticleComposition
        implements ServerController<ParticleComposition>, Controllable<ParticleComposition> {

    // ─── Companion-style static encode/decode ────────────────────────────

    public static void encodeBase(ParticleComposition data, FriendlyByteBuf buf) {
        buf.writeUUID(data.controlUUID);
        buf.writeDouble(data.visibleRange);
        buf.writeBoolean(data.canceled);
        buf.writeDouble(data.position.x());
        buf.writeDouble(data.position.y());
        buf.writeDouble(data.position.z());
        buf.writeDouble(data.axis.getX());
        buf.writeDouble(data.axis.getY());
        buf.writeDouble(data.axis.getZ());
        buf.writeDouble(data.scale);
        buf.writeDouble(data.roll);
        buf.writeInt(data.status.getDisplayStatus());
        buf.writeInt(data.status.getClosedInternal());
        buf.writeInt(data.status.getCurrent());
    }

    public static void decodeBase(ParticleComposition instance, FriendlyByteBuf buf) {
        instance.controlUUID = buf.readUUID();
        instance.visibleRange = buf.readDouble();
        instance.canceled = buf.readBoolean();
        instance.position = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        instance.axis = new RelativeLocation(buf.readDouble(), buf.readDouble(), buf.readDouble());
        instance.scale = buf.readDouble();
        instance.roll = buf.readDouble();
        instance.status.setStatus(buf.readInt());
        instance.status.setClosedInternal(buf.readInt());
        instance.status.updateCurrent(buf.readInt());
    }

    // ─── Fields ──────────────────────────────────────────────────────────

    private Vec3 position;
    private Level world;
    private double visibleRange = 128.0;
    private double scale = 1.0;
    private boolean client;
    private boolean displayed;
    private boolean canceled;
    private UUID controlUUID = UUID.randomUUID();
    private RelativeLocation axis = new RelativeLocation(0.0, 1.0, 0.0);
    private double roll;
    private final CompositionStatusHelper status = new CompositionStatusHelper();
    private final ConcurrentHashMap<UUID, Double> particleDefaultLength = new ConcurrentHashMap<>();
    private final ArrayList<Consumer<ParticleComposition>> invokeQueue = new ArrayList<>();
    private final ArrayList<RelativeLocation> particleRotatedLocations = new ArrayList<>();
    private final ArrayList<CompositionData> displayedEntries = new ArrayList<>();
    private int tick;
    private int maxTick = -1;
    private boolean flushed;

    // ─── Constructors ────────────────────────────────────────────────────

    protected ParticleComposition() {
        this(Vec3.ZERO, null);
    }

    protected ParticleComposition(Vec3 position, Level world) {
        this.position = position != null ? position : Vec3.ZERO;
        this.world = world;
    }

    // ─── Abstract methods ────────────────────────────────────────────────

    /**
     * Returns the particle-data-to-location mapping that defines this composition's shape.
     */
    public abstract Map<CompositionData, RelativeLocation> getParticles();

    /**
     * Called once after display is first invoked.
     */
    public abstract void onDisplay();

    // ─── Property accessors ──────────────────────────────────────────────

    public Vec3 getPosition() {
        return position;
    }

    public void setPosition(Vec3 position) {
        this.position = position;
    }

    public Level getWorld() {
        return world;
    }

    public void setWorld(Level world) {
        this.world = world;
    }

    public double getVisibleRange() {
        return visibleRange;
    }

    public void setVisibleRange(double visibleRange) {
        this.visibleRange = visibleRange;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
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

    protected void setDisplayed(boolean displayed) {
        this.displayed = displayed;
    }

    @Override
    public boolean getCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public UUID getControlUUID() {
        return controlUUID;
    }

    public void setControlUUID(UUID controlUUID) {
        this.controlUUID = controlUUID;
    }

    public RelativeLocation getAxis() {
        return axis;
    }

    public void setAxis(RelativeLocation axis) {
        this.axis = axis;
    }

    public double getRoll() {
        return roll;
    }

    public void setRoll(double roll) {
        this.roll = roll;
    }

    public CompositionStatusHelper getStatus() {
        return status;
    }

    public ConcurrentHashMap<UUID, Double> getParticleDefaultLength() {
        return particleDefaultLength;
    }

    protected ArrayList<RelativeLocation> getParticleRotatedLocations() {
        return particleRotatedLocations;
    }

    public int getTick() {
        return tick;
    }

    public void setMaxTick(int maxTick) {
        this.maxTick = maxTick;
    }

    // ─── Pre-tick actions ────────────────────────────────────────────────

    /**
     * Adds a pre-tick action that runs every tick before the main tick logic.
     * This is the Forge Java adaptation of the original Kotlin
     * {@code Function1<ParticleComposition, Unit>} callbacks.
     */
    public ParticleComposition addPreTickAction(Consumer<ParticleComposition> action) {
        invokeQueue.add(action);
        return this;
    }

    /**
     * Overload accepting a simple {@link Runnable} for convenience.
     */
    public ParticleComposition addPreTickAction(Runnable action) {
        invokeQueue.add(pc -> action.run());
        return this;
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────

    @Override
    public void tick() {
        if (canceled || !displayed) {
            return;
        }
        for (Consumer<ParticleComposition> action : invokeQueue) {
            action.accept(this);
        }
        tick++;
        if (maxTick > 0 && tick >= maxTick) {
            canceled = true;
        }
    }

    public void scale(double d) {
        if (d < 0.0) return;
        this.scale = d;
    }

    public void display() {
        if (displayed) return;
        displayed = true;
        if (world != null) {
            client = world.isClientSide();
        }
        flush();
        status.loadController(this);
        status.initHelper();
        onDisplay();
    }

    public void flush() {
        if (!getParticles().isEmpty()) {
            clear(false);
        }
        displayParticles();
    }

    /**
     * Clears all particles. If {@code cancel} is true, the composition
     * is marked as canceled.
     */
    public void clear(boolean cancel) {
        if (client) {
            for (CompositionData data : displayedEntries) {
                Controllable<?> ctrl = data.getControllable();
                if (ctrl != null) {
                    ctrl.remove();
                    data.setControllable(null);
                }
            }
        }
        displayedEntries.clear();
        particleRotatedLocations.clear();
        particleDefaultLength.clear();
        canceled = cancel;
    }

    @Override
    public void remove() {
        clear(true);
    }

    @Override
    public void cancel() {
        canceled = true;
    }

    public void update(ParticleComposition other) {
        this.visibleRange = other.visibleRange;
        this.position = other.position;
        this.canceled = other.canceled;
        this.roll = other.roll;
        this.controlUUID = other.controlUUID;
        this.axis = other.axis;
        this.status.setStatus(other.status.getDisplayStatus());
        this.status.setClosedInternal(other.status.getClosedInternal());
        this.status.updateCurrent(other.status.getCurrent());
        CodecHelper.INSTANCE.updateFields(this, other);
    }

    // ─── Display / particles ─────────────────────────────────────────────

    public void beforeDisplay(Map<CompositionData, RelativeLocation> map) {
        // Override in subclass to modify locations before display
    }

    /**
     * Called by {@link #flush()} to actually create particles.
     */
    protected void displayParticles() {
        if (!client) return;
        Map<CompositionData, RelativeLocation> locations = getParticles();
        beforeDisplay(locations);
        toggleScale(locations);
        Math3DUtil.rotateAsAxis(
                new ArrayList<>(locations.values()), axis, roll
        );
        for (Map.Entry<CompositionData, RelativeLocation> entry : locations.entrySet()) {
            displayEntry(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Displays a single particle entry using its {@link ParticleDisplayer}.
     * If no displayer builder is set on the data, only the rotated location is tracked.
     */
    protected void displayEntry(CompositionData data, RelativeLocation pos) {
        particleRotatedLocations.add(pos);
        displayedEntries.add(data);
        if (!client) return;
        if (data.getDisplayerBuilder() == null) return;
        if (!(world instanceof ClientLevel clientWorld)) return;

        ParticleDisplayer displayer = data.getDisplayerBuilder().get();
        Vec3 spawnPos = position.add(pos.getX(), pos.getY(), pos.getZ());
        Controllable<?> handle = displayer.display(spawnPos, clientWorld);
        data.setControllable(handle);

        if (handle != null && data.getParticleInit() != null) {
            if (handle instanceof com.reiasu.reiparticlesapi.particles.control.ParticleController pc) {
                data.getParticleInit().accept(pc);
            }
        }
    }

    /**
     * Applies scale to particle locations based on their default lengths.
     */
    public void toggleScale(Map<CompositionData, RelativeLocation> locations) {
        if (canceled) return;
        if (particleDefaultLength.isEmpty()) {
            for (Map.Entry<CompositionData, RelativeLocation> entry : locations.entrySet()) {
                UUID uuid = entry.getKey().getUuid();
                particleDefaultLength.put(uuid, entry.getValue().length());
            }
        }
        for (Map.Entry<CompositionData, RelativeLocation> entry : locations.entrySet()) {
            UUID uuid = entry.getKey().getUuid();
            Double len = particleDefaultLength.get(uuid);
            if (len == null || len <= 0.0) continue;
            RelativeLocation value = entry.getValue();
            double currentLen = value.length();
            if (currentLen > 1.0E-6) {
                value.scale(len * scale / currentLen);
            }
        }
    }

    /**
     * Teleports all particles to their rotated positions relative to the
     * composition origin.
     */
    public void toggleRelative() {
        if (!client) return;
        int size = Math.min(displayedEntries.size(), particleRotatedLocations.size());
        for (int i = 0; i < size; i++) {
            CompositionData data = displayedEntries.get(i);
            Controllable<?> ctrl = data.getControllable();
            if (ctrl == null) continue;
            RelativeLocation rl = particleRotatedLocations.get(i);
            ctrl.teleportTo(position.add(rl.getX(), rl.getY(), rl.getZ()));
        }
    }

    public ParticleComposition setDisabledInterval(int interval) {
        status.setClosedInternal(interval);
        return this;
    }

    // ─── Rotation ────────────────────────────────────────────────────────

    public void rotateToPoint(RelativeLocation to) {
        if (!client) {
            axis = to;
            return;
        }
        Math3DUtil.rotatePointsToPoint(particleRotatedLocations, to, axis);
        axis = to;
        toggleRelative();
    }

    public void rotateToWithAngle(RelativeLocation to, double radian) {
        roll += radian;
        if (roll >= Math.PI * 2) {
            roll -= Math.PI * 2;
        } else if (roll <= -Math.PI * 2) {
            roll += Math.PI * 2;
        }
        if (!client) {
            axis = to;
            return;
        }
        Math3DUtil.rotateAsAxis(particleRotatedLocations, axis, radian);
        Math3DUtil.rotatePointsToPoint(particleRotatedLocations, to, axis);
        axis = to;
        toggleRelative();
    }

    public void rotateAsAxis(double radian) {
        roll += radian;
        if (roll >= Math.PI * 2) {
            roll -= Math.PI * 2;
        } else if (roll <= -Math.PI * 2) {
            roll += Math.PI * 2;
        }
        if (!client) return;
        Math3DUtil.rotateAsAxis(particleRotatedLocations, axis, radian);
        toggleRelative();
    }

    // ─── Pre-rotation helpers (used before display) ──────────────────────

    public void preRotateTo(Map<CompositionData, RelativeLocation> map, RelativeLocation to) {
        Math3DUtil.rotatePointsToPoint(new ArrayList<>(map.values()), to, axis);
        axis = to;
    }

    public void preRotateAsAxis(Map<CompositionData, RelativeLocation> map, RelativeLocation axis, double angle) {
        Math3DUtil.rotateAsAxis(new ArrayList<>(map.values()), axis, angle);
        this.axis = axis;
    }

    public void preRotateAsAxis(Map<CompositionData, RelativeLocation> map, double angle) {
        Math3DUtil.rotateAsAxis(new ArrayList<>(map.values()), axis, angle);
    }

    // ─── Controllable interface support ───────────────────────────────────

    @Override
    public UUID controlUUID() {
        return controlUUID;
    }

    @Override
    public void teleportTo(Vec3 pos) {
        this.position = pos;
        toggleRelative();
    }

    @Override
    public void teleportTo(double x, double y, double z) {
        teleportTo(new Vec3(x, y, z));
    }

    @Override
    public ParticleComposition getControlObject() {
        return this;
    }

    /**
     * Provides compatibility with code that expects a {@code Controllable}-style API.
     * Used internally by CompositionStatusHelper via loadController(Controllable).
     */
    public void loadController(Object controller) {
        // Handled by CompositionStatusHelper via loadController(Controllable)
    }
}
