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

    /**
     * @deprecated Compatibility placeholder retained for downstream callers.
     * The current runtime does not consume this flag.
     */
    @Deprecated
    public boolean isEnabledParticleCountInject() {
        return enabledParticleCountInject;
    }

    @Deprecated
    public void setEnabledParticleCountInject(boolean enabledParticleCountInject) {
        this.enabledParticleCountInject = enabledParticleCountInject;
    }

    /**
     * @deprecated Compatibility placeholder retained for downstream callers.
     * The current runtime does not consume this flag.
     */
    @Deprecated
    public boolean isEnabledParticleAsync() {
        return enabledParticleAsync;
    }

    @Deprecated
    public void setEnabledParticleAsync(boolean enabledParticleAsync) {
        this.enabledParticleAsync = enabledParticleAsync;
    }

    /**
     * Legacy config key retained for compatibility.
     * The runtime currently applies this as a cap on active emitters.
     */
    public int getParticleCountLimit() {
        return particleCountLimit;
    }

    public void setParticleCountLimit(int particleCountLimit) {
        this.particleCountLimit = Math.max(1, particleCountLimit);
    }

    /**
     * @deprecated Compatibility placeholder retained for downstream callers.
     * The current runtime does not consume this value.
     */
    @Deprecated
    public int getCalculateThreadCount() {
        return calculateThreadCount;
    }

    @Deprecated
    public void setCalculateThreadCount(int calculateThreadCount) {
        this.calculateThreadCount = Math.max(1, calculateThreadCount);
    }

    /**
     * Shared budget for visibility create/update sync packets.
     * Remove packets intentionally bypass this limit so teardown is not dropped.
     */
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
