// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.buffer;

import net.minecraft.resources.ResourceLocation;

import java.nio.ByteBuffer;

public final class LongArrayControllerBuffer extends AbstractControllerBuffer<long[]> {
    public static final ParticleControllerDataBuffer.Id ID =
            new ParticleControllerDataBuffer.Id(new ResourceLocation("reiparticlesapi", "long_array"));

    @Override
    public byte[] encode(long[] value) {
        long[] safe = value == null ? new long[0] : value;
        ByteBuffer buffer = ByteBuffer.allocate(4 + safe.length * 8);
        buffer.putInt(safe.length);
        for (long i : safe) {
            buffer.putLong(i);
        }
        return buffer.array();
    }

    @Override
    public long[] decode(byte[] buf) {
        if (buf.length < 4) {
            return new long[0];
        }
        ByteBuffer buffer = ByteBuffer.wrap(buf);
        int len = Math.max(0, buffer.getInt());
        long[] out = new long[Math.min(len, (buf.length - 4) / 8)];
        for (int i = 0; i < out.length; i++) {
            out[i] = buffer.getLong();
        }
        return out;
    }

    @Override
    public ParticleControllerDataBuffer.Id getBufferID() {
        return ID;
    }
}

