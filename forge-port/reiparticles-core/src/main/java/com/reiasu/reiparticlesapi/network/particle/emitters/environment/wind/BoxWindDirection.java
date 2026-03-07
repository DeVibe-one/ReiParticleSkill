// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.environment.wind;

import com.reiasu.reiparticlesapi.barrages.HitBox;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A wind direction with a box-shaped area of effect.
 * Particles outside the {@link HitBox} (offset from the emitter position)
 * are unaffected.
 * <p>
 * When {@code relative} is {@code true}, the wind direction at each particle
 * position is the vector from the emitter to the particle, scaled by the
 * wind speed expression value.  When {@code false}, the fixed direction
 * vector is used.
 * <p>
 * Forge port note: the wind speed expression is evaluated as a constant double
 * (parsed as constant).
 */
public final class BoxWindDirection implements WindDirection {

    public static final String ID = "box";

    private Vec3 direction;
    private HitBox box;
    private RelativeLocation offset;
    private boolean relative;
    private String windSpeedExpress;
    private ParticleEmitters emitters;

    public BoxWindDirection(Vec3 direction, HitBox box, RelativeLocation offset) {
        this.direction = direction;
        this.box = box;
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

    public HitBox getBox() {
        return box;
    }

    public void setBox(HitBox box) {
        this.box = box;
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
        AABB aabb = box.ofBox(center);
        return aabb.contains(pos);
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
