// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.buffer;

import net.minecraft.resources.ResourceLocation;

import java.nio.ByteBuffer;

public final class ShortControllerBuffer extends AbstractControllerBuffer<Short> {
    public static final ParticleControllerDataBuffer.Id ID =
            new ParticleControllerDataBuffer.Id(new ResourceLocation("reiparticlesapi", "short"));

    @Override
    public byte[] encode(Short value) {
        return ByteBuffer.allocate(2).putShort(value == null ? 0 : value).array();
    }

    @Override
    public Short decode(byte[] buf) {
        if (buf.length < 2) {
            return 0;
        }
        return ByteBuffer.wrap(buf).getShort();
    }

    @Override
    public ParticleControllerDataBuffer.Id getBufferID() {
        return ID;
    }
}

