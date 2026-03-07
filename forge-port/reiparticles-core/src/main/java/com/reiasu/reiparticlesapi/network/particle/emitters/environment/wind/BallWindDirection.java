// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.environment.wind;

import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.world.phys.Vec3;

/**
 * A wind direction with a spherical area of effect.
 * Particles outside the {@code radius} from the emitter + offset are unaffected.
 */
public final class BallWindDirection implements WindDirection {

    public static final String ID = "ball";

    private Vec3 direction;
    private double radius;
    private RelativeLocation offset;
    private boolean relative;
    private String windSpeedExpress;
    private ParticleEmitters emitters;

    public BallWindDirection(Vec3 direction, double radius, RelativeLocation offset) {
        this.direction = direction;
        this.radius = radius;
        this.offset = offset;
        this.windSpeedExpress = "1";
    }

    @Override
    public Vec3 getDirection() {
        return direction;
    }

    @Override
    public void setDirection(Vec3 direction) {
        this.direction = direction;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public RelativeLocation getOffset() {
        return offset;
    }

    public void setOffset(RelativeLocation offset) {
        this.offset = offset;
    }

    @Override
    public boolean getRelative() {
        return relative;
    }

    @Override
    public void setRelative(boolean relative) {
        this.relative = relative;
    }

    @Override
    public String getWindSpeedExpress() {
        return windSpeedExpress;
    }

    @Override
    public void setWindSpeedExpress(String express) {
        this.windSpeedExpress = express;
    }

    @Override
    public WindDirection loadEmitters(ParticleEmitters emitters) {
        this.emitters = emitters;
        return this;
    }

    @Override
    public boolean hasLoadedEmitters() {
        return emitters != null;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public Vec3 getWind(Vec3 particlePos) {
        if (relative) {
            if (emitters == null || emitters.position() == null) {
                return direction;
            }
            Vec3 pos = emitters.position();
            Vec3 dir = particlePos.subtract(pos);
            double speed = parseSpeed(windSpeedExpress, dir.length());
            Vec3 normalized = dir.normalize();
            return normalized.scale(speed);
        }
        return direction;
    }

    @Override
    public boolean inRange(Vec3 pos) {
        if (emitters == null) {
            return false;
        }
        Vec3 center = emitters.position().add(offset.toVector());
        return center.subtract(pos).length() <= radius;
    }

    private static double parseSpeed(String express, double length) {
        try {
            return Double.parseDouble(express);
        } catch (NumberFormatException e) {
            return 1.0;
        }
    }
}
