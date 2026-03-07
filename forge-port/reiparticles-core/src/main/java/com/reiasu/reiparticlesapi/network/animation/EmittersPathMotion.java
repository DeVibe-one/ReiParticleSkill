// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.animation;

import com.reiasu.reiparticlesapi.network.animation.api.AbstractPathMotion;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import net.minecraft.world.phys.Vec3;

/**
 * Path motion targeting a {@link ParticleEmitters}. Teleports the emitter
 * along the computed path. Subclasses implement {@link #pathFunction()}.
 */
public abstract class EmittersPathMotion extends AbstractPathMotion {
    private final ParticleEmitters targetEmitters;

    protected EmittersPathMotion(Vec3 origin, ParticleEmitters targetEmitters) {
        super(origin);
        this.targetEmitters = targetEmitters;
    }

    public final ParticleEmitters getTargetEmitters() {
        return targetEmitters;
    }

    @Override
    public void apply(Vec3 actualPos) {
        targetEmitters.teleportTo(actualPos);
    }

    @Override
    public boolean checkValid() {
        return !targetEmitters.getCanceled();
    }
}
