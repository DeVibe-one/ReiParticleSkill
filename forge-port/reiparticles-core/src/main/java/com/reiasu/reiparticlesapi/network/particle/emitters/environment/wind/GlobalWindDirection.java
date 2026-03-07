// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.environment.wind;

import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import net.minecraft.world.phys.Vec3;

/**
 * A wind direction that applies uniformly across all space (infinite range).
 * <p>
 * When {@code relative} is true, the wind vector is computed as the direction
 * from the emitter to the particle, scaled by {@code windSpeedExpress}.
 * When false, the fixed {@code direction} vector is returned directly.
 */
public final class GlobalWindDirection implements WindDirection {

    public static final String ID = "global";

    private Vec3 direction;
    private boolean relative;
    private String windSpeedExpress;
    private ParticleEmitters emitters;

    public GlobalWindDirection(Vec3 direction) {
        this.direction = direction;
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
        return true; // global — always in range
    }

    private static double parseSpeed(String express, double length) {
        try {
            return new com.reiasu.reiparticlesapi.utils.math.ExpressionEvaluator(express)
                    .with("l", length)
                    .evaluate();
        } catch (Exception e) {
            return 1.0;
        }
    }
}
