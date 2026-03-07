// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.extend;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Extension utility for {@link BlockPos} providing conversion from {@link Vec3}.
 * Originally a Kotlin extension function, ported as a static utility method.
 */
public final class BlockPosExtendsKt {

    private BlockPosExtendsKt() {
    }

    /**
     * Returns the {@link BlockPos} containing the given {@link Vec3} position
     * (equivalent to flooring each component).
     */
    public static BlockPos ofFloored(Vec3 vec) {
        return BlockPos.containing(vec.x, vec.y, vec.z);
    }
}
