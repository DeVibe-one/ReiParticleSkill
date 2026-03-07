// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.event.events.particle.emitter;

import com.reiasu.reiparticlesapi.event.api.ReiEvent;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;

public final class EmitterSpawnEvent extends ReiEvent {
    private final ParticleEmitters emitter;
    private final boolean clientSide;

    public EmitterSpawnEvent(ParticleEmitters emitter, boolean clientSide) {
        this.emitter = emitter;
        this.clientSide = clientSide;
    }

    public ParticleEmitters getEmitter() {
        return emitter;
    }

    public boolean isClientSide() {
        return clientSide;
    }
}

