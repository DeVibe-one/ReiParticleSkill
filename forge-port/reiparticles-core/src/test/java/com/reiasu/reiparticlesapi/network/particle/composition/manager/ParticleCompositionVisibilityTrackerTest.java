// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.composition.manager;

import com.reiasu.reiparticlesapi.config.APIConfig;
import com.reiasu.reiparticlesapi.network.ServerSyncPacketBudget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ParticleCompositionVisibilityTrackerTest {
    @AfterEach
    void cleanup() {
        APIConfig.INSTANCE.setPacketsPerTickLimit(512);
    }

    @Test
    void computeLodIntervalScalesWithViewerDistance() {
        assertEquals(1, ParticleCompositionVisibilityTracker.computeLodInterval(5.0, 100.0));
        assertEquals(3, ParticleCompositionVisibilityTracker.computeLodInterval(30.0, 100.0));
        assertEquals(6, ParticleCompositionVisibilityTracker.computeLodInterval(60.0, 100.0));
        assertEquals(12, ParticleCompositionVisibilityTracker.computeLodInterval(90.0, 100.0));
    }

    @Test
    void shouldShardPlayerProcessingByTick() {
        assertTrue(ParticleCompositionVisibilityTracker.shouldProcessPlayerIndex(0, 0));
        assertFalse(ParticleCompositionVisibilityTracker.shouldProcessPlayerIndex(1, 0));
        assertTrue(ParticleCompositionVisibilityTracker.shouldProcessPlayerIndex(1, 1));
        assertFalse(ParticleCompositionVisibilityTracker.shouldProcessPlayerIndex(3, 1));
    }

    @Test
    void shouldOnlyTrackCompositionAfterSuccessfulSend() {
        Set<UUID> visible = new HashSet<>();
        UUID compositionId = UUID.randomUUID();

        assertFalse(ParticleCompositionVisibilityTracker.markVisibleAfterSuccessfulSend(visible, compositionId, () -> false));
        assertFalse(visible.contains(compositionId));
        assertTrue(ParticleCompositionVisibilityTracker.markVisibleAfterSuccessfulSend(visible, compositionId, () -> true));
        assertTrue(visible.contains(compositionId));

        AtomicInteger sendAttempts = new AtomicInteger();
        assertFalse(ParticleCompositionVisibilityTracker.markVisibleAfterSuccessfulSend(visible, compositionId, () -> {
            sendAttempts.incrementAndGet();
            return true;
        }));
        assertEquals(0, sendAttempts.get());
    }

    @Test
    void shouldRespectSharedPacketBudgetWhenTrackingNewComposition() {
        Set<UUID> visible = new HashSet<>();
        APIConfig.INSTANCE.setPacketsPerTickLimit(16);
        ServerSyncPacketBudget.beginServerTick(System.nanoTime());

        for (int i = 0; i < 16; i++) {
            UUID compositionId = UUID.randomUUID();
            assertTrue(ParticleCompositionVisibilityTracker.markVisibleAfterSuccessfulSend(
                    visible,
                    compositionId,
                    ServerSyncPacketBudget::tryAcquire));
        }

        UUID throttledCompositionId = UUID.randomUUID();
        assertFalse(ParticleCompositionVisibilityTracker.markVisibleAfterSuccessfulSend(
                visible,
                throttledCompositionId,
                ServerSyncPacketBudget::tryAcquire));
        assertFalse(visible.contains(throttledCompositionId));
        assertEquals(16, visible.size());
    }
}
