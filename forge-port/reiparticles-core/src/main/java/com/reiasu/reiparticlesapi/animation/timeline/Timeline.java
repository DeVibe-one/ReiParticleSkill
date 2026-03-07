// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.animation.timeline;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;

/**
 * A simple sequential step-based timeline.
 * Each step is a BooleanSupplier that returns true when the step is done.
 * On each tick, the current step is polled; when it returns true, the next step begins.
 */
public final class Timeline {
    private final Deque<BooleanSupplier> steps = new ArrayDeque<>();

    public Timeline step(BooleanSupplier block) {
        this.steps.addLast(block);
        return this;
    }

    public void doTick() {
        if (this.steps.isEmpty()) {
            return;
        }
        boolean done = this.steps.peekFirst().getAsBoolean();
        if (done) {
            this.steps.removeFirst();
        }
    }

    public boolean isEmpty() {
        return this.steps.isEmpty();
    }
}
