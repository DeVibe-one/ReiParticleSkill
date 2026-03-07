// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn.runtime.emitter;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public interface RespawnEmitter {
    int tick(ServerLevel level, Vec3 center);

    boolean done();
}
