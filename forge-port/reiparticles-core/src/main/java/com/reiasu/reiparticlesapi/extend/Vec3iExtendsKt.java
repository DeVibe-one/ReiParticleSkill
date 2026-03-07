// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.extend;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

/**
 * Extension utility for {@link Vec3i} providing conversion to {@link Vec3}.
 * Originally a Kotlin extension function, ported as a static utility method.
 */
public final class Vec3iExtendsKt {

    private Vec3iExtendsKt() {
    }

    /**
     * Converts a {@link Vec3i} to a {@link Vec3} (double-precision).
     */
    public static Vec3 asVec3(Vec3i v) {
        return new Vec3(v.getX(), v.getY(), v.getZ());
    }
}
