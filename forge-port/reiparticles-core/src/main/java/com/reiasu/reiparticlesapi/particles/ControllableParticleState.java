// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class ControllableParticleState {
    private Vec3 pendingTeleport;
    private boolean teleportPending;
    private float previewPitch;
    private float currentPitch;
    private float previewYaw;
    private float currentYaw;
    private float previewRoll;
    private float currentRoll;
    private Vector3f pendingRotation;
    private boolean rotationPending;

    ControllableParticleState(Vec3 initialPos) {
        this.pendingTeleport = initialPos;
        this.pendingRotation = new Vector3f();
    }

    void scheduleTeleport(Vec3 target) {
        this.pendingTeleport = target;
        this.teleportPending = true;
    }

    Vec3 consumePendingTeleport() {
        if (!teleportPending) {
            return null;
        }
        this.teleportPending = false;
        return pendingTeleport;
    }

    void capturePreviousRotation() {
        this.previewPitch = this.currentPitch;
        this.previewYaw = this.currentYaw;
        this.previewRoll = this.currentRoll;
    }

    boolean applyPendingRotation() {
        if (!rotationPending) {
            return false;
        }
        this.currentPitch = this.pendingRotation.x;
        this.currentYaw = this.pendingRotation.y;
        this.currentRoll = this.pendingRotation.z;
        this.rotationPending = false;
        return true;
    }

    void scheduleRotation(Vector3f targetRotation) {
        this.pendingRotation = new Vector3f(targetRotation);
        this.rotationPending = true;
    }

    float getPreviewPitch() {
        return previewPitch;
    }

    void setPreviewPitch(float previewPitch) {
        this.previewPitch = previewPitch;
    }

    float getCurrentPitch() {
        return currentPitch;
    }

    void setCurrentPitch(float currentPitch) {
        this.currentPitch = currentPitch;
    }

    float getPreviewYaw() {
        return previewYaw;
    }

    void setPreviewYaw(float previewYaw) {
        this.previewYaw = previewYaw;
    }

    float getCurrentYaw() {
        return currentYaw;
    }

    void setCurrentYaw(float currentYaw) {
        this.currentYaw = currentYaw;
    }

    float getPreviewRoll() {
        return previewRoll;
    }

    void setPreviewRoll(float previewRoll) {
        this.previewRoll = previewRoll;
    }

    float getCurrentRoll() {
        return currentRoll;
    }

    void setCurrentRoll(float currentRoll) {
        this.currentRoll = currentRoll;
    }

    Vector3f getPendingRotation() {
        return pendingRotation;
    }

    void setPendingRotation(Vector3f pendingRotation) {
        this.pendingRotation = pendingRotation;
    }

    boolean isRotationPending() {
        return rotationPending;
    }

    void setRotationPending(boolean rotationPending) {
        this.rotationPending = rotationPending;
    }
}
