// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.buffer;

import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class UUIDControllerBuffer extends AbstractControllerBuffer<UUID> {
    public static final ParticleControllerDataBuffer.Id ID =
            new ParticleControllerDataBuffer.Id(new ResourceLocation("reiparticlesapi", "uuid"));

    @Override
    public byte[] encode(UUID value) {
        UUID safe = value == null ? new UUID(0L, 0L) : value;
        return safe.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public UUID decode(byte[] buf) {
        if (buf.length == 0) {
            return new UUID(0L, 0L);
        }
        return UUID.fromString(new String(buf, StandardCharsets.UTF_8));
    }

    @Override
    public ParticleControllerDataBuffer.Id getBufferID() {
        return ID;
    }
}

