// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.emitters;

import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Base class for emitters registered via {@code @ReiAutoRegister}.
 * <p>
 * Convention: subclasses declare
 * {@code public static final ResourceLocation CODEC_ID = ...;}
 * This constructor auto-reads that field and calls
 * {@link #setEmittersID(ResourceLocation)} so every instance is
 * immediately sync-ready without manual wiring.
 */
public class AutoParticleEmitters extends ParticleEmitters {

    protected AutoParticleEmitters() {
        ResourceLocation id = resolveCodecId(getClass());
        if (id != null) {
            setEmittersID(id);
        }
    }

    private static ResourceLocation resolveCodecId(Class<?> clazz) {
        try {
            Field f = clazz.getDeclaredField("CODEC_ID");
            if (Modifier.isStatic(f.getModifiers())
                    && ResourceLocation.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return (ResourceLocation) f.get(null);
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
        return null;
    }
}

