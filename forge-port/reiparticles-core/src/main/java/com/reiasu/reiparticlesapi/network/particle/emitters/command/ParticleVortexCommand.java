// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.command;

import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import com.reiasu.reiparticlesapi.utils.GraphMathHelper;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Creates a vortex/spiral force effect. Combines tangential swirl, radial pull
 * inward, and axial lift, all attenuated by inverse-power distance falloff.
 */
public final class ParticleVortexCommand implements ParticleCommand {

    private Supplier<Vec3> center = () -> Vec3.ZERO;
    private Vec3 axis = new Vec3(0.0, 1.0, 0.0);
    private double swirlStrength = 0.8;
    private double radialPull = 0.35;
    private double axialLift = 0.0;
    private double range = 10.0;
    private double falloffPower = 2.0;
    private double minDistance = 0.2;

    public ParticleVortexCommand() {
    }

    public ParticleVortexCommand(Supplier<Vec3> center, Vec3 axis, double swirlStrength,
                                 double radialPull, double axialLift, double range,
                                 double falloffPower, double minDistance) {
        this.center = center;
        this.axis = axis;
        this.swirlStrength = swirlStrength;
        this.radialPull = radialPull;
        this.axialLift = axialLift;
        this.range = range;
        this.falloffPower = falloffPower;
        this.minDistance = minDistance;
    }

    // Fluent setters

    public ParticleVortexCommand center(Supplier<Vec3> v) { this.center = v; return this; }
    public ParticleVortexCommand axis(Vec3 v) { this.axis = v; return this; }
    public ParticleVortexCommand swirlStrength(double v) { this.swirlStrength = v; return this; }
    public ParticleVortexCommand radialPull(double v) { this.radialPull = v; return this; }
    public ParticleVortexCommand axialLift(double v) { this.axialLift = v; return this; }
    public ParticleVortexCommand range(double v) { this.range = v; return this; }
    public ParticleVortexCommand falloffPower(double v) { this.falloffPower = v; return this; }
    public ParticleVortexCommand minDistance(double v) { this.minDistance = v; return this; }

    // Standard getters/setters

    public Supplier<Vec3> getCenter() { return center; }
    public void setCenter(Supplier<Vec3> center) { this.center = center; }
    public Vec3 getAxis() { return axis; }
    public void setAxis(Vec3 axis) { this.axis = axis; }
    public double getSwirlStrength() { return swirlStrength; }
    public void setSwirlStrength(double swirlStrength) { this.swirlStrength = swirlStrength; }
    public double getRadialPull() { return radialPull; }
    public void setRadialPull(double radialPull) { this.radialPull = radialPull; }
    public double getAxialLift() { return axialLift; }
    public void setAxialLift(double axialLift) { this.axialLift = axialLift; }
    public double getRange() { return range; }
    public void setRange(double range) { this.range = range; }
    public double getFalloffPower() { return falloffPower; }
    public void setFalloffPower(double falloffPower) { this.falloffPower = falloffPower; }
    public double getMinDistance() { return minDistance; }
    public void setMinDistance(double minDistance) { this.minDistance = minDistance; }

    @Override
    public void execute(ControllableParticleData data, ControllableParticle particle) {
        Vec3 pos = data.getPosition();
        Vec3 ax = axis.normalize();
        Vec3 r = pos.subtract(center.get());
        Vec3 axialComp = ax.scale(r.dot(ax));
        Vec3 radial = r.subtract(axialComp);
        double dist = radial.length();
        if (dist < 1.0E-9) {
            dist = 0.0;
        }
        double d = Math.max(dist, minDistance);
        double falloff = GraphMathHelper.inversePowerFalloff(d, range, falloffPower);

        Vec3 tangential = ax.cross(radial);
        double tLen = tangential.length();
        if (tLen > 1.0E-9) {
            tangential = tangential.scale(1.0 / tLen);
        }

        Vec3 inward = Vec3.ZERO;
        double rLen = radial.length();
        if (rLen > 1.0E-9) {
            inward = radial.scale(-1.0 / rLen);
        }

        Vec3 dv = tangential.scale(swirlStrength * falloff)
                .add(inward.scale(radialPull * falloff))
                .add(ax.scale(axialLift * falloff));
        data.setVelocity(data.getVelocity().add(dv));
    }
}
