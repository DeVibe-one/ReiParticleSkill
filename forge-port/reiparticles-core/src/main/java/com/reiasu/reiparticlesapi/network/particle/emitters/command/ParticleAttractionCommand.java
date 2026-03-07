// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.command;

import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import com.reiasu.reiparticlesapi.utils.GraphMathHelper;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Applies an attraction force toward a target position. Particles accelerate
 * toward the target with configurable strength, range, and inverse-power falloff.
 */
public final class ParticleAttractionCommand implements ParticleCommand {

    private Supplier<Vec3> target = () -> Vec3.ZERO;
    private double strength = 0.8;
    private double range = 8.0;
    private double falloffPower = 2.0;
    private double minDistance = 0.25;

    public ParticleAttractionCommand() {
    }

    public ParticleAttractionCommand(Supplier<Vec3> target, double strength, double range,
                                     double falloffPower, double minDistance) {
        this.target = target;
        this.strength = strength;
        this.range = range;
        this.falloffPower = falloffPower;
        this.minDistance = minDistance;
    }

    // Fluent setters

    public ParticleAttractionCommand target(Supplier<Vec3> v) { this.target = v; return this; }
    public ParticleAttractionCommand strength(double v) { this.strength = v; return this; }
    public ParticleAttractionCommand range(double v) { this.range = v; return this; }
    public ParticleAttractionCommand falloffPower(double v) { this.falloffPower = v; return this; }
    public ParticleAttractionCommand minDistance(double v) { this.minDistance = v; return this; }

    // Standard getters/setters

    public Supplier<Vec3> getTarget() { return target; }
    public void setTarget(Supplier<Vec3> target) { this.target = target; }
    public double getStrength() { return strength; }
    public void setStrength(double strength) { this.strength = strength; }
    public double getRange() { return range; }
    public void setRange(double range) { this.range = range; }
    public double getFalloffPower() { return falloffPower; }
    public void setFalloffPower(double falloffPower) { this.falloffPower = falloffPower; }
    public double getMinDistance() { return minDistance; }
    public void setMinDistance(double minDistance) { this.minDistance = minDistance; }

    @Override
    public void execute(ControllableParticleData data, ControllableParticle particle) {
        Vec3 pos = data.getPosition();
        Vec3 dir = target.get().subtract(pos);
        double d = dir.length();
        if (d < 1.0E-9) {
            return;
        }
        d = Math.max(d, minDistance);
        double falloff = GraphMathHelper.inversePowerFalloff(d, range, falloffPower);
        Vec3 dv = dir.normalize().scale(strength * falloff);
        data.setVelocity(data.getVelocity().add(dv));
    }
}
