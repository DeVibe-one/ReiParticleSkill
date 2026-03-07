/*
 * Copyright (C) 2025 Reiasu
 *
 * This file is part of ReiParticlesAPI.
 *
 * ReiParticlesAPI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * ReiParticlesAPI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReiParticlesAPI. If not, see <https://www.gnu.org/licenses/>.
 */
// SPDX-License-Identifier: LGPL-3.0-only
package com.reiasu.reiparticlesapi.network.buffer;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ParticleControllerDataBuffersTest {
    @Test
    void shouldRoundTripBufferWithEnvelopeEncoding() {
        StringControllerBuffer source = ParticleControllerDataBuffers.INSTANCE.string("forge-runtime");
        byte[] encoded = ParticleControllerDataBuffers.INSTANCE.encode(source);
        ParticleControllerDataBuffer<?> decoded = ParticleControllerDataBuffers.INSTANCE.decodeToBuffer(encoded);

        assertInstanceOf(StringControllerBuffer.class, decoded);
        assertEquals("forge-runtime", decoded.getLoadedValue());
    }

    @Test
    void shouldDecodeByBufferId() {
        UUID uuid = UUID.randomUUID();
        UUIDControllerBuffer source = ParticleControllerDataBuffers.INSTANCE.uuid(uuid);

        ParticleControllerDataBuffer<?> decoded =
                ParticleControllerDataBuffers.INSTANCE.withIdDecode(UUIDControllerBuffer.ID, source.encode());

        assertNotNull(decoded);
        assertEquals(uuid, decoded.getLoadedValue());
    }

    @Test
    void shouldResolveWrapperClassToPrimitiveRegistration() {
        ParticleControllerDataBuffer<?> buffer = ParticleControllerDataBuffers.INSTANCE.fromBufferType(42, Integer.class);

        assertNotNull(buffer);
        assertInstanceOf(IntControllerBuffer.class, buffer);
        assertEquals(42, buffer.getLoadedValue());
    }

    @Test
    void shouldRoundTripPrimitiveArrays() {
        int[] input = new int[]{2, 4, 6, 8};
        IntArrayControllerBuffer source = ParticleControllerDataBuffers.INSTANCE.intArray(input);

        ParticleControllerDataBuffer<?> decoded =
                ParticleControllerDataBuffers.INSTANCE.withIdDecode(IntArrayControllerBuffer.ID, source.encode());

        assertNotNull(decoded);
        assertArrayEquals(input, (int[]) decoded.getLoadedValue());
    }
}

