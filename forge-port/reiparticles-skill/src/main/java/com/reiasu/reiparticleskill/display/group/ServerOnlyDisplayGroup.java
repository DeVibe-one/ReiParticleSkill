// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display.group;

import com.reiasu.reiparticlesapi.display.DisplayEntity;
import com.reiasu.reiparticlesapi.display.DisplayEntityManager;
import com.reiasu.reiparticlesapi.network.particle.ServerController;
import com.reiasu.reiparticlesapi.network.particle.composition.ParticleComposition;
import com.reiasu.reiparticlesapi.network.particle.composition.manager.ParticleCompositionManager;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleGroupStyle;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleStyleManager;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticleskill.display.ServerMovableDisplay;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class ServerOnlyDisplayGroup implements ServerController<ServerOnlyDisplayGroup> {
    private Vec3 pos;
    private Level world;
    private final Map<UUID, Object> manageDisplayers = new HashMap<>();
    private final Map<Object, RelativeLocation> displayed = new HashMap<>();
    private final List<RelativeLocation> locations = new ArrayList<>();
    private UUID uuid = UUID.randomUUID();
    private boolean canceled;
    private boolean start;
    private RelativeLocation axis = new RelativeLocation(0.0, 1.0, 0.0);

    protected ServerOnlyDisplayGroup(Vec3 pos, Level world) {
        this.pos = pos == null ? Vec3.ZERO : pos;
        this.world = world;
    }

    public Vec3 getPos() {
        return pos;
    }

    public void setPos(Vec3 pos) {
        this.pos = pos == null ? Vec3.ZERO : pos;
    }

    public Level getWorld() {
        return world;
    }

    public void setWorld(Level world) {
        this.world = world;
    }

    public Map<UUID, Object> getManageDisplayers() {
        return manageDisplayers;
    }

    public Map<Object, RelativeLocation> getDisplayed() {
        return displayed;
    }

    public List<RelativeLocation> getLocations() {
        return locations;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public boolean getCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean getStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public RelativeLocation getAxis() {
        return axis.copy();
    }

    public void setAxis(RelativeLocation axis) {
        this.axis = axis == null ? new RelativeLocation(0.0, 1.0, 0.0) : axis.copy();
    }

    public abstract Map<Supplier<Object>, RelativeLocation> getDisplayers();

    @Override
    public abstract void tick();

    public abstract void onDisplay();

    public void flush() {
        clear();
        displayAll();
    }

    public void clear() {
        for (Object displayer : displayed.keySet()) {
            removeControl(displayer);
        }
        manageDisplayers.clear();
        displayed.clear();
        locations.clear();
    }

    public void displayAll() {
        Map<Supplier<Object>, RelativeLocation> displayers = getDisplayers();
        if (displayers == null || displayers.isEmpty()) {
            return;
        }
        for (Map.Entry<Supplier<Object>, RelativeLocation> entry : displayers.entrySet()) {
            displayEntry(entry.getKey(), entry.getValue());
        }
    }

    protected void displayEntry(Supplier<Object> supplier, RelativeLocation relative) {
        if (supplier == null || relative == null) {
            return;
        }
        Object control = supplier.get();
        if (control == null) {
            return;
        }

        RelativeLocation offset = relative.copy();
        Vec3 targetPos = pos.add(offset.toVector());
        teleportControl(control, targetPos);
        spawnControl(control, targetPos);

        UUID id = idOf(control);
        if (id != null) {
            manageDisplayers.put(id, control);
        }
        displayed.put(control, offset);
        locations.add(offset);
    }

    public void toggleRelative() {
        for (Map.Entry<Object, RelativeLocation> entry : displayed.entrySet()) {
            Vec3 targetPos = pos.add(entry.getValue().toVector());
            teleportControl(entry.getKey(), targetPos);
        }
    }

    public void teleportTo(Vec3 to) {
        if (to == null) {
            return;
        }
        pos = to;
        toggleRelative();
    }

    public void teleportTo(double x, double y, double z) {
        teleportTo(new Vec3(x, y, z));
    }

    public void rotateToPoint(RelativeLocation to) {
        if (to == null || locations.isEmpty()) {
            axis = to == null ? axis : to.copy();
            return;
        }
        rotateLocationsFromTo(axis, to);
        axis = to.copy();
        toggleRelative();
    }

    public void rotateToWithAngle(RelativeLocation to, double radian) {
        rotateAsAxis(radian);
        rotateToPoint(to);
    }

    public void rotateAsAxis(double radian) {
        if (locations.isEmpty()) {
            return;
        }
        RelativeLocation normalizedAxis = axis.copy().normalize();
        if (normalizedAxis.length() < 1.0E-8) {
            normalizedAxis = new RelativeLocation(0.0, 1.0, 0.0);
        }
        rotateLocationsAroundAxis(normalizedAxis, radian);
        toggleRelative();
    }

    public void remove() {
        clear();
        canceled = true;
        start = false;
    }

    @Override
    public void cancel() {
        remove();
    }

    public void spawn(Level world, Vec3 pos) {
        this.world = world;
        this.pos = pos == null ? Vec3.ZERO : pos;
        display();
    }

    public void display() {
        if (start) {
            return;
        }
        canceled = false;
        start = true;
        flush();
        onDisplay();
    }

    public ServerOnlyDisplayGroup getValue() {
        return this;
    }

    private void spawnControl(Object control, Vec3 targetPos) {
        if (control instanceof ParticleComposition composition) {
            composition.setWorld(world);
            composition.setPosition(targetPos);
            ParticleCompositionManager.INSTANCE.spawn(composition);
            return;
        }
        if (control instanceof ServerOnlyDisplayGroup group) {
            group.spawn(world, targetPos);
            ServerDisplayGroupManager.INSTANCE.spawn(group);
            return;
        }
        if (control instanceof DisplayEntity displayEntity) {
            ServerLevel serverLevel = world instanceof ServerLevel sl ? sl : null;
            DisplayEntityManager.INSTANCE.spawn(displayEntity, serverLevel);
            return;
        }
        if (control instanceof ParticleEmitters emitters) {
            if (world != null) {
                emitters.bind(world, targetPos.x, targetPos.y, targetPos.z);
            }
            ParticleEmittersManager.spawnEmitters(emitters);
            return;
        }
        if (control instanceof ParticleGroupStyle style && world != null) {
            ParticleStyleManager.spawnStyle(world, targetPos, style);
        }
    }

    private void teleportControl(Object control, Vec3 targetPos) {
        if (control instanceof ParticleComposition composition) {
            composition.setPosition(targetPos);
            if (world != null) {
                composition.setWorld(world);
            }
            return;
        }
        if (control instanceof ServerOnlyDisplayGroup group) {
            group.teleportTo(targetPos);
            return;
        }
        if (control instanceof ServerMovableDisplay movable) {
            movable.teleportTo(targetPos);
            return;
        }
        if (control instanceof ParticleEmitters emitters) {
            emitters.teleportTo(targetPos);
            if (world != null) {
                emitters.bind(world, targetPos.x, targetPos.y, targetPos.z);
            }
            return;
        }
        if (control instanceof ParticleGroupStyle style) {
            style.teleportTo(targetPos);
            if (world != null) {
                style.setWorld(world);
            }
        }
    }

    private void removeControl(Object control) {
        if (control instanceof ParticleComposition composition) {
            composition.remove();
            return;
        }
        if (control instanceof ServerOnlyDisplayGroup group) {
            group.remove();
            return;
        }
        if (control instanceof DisplayEntity displayEntity) {
            displayEntity.cancel();
            return;
        }
        if (control instanceof ParticleEmitters emitters) {
            emitters.cancel();
            return;
        }
        if (control instanceof ParticleGroupStyle style) {
            style.remove();
        }
    }

    private UUID idOf(Object control) {
        if (control instanceof ParticleComposition composition) {
            return composition.getControlUUID();
        }
        if (control instanceof ServerOnlyDisplayGroup group) {
            return group.getUuid();
        }
        if (control instanceof DisplayEntity displayEntity) {
            return displayEntity.getControlUUID();
        }
        if (control instanceof ParticleEmitters emitters) {
            return emitters.getUuid();
        }
        if (control instanceof ParticleGroupStyle style) {
            return style.getUuid();
        }
        return null;
    }

    private void rotateLocationsFromTo(RelativeLocation from, RelativeLocation to) {
        RelativeLocation fromUnit = from.copy().normalize();
        RelativeLocation toUnit = to.copy().normalize();
        if (fromUnit.length() < 1.0E-8 || toUnit.length() < 1.0E-8) {
            return;
        }

        double dot = clamp(dot(fromUnit, toUnit), -1.0, 1.0);
        if (dot > 0.999999) {
            return;
        }
        if (dot < -0.999999) {
            RelativeLocation fallback = Math.abs(fromUnit.getX()) < 0.9
                    ? new RelativeLocation(1.0, 0.0, 0.0)
                    : new RelativeLocation(0.0, 1.0, 0.0);
            RelativeLocation axis = cross(fromUnit, fallback).normalize();
            rotateLocationsAroundAxis(axis, Math.PI);
            return;
        }

        RelativeLocation rotationAxis = cross(fromUnit, toUnit);
        double sin = rotationAxis.length();
        if (sin < 1.0E-8) {
            return;
        }
        rotationAxis.normalize();
        double angle = Math.atan2(sin, dot);
        rotateLocationsAroundAxis(rotationAxis, angle);
    }

    private void rotateLocationsAroundAxis(RelativeLocation axis, double radians) {
        for (RelativeLocation location : locations) {
            rotateAroundAxis(location, axis, radians);
        }
    }

    private static void rotateAroundAxis(RelativeLocation point, RelativeLocation axis, double radians) {
        RelativeLocation nAxis = axis.copy().normalize();
        double ux = nAxis.getX();
        double uy = nAxis.getY();
        double uz = nAxis.getZ();
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double x = point.getX();
        double y = point.getY();
        double z = point.getZ();
        double dot = ux * x + uy * y + uz * z;

        double rx = x * cos + (uy * z - uz * y) * sin + ux * dot * (1.0 - cos);
        double ry = y * cos + (uz * x - ux * z) * sin + uy * dot * (1.0 - cos);
        double rz = z * cos + (ux * y - uy * x) * sin + uz * dot * (1.0 - cos);

        point.setX(rx);
        point.setY(ry);
        point.setZ(rz);
    }

    private static RelativeLocation cross(RelativeLocation a, RelativeLocation b) {
        return new RelativeLocation(
                a.getY() * b.getZ() - a.getZ() * b.getY(),
                a.getZ() * b.getX() - a.getX() * b.getZ(),
                a.getX() * b.getY() - a.getY() * b.getX()
        );
    }

    private static double dot(RelativeLocation a, RelativeLocation b) {
        return a.getX() * b.getX() + a.getY() * b.getY() + a.getZ() * b.getZ();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
