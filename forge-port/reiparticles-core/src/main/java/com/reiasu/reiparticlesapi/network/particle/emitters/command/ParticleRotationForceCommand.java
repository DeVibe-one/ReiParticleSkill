// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.command;

import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import com.reiasu.reiparticlesapi.utils.GraphMathHelper;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Applies a rotational (tangential) force to particles around a center axis.
 * The force direction is the cross product of the axis and the radial vector,
 * attenuated by inverse-power distance falloff.
 */
public final class ParticleRotationForceCommand implements ParticleCommand {

    private Supplier<Vec3> center = () -> Vec3.ZERO;
    private Vec3 axis = new Vec3(0.0, 1.0, 0.0);
    private double strength = 0.35;
    private double range = 8.0;
    private double falloffPower = 2.0;

    public ParticleRotationForceCommand() {
    }

    public ParticleRotationForceCommand(Supplier<Vec3> center, Vec3 axis,
                                        double strength, double range, double falloffPower) {
        this.center = center;
        this.axis = axis;
        this.strength = strength;
        this.range = range;
        this.falloffPower = falloffPower;
    }

    // Fluent setters

    public ParticleRotationForceCommand center(Supplier<Vec3> v) { this.center = v; return this; }
    public ParticleRotationForceCommand axis(Vec3 v) { this.axis = v; return this; }
    public ParticleRotationForceCommand strength(double v) { this.strength = v; return this; }
    public ParticleRotationForceCommand range(double v) { this.range = v; return this; }
    public ParticleRotationForceCommand falloffPower(double v) { this.falloffPower = v; return this; }

    // Standard getters/setters

    public Supplier<Vec3> getCenter() { return center; }
    public void setCenter(Supplier<Vec3> center) { this.center = center; }
    public Vec3 getAxis() { return axis; }
    public void setAxis(Vec3 axis) { this.axis = axis; }
    public double getStrength() { return strength; }
    public void setStrength(double strength) { this.strength = strength; }
    public double getRange() { return range; }
    public void setRange(double range) { this.range = range; }
    public double getFalloffPower() { return falloffPower; }
    public void setFalloffPower(double falloffPower) { this.falloffPower = falloffPower; }

    @Override
    public void execute(ControllableParticleData data, ControllableParticle particle) {
        Vec3 pos = data.getPosition();
        Vec3 r = pos.subtract(center.get());
        double dist = r.length();
        if (dist < 1.0E-9) {
            return;
        }

        Vec3 ax = axis.normalize();
        Vec3 t = ax.cross(r);
        double tLen = t.length();
        if (tLen < 1.0E-9) {
            return;
        }
        t = t.scale(1.0 / tLen);

        double falloff = GraphMathHelper.inversePowerFalloff(dist, range, falloffPower);
        Vec3 dv = t.scale(strength * falloff);
        data.setVelocity(data.getVelocity().add(dv));
    }
}
