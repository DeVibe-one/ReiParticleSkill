// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.buffer;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Consumer;
import java.util.function.Function;

public final class FriendlyByteBufs {
    public static final int MAX_PAYLOAD_BYTES = 1_048_576;

    private FriendlyByteBufs() {
    }

    public static byte[] encodeToByteArray(Consumer<FriendlyByteBuf> writer) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writer.accept(buf);
            return ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), true);
        } finally {
            buf.release();
        }
    }

    public static <T> T decodeFromByteArray(byte[] data, Function<FriendlyByteBuf, T> decoder) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        try {
            return decoder.apply(buf);
        } finally {
            buf.release();
        }
    }

    public static byte[] readPayload(FriendlyByteBuf buf, int size, String name) {
        return readPayload(buf, size, MAX_PAYLOAD_BYTES, name);
    }

    public static byte[] readPayload(FriendlyByteBuf buf, int size, int maxSize, String name) {
        String payloadName = name == null ? "payload" : name;
        if (size < 0) {
            throw new IllegalArgumentException(payloadName + " length must not be negative: " + size);
        }
        if (size > maxSize) {
            throw new IllegalArgumentException(
                    payloadName + " length exceeds max " + maxSize + ": " + size);
        }
        int readable = buf.readableBytes();
        if (size > readable) {
            throw new IllegalArgumentException(
                    payloadName + " length " + size + " exceeds readable bytes " + readable);
        }
        byte[] data = new byte[size];
        buf.readBytes(data);
        return data;
    }
}
