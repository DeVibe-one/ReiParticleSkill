// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper;

import com.reiasu.reiparticlesapi.network.particle.style.ParticleGroupStyle;
import com.reiasu.reiparticlesapi.network.particle.style.SequencedParticleStyle;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Drives sequenced particle animations by evaluating predicate conditions
 * each tick and adding/removing particles accordingly.
 *
 * @param <T> the concrete {@link SequencedParticleStyle} type
 */
public final class SequencedAnimationHelper<T extends SequencedParticleStyle> {

    private final ArrayList<Map.Entry<Predicate<T>, Integer>> animationConditions = new ArrayList<>();
    public T style;
    private int animationIndex;
    private boolean clientOnly;

    public T getStyle() {
        if (style == null) throw new IllegalStateException("style has not been initialized");
        return style;
    }

    public void setStyle(T style) { this.style = style; }
    public int getAnimationIndex() { return animationIndex; }
    public boolean getClientOnly() { return clientOnly; }
    public void setClientOnly(boolean clientOnly) { this.clientOnly = clientOnly; }

    public SequencedAnimationHelper<T> addAnimate(Predicate<T> displayAnimatePredicate, int nextCount) {
        animationConditions.add(new AbstractMap.SimpleEntry<>(displayAnimatePredicate, nextCount));
        return this;
    }

    public SequencedAnimationHelper<T> clientOnly() {
        this.clientOnly = true;
        return this;
    }

    @SuppressWarnings("unchecked")
    public SequencedAnimationHelper<T> loadStyle(T style) {
        this.style = style;
        style.addPreTickAction(gs -> {
            boolean clientDisable = style.getClient() && !clientOnly;
            boolean serverDisable = clientOnly && !style.getClient();
            if (clientDisable || serverDisable) return;

            if (animationIndex >= animationConditions.size()) return;

            Map.Entry<Predicate<T>, Integer> entry = animationConditions.get(animationIndex);
            Predicate<T> predicate = entry.getKey();
            int add = entry.getValue();

            if (predicate.test((T) style)) {
                if (add > 0) {
                    style.addMultiple(add);
                } else {
                    style.removeMultiple(-add);
                }
                animationIndex++;
            }
        });
        return this;
    }
}
