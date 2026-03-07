// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.animats;

import com.reiasu.reiparticlesapi.animation.AnimateAction;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmittersManager;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class EmitterCrafterAction extends AnimateAction {
    private final Supplier<ParticleEmitters> supplier;
    private final int interval;
    private final Predicate<EmitterCrafterAction> cancelPredicate;
    private boolean canceled;
    private int count;

    public EmitterCrafterAction(
            Supplier<ParticleEmitters> supplier,
            int interval,
            Predicate<EmitterCrafterAction> cancelPredicate
    ) {
        this.supplier = Objects.requireNonNull(supplier, "supplier");
        this.interval = Math.max(1, interval);
        this.cancelPredicate = Objects.requireNonNull(cancelPredicate, "cancelPredicate");
    }

    public Supplier<ParticleEmitters> getSupplier() {
        return supplier;
    }

    public int getInterval() {
        return interval;
    }

    public Predicate<EmitterCrafterAction> getCancelPredicate() {
        return cancelPredicate;
    }

    public boolean getCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public boolean checkDone() {
        return canceled;
    }

    @Override
    public void tick() {
        int step = count++ % interval;
        if (step == 0) {
            ParticleEmitters emitters = supplier.get();
            if (emitters != null) {
                ParticleEmittersManager.spawnEmitters(emitters);
            }
        }
        canceled = cancelPredicate.test(this);
    }

    @Override
    public void onStart() {
        canceled = false;
        count = 0;
    }

    @Override
    public void onDone() {
        canceled = true;
        setDone(true);
    }

    @Override
    public void cancel() {
        super.cancel();
        canceled = true;
    }
}
