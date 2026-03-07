// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.command;

import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.particles.ControllableParticle;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Applies spatial distortion/deformation effects to particles around a center
 * position. Uses procedural noise to push particles along radial, axial, and
 * tangential directions with configurable intensity and life-curve attenuation.
 */
public final class ParticleDistortionCommand implements ParticleCommand {

    private Supplier<Vec3> center = () -> Vec3.ZERO;
    private Vec3 axis = new Vec3(0.0, 1.0, 0.0);
    private double radius = 3.0;
    private double radialStrength = 0.35;
    private double axialStrength = 0.25;
    private double tangentialStrength = 0.0;
    private double frequency = 0.25;
    private double timeScale = 0.1;
    private double phaseOffset = 0.0;
    private double followStrength = 0.35;
    private double maxStep = 0.6;
    private double baseAxial = 0.0;
    private int seedOffset = 0;
    private boolean useLifeCurve = false;

    public ParticleDistortionCommand() {
    }

    public ParticleDistortionCommand(Supplier<Vec3> center, Vec3 axis, double radius,
                                     double radialStrength, double axialStrength,
                                     double tangentialStrength, double frequency,
                                     double timeScale, double phaseOffset,
                                     double followStrength, double maxStep,
                                     double baseAxial, int seedOffset, boolean useLifeCurve) {
        this.center = center;
        this.axis = axis;
        this.radius = radius;
        this.radialStrength = radialStrength;
        this.axialStrength = axialStrength;
        this.tangentialStrength = tangentialStrength;
        this.frequency = frequency;
        this.timeScale = timeScale;
        this.phaseOffset = phaseOffset;
        this.followStrength = followStrength;
        this.maxStep = maxStep;
        this.baseAxial = baseAxial;
        this.seedOffset = seedOffset;
        this.useLifeCurve = useLifeCurve;
    }

    // Fluent setters

    public ParticleDistortionCommand center(Supplier<Vec3> v) { this.center = v; return this; }
    public ParticleDistortionCommand axis(Vec3 v) { this.axis = v; return this; }
    public ParticleDistortionCommand radius(double v) { this.radius = v; return this; }
    public ParticleDistortionCommand radialStrength(double v) { this.radialStrength = v; return this; }
    public ParticleDistortionCommand axialStrength(double v) { this.axialStrength = v; return this; }
    public ParticleDistortionCommand tangentialStrength(double v) { this.tangentialStrength = v; return this; }
    public ParticleDistortionCommand frequency(double v) { this.frequency = v; return this; }
    public ParticleDistortionCommand timeScale(double v) { this.timeScale = v; return this; }
    public ParticleDistortionCommand phaseOffset(double v) { this.phaseOffset = v; return this; }
    public ParticleDistortionCommand followStrength(double v) { this.followStrength = v; return this; }
    public ParticleDistortionCommand maxStep(double v) { this.maxStep = v; return this; }
    public ParticleDistortionCommand baseAxial(double v) { this.baseAxial = v; return this; }
    public ParticleDistortionCommand seedOffset(int v) { this.seedOffset = v; return this; }
    public ParticleDistortionCommand useLifeCurve(boolean v) { this.useLifeCurve = v; return this; }

    // Standard getters/setters

