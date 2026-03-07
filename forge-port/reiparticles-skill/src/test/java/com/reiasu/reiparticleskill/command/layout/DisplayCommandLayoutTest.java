/*
 * Copyright (C) 2025 Reiasu
 *
 * This file is part of ReiParticleSkill.
 *
 * ReiParticleSkill is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * ReiParticleSkill is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReiParticleSkill. If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: LGPL-3.0-only
package com.reiasu.reiparticleskill.command.layout;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DisplayCommandLayoutTest {
    @Test
    void shouldBuildFacingUpOrientation() {
        DisplayOrientation orientation = DisplayCommandLayout.computeOrientation(new RelativeLocation(0.0, 1.0, 0.0));
        assertEquals(0.0f, orientation.yawDegrees(), 0.0001f);
        assertEquals(-90.0f, orientation.pitchDegrees(), 0.0001f);
    }

    @Test
    void shouldProvideProfilesForDisplayIndexes() {
        assertTrue(DisplayCommandLayout.profileForIndex(2).isPresent());
        assertTrue(DisplayCommandLayout.profileForIndex(3).isPresent());
        assertTrue(DisplayCommandLayout.profileForIndex(6).isPresent());
        assertTrue(DisplayCommandLayout.profileForIndex(0).isEmpty());
    }

    @Test
    void shouldUseExpectedScaleAndSpeed() {
        DisplaySpawnProfile index2 = DisplayCommandLayout.profileForIndex(2).orElseThrow();
        DisplaySpawnProfile index6 = DisplayCommandLayout.profileForIndex(6).orElseThrow();

        assertEquals(1.0f, index2.targetScale());
        assertEquals(1.0f, index2.scaledSpeed());

        assertEquals(10.0f, index6.targetScale());
        assertEquals(0.5f, index6.scaledSpeed());
    }
}

