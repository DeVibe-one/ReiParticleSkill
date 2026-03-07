// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class AnimateNode {
    private final List<AnimateAction> actions = new ArrayList<>();
    private final List<Predicate<AnimateNode>> cancelPredicates = new ArrayList<>();
    private int timestrap;

    public AnimateNode addAction(AnimateAction action) {
        if (action != null) {
            actions.add(action);
        }
        return this;
    }

    public AnimateNode addCancelPredicate(Predicate<AnimateNode> predicate) {
        if (predicate != null) {
            cancelPredicates.add(predicate);
        }
        return this;
    }

    public void onStart() {
        timestrap = 0;
        for (AnimateAction action : actions) {
            action.setDone(false);
            action.setTickCount(0);
            action.onStart();
        }
    }

    public void tick() {
        for (AnimateAction action : actions) {
            if (action.check()) {
                continue;
            }
            if (action.getTimeInterval() > timestrap) {
                continue;
            }
            action.doTick();
            if (action.check()) {
                action.onDone();
            }
        }

        boolean shouldCancel = false;
        if (!cancelPredicates.isEmpty()) {
            for (Predicate<AnimateNode> predicate : cancelPredicates) {
                if (predicate.test(this)) {
                    shouldCancel = true;
                    break;
                }
            }
        }
        if (shouldCancel) {
            cancel();
            return;
        }

        timestrap++;
    }

    public boolean checkDone() {
        if (actions.isEmpty()) {
            return true;
        }
        for (AnimateAction action : actions) {
            if (!action.check()) {
                return false;
            }
        }
        return true;
    }

    public boolean done() {
        return checkDone();
    }

    public void cancel() {
        for (AnimateAction action : actions) {
            if (action.check()) {
                continue;
            }
            action.onDone();
            action.cancel();
        }
    }
}
