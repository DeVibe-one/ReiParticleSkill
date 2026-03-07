// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.end.respawn;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DragonRespawnPulseTrackerTest {
    @Test
    void duePulsesAreScheduledFromBeamChanges() {
        DragonRespawnPulseTracker tracker = new DragonRespawnPulseTracker(39);
        UUID crystalId = UUID.randomUUID();
        BlockPos beam = new BlockPos(0, 80, 0);

        tracker.observeCrystal(crystalId, beam, 10L);

        assertEquals(1, tracker.pendingPulseCount());
        assertTrue(tracker.drainDuePulses(48L).isEmpty());

        List<DragonRespawnPulseTracker.PillarPulseTrigger> due = tracker.drainDuePulses(49L);
        assertEquals(1, due.size());
        assertEquals(beam, due.get(0).beamTarget());
        assertEquals(crystalId, due.get(0).crystalId());
    }

    @Test
    void fallbackOnlyOpensWhenNoTrackedBeamsRemain() {
        DragonRespawnPulseTracker tracker = new DragonRespawnPulseTracker(39);
        UUID crystalId = UUID.randomUUID();
        tracker.observeCrystal(crystalId, new BlockPos(1, 90, 1), 0L);

        assertFalse(tracker.shouldFallback());

        tracker.clear();
        assertTrue(tracker.shouldFallback());

        tracker.markPulseTriggered(5L);
        assertFalse(tracker.shouldFallback());

        tracker.clear();
        tracker.removeMissing(Set.of());
        assertTrue(tracker.shouldFallback());
    }
}
