// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils;

import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility singleton for managing client camera shake effects.
 * <p>
 * Stores per-tick shake offsets (position + angle) that a client-side camera
 * mixin or event handler can apply. Call {@link #startShakeCamera(int, double)}
 * to begin a shake, and {@link #tick()} each client tick to update the offsets.
 * The shake amplitude linearly decreases to zero over the specified duration.
 */
public final class ClientCameraUtil {

    public static final ClientCameraUtil INSTANCE = new ClientCameraUtil();

    // Per-tick shake offsets
    private float shakeYawOffset;
    private float shakePitchOffset;
    private double shakeXOffset;
    private double shakeYOffset;
    private double shakeZOffset;

    // Manual (non-shake) persistent offsets
    private float currentYawOffset;
    private float currentPitchOffset;
    private double currentXOffset;
    private double currentYOffset;
    private double currentZOffset;

    // Shake state
    private int tick;
    private double ampStep;
    private double amp;

    private ClientCameraUtil() {
    }

    // ---- Shake offsets (read by camera mixin) ----

    public float getShakeYawOffset() { return shakeYawOffset; }
    public void setShakeYawOffset(float v) { shakeYawOffset = v; }

    public float getShakePitchOffset() { return shakePitchOffset; }
    public void setShakePitchOffset(float v) { shakePitchOffset = v; }

    public double getShakeXOffset() { return shakeXOffset; }
    public void setShakeXOffset(double v) { shakeXOffset = v; }

    public double getShakeYOffset() { return shakeYOffset; }
    public void setShakeYOffset(double v) { shakeYOffset = v; }

    public double getShakeZOffset() { return shakeZOffset; }
    public void setShakeZOffset(double v) { shakeZOffset = v; }

    // ---- Manual persistent offsets ----

    public float getCurrentYawOffset() { return currentYawOffset; }
    public void setCurrentYawOffset(float v) { currentYawOffset = v; }

    public float getCurrentPitchOffset() { return currentPitchOffset; }
    public void setCurrentPitchOffset(float v) { currentPitchOffset = v; }

    public double getCurrentXOffset() { return currentXOffset; }
    public void setCurrentXOffset(double v) { currentXOffset = v; }

    public double getCurrentYOffset() { return currentYOffset; }
    public void setCurrentYOffset(double v) { currentYOffset = v; }

    public double getCurrentZOffset() { return currentZOffset; }
    public void setCurrentZOffset(double v) { currentZOffset = v; }

    // ---- Shake control state ----

    public int getTick() { return tick; }
    public void setTick(int v) { tick = v; }

    public double getAmpStep() { return ampStep; }
    public void setAmpStep(double v) { ampStep = v; }

    public double getAmp() { return amp; }
    public void setAmp(double v) { amp = v; }

    // ---- Operations ----

    /**
     * Sets the manual position offset from a Vec3.
     */
    public void setOffsetPosition(Vec3 offset) {
        currentXOffset = offset.x;
        currentYOffset = offset.y;
        currentZOffset = offset.z;
    }

    /**
     * Resets manual position offsets to zero.
     */
    public void resetPosOffset() {
        currentXOffset = 0.0;
        currentYOffset = 0.0;
        currentZOffset = 0.0;
    }

    /**
     * Resets manual angle offsets to zero.
     */
    public void resetAngleOffset() {
        currentYawOffset = 0.0f;
        currentPitchOffset = 0.0f;
    }

    /**
     * Resets all manual offsets (position + angle) to zero.
     */
    public void resetOffset() {
        resetAngleOffset();
        resetPosOffset();
    }

    /**
     * Starts a camera shake effect that decays linearly over the given ticks.
     *
     * @param tick      number of ticks the shake should last
     * @param amplitude initial shake amplitude
     */
    public void startShakeCamera(int tick, double amplitude) {
        this.amp = amplitude;
        this.ampStep = amp / (double) tick;
        this.tick = tick;
    }

    /**
     * Call each client tick. While a shake is active, randomises per-tick
     * offsets and decays amplitude.
     */
    public void tick() {
        if (tick > 0) {
            ThreadLocalRandom rng = ThreadLocalRandom.current();
            shakeXOffset = amp * rng.nextDouble(-0.5, 0.5);
            shakeYOffset = amp * rng.nextDouble(-0.5, 0.5);
            shakeZOffset = amp * rng.nextDouble(-0.5, 0.5);
            shakeYawOffset = (float) (amp * rng.nextDouble(-2.0, 2.0));
            shakePitchOffset = (float) (amp * rng.nextDouble(-2.0, 2.0));
            amp -= ampStep;
            tick--;
        }
    }
}
