// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.buffer;

import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Buffer that contains a list of nested ParticleControllerDataBuffer entries.
 * Encodes as: [count:int] then for each entry: [idLen:int][idString][dataLen:int][data].
 */
public final class NestedBuffersControllerBuffer extends AbstractControllerBuffer<List<ParticleControllerDataBuffer<?>>> {
    public static final ParticleControllerDataBuffer.Id ID =
            new ParticleControllerDataBuffer.Id(new ResourceLocation("reiparticlesapi", "nested_buffers"));

    @Override
    public byte[] encode(List<ParticleControllerDataBuffer<?>> value) {
        if (value == null || value.isEmpty()) {
            return new byte[]{0, 0, 0, 0};
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(value.size());
            for (ParticleControllerDataBuffer<?> buffer : value) {
                String idStr = buffer.getBufferID().value().toString();
                dos.writeUTF(idStr);
                byte[] data = buffer.encode();
                dos.writeInt(data.length);
                dos.write(data);
            }
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode nested buffers", e);
        }
    }

    @Override
    public List<ParticleControllerDataBuffer<?>> decode(byte[] buf) {
        List<ParticleControllerDataBuffer<?>> result = new ArrayList<>();
        if (buf == null || buf.length < 4) {
            return result;
        }
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            DataInputStream dis = new DataInputStream(bais);
            int count = dis.readInt();
            for (int i = 0; i < count; i++) {
                String idStr = dis.readUTF();
                int dataLen = dis.readInt();
                byte[] data = new byte[dataLen];
                dis.readFully(data);
                ParticleControllerDataBuffer.Id id =
                        new ParticleControllerDataBuffer.Id(new ResourceLocation(idStr));
                ParticleControllerDataBuffer<?> decoded =
                        ParticleControllerDataBuffers.INSTANCE.withIdDecode(id, data);
                if (decoded != null) {
                    result.add(decoded);
                }
            }
        } catch (IOException e) {
            // Partial read is acceptable
        }
        return result;
    }

    @Override
    public ParticleControllerDataBuffer.Id getBufferID() {
        return ID;
    }
}
