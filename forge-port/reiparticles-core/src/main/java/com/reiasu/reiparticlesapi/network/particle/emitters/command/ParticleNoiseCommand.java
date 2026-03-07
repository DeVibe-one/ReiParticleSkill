// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.command;

import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import net.minecraft.world.phys.Vec3;

/**
 * Adds procedural noise-based displacement to particle movement.
 * Uses a built-in value noise function (no external library) to create
 * organic, non-repeating motion patterns.
 */
public final class ParticleNoiseCommand implements ParticleCommand {

    private double strength = 0.03;
    private double frequency = 0.15;
    private double speed = 0.12;
    private double affectY = 1.0;
    private double clampSpeed = 0.8;
    private boolean useLifeCurve = true;

    public ParticleNoiseCommand() {
    }

    public ParticleNoiseCommand(double strength, double frequency, double speed,
                                double affectY, double clampSpeed, boolean useLifeCurve) {
        this.strength = strength;
        this.frequency = frequency;
        this.speed = speed;
        this.affectY = affectY;
        this.clampSpeed = clampSpeed;
        this.useLifeCurve = useLifeCurve;
    }

    // Fluent setters

    public ParticleNoiseCommand strength(double v) { this.strength = v; return this; }
    public ParticleNoiseCommand frequency(double v) { this.frequency = v; return this; }
    public ParticleNoiseCommand speed(double v) { this.speed = v; return this; }
    public ParticleNoiseCommand affectY(double v) { this.affectY = v; return this; }
    public ParticleNoiseCommand clampSpeed(double v) { this.clampSpeed = v; return this; }
    public ParticleNoiseCommand useLifeCurve(boolean v) { this.useLifeCurve = v; return this; }

    // Standard getters/setters

    public double getStrength() { return strength; }
    public void setStrength(double strength) { this.strength = strength; }
    public double getFrequency() { return frequency; }
    public void setFrequency(double frequency) { this.frequency = frequency; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public double getAffectY() { return affectY; }
    public void setAffectY(double affectY) { this.affectY = affectY; }
    public double getClampSpeed() { return clampSpeed; }
    public void setClampSpeed(double clampSpeed) { this.clampSpeed = clampSpeed; }
    public boolean isUseLifeCurve() { return useLifeCurve; }
    public void setUseLifeCurve(boolean useLifeCurve) { this.useLifeCurve = useLifeCurve; }

    @Override
    public void execute(ControllableParticleData data, ControllableParticle particle) {
        int age = data.getAge();
        int maxAge = data.getParticleMaxAge();
        double t = maxAge > 0 ? (double) age / maxAge : 0.0;

        int seed = data.getUuid().hashCode();
        double time = age * speed;

        Vec3 pos = data.getPosition();
        Vec3 p = new Vec3(pos.x * frequency, pos.y * frequency, pos.z * frequency)
                .add(time, time * 0.7, time * 1.3);

        double amp = strength;
        if (useLifeCurve) {
            amp *= (1.0 - t);
        }

        Vec3 n = noiseVec3(p, seed);
        Vec3 dv = new Vec3(n.x * amp, n.y * affectY * amp, n.z * amp);

        Vec3 v = data.getVelocity().add(dv);
        double sp2 = v.lengthSqr();
        double max2 = clampSpeed * clampSpeed;
        if (sp2 > max2) {
            v = v.normalize().scale(clampSpeed);
        }

        data.setVelocity(v);
    }

    // ---- Value noise implementation (matches original Fabric code) ----

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double hash3(int ix, int iy, int iz, int seed) {
        int n = ix * 374761393 + iy * 668265263 + iz * Integer.MAX_VALUE + seed * 374761;
        n = (n ^ (n >>> 13)) * 1274126177;
        n ^= (n >>> 16);
        return (double) (n & Integer.MAX_VALUE) / 2_147_483_647.0;
    }

    private static double valueNoise3(Vec3 p, int seed) {
        int x0 = (int) Math.floor(p.x);
        int y0 = (int) Math.floor(p.y);
        int z0 = (int) Math.floor(p.z);
        double fx = p.x - x0;
        double fy = p.y - y0;
        double fz = p.z - z0;
        double u = fade(fx);
        double v = fade(fy);
        double w = fade(fz);

        double n000 = hash3(x0, y0, z0, seed);
        double n100 = hash3(x0 + 1, y0, z0, seed);
        double n010 = hash3(x0, y0 + 1, z0, seed);
        double n110 = hash3(x0 + 1, y0 + 1, z0, seed);
        double n001 = hash3(x0, y0, z0 + 1, seed);
        double n101 = hash3(x0 + 1, y0, z0 + 1, seed);
        double n011 = hash3(x0, y0 + 1, z0 + 1, seed);
        double n111 = hash3(x0 + 1, y0 + 1, z0 + 1, seed);

        double nx00 = lerp(n000, n100, u);
        double nx10 = lerp(n010, n110, u);
        double nx01 = lerp(n001, n101, u);
        double nx11 = lerp(n011, n111, u);
        double nxy0 = lerp(nx00, nx10, v);
        double nxy1 = lerp(nx01, nx11, v);
        return lerp(nxy0, nxy1, w);
    }

    private static Vec3 noiseVec3(Vec3 pos, int seed) {
        double nx = valueNoise3(pos, seed + 11) * 2.0 - 1.0;
        double ny = valueNoise3(pos, seed + 23) * 2.0 - 1.0;
        double nz = valueNoise3(pos, seed + 37) * 2.0 - 1.0;
        Vec3 v = new Vec3(nx, ny, nz);
        if (v.lengthSqr() < 1.0E-8) {
            return Vec3.ZERO;
        }
        return v.normalize();
    }
}
