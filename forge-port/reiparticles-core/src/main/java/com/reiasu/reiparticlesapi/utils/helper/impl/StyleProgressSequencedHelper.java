// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper.impl;

import com.reiasu.reiparticlesapi.network.particle.style.SequencedParticleStyle;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.utils.helper.ProgressSequencedHelper;

/**
 * {@link ProgressSequencedHelper} implementation for {@link com.reiasu.reiparticlesapi.network.particle.style.SequencedParticleStyle}.
 * <p>
 * Delegates add/remove operations through {@link SequencedParticleStyle}.
 */
public final class StyleProgressSequencedHelper extends ProgressSequencedHelper {
    private Controllable<?> linkedStyle;

    public StyleProgressSequencedHelper(int maxCount, int progressMaxTick) {
        super(maxCount, progressMaxTick);
    }

    @Override
    public void addMultiple(int count) {
        if (linkedStyle instanceof SequencedParticleStyle ss) {
            ss.addMultiple(count);
        }
    }

    @Override
    public void removeMultiple(int count) {
        if (linkedStyle instanceof SequencedParticleStyle ss) {
            ss.removeMultiple(count);
        }
    }

    @Override
    public Controllable<?> getLoadedStyle() {
        return linkedStyle;
    }

    @Override
    protected void changeStatusBatch(int[] indexes, boolean status) {
        if (linkedStyle instanceof SequencedParticleStyle ss) {
            for (int index : indexes) {
                ss.setStatus(index, status);
            }
        }
    }

    /**
     * Sync progress from server-side current tick to client-side display.
     */
    public void syncProgressFromServer(int current) {
        setCurrent(Math.max(0, Math.min(getProgressMaxTick(), current)));
        int targetCount = (int) Math.round(
                (double) current / (double) getProgressMaxTick() * (double) getMaxCount()
        );
        if (linkedStyle instanceof SequencedParticleStyle ss) {
            int currentCount = ss.getDisplayedParticleCount();
            if (targetCount > currentCount) {
                ss.addMultiple(targetCount - currentCount);
            } else if (targetCount < currentCount) {
                ss.removeMultiple(currentCount - targetCount);
            }
        }
    }

    @Override
    public void loadController(Controllable<?> controller) {
        if (controller instanceof SequencedParticleStyle) {
            this.linkedStyle = controller;
        }
    }
}
