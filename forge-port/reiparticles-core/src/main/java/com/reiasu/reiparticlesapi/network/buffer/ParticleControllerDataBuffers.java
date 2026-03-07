// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.network.buffer;

import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ParticleControllerDataBuffers {
    public static final ParticleControllerDataBuffers INSTANCE = new ParticleControllerDataBuffers();

    private final Map<Class<?>, Class<?>> wrapperToPrimitive = new ConcurrentHashMap<>();
    private final Map<ParticleControllerDataBuffer.Id, Supplier<? extends ParticleControllerDataBuffer<?>>> registerBuilder =
            new ConcurrentHashMap<>();
    private final Map<Class<?>, ParticleControllerDataBuffer.Id> registerTypes = new ConcurrentHashMap<>();

    private ParticleControllerDataBuffers() {
        wrapperToPrimitive.put(Integer.class, int.class);
        wrapperToPrimitive.put(Double.class, double.class);
        wrapperToPrimitive.put(Long.class, long.class);
        wrapperToPrimitive.put(Float.class, float.class);
        wrapperToPrimitive.put(Boolean.class, boolean.class);
        wrapperToPrimitive.put(Short.class, short.class);

        register(boolean.class, BooleanControllerBuffer.ID, BooleanControllerBuffer::new);
        register(long.class, LongControllerBuffer.ID, LongControllerBuffer::new);
        register(int.class, IntControllerBuffer.ID, IntControllerBuffer::new);
        register(double.class, DoubleControllerBuffer.ID, DoubleControllerBuffer::new);
        register(float.class, FloatControllerBuffer.ID, FloatControllerBuffer::new);
        register(String.class, StringControllerBuffer.ID, StringControllerBuffer::new);
        register(int[].class, IntArrayControllerBuffer.ID, IntArrayControllerBuffer::new);
        register(long[].class, LongArrayControllerBuffer.ID, LongArrayControllerBuffer::new);
        register(UUID.class, UUIDControllerBuffer.ID, UUIDControllerBuffer::new);
        register(Vec3.class, Vec3dControllerBuffer.ID, Vec3dControllerBuffer::new);
        register(RelativeLocation.class, RelativeLocationControllerBuffer.ID, RelativeLocationControllerBuffer::new);
        register(short.class, ShortControllerBuffer.ID, ShortControllerBuffer::new);
        register(Void.class, EmptyControllerBuffer.ID, EmptyControllerBuffer::new);
    }

    public Map<ParticleControllerDataBuffer.Id, Supplier<? extends ParticleControllerDataBuffer<?>>> getRegisterBuilder() {
        return registerBuilder;
    }

    public Map<Class<?>, ParticleControllerDataBuffer.Id> getRegisterTypes() {
        return registerTypes;
    }

    public BooleanControllerBuffer bool(boolean value) {
        BooleanControllerBuffer buffer = new BooleanControllerBuffer();
        buffer.setLoadedValue(value);
        return buffer;
    }

    public StringControllerBuffer string(String value) {
        StringControllerBuffer buffer = new StringControllerBuffer();
        buffer.setLoadedValue(value);
        return buffer;
    }

    public IntControllerBuffer intValue(int value) {
        IntControllerBuffer buffer = new IntControllerBuffer();
        buffer.setLoadedValue(value);
        return buffer;
    }

    public DoubleControllerBuffer doubleValue(double value) {
        DoubleControllerBuffer buffer = new DoubleControllerBuffer();
        buffer.setLoadedValue(value);
        return buffer;
    }

    public FloatControllerBuffer floatValue(float value) {
        FloatControllerBuffer buffer = new FloatControllerBuffer();
        buffer.setLoadedValue(value);
        return buffer;
    }

    public LongControllerBuffer longValue(long value) {
        LongControllerBuffer buffer = new LongControllerBuffer();
        buffer.setLoadedValue(value);
        return buffer;
    }

    public ShortControllerBuffer shortValue(short value) {
        ShortControllerBuffer buffer = new ShortControllerBuffer();
        buffer.setLoadedValue(value);
        return buffer;
    }

    public IntArrayControllerBuffer intArray(int[] value) {
        IntArrayControllerBuffer buffer = new IntArrayControllerBuffer();
        buffer.setLoadedValue(value);
        return buffer;
    }

    public LongArrayControllerBuffer longArray(long[] value) {
        LongArrayControllerBuffer buffer = new LongArrayControllerBuffer();
        buffer.setLoadedValue(value);
        return buffer;
    }

    public UUIDControllerBuffer uuid(UUID value) {
        UUIDControllerBuffer buffer = new UUIDControllerBuffer();
        buffer.setLoadedValue(value);
        return buffer;
    }

    public Vec3dControllerBuffer vec3d(Vec3 value) {
        Vec3dControllerBuffer buffer = new Vec3dControllerBuffer();
        buffer.setLoadedValue(value);
        return buffer;
    }

    public RelativeLocationControllerBuffer relative(RelativeLocation value) {
        RelativeLocationControllerBuffer buffer = new RelativeLocationControllerBuffer();
        buffer.setLoadedValue(value);
        return buffer;
    }

    public EmptyControllerBuffer empty() {
        return new EmptyControllerBuffer();
    }

    public void register(
            Class<?> bufferType,
            ParticleControllerDataBuffer.Id id,
            Supplier<? extends ParticleControllerDataBuffer<?>> supplier
    ) {
        registerBuilder.put(id, supplier);
        registerTypes.put(bufferType, id);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public ParticleControllerDataBuffer<?> withType(Object value, Class<? extends ParticleControllerDataBuffer<?>> clazz) {
        ParticleControllerDataBuffer<?> instance = newInstance(clazz);
        ((ParticleControllerDataBuffer) instance).setLoadedValue(value);
        return instance;
    }

    public ParticleControllerDataBuffer<?> fromBufferType(Object value, Class<?> clazz) {
        ParticleControllerDataBuffer.Id id = registerTypes.get(clazz);
        if (id == null) {
            Class<?> primitiveType = wrapperToPrimitive.get(clazz);
            if (primitiveType != null) {
                id = registerTypes.get(primitiveType);
            }
        }
        if (id == null) {
            return null;
        }
        return withId(id, value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public ParticleControllerDataBuffer<?> withId(ParticleControllerDataBuffer.Id id, Object value) {
        Supplier<? extends ParticleControllerDataBuffer<?>> supplier = registerBuilder.get(id);
        if (supplier == null) {
            return null;
        }
        ParticleControllerDataBuffer<?> buffer = supplier.get();
        ((ParticleControllerDataBuffer) buffer).setLoadedValue(value);
        return buffer;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public ParticleControllerDataBuffer<?> withIdDecode(ParticleControllerDataBuffer.Id id, byte[] array) {
        Supplier<? extends ParticleControllerDataBuffer<?>> supplier = registerBuilder.get(id);
        if (supplier == null) {
            return null;
        }
        ParticleControllerDataBuffer<?> buffer = supplier.get();
        Object decoded = buffer.decode(array);
        ((ParticleControllerDataBuffer) buffer).setLoadedValue(decoded);
        return buffer;
    }

    public ParticleControllerDataBuffer<?> withId(ResourceLocation id, Object value) {
        return withId(new ParticleControllerDataBuffer.Id(id), value);
    }

    public ParticleControllerDataBuffer<?> withIdDecode(ResourceLocation id, byte[] array) {
        return withIdDecode(new ParticleControllerDataBuffer.Id(id), array);
    }

    public <T> byte[] encode(ParticleControllerDataBuffer<T> buffer) {
        byte[] payload = buffer.encode();
        String className = buffer.getClass().getName();
        return wrapPayload(className, payload);
    }

    public <T> byte[] encode(T value, ParticleControllerDataBuffer<T> buffer) {
        byte[] payload = buffer.encode(value);
        String className = buffer.getClass().getName();
        return wrapPayload(className, payload);
    }

    @SuppressWarnings("unchecked")
    public <T> T decode(byte[] bytes) {
        return (T) decodeToBuffer(bytes).getLoadedValue();
    }

    @SuppressWarnings("unchecked")
    public <T> ParticleControllerDataBuffer<T> decodeToBuffer(byte[] bytes) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            int classLen = input.readInt();
            byte[] classBytes = input.readNBytes(classLen);
            String className = new String(classBytes, StandardCharsets.UTF_8);
            int payloadLen = input.readInt();
            byte[] payload = input.readNBytes(payloadLen);

            Class<?> clazz = Class.forName(className);
            if (!ParticleControllerDataBuffer.class.isAssignableFrom(clazz)) {
                throw new IllegalStateException("Not a buffer class: " + className);
            }
            ParticleControllerDataBuffer<?> instance = newInstance((Class<? extends ParticleControllerDataBuffer<?>>) clazz);
            Object decoded = instance.decode(payload);
            //noinspection rawtypes,unchecked
            ((ParticleControllerDataBuffer) instance).setLoadedValue(decoded);
            return (ParticleControllerDataBuffer<T>) instance;
        } catch (IOException | ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to decode buffer payload", e);
        }
    }

    private static byte[] wrapPayload(String className, byte[] payload) {
        try (ByteArrayOutputStream raw = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(raw)) {
            byte[] classBytes = className.getBytes(StandardCharsets.UTF_8);
            output.writeInt(classBytes.length);
            output.write(classBytes);
            output.writeInt(payload.length);
            output.write(payload);
            output.flush();
            return raw.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode buffer payload", e);
        }
    }

    private static <T extends ParticleControllerDataBuffer<?>> T newInstance(Class<T> clazz) {
        try {
            Constructor<T> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate buffer " + clazz.getName(), e);
        }
    }
}
