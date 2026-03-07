// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.animation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A sequential animation state machine that plays a chain of {@link AnimateNode}s.
 * <p>
 * Nodes are added via {@link #addNode(AnimateNode, int)} with an optional interval
 * (in ticks) between the current node finishing and the next starting.
 * Call {@link #start()} once, then {@link #tick()} every game tick.
 * <p>
 * Cancel predicates can abort the entire chain early via
 * {@link #addCancelPredicate(java.util.function.Predicate)}.
 * <p>
 * Key state accessors:
 * <ul>
 *   <li>{@link #getTimestamp()} — ticks elapsed since the current node started</li>
 *   <li>{@link #getCurrentIndex()} — index of the current node in the chain</li>
 *   <li>{@link #getDone()} — whether the animation has finished or been cancelled</li>
 *   <li>{@link #getDisplay()} — whether the animation is currently running</li>
 * </ul>
 */
public class Animate {
    private final List<NodeSlot> nodes = new ArrayList<>();
    private final List<Predicate<Animate>> cancelPredicates = new ArrayList<>();
    private AnimateNode currentNode;
    private int currentInterval;
    private int currentIndex;
    private int timestamp;
    private boolean done;
    private boolean display;

    public Animate addNode(AnimateNode node) {
        return addNode(node, 0);
    }

    public Animate addNode(AnimateNode node, int interval) {
        if (node != null) {
            nodes.add(new NodeSlot(node, Math.max(0, interval)));
        }
        return this;
    }

    public Animate addCancelPredicate(Predicate<Animate> predicate) {
        if (predicate != null) {
            cancelPredicates.add(predicate);
        }
        return this;
    }

    public void skip() {
        if (!display || done) {
            return;
        }

        if (currentNode != null && !currentNode.checkDone()) {
            currentNode.cancel();
        }

        if (currentIndex >= nodes.size()) {
            done = true;
            return;
        }

        NodeSlot slot = nodes.get(currentIndex++);
        currentNode = slot.node();
        currentNode.onStart();
        currentInterval = slot.interval();
        timestamp = 0;
    }

    public void start() {
        if (display || done) {
            return;
        }
        display = true;
        if (!nodes.isEmpty()) {
            skip();
        }
    }

    public void tick() {
        if (!display || done) {
            return;
        }

        if (currentNode == null) {
            skip();
            if (done) {
                return;
            }
        }

        if (currentNode != null && currentNode.checkDone()) {
            if (currentInterval < timestamp) {
                skip();
            }
        } else if (currentNode != null) {
            currentNode.tick();
        }

        boolean shouldCancel = false;
        if (!cancelPredicates.isEmpty()) {
            for (Predicate<Animate> predicate : cancelPredicates) {
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

        timestamp++;
    }

    public void cancel() {
        done = true;
        display = false;
        for (NodeSlot slot : nodes) {
            AnimateNode node = slot.node();
            if (!node.checkDone()) {
                node.cancel();
            }
        }
    }

    public int getTimestamp() {
        return timestamp;
    }

    public int getCurrentInterval() {
        return currentInterval;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean getDone() {
        return done;
    }

    public boolean getDisplay() {
        return display;
    }

    private record NodeSlot(AnimateNode node, int interval) {
    }
}
