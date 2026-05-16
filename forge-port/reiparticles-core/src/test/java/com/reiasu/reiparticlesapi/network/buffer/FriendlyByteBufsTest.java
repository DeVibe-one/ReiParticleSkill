// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.buffer;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FriendlyByteBufsTest {
    @Test
    void shouldReleaseWrappedBufferAfterSuccessfulDecode() {
        AtomicReference<FriendlyByteBuf> seen = new AtomicReference<>();

        int decoded = FriendlyByteBufs.decodeFromByteArray(new byte[]{42}, buf -> {
            seen.set(buf);
            return buf.readUnsignedByte();
        });

        assertEquals(42, decoded);
        assertEquals(0, seen.get().refCnt());
    }

    @Test
    void shouldReleaseWrappedBufferWhenDecoderThrows() {
        AtomicReference<FriendlyByteBuf> seen = new AtomicReference<>();

        assertThrows(IllegalStateException.class, () ->
                FriendlyByteBufs.decodeFromByteArray(new byte[]{1}, buf -> {
                    seen.set(buf);
                    throw new IllegalStateException("boom");
                }));

        assertEquals(0, seen.get().refCnt());
    }

    @Test
    void shouldReadPayloadWhenLengthIsValid() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(new byte[]{1, 2, 3}));

        try {
            assertArrayEquals(new byte[]{1, 2}, FriendlyByteBufs.readPayload(buf, 2, "test payload"));
            assertEquals(1, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void shouldRejectNegativePayloadLength() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.EMPTY_BUFFER);

        try {
            assertThrows(IllegalArgumentException.class,
                    () -> FriendlyByteBufs.readPayload(buf, -1, "test payload"));
        } finally {
            buf.release();
        }
    }

    @Test
    void shouldRejectPayloadLengthAboveLimit() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.EMPTY_BUFFER);

        try {
            assertThrows(IllegalArgumentException.class,
                    () -> FriendlyByteBufs.readPayload(buf, 3, 2, "test payload"));
        } finally {
            buf.release();
        }
    }

    @Test
    void shouldRejectPayloadLengthAboveReadableBytes() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(new byte[]{1}));

        try {
            assertThrows(IllegalArgumentException.class,
                    () -> FriendlyByteBufs.readPayload(buf, 2, "test payload"));
        } finally {
            buf.release();
        }
    }
}
