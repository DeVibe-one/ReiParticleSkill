// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.buffer;

import net.minecraft.resources.ResourceLocation;

import java.nio.ByteBuffer;

public final class LongControllerBuffer extends AbstractControllerBuffer<Long> {
    public static final ParticleControllerDataBuffer.Id ID =
            new ParticleControllerDataBuffer.Id(new ResourceLocation("reiparticlesapi", "long"));

    @Override
    public byte[] encode(Long value) {
        return ByteBuffer.allocate(8).putLong(value == null ? 0L : value).array();
    }

    @Override
    public Long decode(byte[] buf) {
        if (buf.length < 8) {
            return 0L;
        }
        return ByteBuffer.wrap(buf).getLong();
    }

    @Override
    public ParticleControllerDataBuffer.Id getBufferID() {
        return ID;
    }
}

