// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper;

import com.reiasu.reiparticlesapi.network.particle.composition.ParticleComposition;
import com.reiasu.reiparticlesapi.network.particle.composition.SequencedParticleComposition;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Animation helper for {@link SequencedParticleComposition}.
 * <p>
 * Allows defining a series of animation steps, each gated by a predicate.
 * When the predicate passes, the composition adds or removes the specified
 * number of particles and advances to the next animation step.
 *
 * @param <T> the concrete composition type
 */
public final class SequencedCompositionAnimationHelper<T extends SequencedParticleComposition> {

    /**
     * List of (predicate, count) pairs. When the predicate fires,
     * the composition adds (positive) or removes (negative) the count.
     */
    private final List<Map.Entry<Predicate<T>, Integer>> animationConditions = new ArrayList<>();
    private T composition;
    private int animationIndex;
    private boolean clientOnly;

    public T getComposition() {
        return composition;
    }

    public void setComposition(T composition) {
        this.composition = composition;
    }

    public int getAnimationIndex() {
        return animationIndex;
    }

    public boolean getClientOnly() {
        return clientOnly;
    }

    public void setClientOnly(boolean clientOnly) {
        this.clientOnly = clientOnly;
    }

    /**
     * Adds an animation step: when {@code displayAnimatePredicate} returns true,
     * the composition will add or remove {@code nextCount} particles, then
     * advance to the next step.
     */
    public SequencedCompositionAnimationHelper<T> addAnimate(int nextCount, Predicate<T> displayAnimatePredicate) {
        animationConditions.add(new AbstractMap.SimpleEntry<>(displayAnimatePredicate, nextCount));
        return this;
    }

    /**
     * Marks this animation as client-only (server-side compositions will skip it).
     */
    public SequencedCompositionAnimationHelper<T> clientOnly() {
        this.clientOnly = true;
        return this;
    }

    /**
     * Attaches this helper to the given composition and installs a pre-tick
     * action that drives the animation forward.
     */
    @SuppressWarnings("unchecked")
    public SequencedCompositionAnimationHelper<T> loadComposition(T comp) {
        this.composition = comp;
        comp.addPreTickAction(pc -> {
            // Determine if we should skip this tick
            boolean clientDisable = comp.getClient() && !clientOnly;
            boolean serverDisable = clientOnly && !comp.getClient();
            if (clientDisable || serverDisable) {
                return;
            }
            if (animationIndex >= animationConditions.size()) {
                return;
            }
            Map.Entry<Predicate<T>, Integer> entry = animationConditions.get(animationIndex);
            Predicate<T> predicate = entry.getKey();
            int add = entry.getValue();
            if (predicate.test(comp)) {
                if (add > 0) {
                    comp.addMultiple(add);
                } else {
                    comp.removeMultiple(-add);
                }
                animationIndex++;
            }
        });
        return this;
    }
}
