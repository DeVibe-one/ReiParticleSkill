// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.utils.helper.buffer;

import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffer;
import com.reiasu.reiparticlesapi.network.buffer.ParticleControllerDataBuffers;
import com.reiasu.reiparticlesapi.particles.Controllable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reflection-based helper that reads/writes fields annotated with
 * {@link ControllableBuffer} on a {@link Controllable} as buffer pairs.
 */
public final class ControllableBufferHelper {
    public static final ControllableBufferHelper INSTANCE = new ControllableBufferHelper();
    private static final Logger LOGGER = LoggerFactory.getLogger("ReiParticlesAPI");

    private ControllableBufferHelper() {
    }

    /**
     * Collect all {@link ControllableBuffer}-annotated fields from the given
     * {@link Controllable} and convert their values into buffer entries.
     */
    public Map<String, ParticleControllerDataBuffer<?>> getPairs(Controllable<?> buf) {
        Map<String, ParticleControllerDataBuffer<?>> res = new LinkedHashMap<>();
        List<Field> annotatedFields = getAnnotatedFields(buf.getClass());
        for (Field field : annotatedFields) {
            field.setAccessible(true);
            ControllableBuffer anno = field.getAnnotation(ControllableBuffer.class);
            if (anno == null) {
                continue;
            }
            try {
                Object value = field.get(buf);
                if (value == null) {
                    continue;
                }
                ParticleControllerDataBuffer<?> buffer =
                        ParticleControllerDataBuffers.INSTANCE.fromBufferType(value, value.getClass());
                if (buffer != null) {
                    res.put(anno.name(), buffer);
                }
            } catch (IllegalAccessException e) {
                LOGGER.warn("Failed to read field {} on {}", field.getName(), buf.getClass().getSimpleName(), e);
            }
        }
        return res;
    }

    /**
     * Write buffer values back into the {@link ControllableBuffer}-annotated fields
     * of the given {@link Controllable}.
     */
    public void setPairs(Controllable<?> buf, Map<String, ? extends ParticleControllerDataBuffer<?>> args) {
        List<Field> annotatedFields = getAnnotatedFields(buf.getClass());
        for (Field field : annotatedFields) {
            field.setAccessible(true);
            ControllableBuffer anno = field.getAnnotation(ControllableBuffer.class);
            if (anno == null) {
                continue;
            }
            ParticleControllerDataBuffer<?> value = args.get(anno.name());
            if (value == null) {
                continue;
            }
            if (Modifier.isFinal(field.getModifiers())) {
                LOGGER.warn("Cannot set final field {} to {}", field.getName(), value);
                continue;
            }
            try {
                field.set(buf, value.getLoadedValue());
            } catch (IllegalAccessException e) {
                LOGGER.warn("Failed to set field {} on {}", field.getName(), buf.getClass().getSimpleName(), e);
            }
        }
    }

    private List<Field> getAnnotatedFields(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        List<Field> result = new ArrayList<>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ControllableBuffer.class)) {
                result.add(field);
            }
        }
        return result;
    }
}
