// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters.type;

import com.reiasu.reiparticlesapi.barrages.HitBox;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Emitter shoot type that distributes particles randomly within an
 * axis-aligned {@link HitBox} volume.
 * <p>
 * Each particle's spawn position is a random point inside the box,
 * offset from the emitter origin.  If the emitter's base direction
 * vector is (near-)zero the default direction is randomised; otherwise
 * the base direction is passed through unchanged.
 */
public final class BoxEmittersShootType implements EmittersShootType {

    public static final String ID = "box";

    private final HitBox box;

    public BoxEmittersShootType(HitBox box) {
        this.box = box;
    }

    public HitBox getBox() {
        return box;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public List<Vec3> getPositions(Vec3 origin, int tick, int count) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        List<Vec3> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double x = rng.nextDouble(box.getX1(), box.getX2());
            double y = rng.nextDouble(box.getY1(), box.getY2());
            double z = rng.nextDouble(box.getZ1(), box.getZ2());
            result.add(origin.add(new Vec3(x, y, z)));
        }
        return result;
    }

    @Override
    public Vec3 getDefaultDirection(Vec3 enter, int tick, Vec3 pos, Vec3 origin) {
        if (enter.lengthSqr() < 1.0E-14) {
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            return new Vec3(
                    rng.nextDouble(-1.0, 1.0),
                    rng.nextDouble(-1.0, 1.0),
                    rng.nextDouble(-1.0, 1.0)
            );
        }
        return enter;
    }
}
