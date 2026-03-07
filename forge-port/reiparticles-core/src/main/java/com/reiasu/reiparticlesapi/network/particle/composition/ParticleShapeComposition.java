// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.composition;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.builder.PointsBuilder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class ParticleShapeComposition extends ParticleComposition {
    private final Vec3 origin;
    private final List<RelativeLocation> points = new ArrayList<>();
    private RelativeLocation axis = new RelativeLocation(0.0, 0.0, 1.0);
    private ServerLevel level;

    public ParticleShapeComposition(Object position) {
        if (position instanceof Vec3 vec3) {
            this.origin = vec3;
        } else {
            this.origin = Vec3.ZERO;
        }
    }

    public ParticleShapeComposition withLevel(ServerLevel level) {
        this.level = level;
        return this;
    }

    public ParticleShapeComposition applyBuilder(PointsBuilder builder, Function<RelativeLocation, CompositionData> mapper) {
        points.clear();
        for (RelativeLocation point : builder.create()) {
            points.add(point.copy());
            mapper.apply(point);
        }
        return this;
    }

    public ParticleShapeComposition applyDisplayAction(Consumer<ParticleShapeComposition> consumer) {
        consumer.accept(this);
        return this;
    }

    public void setAxis(RelativeLocation axis) {
        this.axis = axis.copy();
    }

    public void rotateAsAxis(double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        for (RelativeLocation point : points) {
            double x = point.getX();
            double z = point.getZ();
            point.setX(x * cos - z * sin);
            point.setZ(x * sin + z * cos);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (getCanceled()) {
            return;
        }
        if (level == null) {
            return;
        }
        for (RelativeLocation point : points) {
            level.sendParticles(
                    ParticleTypes.END_ROD,
                    origin.x + point.getX(),
                    origin.y + point.getY(),
                    origin.z + point.getZ(),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    public RelativeLocation axis() {
        return axis.copy();
    }

    @Override
    public Map<CompositionData, RelativeLocation> getParticles() {
        return Collections.emptyMap();
    }

    @Override
    public void onDisplay() {
        // No-op — this class uses its own tick-based rendering
    }
}
