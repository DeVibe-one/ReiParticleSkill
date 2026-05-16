// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DisplayableEmitterDataTest {
    @Test
    void shouldRepresentStatelessServerOnlyEmitterData() {
        DisplayableEmitterData data = new DisplayableEmitterData();

        assertSame(data, data.clone());
        assertNull(data.createDisplayer());
    }
}
