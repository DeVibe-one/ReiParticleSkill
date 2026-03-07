// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.buffer;

import net.minecraft.resources.ResourceLocation;

import java.nio.ByteBuffer;

public final class DoubleControllerBuffer extends AbstractControllerBuffer<Double> {
    public static final ParticleControllerDataBuffer.Id ID =
            new ParticleControllerDataBuffer.Id(new ResourceLocation("reiparticlesapi", "double"));

    @Override
    public byte[] encode(Double value) {
        return ByteBuffer.allocate(8).putDouble(value == null ? 0.0 : value).array();
    }

    @Override
    public Double decode(byte[] buf) {
        if (buf.length < 8) {
            return 0.0;
        }
        return ByteBuffer.wrap(buf).getDouble();
    }

    @Override
    public ParticleControllerDataBuffer.Id getBufferID() {
        return ID;
    }
}

