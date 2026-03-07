// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.animats;

import com.reiasu.reiparticlesapi.animation.AnimateAction;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;

import java.util.Objects;
import java.util.function.Consumer;

public final class EmitterAction extends AnimateAction {
    private final ParticleEmitters emitter;
    private final Consumer<EmitterAction> ticking;
    private boolean firstTick;

    public EmitterAction(ParticleEmitters emitter, Consumer<EmitterAction> ticking) {
        this.emitter = Objects.requireNonNull(emitter, "emitter");
        this.ticking = Objects.requireNonNull(ticking, "ticking");
    }

    public ParticleEmitters getEmitter() {
        return emitter;
    }

    public Consumer<EmitterAction> getTicking() {
        return ticking;
    }

    public boolean getFirstTick() {
        return firstTick;
    }

    public void setFirstTick(boolean firstTick) {
        this.firstTick = firstTick;
    }

    @Override
    public boolean checkDone() {
        return emitter.getCanceled();
    }

    @Override
    public void tick() {
        if (!firstTick) {
            firstTick = true;
            ParticleEmittersManager.spawnEmitters(emitter);
        }
        ticking.accept(this);
    }

    @Override
    public void onStart() {
        firstTick = false;
    }

    @Override
    public void onDone() {
        emitter.cancel();
        setDone(true);
    }
}
