// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper.impl;

import com.reiasu.reiparticlesapi.network.particle.style.ParticleGroupStyle;
import com.reiasu.reiparticlesapi.particles.Controllable;
import com.reiasu.reiparticlesapi.utils.helper.ScaleHelper;

public final class StyleScaleHelper extends ScaleHelper {
    private ParticleGroupStyle group;

    public StyleScaleHelper(double minScale, double maxScale, int scaleTick) {
        super(minScale, maxScale, scaleTick);
    }

    public ParticleGroupStyle getGroup() {
        return group;
    }

    public void setGroup(ParticleGroupStyle group) {
        this.group = group;
    }

    @Override
    public void loadController(Controllable<?> controller) {
        if (!(controller instanceof ParticleGroupStyle particleGroupStyle)) {
            return;
        }
        this.group = particleGroupStyle;
        particleGroupStyle.scale(getMinScale());
    }

    @Override
    public Controllable<?> getLoadedGroup() {
        return group;
    }

    @Override
    public double getGroupScale() {
        return group == null ? getMinScale() : group.getScale();
    }

    @Override
    public void scale(double scale) {
        if (group == null) {
            return;
        }
        double clamped = Math.max(getMinScale(), Math.min(getMaxScale(), scale));
        group.scale(clamped);
    }
}
