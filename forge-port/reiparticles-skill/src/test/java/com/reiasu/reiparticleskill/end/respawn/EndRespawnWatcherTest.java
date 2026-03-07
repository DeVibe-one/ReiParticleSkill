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

class EndRespawnWatcherTest {

    // --- Basic tick-only fallback (no crystal info) ---

    @Test
    void shouldMapSyntheticTicksToExpectedPhases() {
        assertEquals(EndRespawnPhase.START, EndRespawnWatcher.syntheticPhaseForTick(0));
        assertEquals(EndRespawnPhase.START, EndRespawnWatcher.syntheticPhaseForTick(149));

        assertEquals(EndRespawnPhase.SUMMON_PILLARS, EndRespawnWatcher.syntheticPhaseForTick(150));
        assertEquals(EndRespawnPhase.SUMMON_PILLARS, EndRespawnWatcher.syntheticPhaseForTick(649));

        assertEquals(EndRespawnPhase.SUMMONING_DRAGON, EndRespawnWatcher.syntheticPhaseForTick(650));
        assertEquals(EndRespawnPhase.SUMMONING_DRAGON, EndRespawnWatcher.syntheticPhaseForTick(749));

        assertEquals(EndRespawnPhase.BEFORE_END_WAITING, EndRespawnWatcher.syntheticPhaseForTick(750));
        assertEquals(EndRespawnPhase.BEFORE_END_WAITING, EndRespawnWatcher.syntheticPhaseForTick(779));

        assertEquals(EndRespawnPhase.END, EndRespawnWatcher.syntheticPhaseForTick(780));
        assertEquals(EndRespawnPhase.END, EndRespawnWatcher.syntheticPhaseForTick(1200));
    }

    // --- Crystal-count-aware override tests ---

    @Test
    void crystalOverrideShouldKeepSummonPillarsWhenCrystalsAlive() {
        // tick 660 > SUMMON_PILLARS_END(650), tick-only = SUMMONING_DRAGON.
        // But 10 crystals alive + within tolerance(40) => still SUMMON_PILLARS.
        assertEquals(EndRespawnPhase.SUMMON_PILLARS,
                EndRespawnWatcher.syntheticPhaseForTick(660, 10));
    }

    @Test
    void crystalOverrideShouldNotApplyBeyondTolerance() {
        // tick 700 > SUMMON_PILLARS_END + TOLERANCE(690), override exhausted
        assertEquals(EndRespawnPhase.SUMMONING_DRAGON,
                EndRespawnWatcher.syntheticPhaseForTick(700, 10));
    }

    @Test
    void zeroCrystalsShouldJumpToBeforeEndOrEnd() {
        // 0 crystals at tick 200 (past START) => BEFORE_END_WAITING
        assertEquals(EndRespawnPhase.BEFORE_END_WAITING,
                EndRespawnWatcher.syntheticPhaseForTick(200, 0));

        // 0 crystals at tick 830 (past BEFORE_END_END + TOLERANCE=820) => END
        assertEquals(EndRespawnPhase.END,
                EndRespawnWatcher.syntheticPhaseForTick(830, 0));
    }

    @Test
    void negativeCrystalCountShouldFallBackToTickOnly() {
        // crystalCount=-1 means no signal => tick-only fallback
        assertEquals(EndRespawnPhase.SUMMONING_DRAGON,
                EndRespawnWatcher.syntheticPhaseForTick(660, -1));
    }

    // --- Threshold constants ---

    @Test
    void thresholdConstantsShouldBeOrdered() {
        assertTrue(EndRespawnWatcher.PHASE_START_END < EndRespawnWatcher.PHASE_SUMMON_PILLARS_END);
        assertTrue(EndRespawnWatcher.PHASE_SUMMON_PILLARS_END < EndRespawnWatcher.PHASE_SUMMONING_DRAGON_END);
        assertTrue(EndRespawnWatcher.PHASE_SUMMONING_DRAGON_END < EndRespawnWatcher.PHASE_BEFORE_END_END);
    }
}

