// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.buffer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;

public final class Vec3dControllerBuffer extends AbstractControllerBuffer<Vec3> {
    public static final ParticleControllerDataBuffer.Id ID =
            new ParticleControllerDataBuffer.Id(new ResourceLocation("reiparticlesapi", "vec3d"));

    @Override
    public byte[] encode(Vec3 value) {
        Vec3 safe = value == null ? Vec3.ZERO : value;
        return ByteBuffer.allocate(24)
                .putDouble(safe.x)
                .putDouble(safe.y)
                .putDouble(safe.z)
                .array();
    }

    @Override
    public Vec3 decode(byte[] buf) {
        if (buf.length < 24) {
            return Vec3.ZERO;
        }
        ByteBuffer buffer = ByteBuffer.wrap(buf);
        return new Vec3(buffer.getDouble(), buffer.getDouble(), buffer.getDouble());
    }

    @Override
    public ParticleControllerDataBuffer.Id getBufferID() {
        return ID;
    }
}

