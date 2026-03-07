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
package com.reiasu.reiparticleskill.end.respawn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndRespawnPhaseTest {
    @Test
    void shouldMapKnownIdsIgnoringCaseAndSpaces() {
        assertEquals(EndRespawnPhase.START, EndRespawnPhase.fromId("START").orElseThrow());
        assertEquals(EndRespawnPhase.SUMMON_PILLARS, EndRespawnPhase.fromId("  summon_pillars ").orElseThrow());
        assertEquals(EndRespawnPhase.SUMMONING_DRAGON, EndRespawnPhase.fromId("summoning_dragon").orElseThrow());
        assertEquals(EndRespawnPhase.BEFORE_END_WAITING, EndRespawnPhase.fromId("before_end_waiting").orElseThrow());
        assertEquals(EndRespawnPhase.END, EndRespawnPhase.fromId("end").orElseThrow());
    }

    @Test
    void shouldReturnEmptyForUnknownId() {
        assertTrue(EndRespawnPhase.fromId("unknown").isEmpty());
    }

    @Test
    void shouldMapKnownRespawnStageNames() {
        assertEquals(EndRespawnPhase.START, EndRespawnPhase.fromStageName("START").orElseThrow());
        assertEquals(EndRespawnPhase.SUMMON_PILLARS, EndRespawnPhase.fromStageName("PREPARING_TO_SUMMON_PILLARS").orElseThrow());
        assertEquals(EndRespawnPhase.SUMMON_PILLARS, EndRespawnPhase.fromStageName("summoning pillars").orElseThrow());
        assertEquals(EndRespawnPhase.SUMMONING_DRAGON, EndRespawnPhase.fromStageName("SUMMONING_DRAGON").orElseThrow());
        assertEquals(EndRespawnPhase.BEFORE_END_WAITING, EndRespawnPhase.fromStageName("WAITING").orElseThrow());
        assertEquals(EndRespawnPhase.END, EndRespawnPhase.fromStageName("END").orElseThrow());
        assertTrue(EndRespawnPhase.fromStageName("SOMETHING_ELSE").isEmpty());
    }
}
