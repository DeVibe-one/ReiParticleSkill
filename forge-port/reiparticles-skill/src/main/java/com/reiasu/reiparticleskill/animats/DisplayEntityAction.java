// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticleskill.animats;

import com.reiasu.reiparticlesapi.animation.AnimateAction;
import com.reiasu.reiparticlesapi.display.DisplayEntity;
import com.reiasu.reiparticlesapi.display.DisplayEntityManager;

import java.util.Objects;
import java.util.function.Consumer;

public final class DisplayEntityAction extends AnimateAction {
    private final DisplayEntity display;
    private final Consumer<DisplayEntityAction> ticking;
    private boolean firstTick;

    public DisplayEntityAction(DisplayEntity display, Consumer<DisplayEntityAction> ticking) {
        this.display = Objects.requireNonNull(display, "display");
        this.ticking = Objects.requireNonNull(ticking, "ticking");
    }

    public DisplayEntity getDisplay() {
        return display;
    }

    public Consumer<DisplayEntityAction> getTicking() {
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
        return !display.getValid();
    }

    @Override
    public void tick() {
        if (!firstTick) {
            firstTick = true;
            DisplayEntityManager.INSTANCE.spawn(display);
        }
        ticking.accept(this);
    }

    @Override
    public void onStart() {
        firstTick = false;
    }

    @Override
    public void onDone() {
        display.cancel();
        setDone(true);
    }
}
