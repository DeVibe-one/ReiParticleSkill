// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.buffer;

import net.minecraft.resources.ResourceLocation;

public final class BooleanControllerBuffer extends AbstractControllerBuffer<Boolean> {
    public static final ParticleControllerDataBuffer.Id ID =
            new ParticleControllerDataBuffer.Id(new ResourceLocation("reiparticlesapi", "boolean"));

    @Override
    public byte[] encode(Boolean value) {
        return new byte[]{(byte) (Boolean.TRUE.equals(value) ? 1 : 0)};
    }

    @Override
    public Boolean decode(byte[] buf) {
        return buf.length > 0 && buf[0] != 0;
    }

    @Override
    public ParticleControllerDataBuffer.Id getBufferID() {
        return ID;
    }
}

