// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class APIConfigTest {
    @AfterEach
    void cleanup() {
        APIConfig.INSTANCE.setEnabledParticleCountInject(true);
        APIConfig.INSTANCE.setEnabledParticleAsync(true);
        APIConfig.INSTANCE.setCalculateThreadCount(4);
        APIConfig.INSTANCE.setParticleCountLimit(131072);
        APIConfig.INSTANCE.setPacketsPerTickLimit(512);
        APIConfig.INSTANCE.setMaxEmitterVisibleRange(256);
    }

    @Test
    void deprecatedCompatibilityAccessorsShouldRemainAvailable() {
        APIConfig.INSTANCE.setEnabledParticleCountInject(false);
        APIConfig.INSTANCE.setEnabledParticleAsync(false);
        APIConfig.INSTANCE.setCalculateThreadCount(8);

        assertFalse(APIConfig.INSTANCE.isEnabledParticleCountInject());
        assertFalse(APIConfig.INSTANCE.isEnabledParticleAsync());
        assertEquals(8, APIConfig.INSTANCE.getCalculateThreadCount());

        APIConfig.INSTANCE.setEnabledParticleCountInject(true);
        APIConfig.INSTANCE.setEnabledParticleAsync(true);
        APIConfig.INSTANCE.setCalculateThreadCount(2);

        assertTrue(APIConfig.INSTANCE.isEnabledParticleCountInject());
        assertTrue(APIConfig.INSTANCE.isEnabledParticleAsync());
        assertEquals(2, APIConfig.INSTANCE.getCalculateThreadCount());
    }
}
