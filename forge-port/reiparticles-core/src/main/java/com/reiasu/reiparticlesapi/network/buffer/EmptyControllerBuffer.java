// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.buffer;

import net.minecraft.resources.ResourceLocation;

public final class EmptyControllerBuffer extends AbstractControllerBuffer<Void> {
    public static final ParticleControllerDataBuffer.Id ID =
            new ParticleControllerDataBuffer.Id(new ResourceLocation("reiparticlesapi", "empty"));

    @Override
    public byte[] encode(Void value) {
        return new byte[0];
    }

    @Override
    public byte[] encode() {
        return new byte[0];
    }

    @Override
    public Void decode(byte[] buf) {
        return null;
    }

    @Override
    public ParticleControllerDataBuffer.Id getBufferID() {
        return ID;
    }
}

