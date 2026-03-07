// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper.impl.composition;

import com.reiasu.reiparticlesapi.network.particle.composition.ParticleComposition;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.utils.helper.StatusHelper;

/**
 * {@link StatusHelper} implementation for {@link ParticleComposition}.
 * Attaches a pre-tick action that increments the current counter when the
 * status is DISABLE, and removes the composition when the counter reaches
 * the closed interval.
 */
public final class CompositionStatusHelper extends StatusHelper {
    private ParticleComposition composition;
    private boolean init;

    @Override
    public void changeStatus(int status) {
        // Composition status changes are handled by the pre-tick action
    }

    public void updateCurrent(int current) {
        setCurrent(current);
    }

    @Override
    public void initHelper() {
        if (composition == null || init) {
            return;
        }
        init = true;
        composition.addPreTickAction(() -> {
            if (getDisplayStatus() != Status.DISABLE.id()) {
                return;
            }
            setCurrent(getCurrent() + 1);
            if (getCurrent() >= getClosedInternal()) {
                composition.remove();
            }
        });
    }

    @Override
    public void loadController(Controllable<?> controller) {
        if (!(controller instanceof ParticleComposition pc)) {
            return;
        }
        this.composition = pc;
    }
}
