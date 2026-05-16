// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.particles.preview.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangingCompositionTest {
    @Test
    void shouldEmitEndRodBurstEveryFiveTicks() {
        assertFalse(ChangingComposition.shouldEmitEndRodBurst(1));
        assertTrue(ChangingComposition.shouldEmitEndRodBurst(5));
        assertFalse(ChangingComposition.shouldEmitEndRodBurst(6));
        assertTrue(ChangingComposition.shouldEmitEndRodBurst(10));
    }

    @Test
    void shouldEmitEnchantBurstEveryEightTicks() {
        assertFalse(ChangingComposition.shouldEmitEnchantBurst(7));
        assertTrue(ChangingComposition.shouldEmitEnchantBurst(8));
        assertFalse(ChangingComposition.shouldEmitEnchantBurst(9));
        assertTrue(ChangingComposition.shouldEmitEnchantBurst(16));
    }
}
