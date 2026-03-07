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
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllableParticleStateTest {
    @Test
    void shouldConsumePendingTeleportOnce() {
        ControllableParticleState state = new ControllableParticleState(new Vec3(1.0, 2.0, 3.0));
        Vec3 target = new Vec3(-4.0, 6.0, 8.0);

        state.scheduleTeleport(target);

        assertEquals(target, state.consumePendingTeleport());
        assertNull(state.consumePendingTeleport());
    }

    @Test
    void shouldCapturePreviousRotationBeforeApplyingPendingRotation() {
        ControllableParticleState state = new ControllableParticleState(Vec3.ZERO);
        state.setCurrentPitch(10.0f);
        state.setCurrentYaw(20.0f);
        state.setCurrentRoll(30.0f);
        state.setPendingRotation(new Vector3f(40.0f, 50.0f, 60.0f));
        state.setRotationPending(true);

        state.capturePreviousRotation();
        boolean applied = state.applyPendingRotation();

        assertTrue(applied);
        assertEquals(10.0f, state.getPreviewPitch());
        assertEquals(20.0f, state.getPreviewYaw());
        assertEquals(30.0f, state.getPreviewRoll());
        assertEquals(40.0f, state.getCurrentPitch());
        assertEquals(50.0f, state.getCurrentYaw());
        assertEquals(60.0f, state.getCurrentRoll());
        assertFalse(state.isRotationPending());
    }

    @Test
    void shouldMarkRotationPendingWhenSchedulingRotation() {
        ControllableParticleState state = new ControllableParticleState(Vec3.ZERO);
        Vector3f rotation = new Vector3f(1.0f, 2.0f, 3.0f);

        state.scheduleRotation(rotation);

        assertTrue(state.isRotationPending());
        assertEquals(rotation, state.getPendingRotation());
        rotation.set(9.0f, 9.0f, 9.0f);
        assertEquals(new Vector3f(1.0f, 2.0f, 3.0f), state.getPendingRotation());
    }
}
