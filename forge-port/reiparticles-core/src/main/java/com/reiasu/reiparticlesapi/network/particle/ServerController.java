// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public interface ServerController<T> {
    default void tick() {
    }

    /**
     * Spawn this controller's visual representation in the world.
     * Override in each implementation to dispatch to the appropriate manager.
     */
    default void spawnInWorld(ServerLevel world, Vec3 pos) {
    }

    default boolean getCanceled() {
        return false;
    }

    default void teleportTo(net.minecraft.world.phys.Vec3 pos) {
    }

    default void cancel() {
    }
}

