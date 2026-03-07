// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.display.group.layout;

import com.reiasu.reiparticlesapi.utils.builder.PointsBuilder;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;

import java.util.List;

public final class SimpleSwordFormationLayout {
    private RelativeLocation direction;
    private final double rotationSpeed;
    private int age;
    private double radius;
    private int count;
    private double phase;

    public SimpleSwordFormationLayout(RelativeLocation direction, double rotationSpeed) {
        this.direction = direction.copy();
        this.rotationSpeed = rotationSpeed;
        this.radius = 8.0;
        this.count = 12;
        this.phase = 0.0;
    }

    public RelativeLocation getDirection() {
        return direction.copy();
    }

    public void setDirection(RelativeLocation direction) {
        this.direction = direction.copy();
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getAge() {
        return age;
    }

    public void tick() {
        age++;
        phase += rotationSpeed;
    }

    public List<RelativeLocation> currentOffsets() {
        return new PointsBuilder()
                .addCircle(radius, count)
                .rotateAroundY(phase)
                .create();
    }

    public static SimpleSwordFormationLayout fromSpec(RelativeLocation direction, FormationLayerSpec spec) {
        SimpleSwordFormationLayout layout = new SimpleSwordFormationLayout(direction, spec.rotationSpeed());
        layout.setRadius(spec.radius());
        layout.setCount(spec.count());
        return layout;
    }
}


