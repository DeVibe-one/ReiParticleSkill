// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.command;

import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import net.minecraft.world.phys.Vec3;

/**
 * Drives particles along a procedural 3D flow/vector field.
 * The field is computed from sine/cosine functions of position and time,
 * creating an organic turbulent motion pattern.
 */
public final class ParticleFlowFieldCommand implements ParticleCommand {

    private double amplitude = 0.15;
    private double frequency = 0.25;
    private double timeScale = 0.06;
    private double phaseOffset = 0.0;
    private Vec3 worldOffset = Vec3.ZERO;

    public ParticleFlowFieldCommand() {
    }

    public ParticleFlowFieldCommand(double amplitude, double frequency, double timeScale,
                                    double phaseOffset, Vec3 worldOffset) {
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.timeScale = timeScale;
        this.phaseOffset = phaseOffset;
        this.worldOffset = worldOffset;
    }

    // Fluent setters

    public ParticleFlowFieldCommand amplitude(double v) { this.amplitude = v; return this; }
    public ParticleFlowFieldCommand frequency(double v) { this.frequency = v; return this; }
    public ParticleFlowFieldCommand timeScale(double v) { this.timeScale = v; return this; }
    public ParticleFlowFieldCommand phaseOffset(double v) { this.phaseOffset = v; return this; }
    public ParticleFlowFieldCommand worldOffset(Vec3 v) { this.worldOffset = v; return this; }

    // Standard getters/setters

    public double getAmplitude() { return amplitude; }
    public void setAmplitude(double amplitude) { this.amplitude = amplitude; }
    public double getFrequency() { return frequency; }
    public void setFrequency(double frequency) { this.frequency = frequency; }
    public double getTimeScale() { return timeScale; }
    public void setTimeScale(double timeScale) { this.timeScale = timeScale; }
    public double getPhaseOffset() { return phaseOffset; }
    public void setPhaseOffset(double phaseOffset) { this.phaseOffset = phaseOffset; }
    public Vec3 getWorldOffset() { return worldOffset; }
    public void setWorldOffset(Vec3 worldOffset) { this.worldOffset = worldOffset; }

    @Override
    public void execute(ControllableParticleData data, ControllableParticle particle) {
        Vec3 p = data.getPosition().add(worldOffset);
        double t = data.getAge() * timeScale + phaseOffset;

        double fx = Math.sin((p.y + t) * frequency) + Math.cos((p.z - t) * frequency);
        double fy = Math.sin((p.z + t) * frequency) + Math.cos((p.x + t) * frequency);
        double fz = Math.sin((p.x - t) * frequency) + Math.cos((p.y - t) * frequency);

        double scale = 0.5;
        Vec3 dv = new Vec3(fx * scale, fy * scale, fz * scale).scale(amplitude);
        data.setVelocity(data.getVelocity().add(dv));
    }
}
