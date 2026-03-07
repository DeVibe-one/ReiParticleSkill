// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.type;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Shoot type that distributes particles along a line defined by a direction vector.
 * Each successive particle is offset by {@code step} blocks along the normalized direction.
 */
public final class LineEmittersShootType implements EmittersShootType {

    public static final String ID = "line";

    private final Vec3 dir;
    private final double step;

    public LineEmittersShootType(Vec3 dir, double step) {
        this.dir = dir;
        this.step = step;
    }

    public Vec3 getDir() {
        return dir;
    }

    public double getStep() {
        return step;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public List<Vec3> getPositions(Vec3 origin, int tick, int count) {
        List<Vec3> result = new ArrayList<>(count);
        Vec3 normalizedDir = dir.normalize();
        for (int i = 0; i < count; i++) {
            result.add(origin.add(normalizedDir.scale(i * step)));
        }
        return result;
    }

    @Override
    public Vec3 getDefaultDirection(Vec3 enter, int tick, Vec3 pos, Vec3 origin) {
        return enter;
    }
}
