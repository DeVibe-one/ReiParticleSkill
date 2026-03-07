// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper.impl;

import com.reiasu.reiparticlesapi.network.particle.ServerParticleGroup;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleGroupStyle;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.utils.helper.ScaleHelper;

/**
 * {@link ScaleHelper} implementation for particle groups (ControllableParticleGroup).
 * <p>
 * Delegates scale operations through {@link ServerParticleGroup} or
 * {@link ParticleGroupStyle} depending on the loaded controller type.
 */
public final class GroupScaleHelper extends ScaleHelper {
    private Controllable<?> group;

    public GroupScaleHelper(double minScale, double maxScale, int scaleTick) {
        super(minScale, maxScale, scaleTick);
    }

    public Controllable<?> getGroup() {
        return group;
    }

    public void setGroup(Controllable<?> group) {
        this.group = group;
    }

    @Override
    public void loadController(Controllable<?> controller) {
        if (controller instanceof ServerParticleGroup || controller instanceof ParticleGroupStyle) {
            this.group = controller;
        }
    }

    @Override
    public Controllable<?> getLoadedGroup() {
        return group;
    }

    @Override
    public double getGroupScale() {
        if (group instanceof ParticleGroupStyle pgs) {
            return pgs.getScale();
        }
        return getMinScale();
    }

    @Override
    public void scale(double scale) {
        if (group == null) {
            return;
        }
        double clamped = Math.max(getMinScale(), Math.min(getMaxScale(), scale));
        if (group instanceof ParticleGroupStyle pgs) {
            pgs.scale(clamped);
        }
    }
}
