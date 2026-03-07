// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.particle.style;

import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;

import java.util.Map;
import java.util.UUID;

public interface ParticleStyleProvider<T extends ParticleGroupStyle> {
    T create();

    default T createStyle(UUID uuid, Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
        return create();
    }

    default void changeStyle(T style, Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
    }
}
