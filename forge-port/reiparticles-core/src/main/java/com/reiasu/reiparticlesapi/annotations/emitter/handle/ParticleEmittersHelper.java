// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.annotations.emitter.handle;

import com.reiasu.reiparticlesapi.annotations.CodecField;
import com.reiasu.reiparticlesapi.annotations.codec.BufferCodec;
import com.reiasu.reiparticlesapi.annotations.codec.CodecHelper;
import com.reiasu.reiparticlesapi.network.particle.emitters.ClassParticleEmitters;
import com.reiasu.reiparticlesapi.network.particle.emitters.ParticleEmitters;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Helper for {@link ClassParticleEmitters} that provides:
 * <ul>
 *   <li>Reflection-based field copy via {@link #updateEmitter}</li>
 *   <li>Auto-generated codec (encode/decode) based on {@link CodecField} annotations</li>
 * </ul>
 */
public final class ParticleEmittersHelper {
    public static final ParticleEmittersHelper INSTANCE = new ParticleEmittersHelper();

    private ParticleEmittersHelper() {}

    /**
     * Copies all {@link CodecField}-annotated fields from {@code other} to {@code current},
     * provided they share the same emitter ID.
     */
    public void updateEmitter(ClassParticleEmitters current, ClassParticleEmitters other) {
        if (current == null || other == null) return;
        if (!current.getEmittersID().equals(other.getEmittersID())) return;
        CodecHelper.INSTANCE.updateFields(current, other);
    }

    /**
     * Generates a {@link BufferCodec} for the given {@link ClassParticleEmitters} subclass
     * by reflecting over its {@link CodecField}-annotated fields.
     * <p>
     * The encoder writes the base emitter data followed by each annotated field
     * (sorted by field name). The decoder constructs a new instance via the
     * {@code (Vec3, Level)} constructor, reads base data, then reads each field.
     */
    public BufferCodec<ParticleEmitters> generateCodec(ClassParticleEmitters randomInstance) {
        Class<?> type = randomInstance.getClass();

        Constructor<?> constructor;
        try {
            constructor = type.getConstructor(Vec3.class, net.minecraft.world.level.Level.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "ClassParticleEmitters subclass " + type.getName()
                            + " must have a constructor (Vec3, Level)", e);
        }

        return BufferCodec.of(
                (buf, emitter) -> encodeEmitter(type, buf, (ClassParticleEmitters) emitter),
                buf -> decodeEmitter(constructor, type, buf)
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void encodeEmitter(Class<?> type, FriendlyByteBuf buf, ClassParticleEmitters emitter) {
        ClassParticleEmitters.Companion.encodeBase(emitter, buf);
        List<Field> fields = getCodecFields(type);
        for (Field field : fields) {
            field.setAccessible(true);
            String codecKey = field.getType().getName();
            BufferCodec codec = CodecHelper.INSTANCE.getSupposedTypes().get(codecKey);
            if (codec == null) {
                throw new IllegalArgumentException(
                        "Unsupported codec type: " + codecKey + "; register it via CodecHelper.INSTANCE.register()");
            }
            try {
                codec.encode(buf, field.get(emitter));
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to encode field: " + field.getName(), e);
            }
        }
    }

    private ParticleEmitters decodeEmitter(Constructor<?> constructor, Class<?> type, FriendlyByteBuf buf) {
        ClassParticleEmitters instance;
        try {
            instance = (ClassParticleEmitters) constructor.newInstance(Vec3.ZERO, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct emitter: " + type.getName(), e);
        }

        ClassParticleEmitters.Companion.decodeBase(instance, buf);

        List<Field> fields = getCodecFields(type);
        for (Field field : fields) {
            field.setAccessible(true);
            String codecKey = field.getType().getName();
            BufferCodec<?> codec = CodecHelper.INSTANCE.getSupposedTypes().get(codecKey);
            if (codec == null) {
                throw new IllegalArgumentException(
                        "Unsupported codec type: " + codecKey + "; register it via CodecHelper.INSTANCE.register()");
            }
            try {
                Object value = codec.decode(buf);
                field.set(instance, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to decode field: " + field.getName(), e);
            }
        }
        return instance;
    }

    /**
     * Returns all {@link CodecField}-annotated, non-final fields of the given class,
     * sorted by field name for deterministic serialization order.
     */
    private List<Field> getCodecFields(Class<?> type) {
        Field[] allFields = type.getDeclaredFields();
        List<Field> result = new ArrayList<>();
        for (Field f : allFields) {
            if (f.isAnnotationPresent(CodecField.class) && !Modifier.isFinal(f.getModifiers())) {
                result.add(f);
            }
        }
        result.sort(Comparator.comparing(Field::getName));
        return result;
    }
}
