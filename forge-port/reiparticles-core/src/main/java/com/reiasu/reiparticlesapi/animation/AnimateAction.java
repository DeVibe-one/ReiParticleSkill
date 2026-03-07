// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.animation;

public abstract class AnimateAction {
    private boolean done;
    private int timeInterval;
    private int tickCount;

    public boolean getDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public int getTimeInterval() {
        return timeInterval;
    }

    public void setTimeInterval(int timeInterval) {
        this.timeInterval = Math.max(0, timeInterval);
    }

    public int getTickCount() {
        return tickCount;
    }

    public void setTickCount(int tickCount) {
        this.tickCount = Math.max(0, tickCount);
    }

    public abstract boolean checkDone();

    public abstract void tick();

    public final void doTick() {
        tick();
        tickCount++;
    }

    public abstract void onStart();

    public abstract void onDone();

    public final boolean check() {
        if (!done && checkDone()) {
            done = true;
        }
        return done;
    }

    public void cancel() {
        done = true;
    }
}
