// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn;

import net.minecraft.world.phys.Vec3;

public record EndRespawnSnapshot(String levelId, Vec3 center, EndRespawnPhase phase, long phaseTick) {
}

