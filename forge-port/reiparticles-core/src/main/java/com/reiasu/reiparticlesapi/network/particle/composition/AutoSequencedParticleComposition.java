// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.composition;

import com.reiasu.reiparticlesapi.annotations.codec.BufferCodec;
import com.reiasu.reiparticlesapi.annotations.composition.handler.ParticleCompositionHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Abstract sequenced composition that auto-generates its network codec
 * via {@link ParticleCompositionHelper}.
 * <p>
 * Subclasses must:
 * <ul>
 *   <li>Provide a public constructor {@code (Vec3, Level)}</li>
 *   <li>Annotate serializable fields with {@link com.reiasu.reiparticlesapi.annotations.CodecField}</li>
 *   <li>Implement {@link #getParticleSequenced()} and {@link #onDisplay()}</li>
 * </ul>
 */
public abstract class AutoSequencedParticleComposition extends SequencedParticleComposition {

    protected AutoSequencedParticleComposition(Vec3 position, Level world) {
        super(position, world);
    }

    protected AutoSequencedParticleComposition(Vec3 position) {
        super(position);
    }

    /**
     * Returns the auto-generated codec for this composition type.
     * Uses reflection to serialize all {@link com.reiasu.reiparticlesapi.annotations.CodecField}
     * annotated fields.
     */
    public BufferCodec<ParticleComposition> getCodec() {
        return ParticleCompositionHelper.INSTANCE.generateCodec(this);
    }
}
