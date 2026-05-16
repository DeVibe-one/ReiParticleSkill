// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.annotations.composition.handler;

import com.reiasu.reiparticlesapi.annotations.CodecField;
import com.reiasu.reiparticlesapi.annotations.codec.BufferCodec;
import com.reiasu.reiparticlesapi.annotations.codec.CodecHelper;
import com.reiasu.reiparticlesapi.network.particle.composition.ParticleComposition;
import com.reiasu.reiparticlesapi.network.particle.composition.SequencedParticleComposition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Singleton helper that auto-generates {@link BufferCodec} instances for
 * {@link ParticleComposition} subclasses by scanning {@link CodecField}
 * annotated fields via reflection.
 * <p>
 * Fields are sorted by {@link CodecField#index()} for deterministic encode/decode order.
 * The codec also handles the base composition fields via
 * {@link ParticleComposition#encodeBase}/{@link ParticleComposition#decodeBase}
 * (and the sequenced variant for {@link SequencedParticleComposition} subclasses).
 */
public final class ParticleCompositionHelper {

    public static final ParticleCompositionHelper INSTANCE = new ParticleCompositionHelper();

    private ParticleCompositionHelper() {
    }

    /**
     * Generates a {@link BufferCodec} for the concrete composition type.
     * <p>
     * The composition class must have a constructor {@code (Vec3, Level)} for
     * reflective instantiation during decode.
     *
     * @param randomInstance any instance of the target composition class
     *                       (used only to determine the concrete type)
     * @return a codec that encodes/decodes the full composition state
     */
    public BufferCodec<ParticleComposition> generateCodec(ParticleComposition randomInstance) {
        Class<?> type = randomInstance.getClass();

        Constructor<?> constructor;
        try {
            constructor = type.getConstructor(Vec3.class, Level.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "Composition class " + type.getName() +
                    " must have a public constructor (Vec3, Level)", e);
        }

        return BufferCodec.of(
                (buf, composition) -> encodeComposition(type, buf, composition),
                buf -> decodeComposition(constructor, type, buf)
        );
    }

    private static void encodeComposition(Class<?> type, FriendlyByteBuf buf,
                                           ParticleComposition composition) {
        if (composition instanceof SequencedParticleComposition seq) {
            SequencedParticleComposition.encodeBase(seq, buf);
        } else {
            ParticleComposition.encodeBase(composition, buf);
        }

        List<Field> fields = CodecHelper.INSTANCE.getCodecFields(type);
        for (Field field : fields) {
            BufferCodec<Object> codec = getCodecOrThrow(field.getType());
            try {
                codec.encode(buf, field.get(composition));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to encode field " + field.getName(), e);
            }
        }
    }

    private static ParticleComposition decodeComposition(Constructor<?> constructor,
                                                          Class<?> type,
                                                          FriendlyByteBuf buf) {
        ParticleComposition instance;
        try {
            instance = (ParticleComposition) constructor.newInstance(Vec3.ZERO, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate " + type.getName(), e);
        }

        if (instance instanceof SequencedParticleComposition seq) {
            SequencedParticleComposition.decodeBase(seq, buf);
        } else {
            ParticleComposition.decodeBase(instance, buf);
        }

        List<Field> fields = CodecHelper.INSTANCE.getCodecFields(type);
        for (Field field : fields) {
            BufferCodec<Object> codec = getCodecOrThrow(field.getType());
            try {
                Object value = codec.decode(buf);
                field.set(instance, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to decode field " + field.getName(), e);
            }
        }

        return instance;
    }

    @SuppressWarnings("unchecked")
    private static BufferCodec<Object> getCodecOrThrow(Class<?> fieldType) {
        return (BufferCodec<Object>) CodecHelper.INSTANCE.getCodecOrThrow(fieldType);
    }
}
