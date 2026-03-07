// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.animats;

import com.reiasu.reiparticlesapi.animation.AnimateAction;

import java.util.Objects;
import java.util.function.Consumer;

public final class TickingAction extends AnimateAction {
    private final int maxTick;
    private final Consumer<TickingAction> ticking;
    private boolean firstTick;
    private boolean canceled;

    public TickingAction(int maxTick, Consumer<TickingAction> ticking) {
        this.maxTick = maxTick;
        this.ticking = Objects.requireNonNull(ticking, "ticking");
    }

    public int getMaxTick() {
        return maxTick;
    }

    public Consumer<TickingAction> getTicking() {
        return ticking;
    }

    public boolean getFirstTick() {
        return firstTick;
    }

    public void setFirstTick(boolean firstTick) {
        this.firstTick = firstTick;
    }

    public boolean getCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    @Override
    public boolean checkDone() {
        return canceled;
    }

    @Override
    public void tick() {
        ticking.accept(this);
        if (!firstTick) {
            firstTick = true;
        }
        if (maxTick != -1 && getTickCount() >= maxTick) {
            onDone();
        }
    }

    @Override
    public void onStart() {
        firstTick = false;
        canceled = false;
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
