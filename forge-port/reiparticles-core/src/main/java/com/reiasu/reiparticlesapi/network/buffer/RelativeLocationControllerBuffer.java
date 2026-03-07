// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.buffer;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.resources.ResourceLocation;

import java.nio.ByteBuffer;

public final class RelativeLocationControllerBuffer extends AbstractControllerBuffer<RelativeLocation> {
    public static final ParticleControllerDataBuffer.Id ID =
            new ParticleControllerDataBuffer.Id(new ResourceLocation("reiparticlesapi", "relative_location"));

    @Override
    public byte[] encode(RelativeLocation value) {
        RelativeLocation safe = value == null ? new RelativeLocation() : value;
        return ByteBuffer.allocate(24)
                .putDouble(safe.getX())
                .putDouble(safe.getY())
                .putDouble(safe.getZ())
                .array();
    }

    @Override
    public RelativeLocation decode(byte[] buf) {
        if (buf.length < 24) {
            return new RelativeLocation();
        }
        ByteBuffer buffer = ByteBuffer.wrap(buf);
        return new RelativeLocation(buffer.getDouble(), buffer.getDouble(), buffer.getDouble());
    }

    @Override
    public ParticleControllerDataBuffer.Id getBufferID() {
        return ID;
    }
}

