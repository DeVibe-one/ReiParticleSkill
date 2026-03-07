// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.annotations.codec;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Replacement for Fabric/Mojang's {@code StreamCodec<FriendlyByteBuf, T>} which
 * was introduced in Minecraft 1.20.5 and does not exist in Forge 1.20.1.
 * <p>
 * Provides symmetric encode/decode for a type {@code T} to/from a
 * {@link FriendlyByteBuf}.
 *
 * @param <T> the type to serialize
 */
public interface BufferCodec<T> {

    /**
     * Creates a {@link BufferCodec} from explicit encoder and decoder lambdas.
     */
    static <T> BufferCodec<T> of(Encoder<T> encoder, Decoder<T> decoder) {
        return new BufferCodec<T>() {
            @Override
            public void encode(FriendlyByteBuf buf, T value) {
                encoder.encode(buf, value);
            }

            @Override
            public T decode(FriendlyByteBuf buf) {
                return decoder.decode(buf);
            }
        };
    }

    /**
     * Writes {@code value} into the buffer.
     */
    void encode(FriendlyByteBuf buf, T value);

    /**
     * Reads a value from the buffer.
     */
    T decode(FriendlyByteBuf buf);

    @FunctionalInterface
    interface Encoder<T> {
        void encode(FriendlyByteBuf buf, T value);
    }

    @FunctionalInterface
    interface Decoder<T> {
        T decode(FriendlyByteBuf buf);
    }
}