    public Supplier<Vec3> getCenter() { return center; }
    public void setCenter(Supplier<Vec3> center) { this.center = center; }
    public Vec3 getAxis() { return axis; }
    public void setAxis(Vec3 axis) { this.axis = axis; }
    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }
    public double getRadialStrength() { return radialStrength; }
    public void setRadialStrength(double radialStrength) { this.radialStrength = radialStrength; }
    public double getAxialStrength() { return axialStrength; }
    public void setAxialStrength(double axialStrength) { this.axialStrength = axialStrength; }
    public double getTangentialStrength() { return tangentialStrength; }
    public void setTangentialStrength(double tangentialStrength) { this.tangentialStrength = tangentialStrength; }
    public double getFrequency() { return frequency; }
    public void setFrequency(double frequency) { this.frequency = frequency; }
    public double getTimeScale() { return timeScale; }
    public void setTimeScale(double timeScale) { this.timeScale = timeScale; }
    public double getPhaseOffset() { return phaseOffset; }
    public void setPhaseOffset(double phaseOffset) { this.phaseOffset = phaseOffset; }
    public double getFollowStrength() { return followStrength; }
    public void setFollowStrength(double followStrength) { this.followStrength = followStrength; }
    public double getMaxStep() { return maxStep; }
    public void setMaxStep(double maxStep) { this.maxStep = maxStep; }
    public double getBaseAxial() { return baseAxial; }
    public void setBaseAxial(double baseAxial) { this.baseAxial = baseAxial; }
    public int getSeedOffset() { return seedOffset; }
    public void setSeedOffset(int seedOffset) { this.seedOffset = seedOffset; }
    public boolean isUseLifeCurve() { return useLifeCurve; }
    public void setUseLifeCurve(boolean useLifeCurve) { this.useLifeCurve = useLifeCurve; }

    @Override
    public void execute(ControllableParticleData data, ControllableParticle particle) {
        Vec3 centerPos = center.get();
        Vec3 ax = safeNormalize(axis);
        Vec3 pos = data.getPosition();

        Vec3 r = pos.subtract(centerPos);
        Vec3 axialComp = ax.scale(r.dot(ax));
        Vec3 radial = r.subtract(axialComp);
        double radialLen = radial.length();

        if (radialLen < 1.0E-9) {
            radial = anyPerp(ax);
            radialLen = 1.0;
        } else {
            radial = radial.scale(1.0 / radialLen);
        }

        Vec3 tangent = ax.cross(radial);
        double tLen = tangent.length();
        tangent = tLen < 1.0E-9 ? anyPerp(ax) : tangent.scale(1.0 / tLen);

        int age = data.getAge();
        int maxAge = data.getParticleMaxAge();
        double lifeT = maxAge > 0 ? (double) age / maxAge : 0.0;
        double lifeMul = useLifeCurve ? Math.max(0.0, Math.min(1.0, 1.0 - lifeT)) : 1.0;

        double time = age * timeScale + phaseOffset;
        Vec3 local = pos.subtract(centerPos);
        Vec3 p = local.scale(frequency).add(time, time * 0.7, time * 1.3);

        int seed = data.getUuid().hashCode() + seedOffset;
        double nr = noise(p, seed + 11);
        double na = noise(p, seed + 23);
        double nt = noise(p, seed + 37);

        double targetRadius = Math.max(0.0, radius + nr * radialStrength * lifeMul);
        double targetAxial = baseAxial + na * axialStrength * lifeMul;
        double targetTangential = nt * tangentialStrength * lifeMul;

        Vec3 targetPos = centerPos
                .add(ax.scale(targetAxial))
                .add(radial.scale(targetRadius))
                .add(tangent.scale(targetTangential));

        Vec3 dv = targetPos.subtract(pos).scale(followStrength);
        double dvLen = dv.length();
        if (maxStep > 0.0 && dvLen > maxStep) {
            dv = dv.scale(maxStep / dvLen);
        }

        data.setVelocity(data.getVelocity().add(dv));
    }

    // ---- Helper methods ----

    private static Vec3 safeNormalize(Vec3 v) {
        double len = v.length();
        if (len < 1.0E-9) {
            return new Vec3(0.0, 1.0, 0.0);
        }
        return v.scale(1.0 / len);
    }

    private static Vec3 anyPerp(Vec3 axis) {
        Vec3 ref = Math.abs(axis.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 perp = axis.cross(ref);
        double len = perp.length();
        if (len < 1.0E-9) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return perp.scale(1.0 / len);
    }

    private static double noise(Vec3 p, int seed) {
        return valueNoise3(p, seed) * 2.0 - 1.0;
    }

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
}
