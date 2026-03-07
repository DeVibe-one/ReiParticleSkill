// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.annotations.codec;

import com.reiasu.reiparticlesapi.annotations.CodecField;
import com.reiasu.reiparticlesapi.barrages.HitBox;
import com.reiasu.reiparticlesapi.network.particle.data.DoubleRangeData;
import com.reiasu.reiparticlesapi.network.particle.data.FloatRangeData;
import com.reiasu.reiparticlesapi.network.particle.data.IntRangeData;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton codec registry for serializing annotated {@link CodecField} fields
 * to/from {@link FriendlyByteBuf}.
 * <p>
 * Replaces the original Fabric implementation that used {@code StreamCodec}
 * (which does not exist in Forge 1.20.1). Instead uses {@link BufferCodec}.
 */
public final class CodecHelper {

    public static final CodecHelper INSTANCE = new CodecHelper();
    private static final Logger LOGGER = LogUtils.getLogger();

    private final HashMap<String, BufferCodec<?>> supposedTypes = new HashMap<>();

    private CodecHelper() {
    }

    public Map<String, BufferCodec<?>> getSupposedTypes() {
        return supposedTypes;
    }

    /**
     * Registers a codec for the given type.
     */
    public <T> void register(Class<T> type, BufferCodec<T> codec) {
        supposedTypes.put(type.getName(), codec);
    }

    /**
     * Copies all {@link CodecField}-annotated, non-final fields from {@code other}
     * to {@code current} via reflection. Both objects must be the same class.
     * Fields are processed in stable {@link CodecField#index()} order.
     */
    public void updateFields(Object current, Object other) {
        if (current == null || other == null) return;
        if (!current.getClass().equals(other.getClass())) return;

        for (Field field : getCodecFields(current.getClass())) {
            field.setAccessible(true);
            try {
                field.set(current, field.get(other));
            } catch (IllegalAccessException e) {
                // Shouldn't happen since we called setAccessible(true)
            }
        }
    }

    /**
     * Encodes all {@link CodecField}-annotated fields of {@code obj} into {@code buf},
     * in stable {@link CodecField#index()} order.
     *
     * @throws IllegalStateException if a field's type has no registered codec
     */
    @SuppressWarnings("unchecked")
    public void encodeAnnotatedFields(FriendlyByteBuf buf, Object obj) {
        for (Field field : getCodecFields(obj.getClass())) {
            field.setAccessible(true);
            BufferCodec<Object> codec = (BufferCodec<Object>) supposedTypes.get(field.getType().getName());
            if (codec == null) {
                throw new IllegalStateException("No codec registered for type: " + field.getType().getName());
            }
            try {
                codec.encode(buf, field.get(obj));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to read field: " + field.getName(), e);
            }
        }
    }

    /**
     * Decodes all {@link CodecField}-annotated fields of {@code obj} from {@code buf},
     * in stable {@link CodecField#index()} order.
     *
     * @throws IllegalStateException if a field's type has no registered codec
     */
    @SuppressWarnings("unchecked")
    public void decodeAnnotatedFields(FriendlyByteBuf buf, Object obj) {
        for (Field field : getCodecFields(obj.getClass())) {
            field.setAccessible(true);
            BufferCodec<Object> codec = (BufferCodec<Object>) supposedTypes.get(field.getType().getName());
            if (codec == null) {
                throw new IllegalStateException("No codec registered for type: " + field.getType().getName());
            }
            try {
                field.set(obj, codec.decode(buf));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to set field: " + field.getName(), e);
            }
        }
    }

