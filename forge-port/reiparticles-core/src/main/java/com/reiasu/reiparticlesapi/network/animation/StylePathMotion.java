// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.animation;

import com.reiasu.reiparticlesapi.network.animation.api.AbstractPathMotion;
import com.reiasu.reiparticlesapi.network.particle.style.ParticleGroupStyle;
import net.minecraft.world.phys.Vec3;

/**
 * Path motion targeting a {@link ParticleGroupStyle}. Teleports the style
 * along the computed path. Subclasses implement {@link #pathFunction()}.
 */
public abstract class StylePathMotion extends AbstractPathMotion {
    private final ParticleGroupStyle targetStyle;

    protected StylePathMotion(Vec3 origin, ParticleGroupStyle targetStyle) {
        super(origin);
        this.targetStyle = targetStyle;
    }

    public final ParticleGroupStyle getTargetStyle() {
        return targetStyle;
    }

    @Override
    public void apply(Vec3 actualPos) {
        targetStyle.teleportTo(actualPos);
    }

    @Override
    public boolean checkValid() {
        return !targetStyle.getCanceled();
    }
}
