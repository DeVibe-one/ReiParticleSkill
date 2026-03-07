// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.builder;

import com.reiasu.reiparticlesapi.utils.GraphMathHelper;
import com.reiasu.reiparticlesapi.utils.PhysicsUtil;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SharedGeometryMigrationTest {
    @Test
    void rotateAroundYPreservesSkillFormationRotation() {
        List<RelativeLocation> rotated = new PointsBuilder()
                .addCircle(2.0, 4)
                .rotateAroundY(Math.PI / 2.0)
                .create();

        assertEquals(0.0, rotated.get(0).getX(), 1.0E-6);
        assertEquals(-2.0, rotated.get(0).getZ(), 1.0E-6);
    }

    @Test
    void sharedPhysicsHelpersStillSupportSkillSteeringMath() {
        RelativeLocation velocity = new RelativeLocation(4.0, 0.0, 0.0);
        RelativeLocation desired = new RelativeLocation(0.0, 4.0, 0.0);

        PhysicsUtil.steer(velocity, desired, 0.25);
        PhysicsUtil.capSpeed(velocity, 3.0);

        assertEquals(3.0, velocity.length(), 1.0E-6);
        assertTrue(velocity.getY() > 0.0);
    }

    @Test
    void sharedGraphMathHelperRetainsPreviewFadeInterpolation() {
        float alpha = GraphMathHelper.lerp(0.25f, 1.0f, 0.0f);
        assertEquals(0.75f, alpha, 1.0E-6f);
    }
}
