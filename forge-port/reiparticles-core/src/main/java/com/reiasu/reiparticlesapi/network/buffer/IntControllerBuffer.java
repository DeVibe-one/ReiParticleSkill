// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.buffer;

import net.minecraft.resources.ResourceLocation;

import java.nio.ByteBuffer;

public final class IntControllerBuffer extends AbstractControllerBuffer<Integer> {
    public static final ParticleControllerDataBuffer.Id ID =
            new ParticleControllerDataBuffer.Id(new ResourceLocation("reiparticlesapi", "int"));

    @Override
    public byte[] encode(Integer value) {
        return ByteBuffer.allocate(4).putInt(value == null ? 0 : value).array();
    }

    @Override
    public Integer decode(byte[] buf) {
        if (buf.length < 4) {
            return 0;
        }
        return ByteBuffer.wrap(buf).getInt();
    }

    @Override
    public ParticleControllerDataBuffer.Id getBufferID() {
        return ID;
    }
}

