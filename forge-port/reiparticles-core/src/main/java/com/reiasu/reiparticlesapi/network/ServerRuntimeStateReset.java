// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network;

import com.reiasu.reiparticlesapi.animation.AnimateManager;
import com.reiasu.reiparticlesapi.display.DisplayEntityManager;
import com.reiasu.reiparticlesapi.network.particle.ServerParticleGroupManager;
import com.reiasu.reiparticlesapi.network.particle.composition.manager.ParticleCompositionManager;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleStyleManager;
import com.reiasu.reiparticlesapi.renderer.server.ServerRenderEntityManager;
import com.reiasu.reiparticlesapi.scheduler.ReiScheduler;
import com.reiasu.reiparticlesapi.test.TestManager;

public final class ServerRuntimeStateReset {
    private ServerRuntimeStateReset() {
    }

    public static void reset() {
        ServerSyncPacketBudget.reset();
        AnimateManager.INSTANCE.clearServer();
        ParticleEmittersManager.clearServer();
        DisplayEntityManager.INSTANCE.clearServer();
        ParticleCompositionManager.INSTANCE.clearServer();
        ParticleStyleManager.clearServer();
        ServerParticleGroupManager.INSTANCE.clear();
        ServerRenderEntityManager.INSTANCE.clear();
        ReiScheduler.INSTANCE.clearServer();
        TestManager.INSTANCE.clearServer();
    }
}
