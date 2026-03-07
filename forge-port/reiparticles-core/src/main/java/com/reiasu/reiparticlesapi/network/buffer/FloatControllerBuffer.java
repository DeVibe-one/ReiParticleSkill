// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.buffer;

import net.minecraft.resources.ResourceLocation;

import java.nio.ByteBuffer;

public final class FloatControllerBuffer extends AbstractControllerBuffer<Float> {
    public static final ParticleControllerDataBuffer.Id ID =
            new ParticleControllerDataBuffer.Id(new ResourceLocation("reiparticlesapi", "float"));

    @Override
    public byte[] encode(Float value) {
        return ByteBuffer.allocate(4).putFloat(value == null ? 0.0f : value).array();
    }

    @Override
    public Float decode(byte[] buf) {
        if (buf.length < 4) {
            return 0.0f;
        }
        return ByteBuffer.wrap(buf).getFloat();
    }

    @Override
    public ParticleControllerDataBuffer.Id getBufferID() {
        return ID;
    }
}

