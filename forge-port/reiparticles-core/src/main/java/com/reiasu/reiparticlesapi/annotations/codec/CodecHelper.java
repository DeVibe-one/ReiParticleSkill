// SPDX-License-Identifier: LGPL-3.0-only
// Copyright (C) 2025 Reiasu
package com.reiasu.reiparticlesapi.annotations.codec;

import com.reiasu.reiparticlesapi.annotations.CodecField;
import com.reiasu.reiparticlesapi.barrages.HitBox;
import com.reiasu.reiparticlesapi.network.particle.data.DoubleRangeData;
import com.reiasu.reiparticlesapi.network.particle.data.FloatRangeData;
import com.reiasu.reiparticlesapi.network.particle.data.IntRangeData;
import com.reiasu.reiparticlesapi.network.particle.emitters.ControllableParticleData;
import com.reiasu.reiparticlesapi.network.particle.emitters.SimpleRandomParticleData;
import com.reiasu.reiparticlesapi.utils.RelativeLocation;
import com.reiasu.reiparticlesapi.utils.interpolator.data.InterpolatorDouble;
import com.reiasu.reiparticlesapi.utils.interpolator.data.InterpolatorFloat;
import com.reiasu.reiparticlesapi.utils.interpolator.data.InterpolatorRelativeLocation;
import com.reiasu.reiparticlesapi.utils.interpolator.data.InterpolatorVec3d;
import com.reiasu.reiparticlesapi.utils.interpolator.data.InterpolatorVector3f;
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
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<String, BufferCodec<?>> supposedTypes = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Field>> codecFieldsByClass = new ConcurrentHashMap<>();

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
            BufferCodec<Object> codec = (BufferCodec<Object>) getCodecOrThrow(field.getType());
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
            BufferCodec<Object> codec = (BufferCodec<Object>) getCodecOrThrow(field.getType());
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
    public List<Field> getCodecFields(Class<?> clazz) {
        if (clazz == null) {
            return List.of();
        }
        return codecFieldsByClass.computeIfAbsent(clazz, CodecHelper::scanCodecFields);
    }

    public BufferCodec<?> getCodecOrThrow(Class<?> fieldType) {
        if (fieldType == null) {
            throw new IllegalArgumentException("Codec type must not be null");
        }
        BufferCodec<?> codec = supposedTypes.get(fieldType.getName());
        if (codec == null) {
            throw new IllegalStateException("No codec registered for type: " + fieldType.getName());
        }
        return codec;
    }

    private static List<Field> scanCodecFields(Class<?> clazz) {
        List<Field> result = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(CodecField.class) && !Modifier.isFinal(f.getModifiers())) {
                f.setAccessible(true);
                result.add(f);
            }
        }
        result.sort(Comparator.comparingInt((Field f) -> f.getAnnotation(CodecField.class).index())
                .thenComparing(Field::getName));
        return List.copyOf(result);
    }

    // ────────────────── Static registration of built-in types ──────────────────

    static {
        registerPrimitives();
        registerArrays();
        registerJavaTypes();
        registerMathTypes();
        registerMinecraftTypes();
        registerProjectTypes();
        registerParticleDataTypes();
        registerInterpolatorTypes();
        registerRangeTypes();
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
        INSTANCE.register(net.minecraft.world.item.ItemStack.class, BufferCodec.of(
                FriendlyByteBuf::writeItem,
                FriendlyByteBuf::readItem
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

    private static void registerParticleDataTypes() {
        INSTANCE.register(ControllableParticleData.class, BufferCodec.of(
                (buf, data) -> data.writeToBuf(buf),
                buf -> {
                    ControllableParticleData data = new ControllableParticleData();
                    data.readFromBuf(buf);
                    return data;
                }
        ));
        INSTANCE.register(SimpleRandomParticleData.class, BufferCodec.of(
                (buf, data) -> data.writeToBuf(buf),
                buf -> {
                    SimpleRandomParticleData data = new SimpleRandomParticleData();
                    data.readFromBuf(buf);
                    return data;
                }
        ));
    }

    private static void registerInterpolatorTypes() {
        INSTANCE.register(InterpolatorDouble.class, BufferCodec.of(
                (buf, value) -> {
                    buf.writeDouble(value.getLast());
                    buf.writeDouble(value.getCurrent());
                },
                buf -> {
                    double last = buf.readDouble();
                    double current = buf.readDouble();
                    InterpolatorDouble value = new InterpolatorDouble(current);
                    value.setLast(last);
                    return value;
                }
        ));
        INSTANCE.register(InterpolatorFloat.class, BufferCodec.of(
                (buf, value) -> {
                    buf.writeFloat(value.getLast());
                    buf.writeFloat(value.getCurrent());
                },
                buf -> {
                    float last = buf.readFloat();
                    float current = buf.readFloat();
                    InterpolatorFloat value = new InterpolatorFloat(current);
                    value.setLast(last);
                    return value;
                }
        ));
        INSTANCE.register(InterpolatorRelativeLocation.class, BufferCodec.of(
                (buf, value) -> {
                    RelativeLocation last = value.getLast();
                    RelativeLocation current = value.getCurrent();
                    buf.writeDouble(last.getX());
                    buf.writeDouble(last.getY());
                    buf.writeDouble(last.getZ());
                    buf.writeDouble(current.getX());
                    buf.writeDouble(current.getY());
                    buf.writeDouble(current.getZ());
                },
                buf -> {
                    RelativeLocation last = new RelativeLocation(buf.readDouble(), buf.readDouble(), buf.readDouble());
                    RelativeLocation current = new RelativeLocation(buf.readDouble(), buf.readDouble(), buf.readDouble());
                    InterpolatorRelativeLocation value = new InterpolatorRelativeLocation(current);
                    value.setLast(last);
                    return value;
                }
        ));
        INSTANCE.register(InterpolatorVec3d.class, BufferCodec.of(
                (buf, value) -> {
                    Vec3 last = value.getLast();
                    Vec3 current = value.getCurrent();
                    buf.writeDouble(last.x());
                    buf.writeDouble(last.y());
                    buf.writeDouble(last.z());
                    buf.writeDouble(current.x());
                    buf.writeDouble(current.y());
                    buf.writeDouble(current.z());
                },
                buf -> {
                    Vec3 last = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                    Vec3 current = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
                    InterpolatorVec3d value = new InterpolatorVec3d(current);
                    value.setLast(last);
                    return value;
                }
        ));
        INSTANCE.register(InterpolatorVector3f.class, BufferCodec.of(
                (buf, value) -> {
                    Vector3f last = value.getLast();
                    Vector3f current = value.getCurrent();
                    buf.writeFloat(last.x());
                    buf.writeFloat(last.y());
                    buf.writeFloat(last.z());
                    buf.writeFloat(current.x());
                    buf.writeFloat(current.y());
                    buf.writeFloat(current.z());
                },
                buf -> {
                    Vector3f last = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
                    Vector3f current = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
                    InterpolatorVector3f value = new InterpolatorVector3f(current);
                    value.setLast(last);
                    return value;
                }
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
