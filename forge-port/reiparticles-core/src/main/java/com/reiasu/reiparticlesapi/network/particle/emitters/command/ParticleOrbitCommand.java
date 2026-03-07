// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.command;

import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Makes particles orbit around a center position with configurable radius,
 * angular speed, radial correction, and orbit mode (PHYSICAL / SPRING / SNAP).
 */
public final class ParticleOrbitCommand implements ParticleCommand {

    private Supplier<Vec3> center = () -> Vec3.ZERO;
    private Vec3 axis = new Vec3(0.0, 1.0, 0.0);
    private double radius = 3.0;
    private double angularSpeed = 0.35;
    private double radialCorrect = 0.25;
    private double minDistance = 0.2;
    private OrbitMode mode = OrbitMode.PHYSICAL;
    private double maxRadialStep = 0.5;

    public ParticleOrbitCommand() {
    }

    public ParticleOrbitCommand(Supplier<Vec3> center, Vec3 axis, double radius,
                                double angularSpeed, double radialCorrect,
                                double minDistance, OrbitMode mode) {
        this.center = center;
        this.axis = axis;
        this.radius = radius;
        this.angularSpeed = angularSpeed;
        this.radialCorrect = radialCorrect;
        this.minDistance = minDistance;
        this.mode = mode;
    }

    // Fluent setters

    public ParticleOrbitCommand center(Supplier<Vec3> v) { this.center = v; return this; }
    public ParticleOrbitCommand axis(Vec3 v) { this.axis = v; return this; }
    public ParticleOrbitCommand radius(double v) { this.radius = v; return this; }
    public ParticleOrbitCommand angularSpeed(double v) { this.angularSpeed = v; return this; }
    public ParticleOrbitCommand radialCorrect(double v) { this.radialCorrect = v; return this; }
    public ParticleOrbitCommand minDistance(double v) { this.minDistance = v; return this; }
    public ParticleOrbitCommand mode(OrbitMode v) { this.mode = v; return this; }
    public ParticleOrbitCommand maxRadialStep(double v) { this.maxRadialStep = v; return this; }

    // Standard getters/setters

    public Supplier<Vec3> getCenter() { return center; }
    public void setCenter(Supplier<Vec3> center) { this.center = center; }
    public Vec3 getAxis() { return axis; }
    public void setAxis(Vec3 axis) { this.axis = axis; }
    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }
    public double getAngularSpeed() { return angularSpeed; }
    public void setAngularSpeed(double angularSpeed) { this.angularSpeed = angularSpeed; }
    public double getRadialCorrect() { return radialCorrect; }
    public void setRadialCorrect(double radialCorrect) { this.radialCorrect = radialCorrect; }
    public double getMinDistance() { return minDistance; }
    public void setMinDistance(double minDistance) { this.minDistance = minDistance; }
    public OrbitMode getMode() { return mode; }
    public void setMode(OrbitMode mode) { this.mode = mode; }
    public double getMaxRadialStep() { return maxRadialStep; }
    public void setMaxRadialStep(double maxRadialStep) { this.maxRadialStep = maxRadialStep; }

    @Override
    public void execute(ControllableParticleData data, ControllableParticle particle) {
        Vec3 pos = data.getPosition();
        Vec3 ax = axis.normalize();
        Vec3 centerPos = center.get();

        Vec3 r0 = pos.subtract(centerPos);
        Vec3 axialComp = ax.scale(r0.dot(ax));
        Vec3 radial = r0.subtract(axialComp);
        double dist = radial.length();
        dist = Math.max(dist, minDistance);
        radial = radial.scale(1.0 / dist);

        Vec3 tangential = ax.cross(radial);
        double tLen = tangential.length();
        if (tLen > 1.0E-9) {
            tangential = tangential.scale(1.0 / tLen);
        }

        Vec3 dvTan = tangential.scale(angularSpeed);
        double err = dist - radius;

        switch (mode) {
            case PHYSICAL -> {
                double raw = -err * radialCorrect;
                double step = Math.max(-maxRadialStep, Math.min(maxRadialStep, raw));
                Vec3 dvRad = radial.scale(step);
                data.setVelocity(data.getVelocity().add(dvTan).add(dvRad));
            }
            case SPRING -> {
                double spring = -err * radialCorrect;
                double step = Math.max(-maxRadialStep, Math.min(maxRadialStep, spring));
                Vec3 dvRad = radial.scale(step);
                data.setVelocity(data.getVelocity().add(dvTan).add(dvRad));
            }
            case SNAP -> {
                Vec3 targetPos = centerPos.add(axialComp).add(radial.scale(radius));
                Vec3 snapVec = targetPos.subtract(pos);
                data.setVelocity(data.getVelocity().add(dvTan).add(snapVec.scale(radialCorrect)));
            }
        }
    }
}
