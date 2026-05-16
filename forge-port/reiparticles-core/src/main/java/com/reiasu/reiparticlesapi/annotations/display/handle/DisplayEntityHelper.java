// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.annotations.display.handle;

import com.reiasu.reiparticlesapi.annotations.CodecField;
import com.reiasu.reiparticlesapi.annotations.codec.BufferCodec;
import com.reiasu.reiparticlesapi.annotations.codec.CodecHelper;
import com.reiasu.reiparticlesapi.display.DisplayEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Singleton helper that auto-generates {@link BufferCodec} instances for
 * {@link DisplayEntity} subclasses by scanning {@link CodecField}
 * annotated fields via reflection.
 * <p>
 * Structurally identical to {@link com.reiasu.reiparticlesapi.annotations.composition.handler.ParticleCompositionHelper},
 * but targets DisplayEntity instead of ParticleComposition.
 */
public final class DisplayEntityHelper {

    public static final DisplayEntityHelper INSTANCE = new DisplayEntityHelper();

    private DisplayEntityHelper() {
    }

    /**
     * Generates a {@link BufferCodec} for the concrete display entity type.
     * <p>
     * The display entity class must have a constructor {@code (Vec3, Level)}
     * for reflective instantiation during decode. In practice the original
     * Fabric code uses {@code (Vec3, World)} which maps to {@code (Vec3, Level)}
     * in Forge MCP mappings.
     *
     * @param randomInstance any instance of the target display entity class
     * @return a codec that encodes/decodes the full display entity state
     */
    public BufferCodec<DisplayEntity> generateCodec(DisplayEntity randomInstance) {
        Class<?> type = randomInstance.getClass();

        Constructor<?> constructor;
        try {
            constructor = type.getConstructor(Vec3.class);
        } catch (NoSuchMethodException e) {
            try {
                constructor = type.getConstructor(Vec3.class, net.minecraft.world.level.Level.class);
            } catch (NoSuchMethodException e2) {
                throw new IllegalStateException(
                        "DisplayEntity class " + type.getName() +
                        " must have a public constructor (Vec3) or (Vec3, Level)", e2);
            }
        }

        Constructor<?> ctor = constructor;
        return BufferCodec.of(
                (buf, entity) -> encodeEntity(type, buf, entity),
                buf -> decodeEntity(ctor, type, buf)
        );
    }

    private static void encodeEntity(Class<?> type, FriendlyByteBuf buf,
                                      DisplayEntity entity) {
        DisplayEntity.encodeBase(entity, buf);

        List<Field> fields = CodecHelper.INSTANCE.getCodecFields(type);
        for (Field field : fields) {
            BufferCodec<Object> codec = getCodecOrThrow(field.getType());
            try {
                codec.encode(buf, field.get(entity));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to encode field " + field.getName(), e);
            }
        }
    }

    private static DisplayEntity decodeEntity(Constructor<?> constructor,
                                               Class<?> type,
                                               FriendlyByteBuf buf) {
        DisplayEntity instance;
        try {
            if (constructor.getParameterCount() == 1) {
                instance = (DisplayEntity) constructor.newInstance(Vec3.ZERO);
            } else {
                instance = (DisplayEntity) constructor.newInstance(Vec3.ZERO, null);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate " + type.getName(), e);
        }

        DisplayEntity.decodeBase(instance, buf);

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
