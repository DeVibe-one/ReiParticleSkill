// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.util;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerParticleUtilTest {
    @Test
    void negativeRangeShouldNeverMatch() {
        assertFalse(ServerParticleUtil.isInRange(Vec3.ZERO, Vec3.ZERO, -1.0));
        assertFalse(ServerParticleUtil.isInRange(Vec3.ZERO, new Vec3(1.0, 0.0, 0.0), -1.0));
    }

    @Test
    void zeroRangeShouldOnlyMatchExactPosition() {
        assertTrue(ServerParticleUtil.isInRange(Vec3.ZERO, Vec3.ZERO, 0.0));
        assertFalse(ServerParticleUtil.isInRange(Vec3.ZERO, new Vec3(0.001, 0.0, 0.0), 0.0));
    }

    @Test
    void positiveRangeUsesSquaredDistance() {
        assertTrue(ServerParticleUtil.isInRange(Vec3.ZERO, new Vec3(3.0, 4.0, 0.0), 5.0));
        assertFalse(ServerParticleUtil.isInRange(Vec3.ZERO, new Vec3(3.0, 4.1, 0.0), 5.0));
    }
}
