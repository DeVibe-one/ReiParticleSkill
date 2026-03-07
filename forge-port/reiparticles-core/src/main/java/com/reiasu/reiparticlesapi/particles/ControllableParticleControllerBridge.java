// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.particles;

import com.reiasu.reiparticlesapi.particles.control.ControlParticleManager;
import com.reiasu.reiparticlesapi.particles.control.ParticleController;

import java.util.UUID;

final class ControllableParticleControllerBridge {
    private final UUID controlUUID;
    private final ParticleController controller;

    private ControllableParticleControllerBridge(UUID controlUUID, ParticleController controller) {
        this.controlUUID = controlUUID;
        this.controller = controller;
    }

    static ControllableParticleControllerBridge attach(UUID controlUUID, ControllableParticle particle) {
        ParticleController controller = ControlParticleManager.INSTANCE.getControl(controlUUID);
        if (controller == null) {
            throw new IllegalStateException("No ParticleController registered for UUID " + controlUUID);
        }
        controller.loadParticle(particle);
        controller.particleInit();
        return new ControllableParticleControllerBridge(controlUUID, controller);
    }

    ParticleController controller() {
        return controller;
    }

    void tick() {
        controller.doTick();
    }

    void release() {
        ControlParticleManager.INSTANCE.removeControl(controlUUID);
    }
}
