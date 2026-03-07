/*
 * Copyright (C) 2025 Reiasu
 *
 * This file is part of ReiParticlesAPI.
 *
 * ReiParticlesAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * ReiParticlesAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReiParticlesAPI. If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: LGPL-3.0-only
package com.reiasu.reiparticlesapi.particles;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControllableParticleRenderHelperTest {
    @Test
    void shouldResolveRenderPositionInCameraSpace() {
        Vec3 previousPos = new Vec3(2.0, 4.0, 6.0);
        Vec3 currentPos = new Vec3(10.0, 8.0, -2.0);
        Vec3 cameraPos = new Vec3(1.0, 3.0, 5.0);

        Vector3f actual = ControllableParticleRenderHelper.resolveRenderPos(
                (prev, current, delta) -> prev.lerp(current, delta),
                previousPos,
                currentPos,
                cameraPos,
                0.25f);

        assertEquals(3.0f, actual.x, 1.0e-6f);
        assertEquals(2.0f, actual.y, 1.0e-6f);
        assertEquals(-1.0f, actual.z, 1.0e-6f);
    }

    @Test
    void shouldUseFixedRotationForWorldAlignedParticles() {
        Quaternionf actual = ControllableParticleRenderHelper.resolveQuaternion(
                false,
                new Quaternionf().rotateY(0.25f),
                0.5f,
                0.2f,
                0.6f,
                -0.4f,
                0.2f,
                0.1f,
                0.5f);
        Quaternionf expected = new Quaternionf().rotateXYZ(0.4f, -0.1f, 0.3f);

        assertVectorEquals(new Vector3f(0.0f, 0.0f, 1.0f).rotate(expected), new Vector3f(0.0f, 0.0f, 1.0f).rotate(actual));
    }

    @Test
    void shouldAppendRollToCameraFacingRotation() {
        Quaternionf cameraRotation = new Quaternionf().rotateY(0.45f);

        Quaternionf actual = ControllableParticleRenderHelper.resolveQuaternion(
                true,
                new Quaternionf(cameraRotation),
                0.5f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.2f,
                0.6f);
        Quaternionf expected = new Quaternionf(cameraRotation).rotateZ(0.4f);

        assertVectorEquals(new Vector3f(1.0f, 0.0f, 0.0f).rotate(expected), new Vector3f(1.0f, 0.0f, 0.0f).rotate(actual));
    }

    private static void assertVectorEquals(Vector3f expected, Vector3f actual) {
        assertEquals(expected.x, actual.x, 1.0e-6f);
        assertEquals(expected.y, actual.y, 1.0e-6f);
        assertEquals(expected.z, actual.z, 1.0e-6f);
    }
}
