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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DisplayIndexRoutingTest {
    @Test
    void shouldResolveAllSupportedIndexes() {
        assertEquals(DisplayIndexKind.EMITTER, DisplayIndexRouting.plan(0).orElseThrow().kind());
        assertEquals(DisplayIndexKind.STYLE, DisplayIndexRouting.plan(1).orElseThrow().kind());
        assertEquals(DisplayIndexKind.ENTITY, DisplayIndexRouting.plan(2).orElseThrow().kind());
        assertEquals(DisplayIndexKind.DISPLAY, DisplayIndexRouting.plan(3).orElseThrow().kind());
        assertEquals(DisplayIndexKind.GROUP, DisplayIndexRouting.plan(4).orElseThrow().kind());
        assertEquals(DisplayIndexKind.COMPOSITION, DisplayIndexRouting.plan(5).orElseThrow().kind());
        assertEquals(DisplayIndexKind.DISPLAY, DisplayIndexRouting.plan(6).orElseThrow().kind());
    }

    @Test
    void shouldRejectUnsupportedIndex() {
        assertTrue(DisplayIndexRouting.plan(-1).isEmpty());
        assertTrue(DisplayIndexRouting.plan(7).isEmpty());
    }

    @Test
    void shouldReturnProfileForDisplayFamilyIndexes() {
        assertTrue(DisplayIndexRouting.spawnProfile(2).isPresent());
        assertTrue(DisplayIndexRouting.spawnProfile(3).isPresent());
        assertTrue(DisplayIndexRouting.spawnProfile(6).isPresent());
        assertTrue(DisplayIndexRouting.spawnProfile(4).isEmpty());
    }
}
