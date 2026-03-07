// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.config;

/**
 * Runtime configuration values consumed by core logic.
 * Forge-specific config specs live in the runtime module and update this holder.
 */
public final class APIConfig {
    public static final APIConfig INSTANCE = new APIConfig();

    private volatile boolean enabledParticleCountInject = true;
    private volatile boolean enabledParticleAsync = true;
    private volatile int particleCountLimit = 131072;
    private volatile int calculateThreadCount = 4;
    private volatile int packetsPerTickLimit = 512;
    private volatile int maxEmitterVisibleRange = 256;

    private APIConfig() {
    }

    public boolean isEnabledParticleCountInject() {
        return enabledParticleCountInject;
    }

    public void setEnabledParticleCountInject(boolean enabledParticleCountInject) {
        this.enabledParticleCountInject = enabledParticleCountInject;
    }

    public boolean isEnabledParticleAsync() {
        return enabledParticleAsync;
    }

    public void setEnabledParticleAsync(boolean enabledParticleAsync) {
        this.enabledParticleAsync = enabledParticleAsync;
    }

    public int getParticleCountLimit() {
        return particleCountLimit;
    }

    public void setParticleCountLimit(int particleCountLimit) {
        this.particleCountLimit = Math.max(1, particleCountLimit);
    }

    public int getCalculateThreadCount() {
        return calculateThreadCount;
    }

    public void setCalculateThreadCount(int calculateThreadCount) {
        this.calculateThreadCount = Math.max(1, calculateThreadCount);
    }

    public int getPacketsPerTickLimit() {
        return packetsPerTickLimit;
    }

    public void setPacketsPerTickLimit(int packetsPerTickLimit) {
        this.packetsPerTickLimit = Math.max(16, packetsPerTickLimit);
    }

    public int getMaxEmitterVisibleRange() {
        return maxEmitterVisibleRange;
    }

    public void setMaxEmitterVisibleRange(int maxEmitterVisibleRange) {
        this.maxEmitterVisibleRange = Math.max(32, maxEmitterVisibleRange);
    }
}