    /**
     * Returns {@link CodecField}-annotated non-final fields sorted by
     * {@link CodecField#index()} then by field name for deterministic ordering.
     */
    private static List<Field> getCodecFields(Class<?> clazz) {
        List<Field> result = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(CodecField.class) && !Modifier.isFinal(f.getModifiers())) {
                result.add(f);
            }
        }
        result.sort(Comparator.comparingInt((Field f) -> f.getAnnotation(CodecField.class).index())
                .thenComparing(Field::getName));
        return result;
    }

    // ────────────────── Static registration of built-in types ──────────────────

    static {
        registerPrimitives();
        registerArrays();
        registerJavaTypes();
        registerMathTypes();
        registerMinecraftTypes();
        registerProjectTypes();
        registerRangeTypes();
        // NOTE: ControllableParticleData, SimpleRandomParticleData, ItemStack,
        // and all InterpolatorXxx types should register themselves via
        //   CodecHelper.INSTANCE.register(MyClass.class, myCodec)
        // in their own static initializers or during mod init, since their
        // codecs are not yet available as static fields in Forge 1.20.1.
    }

    private static void registerPrimitives() {
        BufferCodec<Short> shortCodec = BufferCodec.of(
                (buf, v) -> buf.writeShort(v.shortValue()), FriendlyByteBuf::readShort);
        INSTANCE.register(Short.TYPE, shortCodec);
        INSTANCE.register(Short.class, shortCodec);

        BufferCodec<Integer> intCodec = BufferCodec.of(
                (buf, v) -> buf.writeInt(v), FriendlyByteBuf::readInt);
        INSTANCE.register(Integer.TYPE, intCodec);
        INSTANCE.register(Integer.class, intCodec);

        BufferCodec<Long> longCodec = BufferCodec.of(
                (buf, v) -> buf.writeLong(v), FriendlyByteBuf::readLong);
        INSTANCE.register(Long.TYPE, longCodec);
        INSTANCE.register(Long.class, longCodec);

        BufferCodec<Float> floatCodec = BufferCodec.of(
                (buf, v) -> buf.writeFloat(v), FriendlyByteBuf::readFloat);
        INSTANCE.register(Float.TYPE, floatCodec);
        INSTANCE.register(Float.class, floatCodec);

        BufferCodec<Double> doubleCodec = BufferCodec.of(
                (buf, v) -> buf.writeDouble(v), FriendlyByteBuf::readDouble);
        INSTANCE.register(Double.TYPE, doubleCodec);
        INSTANCE.register(Double.class, doubleCodec);

        BufferCodec<Byte> byteCodec = BufferCodec.of(
                (buf, v) -> buf.writeByte(v.byteValue()), FriendlyByteBuf::readByte);
        INSTANCE.register(Byte.TYPE, byteCodec);
        INSTANCE.register(Byte.class, byteCodec);

        BufferCodec<Boolean> boolCodec = BufferCodec.of(
                FriendlyByteBuf::writeBoolean, FriendlyByteBuf::readBoolean);
        INSTANCE.register(Boolean.TYPE, boolCodec);
        INSTANCE.register(Boolean.class, boolCodec);

        BufferCodec<Character> charCodec = BufferCodec.of(
                (buf, v) -> buf.writeChar(v.charValue()), FriendlyByteBuf::readChar);
        INSTANCE.register(Character.TYPE, charCodec);
        INSTANCE.register(Character.class, charCodec);
    }

    private static void registerArrays() {
        INSTANCE.register(byte[].class, BufferCodec.of(
                FriendlyByteBuf::writeByteArray, FriendlyByteBuf::readByteArray));
        INSTANCE.register(long[].class, BufferCodec.of(
                FriendlyByteBuf::writeLongArray, FriendlyByteBuf::readLongArray));
        INSTANCE.register(int[].class, BufferCodec.of(
                FriendlyByteBuf::writeVarIntArray, FriendlyByteBuf::readVarIntArray));
        INSTANCE.register(float[].class, BufferCodec.of(
                (buf, arr) -> { buf.writeVarInt(arr.length); for (float f : arr) buf.writeFloat(f); },
                buf -> { float[] a = new float[buf.readVarInt()]; for (int i = 0; i < a.length; i++) a[i] = buf.readFloat(); return a; }));
        INSTANCE.register(double[].class, BufferCodec.of(
                (buf, arr) -> { buf.writeVarInt(arr.length); for (double d : arr) buf.writeDouble(d); },
                buf -> { double[] a = new double[buf.readVarInt()]; for (int i = 0; i < a.length; i++) a[i] = buf.readDouble(); return a; }));
        INSTANCE.register(short[].class, BufferCodec.of(
                (buf, arr) -> { buf.writeVarInt(arr.length); for (short s : arr) buf.writeShort(s); },
                buf -> { short[] a = new short[buf.readVarInt()]; for (int i = 0; i < a.length; i++) a[i] = buf.readShort(); return a; }));
    }

    private static void registerJavaTypes() {
        INSTANCE.register(String.class, BufferCodec.of(
                FriendlyByteBuf::writeUtf,
                FriendlyByteBuf::readUtf
        ));
        INSTANCE.register(UUID.class, BufferCodec.of(
                FriendlyByteBuf::writeUUID,
                FriendlyByteBuf::readUUID
        ));
    }

    private static void registerMathTypes() {
        INSTANCE.register(Vector3f.class, BufferCodec.of(
                (buf, v) -> {
                    buf.writeFloat(v.x());
                    buf.writeFloat(v.y());
                    buf.writeFloat(v.z());
                },
                buf -> new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat())
        ));
        INSTANCE.register(Quaternionf.class, BufferCodec.of(
                (buf, q) -> {
                    buf.writeFloat(q.x());
                    buf.writeFloat(q.y());
                    buf.writeFloat(q.z());
                    buf.writeFloat(q.w());
                },
                buf -> new Quaternionf(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat())
        ));
    }

    private static void registerMinecraftTypes() {
        INSTANCE.register(Vec3.class, BufferCodec.of(
                (buf, v) -> {
                    buf.writeDouble(v.x());
                    buf.writeDouble(v.y());
                    buf.writeDouble(v.z());
                },
                buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
        ));
        INSTANCE.register(AABB.class, BufferCodec.of(
                (buf, a) -> {
                    buf.writeDouble(a.minX);
                    buf.writeDouble(a.minY);
                    buf.writeDouble(a.minZ);
                    buf.writeDouble(a.maxX);
                    buf.writeDouble(a.maxY);
                    buf.writeDouble(a.maxZ);
                },
                buf -> new AABB(
                        buf.readDouble(), buf.readDouble(), buf.readDouble(),
                        buf.readDouble(), buf.readDouble(), buf.readDouble()
                )
        ));
    }

    private static void registerProjectTypes() {
        INSTANCE.register(HitBox.class, BufferCodec.of(
                (buf, h) -> {
                    buf.writeDouble(h.getX1());
                    buf.writeDouble(h.getY1());
                    buf.writeDouble(h.getZ1());
                    buf.writeDouble(h.getX2());
                    buf.writeDouble(h.getY2());
                    buf.writeDouble(h.getZ2());
                },
                buf -> new HitBox(
                        buf.readDouble(), buf.readDouble(), buf.readDouble(),
                        buf.readDouble(), buf.readDouble(), buf.readDouble()
                )
        ));
        INSTANCE.register(RelativeLocation.class, BufferCodec.of(
                (buf, r) -> {
                    buf.writeDouble(r.getX());
                    buf.writeDouble(r.getY());
                    buf.writeDouble(r.getZ());
                },
                buf -> new RelativeLocation(buf.readDouble(), buf.readDouble(), buf.readDouble())
        ));
    }

    private static void registerRangeTypes() {
        INSTANCE.register(DoubleRangeData.class, BufferCodec.of(
                (buf, d) -> {
                    buf.writeDouble(d.min());
                    buf.writeDouble(d.max());
                },
                buf -> new DoubleRangeData(buf.readDouble(), buf.readDouble())
        ));
        INSTANCE.register(IntRangeData.class, BufferCodec.of(
                (buf, d) -> {
                    buf.writeInt(d.getMin());
                    buf.writeInt(d.getMax());
                },
                buf -> new IntRangeData(buf.readInt(), buf.readInt())
        ));
        INSTANCE.register(FloatRangeData.class, BufferCodec.of(
                (buf, d) -> {
                    buf.writeFloat(d.getMin());
                    buf.writeFloat(d.getMax());
                },
                buf -> new FloatRangeData(buf.readFloat(), buf.readFloat())
        ));
    }
}
